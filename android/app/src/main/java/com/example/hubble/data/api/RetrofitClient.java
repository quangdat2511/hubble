package com.example.hubble.data.api;

import android.content.Context;
import com.example.hubble.BuildConfig;
import com.example.hubble.utils.TokenManager;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    public static final String BASE_URL = BuildConfig.BASE_URL;
    private static Retrofit retrofit = null;

    public static String getBaseUrl() {
        return BASE_URL;
    }

    private static Retrofit getRetrofit(Context context) {
        if (retrofit == null) {
            TokenManager tokenManager = new TokenManager(context.getApplicationContext());

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
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