package com.example.hubble.view.me;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.hubble.R;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.model.me.AvatarResponse;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.databinding.FragmentAvatarBinding;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.viewmodel.AuthViewModel;
import com.example.hubble.viewmodel.AuthViewModelFactory;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AvatarFragment extends Fragment {

    public interface AvatarListener {
        void onAvatarUpdated(@NonNull UserResponse updatedUser);
    }

    private FragmentAvatarBinding binding;
    private AuthViewModel authViewModel;
    private String token;
    private AvatarListener avatarListener;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            sourceUri -> {
                if (sourceUri != null) {
                    launchCrop(sourceUri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> cropLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri croppedUri = UCrop.getOutput(result.getData());
                    if (croppedUri != null) {
                        previewAvatar(croppedUri);
                        uploadAvatar(croppedUri);
                    }
                } else if (result.getData() != null) {
                    Throwable error = UCrop.getError(result.getData());
                    if (error != null && isAdded()) {
                        Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
    );

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Fragment parent = getParentFragment();
        if (parent instanceof AvatarListener) {
            avatarListener = (AvatarListener) parent;
        } else if (context instanceof AvatarListener) {
            avatarListener = (AvatarListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAvatarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(
                requireActivity(),
                new AuthViewModelFactory(new AuthRepository(requireContext()))
        ).get(AuthViewModel.class);

        String accessToken = new TokenManager(requireContext()).getAccessToken();
        token = accessToken == null ? null : "Bearer " + accessToken;

        renderUser(authViewModel.getCurrentUser());
        loadMyAvatar();

        binding.fabEditAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    public void renderUser(@Nullable UserResponse user) {
        if (binding == null || user == null || !isAdded()) {
            return;
        }

        String displayName = user.getDisplayName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = user.getUsername();
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = "U";
        }

        binding.tvAvatarInitials.setText(displayName.substring(0, 1).toUpperCase());

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(ContextCompat.getColor(requireContext(), R.color.color_primary));
        binding.ivAvatar.setBackground(bg);

        updateStatusDot(user.getStatus());

        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            displayAvatar(avatarUrl);
        } else {
            showInitialsFallback();
        }
    }

    private void loadMyAvatar() {
        if (token == null || token.trim().isEmpty()) {
            showInitialsFallback();
            return;
        }

        setAvatarLoading(true);
        RetrofitClient.getApiService(requireContext()).getMyAvatar(token)
                .enqueue(new Callback<ApiResponse<AvatarResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AvatarResponse>> call,
                                           Response<ApiResponse<AvatarResponse>> response) {
                        if (!isAdded() || binding == null) {
                            return;
                        }

                        setAvatarLoading(false);

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().getResult() != null) {

                            String avatarUrl = response.body().getResult().getAvatarUrl();
                            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                                displayAvatar(avatarUrl);
                                return;
                            }
                        }

                        showInitialsFallback();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AvatarResponse>> call, Throwable t) {
                        if (!isAdded() || binding == null) {
                            return;
                        }

                        setAvatarLoading(false);
                        showInitialsFallback();
                    }
                });
    }

    private void launchCrop(@NonNull Uri sourceUri) {
        Uri destinationUri = Uri.fromFile(
                new File(requireContext().getCacheDir(), "avatar_crop_" + UUID.randomUUID() + ".jpg")
        );

        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setShowCropGrid(false);
        options.setShowCropFrame(true);
        options.setCompressionQuality(90);
        options.setHideBottomControls(false);
        options.setToolbarTitle(getString(R.string.me_avatar_picker_title));
        options.setToolbarColor(ContextCompat.getColor(requireContext(), R.color.color_surface));
        options.setActiveControlsWidgetColor(ContextCompat.getColor(requireContext(), R.color.color_primary));

        Intent cropIntent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(512, 512)
                .withOptions(options)
                .getIntent(requireContext());

        cropLauncher.launch(cropIntent);
    }

    private void previewAvatar(@NonNull Uri uri) {
        if (binding == null) {
            return;
        }

        binding.tvAvatarInitials.setVisibility(View.GONE);
        Glide.with(this)
                .load(uri)
                .transform(new CircleCrop())
                .into(binding.ivAvatar);
    }

    private void uploadAvatar(@NonNull Uri uri) {
        if (token == null || token.trim().isEmpty()) {
            if (isAdded()) {
                Toast.makeText(requireContext(), R.string.me_avatar_error_upload, Toast.LENGTH_LONG).show();
            }
            return;
        }

        setAvatarLoading(true);
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                setAvatarLoading(false);
                Toast.makeText(requireContext(), R.string.me_avatar_error_read, Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] bytes = getBytes(inputStream);
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), bytes);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", "avatar.jpg", requestFile);

            RetrofitClient.getApiService(requireContext()).uploadMyAvatar(token, body)
                    .enqueue(new Callback<ApiResponse<UserResponse>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<UserResponse>> call,
                                               Response<ApiResponse<UserResponse>> response) {
                            if (!isAdded() || binding == null) {
                                return;
                            }

                            if (response.isSuccessful()
                                    && response.body() != null
                                    && response.body().getResult() != null) {

                                UserResponse updatedUser = response.body().getResult();
                                new TokenManager(requireContext()).saveUser(updatedUser);
                                renderUser(updatedUser);
                                loadMyAvatar();

                                if (avatarListener != null) {
                                    avatarListener.onAvatarUpdated(updatedUser);
                                }

                                Toast.makeText(requireContext(), R.string.me_avatar_updated, Toast.LENGTH_SHORT).show();
                            } else {
                                setAvatarLoading(false);
                                Toast.makeText(requireContext(), R.string.me_avatar_error_upload, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                            if (!isAdded()) {
                                return;
                            }

                            setAvatarLoading(false);
                            String message = t.getMessage() != null
                                    ? t.getMessage()
                                    : getString(R.string.me_avatar_error_upload);
                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (Exception e) {
            setAvatarLoading(false);
            if (isAdded()) {
                Toast.makeText(requireContext(), R.string.me_avatar_error_read, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void displayAvatar(@NonNull String avatarUrl) {
        if (binding == null) {
            return;
        }

        binding.tvAvatarInitials.setVisibility(View.GONE);
        Drawable previous = binding.ivAvatar.getDrawable();

        Glide.with(this)
                .load(toAbsoluteAvatarUrl(avatarUrl))
                .transform(new CircleCrop())
                .placeholder(previous)
                .error(previous)
                .into(binding.ivAvatar);
    }

    private void showInitialsFallback() {
        if (binding == null) {
            return;
        }

        binding.ivAvatar.setImageDrawable(null);
        binding.tvAvatarInitials.setVisibility(View.VISIBLE);
    }

    private void updateStatusDot(@Nullable String status) {
        if (binding == null || !isAdded()) {
            return;
        }

        int colorRes;
        String normalizedStatus = status == null ? "OFFLINE" : status.trim().toUpperCase();
        switch (normalizedStatus) {
            case "ONLINE":
                colorRes = R.color.color_online;
                break;
            case "IDLE":
                colorRes = R.color.color_idle;
                break;
            case "DND":
                colorRes = R.color.color_dnd;
                break;
            default:
                colorRes = R.color.color_offline;
                break;
        }

        Drawable background = binding.viewOnlineStatus.getBackground();
        if (background instanceof GradientDrawable) {
            ((GradientDrawable) background).setColor(ContextCompat.getColor(requireContext(), colorRes));
        }
    }

    private void setAvatarLoading(boolean loading) {
        if (binding == null) {
            return;
        }

        binding.progressAvatar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.fabEditAvatar.setEnabled(!loading);
        binding.tvAvatarHint.setText(loading
                ? getString(R.string.me_avatar_uploading)
                : getString(R.string.me_avatar_hint));
    }

    private String toAbsoluteAvatarUrl(@NonNull String avatarUrl) {
        if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) {
            return avatarUrl;
        }

        String baseUrl = RetrofitClient.getBaseUrl();
        if (baseUrl.endsWith("/") && avatarUrl.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + avatarUrl;
        }
        if (!baseUrl.endsWith("/") && !avatarUrl.startsWith("/")) {
            return baseUrl + "/" + avatarUrl;
        }
        return baseUrl + avatarUrl;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private byte[] getBytes(@NonNull InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;

        while ((nRead = inputStream.read(data)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toByteArray();
    }
}
