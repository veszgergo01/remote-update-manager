package com.remoteupdatemanager.download;

import static com.remoteupdatemanager.constants.PraxConstants.ApkUpdate.DOWNLOADED_APK_FILENAME;

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

    public static File download(Context context, URL updateFileUrl, long expectedSizeBytes, ProgressCallback progressCallback) {
        File cacheFolder = context.getCacheDir();
        clearUpdateFolder(cacheFolder);

        ReadableByteChannel readableByteChannel = null;
        InputStream inputStream;
        try {
            inputStream = updateFileUrl.openStream();
            readableByteChannel = new CallbackByteChannel(Channels.newChannel(inputStream), expectedSizeBytes, progressCallback);
        } catch (IOException e) {
            Log.e(TAG, "Error while accessing URL for APK update " + e);
        }
        if (readableByteChannel == null) return null;

        File outputFile = new File(context.getCacheDir(), DOWNLOADED_APK_FILENAME);
        Log.d(TAG, "Greg output path: " + outputFile.getAbsolutePath());
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            Log.e(TAG, "Error during download: " + e);
            return null;
        }

        return outputFile;
    }

    private static void clearUpdateFolder(File folder) {
        File[] files = folder.listFiles();
        if (files == null) throw new IllegalArgumentException("Param folder needs to be a directory");
        for (File file : files) {
            file.delete();
        }
    }

    @Override
    public void callback(CallbackByteChannel rbc, double progress) {

    }
}
