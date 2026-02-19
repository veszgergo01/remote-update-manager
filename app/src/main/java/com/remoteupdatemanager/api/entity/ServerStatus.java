package com.remoteupdatemanager.api.entity;

public class ServerStatus {
    private boolean streamserver;
    private String version;
    private String message;

    public boolean getStreamserver() {
        return streamserver;
    }

    public void setStreamserver(boolean streamserver) {
        this.streamserver = streamserver;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
