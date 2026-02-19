package com.remoteupdatemanager.api.helpers;

import static com.remoteupdatemanager.constants.PraxConstants.Connectivity.NO_WIFI_PERMISSION;
import static com.remoteupdatemanager.constants.PraxConstants.Connectivity.WIFI_NOT_CONNECTED;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.os.Build;

import androidx.core.app.ActivityCompat;

public class PraxWifiManager {
    /**
     * Returns {@code NO_WIFI_PERMISSION} if user has not granted WiFi permissions.
     * If permissions are granted and there is no connected network, this returns {@code WIFI_NOT_CONNECTED}.
     * If permissions are granted and the device is connected to a network, this returns its name (ssid).
     */
    public static String getConnectedNetworkName(Context context) {
        if (!requiredPermissionsGranted(context)) {
            return NO_WIFI_PERMISSION;
        }

        android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                String ssid = wifiInfo.getSSID();
                // Remove quotes from SSID if present
                if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
                if (!ssid.equals("<unknown ssid>")) { // happens sometimes, better to leave it here
                    return ssid;
                }
            }
        }

        return WIFI_NOT_CONNECTED;
    }

    public static boolean isNetworkPraxtourLAN(Context context) {
        return "PraxAP".equals(PraxWifiManager.getConnectedNetworkName(context));
    }

    private static boolean requiredPermissionsGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) ==
                    PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED;
        }
    }
}
