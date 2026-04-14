package com.example.hubble.view.server;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hubble.R;
import com.example.hubble.adapter.server.RolePermissionAdapter;
import com.example.hubble.data.model.server.PermissionResponse;
import com.example.hubble.data.model.server.RolePermissionItem;
import com.example.hubble.data.model.server.RolePermissionSection;
import com.example.hubble.data.repository.RoleRepository;
import com.example.hubble.databinding.FragmentRolePermissionsBinding;
import com.example.hubble.viewmodel.RolesViewModel;
import com.example.hubble.viewmodel.RolesViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Role permissions screen (#3,4,5). Shows permission toggles for @everyone or a custom role.
 */
public class RolePermissionsFragment extends Fragment {

    private static final ConcurrentHashMap<String, List<PermissionResponse>> permissionsCache = new ConcurrentHashMap<>();

    public static void prefetch(Context context, String serverId, String roleId) {
        if (permissionsCache.containsKey(roleId)) return;
        RoleRepository repo = new RoleRepository(context);
        repo.getPermissions(serverId, roleId, result -> {
            if (result.isSuccess() && result.getData() != null) {
                permissionsCache.put(roleId, result.getData());
            }
        });
    }

    private FragmentRolePermissionsBinding binding;
    private RolesViewModel viewModel;

    private String roleId;
    private String roleName;
    private String serverId;
    private List<RolePermissionSection> currentSections;

    // Map backend permission names to UI-friendly names
    private static final Map<String, String> PERM_LABELS = new HashMap<>();
    private static final Map<String, String> PERM_DESCS = new HashMap<>();

    // Section groupings
    private static final List<String> GENERAL_PERMS = Arrays.asList(
            "VIEW_CHANNELS", "MANAGE_CHANNELS", "MANAGE_ROLES",
            "MANAGE_EXPRESSIONS", "VIEW_AUDIT_LOG", "MANAGE_SERVER");
    private static final List<String> MEMBER_PERMS = Arrays.asList(
            "CREATE_INVITE", "CHANGE_NICKNAME", "MANAGE_NICKNAMES",
            "KICK_MEMBERS", "BAN_MEMBERS", "TIMEOUT_MEMBERS");
    private static final List<String> TEXT_PERMS = Arrays.asList(
            "SEND_MESSAGES", "SEND_MESSAGES_IN_THREADS", "CREATE_PUBLIC_THREADS",
            "CREATE_PRIVATE_THREADS", "EMBED_LINKS", "ATTACH_FILES",
            "ADD_REACTIONS", "USE_EXTERNAL_EMOJIS");

    public static RolePermissionsFragment newInstance(String roleId, String roleName, String serverId) {
        RolePermissionsFragment fragment = new RolePermissionsFragment();
        Bundle args = new Bundle();
        args.putString("role_id", roleId);
        args.putString("role_name", roleName);
        args.putString("server_id", serverId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRolePermissionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            roleId = getArguments().getString("role_id");
            roleName = getArguments().getString("role_name");
            serverId = getArguments().getString("server_id");
        }

        initPermLabels();

        viewModel = new ViewModelProvider(requireActivity(),
                new RolesViewModelFactory(new RoleRepository(requireContext())))
                .get(RolesViewModel.class);

        binding.toolbar.setTitle(roleName != null ? roleName : "@everyone");
        binding.toolbar.setSubtitle(getString(R.string.server_settings_roles));
        binding.toolbar.setNavigationOnClickListener(v -> {
            savePermissions();
            requireActivity().onBackPressed();
        });

        // Clear stale data from previous role
        viewModel.resetPermissions();

        // Serve from cache first
        List<PermissionResponse> cached = permissionsCache.get(roleId);
        if (cached != null) {
            displayPermissions(cached);
        }

        viewModel.permissions.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.isSuccess() && result.getData() != null) {
                permissionsCache.put(roleId, result.getData());
                displayPermissions(result.getData());
            } else if (result.isError() && cached == null) {
                Snackbar.make(view, result.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });

        viewModel.loadPermissions(serverId, roleId);
    }

    private void displayPermissions(List<PermissionResponse> perms) {
        Map<String, PermissionResponse> permMap = new HashMap<>();
        for (PermissionResponse p : perms) permMap.put(p.getName(), p);

        currentSections = new ArrayList<>();
        currentSections.add(buildSection(getString(R.string.role_perm_section_general), GENERAL_PERMS, permMap));
        currentSections.add(buildSection(getString(R.string.role_perm_section_member), MEMBER_PERMS, permMap));
        currentSections.add(buildSection(getString(R.string.role_perm_section_text), TEXT_PERMS, permMap));

        RolePermissionAdapter adapter = new RolePermissionAdapter(currentSections);
        binding.rvPermissions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPermissions.setAdapter(adapter);
    }

    private RolePermissionSection buildSection(String title, List<String> keys,
                                                Map<String, PermissionResponse> permMap) {
        List<RolePermissionItem> items = new ArrayList<>();
        for (String key : keys) {
            PermissionResponse pr = permMap.get(key);
            boolean granted = pr != null && Boolean.TRUE.equals(pr.getGranted());
            String label = PERM_LABELS.getOrDefault(key, key);
            String desc = PERM_DESCS.getOrDefault(key, "");
            items.add(new RolePermissionItem(key, label, desc, granted));
        }
        return new RolePermissionSection(title, items);
    }

    private void savePermissions() {
        if (currentSections == null) return;
        List<String> granted = new ArrayList<>();
        Set<String> grantedSet = new HashSet<>();
        for (RolePermissionSection section : currentSections) {
            for (RolePermissionItem item : section.getPermissions()) {
                if (item.isEnabled()) {
                    granted.add(item.getKey());
                    grantedSet.add(item.getKey());
                }
            }
        }

        // Update cache with current toggle states so next entry shows correct values
        List<PermissionResponse> cached = permissionsCache.get(roleId);
        if (cached != null) {
            for (PermissionResponse p : cached) {
                p.setGranted(grantedSet.contains(p.getName()));
            }
        }
        viewModel.updatePermissions(serverId, roleId, granted);
    }

    private void initPermLabels() {
        if (!PERM_LABELS.isEmpty()) return;
        PERM_LABELS.put("VIEW_CHANNELS", getString(R.string.role_perm_view_channels));
        PERM_LABELS.put("MANAGE_CHANNELS", getString(R.string.role_perm_manage_channels));
        PERM_LABELS.put("MANAGE_ROLES", getString(R.string.role_perm_manage_roles));
        PERM_LABELS.put("MANAGE_EXPRESSIONS", getString(R.string.role_perm_manage_expressions));
        PERM_LABELS.put("VIEW_AUDIT_LOG", getString(R.string.role_perm_view_audit_log));
        PERM_LABELS.put("MANAGE_SERVER", getString(R.string.role_perm_manage_server));
        PERM_LABELS.put("CREATE_INVITE", getString(R.string.role_perm_create_invite));
        PERM_LABELS.put("CHANGE_NICKNAME", getString(R.string.role_perm_change_nickname));
        PERM_LABELS.put("MANAGE_NICKNAMES", getString(R.string.role_perm_manage_nicknames));
        PERM_LABELS.put("KICK_MEMBERS", getString(R.string.role_perm_kick_members));
        PERM_LABELS.put("BAN_MEMBERS", getString(R.string.role_perm_ban_members));
        PERM_LABELS.put("TIMEOUT_MEMBERS", getString(R.string.role_perm_timeout_members));
        PERM_LABELS.put("SEND_MESSAGES", getString(R.string.role_perm_send_messages));
        PERM_LABELS.put("SEND_MESSAGES_IN_THREADS", getString(R.string.role_perm_send_messages_threads));
        PERM_LABELS.put("CREATE_PUBLIC_THREADS", getString(R.string.role_perm_create_public_threads));
        PERM_LABELS.put("CREATE_PRIVATE_THREADS", getString(R.string.role_perm_create_private_threads));
        PERM_LABELS.put("EMBED_LINKS", getString(R.string.role_perm_embed_links));
        PERM_LABELS.put("ATTACH_FILES", getString(R.string.role_perm_attach_files));
        PERM_LABELS.put("ADD_REACTIONS", getString(R.string.role_perm_add_reactions));
        PERM_LABELS.put("USE_EXTERNAL_EMOJIS", getString(R.string.role_perm_use_external_emojis));

        PERM_DESCS.put("VIEW_CHANNELS", getString(R.string.role_perm_view_channels_desc));
        PERM_DESCS.put("MANAGE_CHANNELS", getString(R.string.role_perm_manage_channels_desc));
        PERM_DESCS.put("MANAGE_ROLES", getString(R.string.role_perm_manage_roles_desc));
        PERM_DESCS.put("MANAGE_EXPRESSIONS", getString(R.string.role_perm_manage_expressions_desc));
        PERM_DESCS.put("VIEW_AUDIT_LOG", getString(R.string.role_perm_view_audit_log_desc));
        PERM_DESCS.put("MANAGE_SERVER", getString(R.string.role_perm_manage_server_desc));
        PERM_DESCS.put("CREATE_INVITE", getString(R.string.role_perm_create_invite_desc));
        PERM_DESCS.put("CHANGE_NICKNAME", getString(R.string.role_perm_change_nickname_desc));
        PERM_DESCS.put("MANAGE_NICKNAMES", getString(R.string.role_perm_manage_nicknames_desc));
        PERM_DESCS.put("KICK_MEMBERS", getString(R.string.role_perm_kick_members_desc));
        PERM_DESCS.put("BAN_MEMBERS", getString(R.string.role_perm_ban_members_desc));
        PERM_DESCS.put("TIMEOUT_MEMBERS", getString(R.string.role_perm_timeout_members_desc));
        PERM_DESCS.put("SEND_MESSAGES", getString(R.string.role_perm_send_messages_desc));
        PERM_DESCS.put("SEND_MESSAGES_IN_THREADS", getString(R.string.role_perm_send_messages_threads_desc));
        PERM_DESCS.put("CREATE_PUBLIC_THREADS", getString(R.string.role_perm_create_public_threads_desc));
        PERM_DESCS.put("CREATE_PRIVATE_THREADS", getString(R.string.role_perm_create_private_threads_desc));
        PERM_DESCS.put("EMBED_LINKS", getString(R.string.role_perm_embed_links_desc));
        PERM_DESCS.put("ATTACH_FILES", getString(R.string.role_perm_attach_files_desc));
        PERM_DESCS.put("ADD_REACTIONS", getString(R.string.role_perm_add_reactions_desc));
        PERM_DESCS.put("USE_EXTERNAL_EMOJIS", getString(R.string.role_perm_use_external_emojis_desc));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
