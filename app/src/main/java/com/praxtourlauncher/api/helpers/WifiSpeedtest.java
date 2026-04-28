package com.praxtourlauncher.api.helpers;

import static com.praxtourlauncher.constants.PraxConstants.Api.PRAXCLOUD_API_URL;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.praxtourlauncher.api.PraxCloud;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WifiSpeedtest {
    private static final String TAG = WifiSpeedtest.class.getSimpleName();

    public static void getDownloadSpeedMbps(WifiCallback callback) {
        new Thread(() -> {
            try {
                InputStream is = new URL("http://speedtest.tele2.net/10MB.zip\n").openStream();
                byte[] buf = new byte[1024];
                int bytesRead;
                long totalBytesRead = 0;
                long startTime = System.nanoTime();

                while ((bytesRead = is.read(buf)) != -1) {
                    totalBytesRead += bytesRead;
                    if (totalBytesRead >= 200 * 1024) { // stops after 200KB
                        break;
                    }
                }
                is.close();

                long endTime = System.nanoTime();
                long totalTime = endTime - startTime;

                long downloadSpeedMbps = (totalBytesRead * 8L * 1_000_000_000L) / (totalTime * 1024 * 1024);
                Log.d(TAG, "downloadspeed: " + downloadSpeedMbps);

                if (callback != null) {
                    callback.onSuccess(downloadSpeedMbps);
                }
            }
            catch(Exception e){
                e.printStackTrace();
                if (callback != null) {
                    callback.onError(e);
                }
            }
        }).start();
    }

    public static void getPingTo(final String url, WifiCallback callback) {
        new Thread(() -> {
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

                timeoutHandler.removeCallbacksAndMessages(null);

                if (handled.compareAndSet(false, true)) {
                    if (reachable) {
                        long latency = endTime - startTime;
                        callback.onSuccess(latency);
                    } else {
                        callback.onError(new IOException("Could not ping " + url));
                    }
                }
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, e.toString());
                timeoutHandler.removeCallbacks(timeoutRunnable);
                if (handled.compareAndSet(false, true)) {
                    callback.onError(e);
                }
            }
        }).start();
    }
}
