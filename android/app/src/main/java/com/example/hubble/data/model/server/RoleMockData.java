package com.example.hubble.data.model.server;

import android.content.Context;
import android.graphics.Color;

import com.example.hubble.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides static mock data for role management UI until backend is ready.
 */
public class RoleMockData {

    private RoleMockData() {}

    /** Returns mock custom roles for a server. */
    public static List<ServerRoleItem> getMockRoles() {
        List<ServerRoleItem> roles = new ArrayList<>();
        roles.add(new ServerRoleItem("role_1", "vai trò mới", Color.parseColor("#99AAB5"), 0));
        return roles;
    }

    /** Returns mock members for role assignment. */
    public static List<ServerMemberItem> getMockMembers() {
        List<ServerMemberItem> members = new ArrayList<>();
        members.add(new ServerMemberItem(
                "user_1", "danzgne", "Danzg", null,
                Color.parseColor("#5865F2"), null, "online", false));
        return members;
    }

    /** Builds the @everyone default permissions matching Discord screenshots. */
    public static List<RolePermissionSection> getEveryonePermissions(Context ctx) {
        List<RolePermissionSection> sections = new ArrayList<>();

        // General server permissions
        List<RolePermissionItem> general = new ArrayList<>();
        general.add(new RolePermissionItem("view_channels",
                ctx.getString(R.string.role_perm_view_channels),
                ctx.getString(R.string.role_perm_view_channels_desc), true));
        general.add(new RolePermissionItem("manage_channels",
                ctx.getString(R.string.role_perm_manage_channels),
                ctx.getString(R.string.role_perm_manage_channels_desc), false));
        general.add(new RolePermissionItem("manage_roles",
                ctx.getString(R.string.role_perm_manage_roles),
                ctx.getString(R.string.role_perm_manage_roles_desc), false));
        general.add(new RolePermissionItem("create_expressions",
                ctx.getString(R.string.role_perm_create_expressions),
                ctx.getString(R.string.role_perm_create_expressions_desc), true));
        general.add(new RolePermissionItem("manage_expressions",
                ctx.getString(R.string.role_perm_manage_expressions),
                ctx.getString(R.string.role_perm_manage_expressions_desc), false));
        general.add(new RolePermissionItem("view_audit_log",
                ctx.getString(R.string.role_perm_view_audit_log),
                ctx.getString(R.string.role_perm_view_audit_log_desc), false));
        general.add(new RolePermissionItem("manage_webhooks",
                ctx.getString(R.string.role_perm_manage_webhooks),
                ctx.getString(R.string.role_perm_manage_webhooks_desc), true));
        sections.add(new RolePermissionSection(
                ctx.getString(R.string.role_perm_section_general), general));

        // Member permissions
        List<RolePermissionItem> member = new ArrayList<>();
        member.add(new RolePermissionItem("create_invite",
                ctx.getString(R.string.role_perm_create_invite),
                ctx.getString(R.string.role_perm_create_invite_desc), true));
        member.add(new RolePermissionItem("change_nickname",
                ctx.getString(R.string.role_perm_change_nickname),
                ctx.getString(R.string.role_perm_change_nickname_desc), true));
        member.add(new RolePermissionItem("manage_nicknames",
                ctx.getString(R.string.role_perm_manage_nicknames),
                ctx.getString(R.string.role_perm_manage_nicknames_desc), false));
        member.add(new RolePermissionItem("kick_members",
                ctx.getString(R.string.role_perm_kick_members),
                ctx.getString(R.string.role_perm_kick_members_desc), false));
        member.add(new RolePermissionItem("ban_members",
                ctx.getString(R.string.role_perm_ban_members),
                ctx.getString(R.string.role_perm_ban_members_desc), false));
        member.add(new RolePermissionItem("timeout_members",
                ctx.getString(R.string.role_perm_timeout_members),
                ctx.getString(R.string.role_perm_timeout_members_desc), false));
        sections.add(new RolePermissionSection(
                ctx.getString(R.string.role_perm_section_member), member));

        // Text channel permissions
        List<RolePermissionItem> text = new ArrayList<>();
        text.add(new RolePermissionItem("send_messages",
                ctx.getString(R.string.role_perm_send_messages),
                ctx.getString(R.string.role_perm_send_messages_desc), true));
        text.add(new RolePermissionItem("send_messages_threads",
                ctx.getString(R.string.role_perm_send_messages_threads),
                ctx.getString(R.string.role_perm_send_messages_threads_desc), true));
        text.add(new RolePermissionItem("create_public_threads",
                ctx.getString(R.string.role_perm_create_public_threads),
                ctx.getString(R.string.role_perm_create_public_threads_desc), true));
        text.add(new RolePermissionItem("create_private_threads",
                ctx.getString(R.string.role_perm_create_private_threads),
                ctx.getString(R.string.role_perm_create_private_threads_desc), true));
        text.add(new RolePermissionItem("embed_links",
                ctx.getString(R.string.role_perm_embed_links),
                ctx.getString(R.string.role_perm_embed_links_desc), true));
        text.add(new RolePermissionItem("attach_files",
                ctx.getString(R.string.role_perm_attach_files),
                ctx.getString(R.string.role_perm_attach_files_desc), true));
        text.add(new RolePermissionItem("add_reactions",
                ctx.getString(R.string.role_perm_add_reactions),
                ctx.getString(R.string.role_perm_add_reactions_desc), true));
        sections.add(new RolePermissionSection(
                ctx.getString(R.string.role_perm_section_text), text));

        return sections;
    }
}
