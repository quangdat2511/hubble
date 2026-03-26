package com.example.hubble.view.me;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.me.UpdateProfileRequest;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.databinding.FragmentUserProfileBinding;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.viewmodel.AuthViewModel;
import com.example.hubble.viewmodel.AuthViewModelFactory;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserProfileFragment extends Fragment {

    private static final String TAG = "UserProfileFragment";

    private FragmentUserProfileBinding binding;
    private AuthViewModel authViewModel;
    private String token;
    private boolean isEditMode = false;

    public UserProfileFragment() {
        super(R.layout.fragment_user_profile);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUserProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(
                requireActivity(),
                new AuthViewModelFactory(new AuthRepository(requireContext()))
        ).get(AuthViewModel.class);

        token = "Bearer " + authViewModel.getToken();

        setupStatusDropdown();
        setEditMode(false);
        setupActions();
        loadProfile();
    }

    private void setupStatusDropdown() {
        String[] statuses = getResources().getStringArray(R.array.status_options);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                statuses
        );

        binding.etStatus.setAdapter(adapter);
    }

    private void setupActions() {
        binding.btnSaveProfile.setOnClickListener(v -> {
            if (!isEditMode) {
                setEditMode(true);
            } else {
                saveProfile();
            }
        });
    }

    private void loadProfile() {
        authViewModel.getApiService().getProfile(token)
                .enqueue(new Callback<ApiResponse<UserResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<UserResponse>> call,
                                           Response<ApiResponse<UserResponse>> response) {
                        if (!isAdded() || binding == null) return;

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().getResult() != null) {

                            UserResponse user = response.body().getResult();
                            new TokenManager(requireContext()).saveUser(user);
                            populateUserInfo(user);

                        } else {
                            String errorMessage = "Load profile failed";
                            try {
                                if (response.errorBody() != null) {
                                    errorMessage = response.errorBody().string();
                                } else {
                                    errorMessage = "Code: " + response.code();
                                }
                            } catch (Exception e) {
                                errorMessage = "Code: " + response.code() + ", read error: " + e.getMessage();
                            }

                            Log.e(TAG, "getProfile failed: " + errorMessage);
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                            populateUserInfo(authViewModel.getCurrentUser());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                        if (!isAdded() || binding == null) return;

                        String message = t.getMessage() != null ? t.getMessage() : "Unknown network error";
                        Log.e(TAG, "getProfile onFailure", t);
                        Toast.makeText(getContext(), "Load profile error: " + message, Toast.LENGTH_LONG).show();
                        populateUserInfo(authViewModel.getCurrentUser());
                    }
                });
    }

    private void populateUserInfo(@Nullable UserResponse user) {
        if (user == null) return;

        binding.tvUsername.setText(safe(user.getUsername()));
        binding.etDisplayName.setText(safe(user.getDisplayName()));
        binding.etPhone.setText(safe(user.getPhone()));
        binding.etBio.setText(safe(user.getBio()));

        String status = safe(user.getStatus());
        if (status.isEmpty()) {
            binding.etStatus.setText("ONLINE", false);
        } else {
            binding.etStatus.setText(status, false);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void setEditMode(boolean enabled) {
        isEditMode = enabled;

        setFieldEditable(binding.etDisplayName, enabled);
        setFieldEditable(binding.etPhone, enabled);
        setFieldEditable(binding.etBio, enabled);
        setStatusEditable(enabled);

        binding.btnSaveProfile.setText(
                enabled ? getString(R.string.me_save_profile)
                        : getString(R.string.me_edit_profile)
        );
    }

    private void setFieldEditable(EditText editText, boolean enabled) {
        editText.setEnabled(enabled);
        editText.setFocusable(enabled);
        editText.setFocusableInTouchMode(enabled);
        editText.setClickable(enabled);
        editText.setLongClickable(enabled);
        editText.setCursorVisible(enabled);
    }

    private void setStatusEditable(boolean enabled) {
        binding.etStatus.setEnabled(enabled);
        binding.etStatus.setClickable(enabled);
        binding.etStatus.setFocusable(enabled);
        binding.etStatus.setFocusableInTouchMode(false);
    }

    private void saveProfile() {
        String selectedStatus = binding.etStatus.getText() != null
                ? binding.etStatus.getText().toString().trim()
                : "";

        UpdateProfileRequest request = new UpdateProfileRequest(
                binding.etBio.getText().toString().trim(),
                binding.etDisplayName.getText().toString().trim(),
                selectedStatus,
                binding.etPhone.getText().toString().trim()
        );

        Log.d(TAG, "updateProfile bio=" + request.getBio());
        Log.d(TAG, "updateProfile displayName=" + request.getDisplayName());
        Log.d(TAG, "updateProfile status=" + request.getStatus());
        Log.d(TAG, "updateProfile phone=" + request.getPhone());

        authViewModel.getApiService().updateProfile(token, request)
                .enqueue(new Callback<ApiResponse<UserResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<UserResponse>> call,
                                           Response<ApiResponse<UserResponse>> response) {
                        if (!isAdded() || binding == null) return;

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().getResult() != null) {

                            UserResponse updated = response.body().getResult();
                            new TokenManager(requireContext()).saveUser(updated);
                            populateUserInfo(updated);
                            setEditMode(false);

                            Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                        } else {
                            String errorMessage = "Update failed";

                            try {
                                if (response.errorBody() != null) {
                                    errorMessage = response.errorBody().string();
                                } else if (response.body() != null) {
                                    errorMessage = "Code: " + response.code() + ", result is null";
                                } else {
                                    errorMessage = "Code: " + response.code() + ", empty response body";
                                }
                            } catch (Exception e) {
                                errorMessage = "Code: " + response.code() + ", cannot read error: " + e.getMessage();
                            }

                            Log.e(TAG, "updateProfile failed: " + errorMessage);
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                        if (!isAdded() || binding == null) return;

                        String message = t.getMessage() != null ? t.getMessage() : "Unknown network error";
                        Log.e(TAG, "updateProfile onFailure", t);
                        Toast.makeText(getContext(), "Network error: " + message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}