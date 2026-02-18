package com.remoteupdatemanager.api.entity;

/**
 * Analogous to the identically named class in the REST API.
 * Objects of this class contain necessary information that do not require authentication to access.
 */
public class ApkPublicDescription {
    private Integer id;

    private String version;

    private String packageName;

    private String appName;

    public ApkPublicDescription(Integer id, String version, String packageName, String appName) {
        this.id = id;
        this.version = version;
        this.packageName = packageName;
        this.appName = appName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
}
