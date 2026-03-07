package com.example.hubble.data.model;

public abstract class ChannelListItem {

    public static final int TYPE_CATEGORY = 0;
    public static final int TYPE_CHANNEL  = 1;

    public abstract int getType();

    // ── Category ──────────────────────────────────────────────────────────

    public static class Category extends ChannelListItem {
        private final String id;
        private final String name;
        private boolean collapsed;

        public Category(String id, String name) {
            this.id = id;
            this.name = name;
            this.collapsed = false;
        }

        @Override public int getType() { return TYPE_CATEGORY; }

        public String getId()   { return id; }
        public String getName() { return name; }
        public boolean isCollapsed() { return collapsed; }
        public void setCollapsed(boolean collapsed) { this.collapsed = collapsed; }
        public void toggleCollapsed() { this.collapsed = !this.collapsed; }
    }

    // ── Channel ───────────────────────────────────────────────────────────

    public static class Channel extends ChannelListItem {
        public enum ChannelType { TEXT, VOICE }

        private final String id;
        private final String name;
        private final ChannelType channelType;
        private final String categoryId;
        private int unreadCount;

        public Channel(String id, String name, ChannelType channelType, String categoryId) {
            this.id = id;
            this.name = name;
            this.channelType = channelType;
            this.categoryId = categoryId;
            this.unreadCount = 0;
        }

        @Override public int getType() { return TYPE_CHANNEL; }

        public String getId()             { return id; }
        public String getName()           { return name; }
        public ChannelType getChannelType() { return channelType; }
        public String getCategoryId()     { return categoryId; }
        public int getUnreadCount()       { return unreadCount; }
        public void setUnreadCount(int n) { this.unreadCount = n; }
    }
}
