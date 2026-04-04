package com.example.hubble.data.model.dm;

import java.util.ArrayList;
import java.util.List;

public class ReactionDto {
    private String emoji;
    private int count;
    private List<String> userIds;

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public List<String> getUserIds() {
        return userIds != null ? userIds : new ArrayList<>();
    }
    public void setUserIds(List<String> userIds) { this.userIds = userIds; }
}
