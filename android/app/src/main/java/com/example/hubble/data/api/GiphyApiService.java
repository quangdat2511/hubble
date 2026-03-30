package com.example.hubble.data.api;

import com.example.hubble.data.model.gif.GiphyResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GiphyApiService {

    // ── GIF endpoints ──────────────────────────────────────────────────────

    @GET("v1/gifs/trending")
    Call<GiphyResponse> getTrendingGifs(
            @Query("api_key") String apiKey,
            @Query("limit") int limit,
            @Query("rating") String rating
    );

    @GET("v1/gifs/search")
    Call<GiphyResponse> searchGifs(
            @Query("api_key") String apiKey,
            @Query("q") String query,
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("rating") String rating
    );

    // ── Sticker endpoints ──────────────────────────────────────────────────

    @GET("v1/stickers/trending")
    Call<GiphyResponse> getTrendingStickers(
            @Query("api_key") String apiKey,
            @Query("limit") int limit,
            @Query("rating") String rating
    );

    @GET("v1/stickers/search")
    Call<GiphyResponse> searchStickers(
            @Query("api_key") String apiKey,
            @Query("q") String query,
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("rating") String rating
    );
}
