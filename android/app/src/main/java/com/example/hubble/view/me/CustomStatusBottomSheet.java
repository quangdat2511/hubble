package com.example.hubble.view.me;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hubble.R;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.databinding.BottomSheetCustomStatusBinding;
import com.example.hubble.utils.InAppMessageUtils;
import com.example.hubble.utils.TokenManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomStatusBottomSheet extends BottomSheetDialogFragment {

    public interface CustomStatusListener {
        void onCustomStatusChanged(@Nullable String customStatus);
    }

    private static final String ARG_CURRENT = "current_custom_status";

    private BottomSheetCustomStatusBinding binding;
    private String token;

    public static CustomStatusBottomSheet newInstance(@Nullable String currentCustomStatus) {
        CustomStatusBottomSheet sheet = new CustomStatusBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_CURRENT, currentCustomStatus);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetCustomStatusBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TokenManager tokenManager = new TokenManager(requireContext());
        String accessToken = tokenManager.getAccessToken();
        token = accessToken != null ? "Bearer " + accessToken : null;

        String current = getArguments() != null ? getArguments().getString(ARG_CURRENT) : null;
        if (!TextUtils.isEmpty(current)) {
            binding.etCustomStatus.setText(current);
        }

        binding.btnSaveCustomStatus.setOnClickListener(v -> {
            String text = binding.etCustomStatus.getText() != null
                    ? binding.etCustomStatus.getText().toString().trim() : "";
            saveCustomStatus(text.isEmpty() ? null : text);
        });

        binding.btnClearCustomStatus.setOnClickListener(v -> saveCustomStatus(null));
    }

    private void saveCustomStatus(@Nullable String customStatus) {
        if (token == null) return;

        Map<String, String> body = new HashMap<>();
        body.put("customStatus", customStatus);

        RetrofitClient.getApiService(requireContext()).updateCustomStatus(token, body)
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call,
                                           Response<ApiResponse<Object>> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful()) {
                            TokenManager tm = new TokenManager(requireContext());
                            if (tm.getUser() != null) {
                                tm.getUser().setCustomStatus(customStatus);
                                tm.saveUser(tm.getUser());
                            }
                            CustomStatusListener listener = findListener();
                            if (listener != null) {
                                listener.onCustomStatusChanged(customStatus);
                            }
                            InAppMessageUtils.show(binding.getRoot(),
                                    getString(R.string.status_updated));
                            dismiss();
                        } else {
                            InAppMessageUtils.show(binding.getRoot(),
                                    getString(R.string.status_update_failed));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        if (!isAdded()) return;
                        InAppMessageUtils.show(binding.getRoot(),
                                getString(R.string.status_update_failed));
                    }
                });
    }

    @Nullable
    private CustomStatusListener findListener() {
        if (getParentFragment() instanceof CustomStatusListener) {
            return (CustomStatusListener) getParentFragment();
        }
        if (getActivity() instanceof CustomStatusListener) {
            return (CustomStatusListener) getActivity();
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
