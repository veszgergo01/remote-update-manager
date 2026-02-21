package com.remoteupdatemanager.constants;

public class PraxConstants {
    public static final String EXTRA_ACCOUNT_TOKEN = "com.videostreamtest.EXTRA_ACCOUNT_TOKEN";
    /**
     * Only Praxtour Launcher is supposed to send an intent extra with this name, set to true.
     */
    public static final String EXTRA_FROM_LAUNCHER = "com.videostreamtest.EXTRA_FROM_LAUNCHER";

    public static class ApkUpdate {
        public static final String EVENT_INSTALL_COMPLETE = "com.videostreamtest.EVENT_INSTALL_COMPLETE";
        public static final String DOWNLOADED_APK_FILENAME = "downloaded-apk.apk";
    }

    public static class Api {
        /** The URL for the production REST API. */
        public static final String PRAXCLOUD_API_URL_PROD = "https://api.praxcloud.eu";
        /**
         * The URL for the test REST API.
         * <p>
         *     IMPORTANT: {@code localhost} would point to the device actually running the app.
         *     Here are the correct IPs for development/testing:
         *     <ul>
         *      <li> Running a VM on your PC/laptop: {@code 10.0.2.2}. </li>
         *      <li> Physical device: find the IPv4 address of your PC/laptop: {@code 192.168.x.x}. </li>
         *     </ul>
         *     <p> You can set the {@code port} to whichever you exposed for your test/dev backend. </p>
         * </p>
         */
        public static final String PRAXCLOUD_API_URL_TEST = "http://10.0.2.2:8081";

        public static final String PRAXCLOUD_API_URL = PRAXCLOUD_API_URL_TEST;
        public static final String PRAXCLOUD_MEDIA_URL = "https://media.praxcloud.eu";
    }

    public static class Connectivity {
        public static final String NO_WIFI_PERMISSION = "com.videostreamtest.NO_WIFI_PERMISSION";
        public static final String WIFI_NOT_CONNECTED = "com.videostreamtest.WIFI_NOT_CONNECTED";
    }
}
