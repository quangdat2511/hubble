package com.example.hubble.data.model.gif;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GiphyResponse {
    @SerializedName("data")
    public List<GiphyGif> data;

    @SerializedName("meta")
    public GiphyMeta meta;

    public static class GiphyMeta {
        @SerializedName("status")
        public int status;

        @SerializedName("msg")
        public String msg;
    }
}
