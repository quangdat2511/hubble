package com.example.hubble.view.server;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.R;
import com.example.hubble.data.model.server.RoleResponse;
import com.example.hubble.data.repository.RoleRepository;
import com.example.hubble.databinding.BottomSheetEditMemberRolesBinding;
import com.example.hubble.databinding.ItemEditRoleRowBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EditMemberRolesBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_SERVER_ID = "server_id";
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_DISPLAY_NAME = "display_name";

    // Pre-loaded server roles set by MemberEditActivity before each open
    public static volatile List<RoleResponse> sPendingAllRoles;

    // Persists across instances of this sheet for the same user.
    // Populated after the first member-role fetch; updated on every toggle.
    private static volatile Set<String> sCachedMemberRoleIds;
    private static volatile String sCachedForUserId;

    private BottomSheetEditMemberRolesBinding binding;
    private RoleRepository roleRepository;
    private RoleCheckAdapter adapter;

    private String serverId;
    private String userId;
    private String displayName;

    private final List<RoleResponse> allRoles = new ArrayList<>();
    private final Set<String> checkedRoleIds = new HashSet<>();
    // Role IDs currently being saved — row shows spinner while in-flight
    private final Set<String> savingRoleIds = new HashSet<>();

    public static EditMemberRolesBottomSheet newInstance(String serverId, String userId, String displayName) {
        EditMemberRolesBottomSheet sheet = new EditMemberRolesBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_SERVER_ID, serverId);
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_DISPLAY_NAME, displayName);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetEditMemberRolesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            dialog.getBehavior().setSkipCollapsed(true);
        }

        if (getArguments() != null) {
            serverId = getArguments().getString(ARG_SERVER_ID);
            userId = getArguments().getString(ARG_USER_ID);
            displayName = getArguments().getString(ARG_DISPLAY_NAME);
        }

        roleRepository = new RoleRepository(requireContext());

        binding.tvSubtitle.setText(getString(R.string.edit_member_roles_subtitle, displayName));
        binding.rvRoles.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RoleCheckAdapter();
        binding.rvRoles.setAdapter(adapter);

        List<RoleResponse> preAllRoles = sPendingAllRoles;
        sPendingAllRoles = null;
        boolean memberRolesCached = userId.equals(sCachedForUserId) && sCachedMemberRoleIds != null;

        if (preAllRoles != null && memberRolesCached) {
            // Full cache hit — show instantly with no network call
            addFilteredRoles(preAllRoles);
            checkedRoleIds.addAll(sCachedMemberRoleIds);
            showRoles();
        } else if (preAllRoles != null) {
            // Server roles cached; fetch member roles once then cache the result
            addFilteredRoles(preAllRoles);
            setLoading(true);
            roleRepository.getMemberRoles(serverId, userId, memberRolesResult -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    if (memberRolesResult.isSuccess() && memberRolesResult.getData() != null) {
                        for (RoleResponse r : memberRolesResult.getData()) checkedRoleIds.add(r.getId());
                    }
                    sCachedForUserId = userId;
                    sCachedMemberRoleIds = new HashSet<>(checkedRoleIds);
                    showRoles();
                });
            });
        } else {
            loadData();
        }
    }

    private void loadData() {
        setLoading(true);
        roleRepository.getRoles(serverId, rolesResult -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (rolesResult.isSuccess() && rolesResult.getData() != null) {
                    addFilteredRoles(rolesResult.getData());
                    roleRepository.getMemberRoles(serverId, userId, memberRolesResult -> {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            setLoading(false);
                            if (memberRolesResult.isSuccess() && memberRolesResult.getData() != null) {
                                for (RoleResponse r : memberRolesResult.getData()) {
                                    checkedRoleIds.add(r.getId());
                                }
                            }
                            sCachedForUserId = userId;
                            sCachedMemberRoleIds = new HashSet<>(checkedRoleIds);
                            showRoles();
                        });
                    });
                } else {
                    setLoading(false);
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                    binding.rvRoles.setVisibility(View.GONE);
                }
            });
        });
    }

    /** Adds roles from the list to {@link #allRoles}, skipping the default (@everyone) role. */
    private void addFilteredRoles(List<RoleResponse> roles) {
        for (RoleResponse r : roles) {
            if (!Boolean.TRUE.equals(r.getIsDefault())) allRoles.add(r);
        }
    }

    private void showRoles() {
        if (allRoles.isEmpty()) {
            binding.tvEmpty.setVisibility(View.VISIBLE);
            binding.rvRoles.setVisibility(View.GONE);
        } else {
            binding.tvEmpty.setVisibility(View.GONE);
            binding.rvRoles.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    /** Called when user taps a row — immediately saves the change to the server. */
    private void toggleRole(int adapterPos, String roleId, boolean newCheckedState) {
        if (savingRoleIds.contains(roleId)) return;

        // Optimistically update UI and mark as saving
        savingRoleIds.add(roleId);
        if (newCheckedState) checkedRoleIds.add(roleId);
        else checkedRoleIds.remove(roleId);
        adapter.notifyItemChanged(adapterPos);

        if (newCheckedState) {
            List<String> memberList = new ArrayList<>();
            memberList.add(userId);
            roleRepository.assignMembers(serverId, roleId, memberList, result -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    savingRoleIds.remove(roleId);
                    if (result.isError()) {
                        checkedRoleIds.remove(roleId); // revert
                        showSaveError();
                    } else {
                        sCachedForUserId = userId;
                        sCachedMemberRoleIds = new HashSet<>(checkedRoleIds);
                    }
                    adapter.notifyItemChanged(adapterPos);
                });
            });
        } else {
            roleRepository.removeMember(serverId, roleId, userId, result -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    savingRoleIds.remove(roleId);
                    if (result.isError()) {
                        checkedRoleIds.add(roleId); // revert
                        showSaveError();
                    } else {
                        sCachedForUserId = userId;
                        sCachedMemberRoleIds = new HashSet<>(checkedRoleIds);
                    }
                    adapter.notifyItemChanged(adapterPos);
                });
            });
        }
    }

    private void showSaveError() {
        if (binding != null) {
            Snackbar.make(binding.getRoot(),
                    R.string.edit_member_roles_save_error, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.rvRoles.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ─── Inline adapter ───────────────────────────────────────────────────

    private class RoleCheckAdapter extends RecyclerView.Adapter<RoleCheckAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemEditRoleRowBinding rowBinding = ItemEditRoleRowBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(rowBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(allRoles.get(position));
        }

        @Override
        public int getItemCount() {
            return allRoles.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemEditRoleRowBinding b;

            ViewHolder(ItemEditRoleRowBinding binding) {
                super(binding.getRoot());
                this.b = binding;
            }

            void bind(RoleResponse role) {
                b.tvRoleName.setText(role.getName());

                // Color dot
                Drawable bg = b.viewRoleColor.getBackground().mutate();
                if (bg instanceof GradientDrawable) {
                    int color = (role.getColor() != null && role.getColor() != 0)
                            ? (0xFF000000 | role.getColor())
                            : ContextCompat.getColor(b.getRoot().getContext(), R.color.role_default_color);
                    ((GradientDrawable) bg).setColor(color);
                }
                b.viewRoleColor.setBackground(bg);

                boolean isSaving = savingRoleIds.contains(role.getId());
                boolean isChecked = checkedRoleIds.contains(role.getId());

                if (isSaving) {
                    b.cbRole.setVisibility(View.INVISIBLE);
                    b.pbRole.setVisibility(View.VISIBLE);
                    b.getRoot().setClickable(false);
                } else {
                    b.cbRole.setVisibility(View.VISIBLE);
                    b.pbRole.setVisibility(View.GONE);
                    b.cbRole.setChecked(isChecked);
                    b.getRoot().setClickable(true);
                    b.getRoot().setOnClickListener(v -> {
                        int pos = getBindingAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            toggleRole(pos, role.getId(), !checkedRoleIds.contains(role.getId()));
                        }
                    });
                }
            }
        }
    }
}
