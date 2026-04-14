package com.example.hubble.view.server;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hubble.adapter.server.AddRoleMemberAdapter;
import com.example.hubble.data.model.server.ServerMemberItem;
import com.example.hubble.data.repository.ServerMemberRepository;
import com.example.hubble.databinding.BottomSheetAddRoleMemberBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddRoleMemberBottomSheet extends BottomSheetDialogFragment {

    public interface OnMembersSelectedListener {
        void onMembersSelected(List<String> userIds);
    }

    private BottomSheetAddRoleMemberBinding binding;
    private AddRoleMemberAdapter adapter;
    private OnMembersSelectedListener listener;

    private String serverId;
    private String roleName;
    private ArrayList<String> existingUserIds;

    public static AddRoleMemberBottomSheet newInstance(String serverId, String roleName, ArrayList<String> existingUserIds) {
        AddRoleMemberBottomSheet sheet = new AddRoleMemberBottomSheet();
        Bundle args = new Bundle();
        args.putString("server_id", serverId);
        args.putString("role_name", roleName);
        args.putStringArrayList("existing_user_ids", existingUserIds);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnMembersSelectedListener(OnMembersSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAddRoleMemberBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Expand bottom sheet fully
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            dialog.getBehavior().setSkipCollapsed(true);
        }

        if (getArguments() != null) {
            serverId = getArguments().getString("server_id");
            roleName = getArguments().getString("role_name");
            existingUserIds = getArguments().getStringArrayList("existing_user_ids");
        }
        if (existingUserIds == null) existingUserIds = new ArrayList<>();

        binding.tvRoleName.setText(roleName);

        adapter = new AddRoleMemberAdapter();
        adapter.setOnSelectionChangedListener(count -> binding.btnAdd.setEnabled(count > 0));
        binding.rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMembers.setAdapter(adapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnAdd.setOnClickListener(v -> {
            List<String> selected = adapter.getSelectedUserIds();
            if (!selected.isEmpty() && listener != null) {
                listener.onMembersSelected(selected);
            }
            dismiss();
        });

        loadServerMembers();
    }

    private void loadServerMembers() {
        ServerMemberRepository repo = new ServerMemberRepository(requireContext());
        Set<String> existing = new HashSet<>(existingUserIds);

        repo.getServerMembers(serverId, result -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (result.isSuccess() && result.getData() != null) {
                    List<ServerMemberItem> available = new ArrayList<>();
                    for (ServerMemberItem m : result.getData()) {
                        if (!existing.contains(m.getUserId())) {
                            available.add(m);
                        }
                    }
                    adapter.setMembers(available);
                    binding.tvEmpty.setVisibility(available.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.rvMembers.setVisibility(available.isEmpty() ? View.GONE : View.VISIBLE);
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
