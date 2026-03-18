package com.example.hubble.view.server;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Outline;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hubble.R;
import com.example.hubble.databinding.FragmentCustomizeServerBinding;
import com.example.hubble.viewmodel.CreateServerViewModel;
import com.google.android.material.snackbar.Snackbar;

public class CustomizeServerFragment extends Fragment {

    private FragmentCustomizeServerBinding binding;
    private CreateServerViewModel viewModel;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedUri = result.getData().getData();
                    if (selectedUri != null) {
                        viewModel.setIconUri(selectedUri);
                        binding.ivServerIcon.setImageURI(selectedUri);
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomizeServerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CreateServerActivity activity = (CreateServerActivity) requireActivity();
        viewModel = activity.getCreateServerViewModel();

        // Make the icon view circular
        binding.ivServerIcon.setClipToOutline(true);
        binding.ivServerIcon.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });

        binding.toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Pre-fill name if set from template
        String existingName = viewModel.serverName.getValue();
        if (existingName != null && !existingName.isEmpty()) {
            binding.etServerName.setText(existingName);
        }

        binding.etServerName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setServerName(s.toString().trim());
            }
        });

        // Icon picker
        binding.fabPickIcon.setOnClickListener(v -> openImagePicker());
        binding.ivServerIcon.setOnClickListener(v -> openImagePicker());

        binding.btnCreate.setOnClickListener(v -> {
            String name = binding.etServerName.getText() != null
                    ? binding.etServerName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                binding.tilServerName.setError(getString(R.string.error_empty_server_name));
                return;
            }
            binding.tilServerName.setError(null);
            viewModel.setServerName(name);
            viewModel.createServer();
        });

        observeCreateState(activity);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void observeCreateState(CreateServerActivity activity) {
        viewModel.createState.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            if (result.isLoading()) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.btnCreate.setEnabled(false);
            } else {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnCreate.setEnabled(true);

                if (result.isSuccess() && result.getData() != null) {
                    viewModel.resetCreateState();
                    activity.onServerCreated(result.getData());
                } else if (result.isError()) {
                    viewModel.resetCreateState();
                    String msg = result.getMessage() != null
                            ? result.getMessage() : getString(R.string.error_generic);
                    Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
