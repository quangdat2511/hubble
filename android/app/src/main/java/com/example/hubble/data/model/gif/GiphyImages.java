package com.example.hubble.data.model.gif;

import com.google.gson.annotations.SerializedName;

public class GiphyImages {
    @SerializedName("original")
    public GiphyImage original;

    @SerializedName("fixed_height")
    public GiphyImage fixedHeight;

    @SerializedName("fixed_width")
    public GiphyImage fixedWidth;

    @SerializedName("preview_gif")
    public GiphyImage previewGif;

    @SerializedName("downsized")
    public GiphyImage downsized;
}
