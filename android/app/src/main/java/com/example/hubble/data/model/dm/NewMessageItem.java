package com.example.hubble.data.model.dm;

public abstract class NewMessageItem {

    public static final int TYPE_SECTION = 0;
    public static final int TYPE_FRIEND = 1;

    public abstract int getType();

    public static final class Section extends NewMessageItem {
        private final String title;

        public Section(String title) {
            this.title = title;
        }

        @Override
        public int getType() {
            return TYPE_SECTION;
        }

        public String getTitle() {
            return title;
        }
    }

    public static final class Friend extends NewMessageItem {
        private final String id;
        private final String displayName;
        private final String username;
        private final String avatarUrl;
        private final String badge;
        private final boolean online;

        public Friend(String id, String displayName, String username, String avatarUrl, String badge, boolean online) {
            this.id = id;
            this.displayName = displayName;
            this.username = username;
            this.avatarUrl = avatarUrl;
            this.badge = badge;
            this.online = online;
        }

        @Override
        public int getType() {
            return TYPE_FRIEND;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getUsername() {
            return username;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public String getBadge() {
            return badge;
        }

        public boolean isOnline() {
            return online;
        }
    }
}

