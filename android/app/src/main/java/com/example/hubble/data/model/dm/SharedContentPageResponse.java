package com.example.hubble.data.model.dm;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class SharedContentPageResponse {

    @SerializedName("type")
    private String type;

    @SerializedName("page")
    private int page;

    @SerializedName("size")
    private int size;

    @SerializedName("hasMore")
    private boolean hasMore;

    @SerializedName("items")
    private List<SharedContentItemResponse> items;

    public String getType() {
        return type;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public boolean hasMore() {
        return hasMore;
    }

    public List<SharedContentItemResponse> getItems() {
        return items != null ? items : new ArrayList<>();
    }
}
