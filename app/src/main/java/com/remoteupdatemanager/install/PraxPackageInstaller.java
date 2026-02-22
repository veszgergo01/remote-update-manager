package com.remoteupdatemanager.install;

import static com.remoteupdatemanager.constants.PraxConstants.ApkUpdate.DOWNLOADED_APK_FILENAME;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

import com.remoteupdatemanager.api.helpers.UpdateHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class PraxPackageInstaller {
    private final static String TAG = PraxPackageInstaller.class.getSimpleName();
    public static boolean installApk(Context context, String updateFileRemotePath) throws IOException {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
        );

        int sessionId;
        PackageInstaller.Session session;
        try {
            sessionId = packageInstaller.createSession(params);
            session = packageInstaller.openSession(sessionId);
        } catch (IOException e) {
            // TODO not enough disk space OR .apk file isn't there
            return false;
        }

        String filename = UpdateHelper.extractFileName(updateFileRemotePath);
        File apkFile = new File(context.getCacheDir(), filename);

        try (FileInputStream in = new FileInputStream(apkFile);
             OutputStream out = session.openWrite("praxtour-app.apk", 0, apkFile.length())) {

            byte[] buffer = new byte[64 * 1024];
            int c;
            while ((c = in.read(buffer)) != -1) {
                out.write(buffer, 0, c);
            }
            out.flush();
            session.fsync(out);
        } catch (IOException e) {
            // TODO not enough disk space or other IO error
            try { session.abandon(); } catch (Exception ignored) {}
            return false;
        }

        Intent resultIntent = new Intent(context, InstallResultReceiver.class)
                .setAction("com.praxupdatemanager.ACTION_INSTALL_COMMIT")
                .putExtra("sessionId", sessionId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );

        session.commit(pendingIntent.getIntentSender());
        session.close();
        return true;
    }
}
