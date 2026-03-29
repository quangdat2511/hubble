package com.example.hubble.data.api;

import com.example.hubble.BuildConfig;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GiphyClient {

    // Đọc từ local.properties → BuildConfig (không bao giờ hardcode vào source)
    public static final String API_KEY = BuildConfig.GIPHY_API_KEY;

    private static final String BASE_URL = "https://api.giphy.com/";
    private static final int DEFAULT_LIMIT = 24;
    private static final String RATING = "g";

    private static GiphyApiService instance;

    public static GiphyApiService get() {
        if (instance == null) {
            instance = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(GiphyApiService.class);
        }
        return instance;
    }

    public static int defaultLimit() { return DEFAULT_LIMIT; }
    public static String rating() { return RATING; }
}
