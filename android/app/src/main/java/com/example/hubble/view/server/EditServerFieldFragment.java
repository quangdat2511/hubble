package com.example.hubble.view.server;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.repository.ServerMemberRepository;
import com.example.hubble.data.repository.ServerRepository;
import com.example.hubble.databinding.FragmentEditServerFieldBinding;
import com.example.hubble.viewmodel.server.ServerSettingsViewModel;
import com.example.hubble.viewmodel.server.ServerSettingsViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

/**
 * Reusable fragment for editing server name or description.
 * Uses field_type arg: "name" or "description".
 */
public class EditServerFieldFragment extends Fragment {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";

    private FragmentEditServerFieldBinding binding;
    private ServerSettingsViewModel viewModel;

    private String serverId;
    private String fieldType;
    private String currentValue;

    public static EditServerFieldFragment newInstance(String serverId, String fieldType, String currentValue) {
        EditServerFieldFragment fragment = new EditServerFieldFragment();
        Bundle args = new Bundle();
        args.putString("server_id", serverId);
        args.putString("field_type", fieldType);
        args.putString("current_value", currentValue);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEditServerFieldBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            serverId = getArguments().getString("server_id");
            fieldType = getArguments().getString("field_type");
            currentValue = getArguments().getString("current_value", "");
        }

        viewModel = new ViewModelProvider(requireActivity(),
                new ServerSettingsViewModelFactory(
                        new ServerMemberRepository(requireContext()),
                        new ServerRepository(requireContext())))
                .get(ServerSettingsViewModel.class);

        boolean isName = FIELD_NAME.equals(fieldType);

        binding.toolbar.setTitle(isName
                ? getString(R.string.server_settings_server_name)
                : getString(R.string.server_settings_server_description));
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        binding.textInputLayout.setHint(isName
                ? getString(R.string.server_settings_server_name)
                : getString(R.string.server_settings_server_description));
        binding.textInputLayout.setCounterMaxLength(isName ? 100 : 1024);

        if (!isName) {
            binding.etField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            binding.etField.setMinLines(3);
            binding.etField.setMaxLines(8);
        }

        binding.etField.setText(currentValue);
        binding.etField.setSelection(currentValue != null ? currentValue.length() : 0);

        binding.btnSave.setOnClickListener(v -> save());

        viewModel.getUpdateServerState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            if (result.getStatus() == AuthResult.Status.LOADING) {
                binding.btnSave.setEnabled(false);
                return;
            }

            binding.btnSave.setEnabled(true);

            if (result.getStatus() == AuthResult.Status.SUCCESS) {
                viewModel.consumeUpdateServerState();
                requireActivity().onBackPressed();
            } else if (result.getStatus() == AuthResult.Status.ERROR) {
                String msg = result.getMessage() != null
                        ? result.getMessage() : getString(R.string.error_generic);
                Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show();
                viewModel.consumeUpdateServerState();
            }
        });
    }

    private void save() {
        String value = binding.etField.getText() != null
                ? binding.etField.getText().toString().trim() : "";

        boolean isName = FIELD_NAME.equals(fieldType);

        if (isName) {
            if (value.isEmpty()) {
                binding.textInputLayout.setError(getString(R.string.error_empty_server_name));
                return;
            }
            if (value.length() > 100) {
                binding.textInputLayout.setError(getString(R.string.error_server_name_too_long));
                return;
            }
            viewModel.updateServer(serverId, value, null);
        } else {
            viewModel.updateServer(serverId, null, value);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
