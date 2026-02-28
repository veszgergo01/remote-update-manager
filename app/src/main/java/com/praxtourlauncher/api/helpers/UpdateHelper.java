package com.praxtourlauncher.api.helpers;

public class UpdateHelper {

    public static String extractFileName(String updateFileUrl) {
        String[] splitUrl = updateFileUrl.split("/");
        return splitUrl[splitUrl.length - 1];
    }
}
