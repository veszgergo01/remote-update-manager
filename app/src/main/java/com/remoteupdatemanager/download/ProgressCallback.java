package com.remoteupdatemanager.download;

public interface ProgressCallback {
    void callback(CallbackByteChannel rbc, double progress);
}
