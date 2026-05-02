package com.example.hubble.data.api;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import com.example.hubble.utils.TokenManager;

import java.nio.charset.StandardCharsets;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String RAILWAY_HOST = "hubble-production.up.railway.app";
    private static final String[] RAILWAY_FALLBACK_IPS = {
            "151.101.2.15"
    };

    private static Retrofit retrofit = null;
    private static String retrofitBaseUrl = null;

    public static String getBaseUrl() {
        return NetworkConfig.getApiBaseUrl();
    }

    private static Retrofit getRetrofit(Context context) {
        String baseUrl = NetworkConfig.getApiBaseUrl();
        if (retrofit == null || !baseUrl.equals(retrofitBaseUrl)) {
            TokenManager tokenManager = new TokenManager(context.getApplicationContext());

            Interceptor userAgentInterceptor = chain -> {
                Request original = chain.request();
                String deviceName = Build.MANUFACTURER + " " + Build.MODEL; // VD: samsung SM-G998B
                String deviceFingerprint = createDeviceFingerprint(context);
                Request.Builder requestBuilder = original.newBuilder()
                        .header("User-Agent", deviceName)
                        .header("X-Device-Name", deviceName)
                        .header("X-Device-Fingerprint", deviceFingerprint);

                if (isNgrokHost(original.url())) {
                    requestBuilder.header("ngrok-skip-browser-warning", "true");
                }

                return chain.proceed(requestBuilder.build());
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
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(new NullOnEmptyConverterFactory())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            retrofitBaseUrl = baseUrl;
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

    private static String createDeviceFingerprint(Context context) {
        String androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
        String rawValue = (androidId != null ? androidId : "unknown")
                + "|" + Build.MANUFACTURER
                + "|" + Build.MODEL
                + "|" + Build.DEVICE;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawValue.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return rawValue;
        }
    }

    private static boolean isNgrokHost(HttpUrl url) {
        if (url == null || url.host() == null) {
            return false;
        }

        String host = url.host().toLowerCase();
        return host.contains("ngrok");
    }
}
