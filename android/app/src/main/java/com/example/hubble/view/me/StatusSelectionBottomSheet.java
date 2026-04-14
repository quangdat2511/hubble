package com.example.hubble.view.me;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hubble.R;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.databinding.BottomSheetStatusSelectionBinding;
import com.example.hubble.utils.InAppMessageUtils;
import com.example.hubble.utils.TokenManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatusSelectionBottomSheet extends BottomSheetDialogFragment {

    public interface StatusSelectionListener {
        void onStatusChanged(@NonNull String newStatus);
    }

    private static final String ARG_CURRENT_STATUS = "current_status";
    private static final String ARG_CUSTOM_STATUS = "custom_status";

    private BottomSheetStatusSelectionBinding binding;
    private String token;

    public static StatusSelectionBottomSheet newInstance(@Nullable String currentStatus,
                                                         @Nullable String customStatus) {
        StatusSelectionBottomSheet sheet = new StatusSelectionBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_CURRENT_STATUS, currentStatus);
        args.putString(ARG_CUSTOM_STATUS, customStatus);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetStatusSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TokenManager tokenManager = new TokenManager(requireContext());
        String accessToken = tokenManager.getAccessToken();
        token = accessToken != null ? "Bearer " + accessToken : null;

        String currentStatus = getArguments() != null
                ? getArguments().getString(ARG_CURRENT_STATUS, "ONLINE") : "ONLINE";
        String customStatus = getArguments() != null
                ? getArguments().getString(ARG_CUSTOM_STATUS) : null;

        updateCheckmarks(currentStatus);

        if (!TextUtils.isEmpty(customStatus)) {
            binding.tvCurrentCustomStatus.setText(customStatus);
            binding.tvCurrentCustomStatus.setVisibility(View.VISIBLE);
        }

        binding.rowOnline.setOnClickListener(v -> selectStatus("ONLINE"));
        binding.rowIdle.setOnClickListener(v -> selectStatus("IDLE"));
        binding.rowDnd.setOnClickListener(v -> selectStatus("DND"));
        binding.rowInvisible.setOnClickListener(v -> selectStatus("OFFLINE"));

        binding.rowCustomStatus.setOnClickListener(v -> {
            dismiss();
            CustomStatusBottomSheet.newInstance(customStatus)
                    .show(getParentFragmentManager(), "custom_status_sheet");
        });
    }

    private void updateCheckmarks(String status) {
        binding.checkOnline.setVisibility(View.GONE);
        binding.checkIdle.setVisibility(View.GONE);
        binding.checkDnd.setVisibility(View.GONE);
        binding.checkInvisible.setVisibility(View.GONE);

        if (status == null) return;
        switch (status.toUpperCase()) {
            case "ONLINE":
                binding.checkOnline.setVisibility(View.VISIBLE);
                break;
            case "IDLE":
                binding.checkIdle.setVisibility(View.VISIBLE);
                break;
            case "DND":
                binding.checkDnd.setVisibility(View.VISIBLE);
                break;
            case "OFFLINE":
                binding.checkInvisible.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void selectStatus(String status) {
        if (token == null) return;

        Map<String, String> body = new HashMap<>();
        body.put("status", status);

        RetrofitClient.getApiService(requireContext()).updateStatus(token, body)
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call,
                                           Response<ApiResponse<Object>> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful()) {
                            TokenManager tm = new TokenManager(requireContext());
                            if (tm.getUser() != null) {
                                tm.getUser().setStatus(status);
                                tm.saveUser(tm.getUser());
                            }
                            StatusSelectionListener listener = findListener();
                            if (listener != null) {
                                listener.onStatusChanged(status);
                            }
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
    private StatusSelectionListener findListener() {
        if (getParentFragment() instanceof StatusSelectionListener) {
            return (StatusSelectionListener) getParentFragment();
        }
        if (getActivity() instanceof StatusSelectionListener) {
            return (StatusSelectionListener) getActivity();
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
