package com.example.hubble.data.model.emoji;

import java.util.List;

public class EmojiCategory {
    public final String name;
    public final String icon;
    public final List<String> emojis;

    public EmojiCategory(String name, String icon, List<String> emojis) {
        this.name = name;
        this.icon = icon;
        this.emojis = emojis;
    }
}
