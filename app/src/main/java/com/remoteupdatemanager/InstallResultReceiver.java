package com.remoteupdatemanager;

import static com.remoteupdatemanager.UpdateActivity.PRAXTOUR_APP_PACKAGE_NAME;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;

public class InstallResultReceiver extends BroadcastReceiver {
    private final static String TAG = InstallResultReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);

        switch (status) {
            case PackageInstaller.STATUS_SUCCESS:
                launchPraxtourMainApp(context);
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

    private void launchPraxtourMainApp(Context context) {
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(PRAXTOUR_APP_PACKAGE_NAME);
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(launch);
        }
    }
}
