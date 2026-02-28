package com.remoteupdatemanager.api.entity;

public class ApkDescription {
    private Integer id;

    private String apkFilepath;

    private String version;

    private String packageName;

    private String appName;

    private long fileSizeBytes;

    private String appRemoteFolder;

    private String appImageFilepath;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getApkFilepath() {
        return apkFilepath;
    }

    public void setApkFilepath(String apkFilepath) {
        this.apkFilepath = apkFilepath;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String apkVersion) {
        this.version = apkVersion;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String apkPackageName) {
        this.packageName = apkPackageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(int fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getAppRemoteFolder() {
        return appRemoteFolder;
    }

    public void setAppRemoteFolder(String appRemoteFolder) {
        this.appRemoteFolder = appRemoteFolder;
    }

    public String getAppImageFilepath() {
        return appImageFilepath;
    }

    public void setAppImageFilepath(String appImageFilepath) {
        this.appImageFilepath = appImageFilepath;
    }
}
