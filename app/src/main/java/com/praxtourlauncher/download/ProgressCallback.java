package com.praxtourlauncher.download;

public interface ProgressCallback {
    void callback(CallbackByteChannel rbc, double progress);
}
