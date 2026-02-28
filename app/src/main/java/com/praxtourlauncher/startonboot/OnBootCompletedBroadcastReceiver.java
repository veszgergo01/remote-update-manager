package com.praxtourlauncher.startonboot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.praxtourlauncher.R;
import com.praxtourlauncher.ui.LoginActivity;

public class OnBootCompletedBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();

        if(action != null) {
            if ((Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_REBOOT.equals(action))) {
                /*
                 * To enable the start-on-boot you'll need to adjust something in the settings:
                 * Settings > Apps > Special App Access > Display over other apps > Praxtour (set switch on)
                 */

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    final Intent splashActivity = new Intent(context, LoginActivity.class);
                    splashActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(splashActivity);
                } else {
                    Intent serviceIntent = new Intent(context, BootStartupService.class);
                    ContextCompat.startForegroundService(context, serviceIntent);
                }

                if (Settings.canDrawOverlays(context)) {
                    Log.e(this.getClass().getSimpleName(), "Value SplashActivity 1: true");
                } else {
                    Log.e(this.getClass().getSimpleName(), "Value SplashActivity 1: " + Settings.canDrawOverlays(context));
                    Log.e(this.getClass().getSimpleName(), "Value SplashActivity 2: " + Build.VERSION.SDK_INT);
                    Log.e(this.getClass().getSimpleName(), "Value SplashActivity 2: " + context.getString(R.string.app_name));
                }
            }
        }
    }
}
