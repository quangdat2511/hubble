package com.example.hubble.view.me;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri sourceUri = result.getData().getData();
                    if (sourceUri != null) {
                        launchCrop(sourceUri);
                    }
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

        token = "Bearer " + new TokenManager(requireContext()).getAccessToken();

        renderUser(authViewModel.getCurrentUser());
        loadMyAvatar();

        binding.fabEditAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });
    }

    public void renderUser(@Nullable UserResponse user) {
        if (binding == null || user == null || !isAdded()) return;

        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "U";
        String initials = displayName.isEmpty() ? "U" : displayName.substring(0, 1).toUpperCase();
        binding.tvAvatarInitials.setText(initials);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(ContextCompat.getColor(requireContext(), R.color.color_primary));
        binding.ivAvatar.setBackground(bg);

        int color;
        String status = user.getStatus() != null ? user.getStatus() : "OFFLINE";
        switch (status) {
            case "ONLINE":
                color = android.R.color.holo_green_light;
                break;
            case "IDLE":
                color = android.R.color.holo_orange_light;
                break;
            case "DND":
                color = android.R.color.holo_red_dark;
                break;
            default:
                color = android.R.color.darker_gray;
                break;
        }

        GradientDrawable dot = (GradientDrawable) binding.viewOnlineStatus.getBackground();
        dot.setColor(ContextCompat.getColor(requireContext(), color));
    }

    private void loadMyAvatar() {
        RetrofitClient.getApiService(requireContext()).getMyAvatar(token)
                .enqueue(new Callback<ApiResponse<AvatarResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AvatarResponse>> call,
                                           Response<ApiResponse<AvatarResponse>> response) {
                        if (!isAdded() || binding == null) return;

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().getResult() != null) {

                            AvatarResponse avatarResponse = response.body().getResult();
                            String avatarUrl = avatarResponse.getAvatarUrl();

                            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                                binding.tvAvatarInitials.setVisibility(View.GONE);

                                Glide.with(AvatarFragment.this)
                                        .load(toAbsoluteAvatarUrl(avatarUrl))
                                        .transform(new CircleCrop())
                                        .into(binding.ivAvatar);
                            } else {
                                binding.tvAvatarInitials.setVisibility(View.VISIBLE);
                                binding.ivAvatar.setImageDrawable(null);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AvatarResponse>> call, Throwable t) {
                        // keep initials fallback, no toast needed unless you want noisy errors
                    }
                });
    }

    private String toAbsoluteAvatarUrl(@NonNull String avatarUrl) {
        if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) {
            return avatarUrl;
        }

        // change this base if your RetrofitClient uses a different base url
        String baseUrl = RetrofitClient.BASE_URL;

        if (baseUrl.endsWith("/") && avatarUrl.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + avatarUrl;
        } else if (!baseUrl.endsWith("/") && !avatarUrl.startsWith("/")) {
            return baseUrl + "/" + avatarUrl;
        } else {
            return baseUrl + avatarUrl;
        }
    }

    private void previewAvatar(@NonNull Uri uri) {
        if (binding == null) return;
        binding.tvAvatarInitials.setVisibility(View.GONE);
        Glide.with(this)
                .load(uri)
                .transform(new CircleCrop())
                .into(binding.ivAvatar);
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

        Intent cropIntent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(512, 512)
                .withOptions(options)
                .getIntent(requireContext());

        cropLauncher.launch(cropIntent);
    }

    private void uploadAvatar(@NonNull Uri uri) {
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                Toast.makeText(requireContext(), "Cannot read image", Toast.LENGTH_SHORT).show();
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
                            if (!isAdded() || binding == null) return;

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

                                Toast.makeText(requireContext(), "Avatar updated", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(), "Avatar upload failed", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                            if (!isAdded()) return;
                            String message = t.getMessage() != null ? t.getMessage() : "Network error";
                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                        }
                    });

        } catch (Exception e) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Failed to read image", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private byte[] getBytes(@NonNull InputStream inputStream) throws Exception {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;

        while ((nRead = inputStream.read(data)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toByteArray();
    }
}