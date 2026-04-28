package com.praxtourlauncher.api.helpers;

import static com.praxtourlauncher.constants.PraxConstants.ApkUpdate.PRAXTOUR_APP_PACKAGE_NAME;
import static com.praxtourlauncher.constants.PraxConstants.IntentExtra.EXTRA_ACCOUNT_TOKEN;
import static com.praxtourlauncher.constants.PraxConstants.IntentExtra.EXTRA_FROM_LAUNCHER;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

public class NavHelper {
    public static void launchPraxtourMainApp(@NonNull Activity activity, @NonNull String apikey) {
        if (activity == null || apikey == null)
            throw new IllegalArgumentException("Activity and apikey must not be null");

        try {
            Intent launch = activity.getPackageManager().getLaunchIntentForPackage(PRAXTOUR_APP_PACKAGE_NAME);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                launch.putExtra(EXTRA_FROM_LAUNCHER, true);
                launch.putExtra(EXTRA_ACCOUNT_TOKEN, apikey);
                activity.startActivity(launch);
                activity.finishAffinity();
            }
        } catch (NullPointerException ignored) {} // for when we already navigated away
    }
}
