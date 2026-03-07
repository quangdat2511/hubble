package com.example.hubble.data.model;

public class ServerItem {
    private final String id;
    private final String name;
    private final String iconUrl; // null means use initials
    private final int backgroundColor;

    public ServerItem(String id, String name, String iconUrl, int backgroundColor) {
        this.id = id;
        this.name = name;
        this.iconUrl = iconUrl;
        this.backgroundColor = backgroundColor;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getIconUrl() { return iconUrl; }
    public int getBackgroundColor() { return backgroundColor; }

    /** Returns the initials to display when there is no icon image. */
    public String getInitials() {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return String.valueOf(parts[0].charAt(0)).toUpperCase()
                    + String.valueOf(parts[1].charAt(0)).toUpperCase();
        }
        return String.valueOf(name.charAt(0)).toUpperCase();
    }
}
