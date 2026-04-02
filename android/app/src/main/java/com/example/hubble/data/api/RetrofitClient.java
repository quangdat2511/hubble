package com.example.hubble.data.api;

import android.content.Context;
import android.os.Build;

import com.example.hubble.BuildConfig;
import com.example.hubble.utils.TokenManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Dns;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = BuildConfig.BASE_URL;
    private static final String RAILWAY_HOST = "hubble-production.up.railway.app";
    private static final String[] RAILWAY_FALLBACK_IPS = {
            "151.101.2.15"
    };

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
                    .dns(createDnsWithRailwayFallback())
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .authenticator(new TokenAuthenticator(tokenManager, context))
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(new NullOnEmptyConverterFactory())
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

    public static ServerMemberService getServerMemberService(Context context) {
        return getRetrofit(context).create(ServerMemberService.class);
    }

    public static ServerInviteService getServerInviteService(Context context) {
        return getRetrofit(context).create(ServerInviteService.class);
    }

    public static RoleApiService getRoleApiService(Context context) {
        return getRetrofit(context).create(RoleApiService.class);
    }

    private static Dns createDnsWithRailwayFallback() {
        return hostname -> {
            try {
                return Dns.SYSTEM.lookup(hostname);
            } catch (UnknownHostException originalError) {
                if (!RAILWAY_HOST.equalsIgnoreCase(hostname)) {
                    throw originalError;
                }

                List<InetAddress> fallbackAddresses = new ArrayList<>();
                for (String ip : RAILWAY_FALLBACK_IPS) {
                    try {
                        fallbackAddresses.add(InetAddress.getByName(ip));
                    } catch (UnknownHostException ignored) {
                        // Skip malformed fallback IPs and try the remaining entries.
                    }
                }

                if (fallbackAddresses.isEmpty()) {
                    throw originalError;
                }
                return fallbackAddresses;
            }
        };
    }
}