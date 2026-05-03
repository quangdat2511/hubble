package com.example.hubble.view.voice;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.hubble.R;
import com.example.hubble.adapter.voice.VoiceParticipantAdapter;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.api.ServerService;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.voice.VoiceParticipant;
import com.example.hubble.databinding.BottomSheetVoiceChannelBinding;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.view.server.InvitePeopleBottomSheet;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VoiceChannelBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_CHANNEL_ID = "channel_id";
    private static final String ARG_CHANNEL_NAME = "channel_name";
    private static final String ARG_SERVER_ID = "server_id";
    private static final String ARG_SERVER_NAME = "server_name";

    private BottomSheetVoiceChannelBinding binding;
    private VoiceParticipantAdapter adapter;
    private boolean micEnabled = true;

    private final ActivityResultLauncher<String> micPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    joinVoiceChannel();
                } else {
                    Snackbar.make(binding.getRoot(), R.string.voice_permission_required,
                            Snackbar.LENGTH_SHORT).show();
                }
            });

    public static VoiceChannelBottomSheet newInstance(String channelId, String channelName,
                                                       String serverId, String serverName) {
        VoiceChannelBottomSheet sheet = new VoiceChannelBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_CHANNEL_ID, channelId);
        args.putString(ARG_CHANNEL_NAME, channelName);
        args.putString(ARG_SERVER_ID, serverId);
        args.putString(ARG_SERVER_NAME, serverName);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetVoiceChannelBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() == null) return;

        String channelId = getArguments().getString(ARG_CHANNEL_ID);
        String channelName = getArguments().getString(ARG_CHANNEL_NAME);
        String serverId = getArguments().getString(ARG_SERVER_ID);
        String serverName = getArguments().getString(ARG_SERVER_NAME);

        // Top bar
        binding.tvChannelName.setText(channelName);
        binding.btnClose.setOnClickListener(v -> dismiss());

        binding.btnAddUser.setOnClickListener(v -> {
            dismiss();
            InvitePeopleBottomSheet.newInstance(serverId, serverName)
                    .show(requireActivity().getSupportFragmentManager(), "invite_people");
        });

        // Participant list
        adapter = new VoiceParticipantAdapter();
        binding.rvParticipants.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.rvParticipants.setAdapter(adapter);

        // Mic toggle
        updateMicButton();
        binding.btnMicToggle.setOnClickListener(v -> {
            micEnabled = !micEnabled;
            updateMicButton();
        });

        // Join button
        binding.btnJoin.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            } else {
                joinVoiceChannel();
            }
        });

        // Load current participants
        loadParticipants(channelId);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload whenever the sheet becomes visible — covers the case where the user
        // left the voice call and returns to this screen without dismissing it first.
        if (getArguments() != null) {
            loadParticipants(getArguments().getString(ARG_CHANNEL_ID));
        }
    }

    private void loadParticipants(String channelId) {
        TokenManager tm = new TokenManager(requireContext());
        String token = "Bearer " + tm.getAccessToken();

        ServerService service = RetrofitClient.getServerService(requireContext());
        service.getVoiceParticipants(token, channelId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<VoiceParticipant>>> call,
                                   @NonNull Response<ApiResponse<List<VoiceParticipant>>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    List<VoiceParticipant> participants = response.body().getResult();
                    if (participants.isEmpty()) {
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                        binding.tvEmptyHint.setVisibility(View.VISIBLE);
                        binding.rvParticipants.setVisibility(View.GONE);
                    } else {
                        binding.tvEmptyState.setVisibility(View.GONE);
                        binding.tvEmptyHint.setVisibility(View.GONE);
                        binding.rvParticipants.setVisibility(View.VISIBLE);
                        adapter.submitList(participants);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<VoiceParticipant>>> call,
                                  @NonNull Throwable t) {
                if (!isAdded()) return;
                binding.tvEmptyState.setVisibility(View.VISIBLE);
                binding.tvEmptyHint.setVisibility(View.VISIBLE);
                binding.rvParticipants.setVisibility(View.GONE);
            }
        });
    }

    private void joinVoiceChannel() {
        if (getArguments() == null) return;
        String channelId = getArguments().getString(ARG_CHANNEL_ID);
        String channelName = getArguments().getString(ARG_CHANNEL_NAME);
        String serverId = getArguments().getString(ARG_SERVER_ID);
        String serverName = getArguments().getString(ARG_SERVER_NAME);

        startActivity(VoiceCallActivity.createIntent(
                requireContext(), channelId, channelName, serverId, serverName, micEnabled));
    }

    private void updateMicButton() {
        if (micEnabled) {
            binding.btnMicToggle.setImageResource(R.drawable.ic_mic_on);
            binding.btnMicToggle.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.color_text_primary));
        } else {
            binding.btnMicToggle.setImageResource(R.drawable.ic_mic_off);
            binding.btnMicToggle.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.color_error));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            // Make it full screen
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheet.requestLayout();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
