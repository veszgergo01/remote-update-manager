package com.remoteupdatemanager.startonboot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.remoteupdatemanager.R;
import com.remoteupdatemanager.ui.LoginActivity;

public class BootStartupService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, "boot_channel")
                .setContentTitle("Starting " + getString(R.string.app_name))
                .setContentText("Preparing app after reboot...")
                .setSmallIcon(R.drawable.praxtour_logo)
                .build();

        startForeground(1, notification);

        // Delay to give start-up time for system
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent splash = new Intent(this, LoginActivity.class);
            splash.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(splash);
            stopSelf();
        }, 10000);

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "boot_channel",
                    "Boot Startup",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
