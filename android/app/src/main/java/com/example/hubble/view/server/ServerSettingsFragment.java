package com.example.hubble.view.server;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.repository.ServerMemberRepository;
import com.example.hubble.data.repository.ServerRepository;
import com.example.hubble.data.repository.RoleRepository;
import com.example.hubble.databinding.FragmentServerSettingsBinding;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.utils.PermissionsCache;
import com.example.hubble.viewmodel.server.ServerSettingsViewModel;
import com.example.hubble.viewmodel.server.ServerSettingsViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ServerSettingsFragment extends Fragment {

    private static final long MAX_ICON_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"));

    private FragmentServerSettingsBinding binding;
    private ServerSettingsViewModel viewModel;

    private String serverId;
    private String serverName;
    private String ownerId;
    private String currentIconUrl;   // tracks latest iconUrl across update/delete
    private String description;

    private final ActivityResultLauncher<Intent> iconPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null && validateIconFile(uri)) {
                        viewModel.updateServerIcon(serverId, uri);
                    }
                }
            });

    public static ServerSettingsFragment newInstance(String serverId, String serverName,
                                                     String ownerId, String iconUrl,
                                                     String description) {
        ServerSettingsFragment fragment = new ServerSettingsFragment();
        Bundle args = new Bundle();
        args.putString("server_id",   serverId);
        args.putString("server_name", serverName);
        args.putString("owner_id",    ownerId);
        args.putString("icon_url",    iconUrl);
        args.putString("description", description);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentServerSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            serverId       = getArguments().getString("server_id");
            serverName     = getArguments().getString("server_name");
            ownerId        = getArguments().getString("owner_id");
            currentIconUrl = getArguments().getString("icon_url");
            description    = getArguments().getString("description");
        }

        // Determine if current user is the owner
        String currentUserId = new TokenManager(requireContext()).getUser() != null
                ? new TokenManager(requireContext()).getUser().getId() : null;
        boolean isOwner = ownerId != null && ownerId.equals(currentUserId);

        // Build ViewModel with both repos
        viewModel = new ViewModelProvider(
                requireActivity(),
                new ServerSettingsViewModelFactory(
                        new ServerMemberRepository(requireContext()),
                        new ServerRepository(requireContext())))
                .get(ServerSettingsViewModel.class);

        viewModel.loadMembers(serverId);

        // Prefetch roles for sub-screen
        RolesListFragment.prefetch(requireContext(), serverId);

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        // ── Overview section (owner-only) ────────────────────────────────

        if (isOwner) {
            binding.sectionOverview.setVisibility(View.VISIBLE);
            binding.rowServerName.setOnClickListener(v -> {
                ((ServerSettingsActivity) requireActivity()).navigateTo(
                        EditServerFieldFragment.newInstance(serverId, EditServerFieldFragment.FIELD_NAME, serverName), true);
            });
            binding.rowServerDescription.setOnClickListener(v -> {
                ((ServerSettingsActivity) requireActivity()).navigateTo(
                        EditServerFieldFragment.newInstance(serverId, EditServerFieldFragment.FIELD_DESCRIPTION, description), true);
            });
        } else {
            binding.sectionOverview.setVisibility(View.GONE);
        }

        // Server icon row: owner-only
        if (isOwner) {
            binding.rowServerIcon.setVisibility(View.VISIBLE);
            binding.rowServerIcon.setOnClickListener(v -> openIconPicker());

            // Remove icon row: only when there is an existing icon
            updateRemoveIconVisibility();
            binding.rowRemoveIcon.setOnClickListener(v -> confirmRemoveIcon());
        } else {
            binding.rowServerIcon.setVisibility(View.GONE);
            binding.rowRemoveIcon.setVisibility(View.GONE);
        }

        // Delete server card: owner-only
        binding.cardDeleteServer.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        // ── Non-owner permission-gated views ──────────────────────────────
        // For non-owners: hide rows first, then apply cache immediately (no flicker),
        // then always refresh in background so stale cache is silently corrected.
        if (!isOwner) {
            binding.rowRoles.setVisibility(View.GONE);
            binding.rowInvites.setVisibility(View.GONE);
            binding.sectionModeration.setVisibility(View.GONE);

            // Cache-first: apply instantly if permissions are already known
            Set<String> cached = PermissionsCache.get(serverId);
            if (cached != null) {
                applyPermissionVisibility(cached);
            }

            // Background refresh: always re-fetch to keep state fresh
            new RoleRepository(requireContext()).loadMyPermissions(serverId, result -> {
                if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
                    Set<String> perms = result.getData();
                    PermissionsCache.put(serverId, perms);
                    if (getActivity() == null) return;
                    requireActivity().runOnUiThread(() -> {
                        if (binding == null) return;
                        applyPermissionVisibility(perms);
                    });
                }
            });
        }

        // ── Members count ──────────────────────────────────────────────────

        viewModel.getMembersState().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.getStatus() == AuthResult.Status.SUCCESS
                    && result.getData() != null) {
                binding.tvMemberCount.setText(String.valueOf(result.getData().size()));
            }
        });

        // ── Members section rows ───────────────────────────────────────────

        binding.rowMembers.setOnClickListener(v -> {
            if (serverId != null) {
                ((ServerSettingsActivity) requireActivity()).navigateTo(
                        ServerMembersFragment.newInstance(serverId, serverName), true);
            }
        });
        binding.rowInvites.setOnClickListener(v -> {
            if (serverId != null) {
                ((ServerSettingsActivity) requireActivity()).navigateTo(
                        ServerInviteFragment.newInstance(serverId, serverName), true);
            }
        });
        binding.rowRoles.setOnClickListener(v -> {
            if (serverId != null) {
                ((ServerSettingsActivity) requireActivity()).navigateTo(
                        RolesListFragment.newInstance(serverId), true);
            }
        });
        // rowRoles + rowInvites visibility: owner always sees them; non-owners handled by cache-first block above
        binding.cardDeleteServer.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.server_settings_delete_confirm_title)
                        .setMessage(R.string.server_settings_delete_confirm_message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            if (serverId != null) viewModel.deleteServer(serverId);
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show());

        // ── Delete server observer ─────────────────────────────────────────

        viewModel.getDeleteServerState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            if (result.getStatus() == AuthResult.Status.LOADING) {
                binding.cardDeleteServer.setEnabled(false);
                return;
            }

            binding.cardDeleteServer.setEnabled(true);

            if (result.getStatus() == AuthResult.Status.SUCCESS) {
                viewModel.consumeDeleteServerState();
                requireActivity().setResult(Activity.RESULT_OK);
                requireActivity().finish();
            } else if (result.getStatus() == AuthResult.Status.ERROR) {
                String msg = result.getMessage() != null
                        ? result.getMessage() : getString(R.string.error_generic);
                Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show();
                viewModel.consumeDeleteServerState();
            }
        });

        // ── Icon state observer ────────────────────────────────────────────

        viewModel.getIconState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            if (result.getStatus() == AuthResult.Status.LOADING) {
                binding.progressBarIcon.setVisibility(View.VISIBLE);
                binding.rowServerIcon.setEnabled(false);
                binding.rowRemoveIcon.setEnabled(false);
                return;
            }

            binding.progressBarIcon.setVisibility(View.GONE);
            binding.rowServerIcon.setEnabled(true);
            binding.rowRemoveIcon.setEnabled(true);

            if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
                currentIconUrl = result.getData().getIconUrl();
                updateRemoveIconVisibility();
                String msg = currentIconUrl != null
                        ? getString(R.string.server_icon_update_success)
                        : getString(R.string.server_icon_delete_success);
                Snackbar.make(requireView(), msg, Snackbar.LENGTH_SHORT).show();
            } else if (result.getStatus() == AuthResult.Status.ERROR) {
                String msg = result.getMessage() != null
                        ? result.getMessage() : getString(R.string.error_generic);
                Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show();
            }
            viewModel.consumeIconState();
        });

        // ── Update server observer (name / description) ───────────────────

        viewModel.getUpdateServerState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
                serverName  = result.getData().getName();
                description = result.getData().getDescription();
                viewModel.consumeUpdateServerState();
            } else if (result.getStatus() == AuthResult.Status.ERROR) {
                viewModel.consumeUpdateServerState();
            }
        });
    }

    // ── Icon helpers ──────────────────────────────────────────────────────

    /**
     * Applies visibility to all permission-gated views for non-owner users.
     * Safe to call multiple times (idempotent). Must be called on the main thread.
     */
    private void applyPermissionVisibility(Set<String> perms) {
        if (binding == null) return;
        if (perms.contains("MANAGE_ROLES")) {
            binding.rowRoles.setVisibility(View.VISIBLE);
            binding.sectionModeration.setVisibility(View.VISIBLE);
        }
        if (perms.contains("INVITE_MEMBERS")) binding.rowInvites.setVisibility(View.VISIBLE);
        if (perms.contains("MANAGE_SERVER")) {
            binding.sectionOverview.setVisibility(View.VISIBLE);
            binding.rowServerName.setOnClickListener(v ->
                    ((ServerSettingsActivity) requireActivity()).navigateTo(
                            EditServerFieldFragment.newInstance(serverId, EditServerFieldFragment.FIELD_NAME, serverName), true));
            binding.rowServerDescription.setOnClickListener(v ->
                    ((ServerSettingsActivity) requireActivity()).navigateTo(
                            EditServerFieldFragment.newInstance(serverId, EditServerFieldFragment.FIELD_DESCRIPTION, description), true));
            binding.rowServerIcon.setVisibility(View.VISIBLE);
            binding.rowServerIcon.setOnClickListener(v -> openIconPicker());
            updateRemoveIconVisibility();
            binding.rowRemoveIcon.setOnClickListener(v -> confirmRemoveIcon());
        }
    }

    private void openIconPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        iconPickerLauncher.launch(intent);
    }

    private void confirmRemoveIcon() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.server_settings_remove_icon_confirm_title)
                .setMessage(R.string.server_settings_remove_icon_confirm_message)
                .setPositiveButton(android.R.string.ok,
                        (d, w) -> viewModel.deleteServerIcon(serverId))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void updateRemoveIconVisibility() {
        if (binding == null) return;
        binding.rowRemoveIcon.setVisibility(
                currentIconUrl != null && !currentIconUrl.isEmpty()
                        ? View.VISIBLE : View.GONE);
    }

    /** Returns true if the file passes MIME-type and size checks. */
    private boolean validateIconFile(Uri uri) {
        String mimeType = requireContext().getContentResolver().getType(uri);
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            Snackbar.make(requireView(),
                    getString(R.string.error_invalid_file_type), Snackbar.LENGTH_LONG).show();
            return false;
        }
        try {
            ParcelFileDescriptor pfd =
                    requireContext().getContentResolver().openFileDescriptor(uri, "r");
            if (pfd != null) {
                long size = pfd.getStatSize();
                pfd.close();
                if (size > MAX_ICON_BYTES) {
                    Snackbar.make(requireView(),
                            getString(R.string.error_file_too_large), Snackbar.LENGTH_LONG).show();
                    return false;
                }
            }
        } catch (Exception ignored) { }
        return true;
    }

    private void showComingSoon() {
        if (binding != null) {
            Snackbar.make(binding.getRoot(), R.string.main_coming_soon,
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
