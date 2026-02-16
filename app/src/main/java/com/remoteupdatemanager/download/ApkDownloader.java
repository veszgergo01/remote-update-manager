package com.remoteupdatemanager.download;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class ApkDownloader implements ProgressCallback {
    private final static String TAG = ApkDownloader.class.getSimpleName();

    private static final String UPDATE_URL = "https://media.praxcloud.eu/app/";

    public static File download(Context context, String updateFilename, ProgressCallback progressCallback) {
        final String source = UPDATE_URL + updateFilename;

        clearUpdateFolder();

        URL inputUrl;
        try {
            inputUrl = new URL(source);
        } catch (MalformedURLException e) {
            Log.e(TAG, "URL error during download: " + e);
            return null;
        }

        ReadableByteChannel readableByteChannel = null;
        InputStream inputStream = null;
        try {
            inputStream = inputUrl.openStream();
            // I have no idea why Long.MAX_VALUE is used... it was here when I found it
            readableByteChannel = new CallbackByteChannel(Channels.newChannel(inputStream), Long.MAX_VALUE, progressCallback);
        } catch (IOException e) {
            Log.e(TAG, "Error while accessing URL for APK update " + e);
        }
        if (readableByteChannel == null) return null;

        File outputFile = new File(context.getCacheDir(), updateFilename);
        Log.d(TAG, "Greg output path: " + outputFile.getAbsolutePath());
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            Log.e(TAG, "Error during download: " + e);
            return null;
        }

        return outputFile;
    }

    private static void clearUpdateFolder() {
        File updateFolder = new File(UPDATE_URL);
        if (updateFolder.isDirectory()) {
            File[] files = updateFolder.listFiles();
            if (files == null) return; // already empty
            for (File file : files) {
                file.delete();
            }
        }
    }

    @Override
    public void callback(CallbackByteChannel rbc, double progress) {
        // TODO for tracking and displaying progress
    }
}
