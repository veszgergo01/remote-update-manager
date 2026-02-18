package com.remoteupdatemanager.install;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static com.remoteupdatemanager.constants.PraxConstants.ApkUpdate.EVENT_INSTALL_COMPLETE;

public class InstallResultReceiver extends BroadcastReceiver {
    private final static String TAG = InstallResultReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);

        switch (status) {
            case PackageInstaller.STATUS_SUCCESS:
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(EVENT_INSTALL_COMPLETE));
                break;
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                Intent confirmIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(confirmIntent);
                }
                break;
            default:
                Log.e(TAG, "Error during installation\n\tstatus: " + status + "\n\tmsg: " + msg);
                break;
        }
    }
}
