package com.praxtourlauncher.api;

import com.praxtourlauncher.api.entity.ApiKey;
import com.praxtourlauncher.api.entity.ApkDescription;
import com.praxtourlauncher.api.entity.LoginUser;
import com.praxtourlauncher.api.entity.Product;
import com.praxtourlauncher.api.entity.ServerStatus;
import com.praxtourlauncher.api.entity.User;

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
    @POST("/api/user/authenticate")
    Call<User> authenticateUser(@Body LoginUser loginUser);

    // Note: URL path a bit questionable here, better fitting would be: /api/user/{id}/...,
    // but with the current solution this is the most effective in terms of nr of API calls
    @POST("/api/user/authenticate-device/{device-uuid}")
    Call<String> authenticateDevice(@Path("device-uuid") String deviceUuid, @Header("api-key") String accountToken);

    //PRODUCTS
    @GET("/api/users/current/subscriptions")
    Call<List<Product>> getActiveProducts(@Header("api-key") String accountToken);
}
