package com.hubble.enums;

import java.util.List;

public enum Permission {
    VIEW_CHANNELS    (1L),
    MANAGE_CHANNELS  (1L << 1),
    MANAGE_ROLES     (1L << 2),
    MANAGE_SERVER    (1L << 3),
    INVITE_MEMBERS   (1L << 4),
    KICK_MEMBERS     (1L << 5),
    SEND_MESSAGES    (1L << 6),
    ATTACH_FILES     (1L << 7),
    HIDE_FROM_SEARCH (1L << 8);

    public final long bit;

    Permission(long bit) {
        this.bit = bit;
    }

    public static long buildBitmask(List<Permission> permissions) {
        return permissions.stream().mapToLong(p -> p.bit).reduce(0L, (a, b) -> a | b);
    }

    public static boolean hasPermission(long bitmask, Permission permission) {
        return (bitmask & permission.bit) != 0L;
    }

    public static final long ALL = (1L << values().length) - 1;
}
