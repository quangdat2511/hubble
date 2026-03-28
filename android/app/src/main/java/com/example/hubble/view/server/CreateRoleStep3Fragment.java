package com.example.hubble.view.server;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hubble.R;
import com.example.hubble.adapter.server.MemberSelectAdapter;
import com.example.hubble.data.model.server.RoleMockData;
import com.example.hubble.data.model.server.ServerMemberItem;
import com.example.hubble.databinding.FragmentCreateRoleStep3Binding;

import java.util.List;

/**
 * Create Role Step 3 (#12) - Assign members to the new role.
 */
public class CreateRoleStep3Fragment extends Fragment {

    private FragmentCreateRoleStep3Binding binding;
    private String serverId;
    private String roleName;
    private int roleColor;

    public static CreateRoleStep3Fragment newInstance(String serverId, String roleName, int roleColor) {
        CreateRoleStep3Fragment fragment = new CreateRoleStep3Fragment();
        Bundle args = new Bundle();
        args.putString("server_id", serverId);
        args.putString("role_name", roleName);
        args.putInt("role_color", roleColor);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateRoleStep3Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            serverId = getArguments().getString("server_id");
            roleName = getArguments().getString("role_name");
            roleColor = getArguments().getInt("role_color");
        }

        binding.toolbar.setTitle(getString(R.string.create_role_step, 3));
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        List<ServerMemberItem> members = RoleMockData.getMockMembers();

        MemberSelectAdapter adapter = new MemberSelectAdapter(members, selectedCount ->
                binding.btnFinish.setEnabled(selectedCount > 0));

        binding.rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMembers.setAdapter(adapter);

        // Finish button — creates role and returns to roles list
        binding.btnFinish.setOnClickListener(v -> finishRoleCreation());
        binding.btnSkip.setOnClickListener(v -> finishRoleCreation());
    }

    private void finishRoleCreation() {
        // Pop all create role steps and navigate to roles list
        requireActivity().getSupportFragmentManager()
                .popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        ((ServerSettingsActivity) requireActivity()).navigateTo(
                RolesListFragment.newInstance(serverId), false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
