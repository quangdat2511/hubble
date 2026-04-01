package com.hubble.enums;

import java.util.List;

public enum Permission {
    VIEW_CHANNELS            (1L),
    MANAGE_CHANNELS          (1L << 1),
    MANAGE_ROLES             (1L << 2),
    MANAGE_EXPRESSIONS       (1L << 3),
    VIEW_AUDIT_LOG           (1L << 4),
    MANAGE_SERVER            (1L << 5),
    CREATE_INVITE            (1L << 6),
    CHANGE_NICKNAME          (1L << 7),
    MANAGE_NICKNAMES         (1L << 8),
    KICK_MEMBERS             (1L << 9),
    BAN_MEMBERS              (1L << 10),
    TIMEOUT_MEMBERS          (1L << 11),
    SEND_MESSAGES            (1L << 12),
    SEND_MESSAGES_IN_THREADS (1L << 13),
    CREATE_PUBLIC_THREADS    (1L << 14),
    CREATE_PRIVATE_THREADS   (1L << 15),
    EMBED_LINKS              (1L << 16),
    ATTACH_FILES             (1L << 17),
    ADD_REACTIONS            (1L << 18),
    USE_EXTERNAL_EMOJIS      (1L << 19);

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
