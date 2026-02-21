package com.remoteupdatemanager.api;

import com.remoteupdatemanager.api.entity.ApiKey;
import com.remoteupdatemanager.api.entity.ApkDescription;
import com.remoteupdatemanager.api.entity.LoginUser;
import com.remoteupdatemanager.api.entity.ServerStatus;

import java.net.URL;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface PraxCloud {
    //APP UPDATES
    @GET("api/apk/all-packages")
    Call<List<ApkDescription>> getAllPackagesPublicInfo(@Header("api-key") String accountToken);
    @GET("/api/apk/{package-name}/get-version")
    Call<String> getNewestVersion(@Path("package-name") String packageName);

    @GET("/api/apk/{package-name}/get-url")
    Call<URL> getNewestUpdateUrl(@Path("package-name") String packageName, @Header("api-key") String accountToken);

    //SERVER STATUS CHECK
    @GET("/api/server/status")
    Call<ServerStatus> getServerStatus();

    //AUTHENTICATION
    @POST("/api/login")
    Call<ApiKey> authenticateUser(@Body LoginUser loginUser);
}
