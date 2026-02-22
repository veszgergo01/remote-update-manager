package com.remoteupdatemanager.api.helpers;

import java.net.URL;

public class UpdateHelper {

    public static String extractFileName(String updateFileUrl) {
        String[] splitUrl = updateFileUrl.split("/");
        return splitUrl[splitUrl.length - 1];
    }
}
