package com.remoteupdatemanager.api;

import com.remoteupdatemanager.api.entity.ApkPublicDescription;

import java.net.URL;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface PraxCloud {
    //APP UPDATES
    @GET("api/apk/all-packages")
    Call<List<ApkPublicDescription>> getAllPackagesPublicInfo();
    @GET("/api/apk/{package-name}/get-version")
    Call<String> getNewestVersion(@Path("package-name") String packageName);

    @GET("/api/apk/{package-name}/get-url")
    Call<URL> getNewestUpdateUrl(@Path("package-name") String packageName, @Header("api-key") String accountToken);
}
