package com.example.hubble.data.model.gif;

import com.google.gson.annotations.SerializedName;

public class GiphyImage {
    @SerializedName("url")
    public String url;

    @SerializedName("width")
    public String width;

    @SerializedName("height")
    public String height;

    public int getWidthInt() {
        try { return Integer.parseInt(width); } catch (Exception e) { return 0; }
    }

    public int getHeightInt() {
        try { return Integer.parseInt(height); } catch (Exception e) { return 0; }
    }
}
