package com.praxtourlauncher.api.helpers;

import static com.praxtourlauncher.constants.PraxConstants.Api.PRAXCLOUD_API_URL;
import static com.praxtourlauncher.constants.PraxConstants.Connectivity.DOWNLOAD_CONNECTION_TIMEOUT_MS;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.praxtourlauncher.api.PraxCloud;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WifiSpeedtest {
    private static final String TAG = WifiSpeedtest.class.getSimpleName();
    public static final long DEFAULT_NETWORK_VALUE = 0L;
    public static final long ERROR_NETWORK_VALUE = -1L;

    public static void getDownloadSpeedMbps(WifiCallback callback) {
        HandlerThread handlerThread = new HandlerThread("handlerThread");
        if (!handlerThread.isAlive()) {
            handlerThread.start();
        }
        new Handler(handlerThread.getLooper()).post(() -> {
            AtomicBoolean handled = new AtomicBoolean(false);
            Handler timeoutHandler = new Handler(Looper.getMainLooper());
            Runnable timeoutRunnable = () -> {
                if (handled.compareAndSet(false, true)) {
                    callback.onError(new TimeoutException("Internet is too slow or there is no proper connection to server"));
                }
            };
            try {
                timeoutHandler.postDelayed(timeoutRunnable, DOWNLOAD_CONNECTION_TIMEOUT_MS);
                URL url = new URL("https://media.praxcloud.eu/conn_test/1mb.zip");
                HttpURLConnection connection;

                // Force IPv4, IPv6 is broken on some Android 7 stacks
                InetAddress ipv4 = null;
                for (InetAddress addr : InetAddress.getAllByName(url.getHost())) {
                    if (addr instanceof Inet4Address) {
                        ipv4 = addr;
                        break;
                    }
                }

                if (ipv4 == null) {
                    throw new IOException("No IPv4 found for " + url);
                }

                String ipUrl = "http://" + ipv4.getHostAddress() + url.getFile();
                connection = (HttpURLConnection) new URL(ipUrl).openConnection();
                connection.setRequestProperty("Host", url.getHost());

                InputStream is = connection.getInputStream();
                byte[] buf = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;
                long startTime = System.nanoTime();

                while ((bytesRead = is.read(buf)) != -1 && handled.compareAndSet(false, false)) {
                    totalBytesRead += bytesRead;
                    if (totalBytesRead >= 200 * 1024) { // stops after 200KB
                        break;
                    }
                }
                is.close();

                if (handled.compareAndSet(true, true)) {
                    // this means we are past the timeout and onError has been called
                    return;
                }

                long endTime = System.nanoTime();
                long totalTime = endTime - startTime;

                long downloadSpeedKbps = (totalBytesRead * 8L * 1_000_000_000L) / (totalTime * 1024);

                callback.onSuccess(downloadSpeedKbps);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                if (handled.compareAndSet(false, true)) {
                    callback.onError(e);
                }
            } finally {
                handlerThread.quitSafely();
            }
        });
    }

    public static void getPingTo(final String url, WifiCallback callback) {
        HandlerThread handlerThread = new HandlerThread("handlerThread");
        if (!handlerThread.isAlive()) {
            handlerThread.start();
        }
        new Handler(handlerThread.getLooper()).post(() -> {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(PRAXCLOUD_API_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            PraxCloud praxCloud = retrofit.create(PraxCloud.class);

            final long TIMEOUT_MS = 1500;
            AtomicBoolean handled = new AtomicBoolean(false);
            Handler timeoutHandler = new Handler(Looper.getMainLooper());
            Runnable timeoutRunnable = () -> {
                if (handled.compareAndSet(false, true)) {
                    callback.onError(new IOException("Could not ping " + url));
                }
            };
            timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);

            try {
                long startTime = System.currentTimeMillis();
                boolean reachable = praxCloud.getServerStatus().execute().code() == 200;
                long endTime = System.currentTimeMillis();

                timeoutHandler.removeCallbacks(timeoutRunnable);
                if (!handled.compareAndSet(false, true)) return;

                if (reachable) {
                    long latency = endTime - startTime;
                    callback.onSuccess(latency);
                } else {
                    callback.onError(new IOException("Could not ping " + url));
                }
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, e.toString());
                timeoutHandler.removeCallbacks(timeoutRunnable);
                if (handled.compareAndSet(false, true)) {
                    callback.onError(e);
                }
            }
        });
    }
}
