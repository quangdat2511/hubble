package com.example.hubble.data.api;

import android.content.Context;
import android.os.Build;

import com.example.hubble.BuildConfig;
import com.example.hubble.utils.TokenManager;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = BuildConfig.BASE_URL;
    private static Retrofit retrofit = null;

    public static String getBaseUrl() {
        return BASE_URL;
    }

    private static Retrofit getRetrofit(Context context) {
        if (retrofit == null) {
            TokenManager tokenManager = new TokenManager(context.getApplicationContext());

            Interceptor userAgentInterceptor = chain -> {
                Request original = chain.request();
                String deviceName = Build.MANUFACTURER + " " + Build.MODEL; // VD: samsung SM-G998B
                Request request = original.newBuilder()
                        .header("User-Agent", deviceName)
                        .build();
                return chain.proceed(request);
            };

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(userAgentInterceptor)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .authenticator(new TokenAuthenticator(tokenManager, context))
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService(Context context) {
        return getRetrofit(context).create(ApiService.class);
    }

    public static ServerService getServerService(Context context) {
        return getRetrofit(context).create(ServerService.class);
    }
}