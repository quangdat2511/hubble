package com.example.hubble.view.voice;

import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Rational;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.hubble.R;
import com.example.hubble.adapter.voice.VoiceParticipantAdapter;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.api.ServerService;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.model.voice.VoiceParticipant;
import com.example.hubble.data.model.voice.VoiceSignalMessage;
import com.example.hubble.data.realtime.VoiceStompClient;
import com.example.hubble.data.realtime.WebRtcClient;
import com.example.hubble.databinding.ActivityVoiceCallBinding;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.view.server.InvitePeopleBottomSheet;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VoiceCallActivity extends AppCompatActivity {
    private static final String TAG = "VoiceCallActivity";

    private static final String EXTRA_CHANNEL_ID = "channel_id";
    private static final String EXTRA_CHANNEL_NAME = "channel_name";
    private static final String EXTRA_SERVER_ID = "server_id";
    private static final String EXTRA_SERVER_NAME = "server_name";
    private static final String EXTRA_MIC_ENABLED = "mic_enabled";

    private ActivityVoiceCallBinding binding;
    private VoiceParticipantAdapter adapter;

    private WebRtcClient webRtcClient;
    private VoiceStompClient voiceStompClient;
    private TokenManager tokenManager;

    private String channelId;
    private String channelName;
    private String serverId;
    private String serverName;
    private String currentUserId;
    private String currentDisplayName;
    private String currentAvatarUrl;

    private boolean micEnabled;
    private boolean cameraEnabled = false;
    private boolean speakerEnabled = false;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<VoiceParticipant> participants = new ArrayList<>();

    // PiP
    private static final String ACTION_PIP_END_CALL = "com.example.hubble.pip.END_CALL";
    private static final int PIP_REQUEST_CODE = 201;
    private BroadcastReceiver pipReceiver;

    /** Show debug status on screen (channel name) so signaling flow is visible without Logcat */
    private void showStatus(String status) {
        Log.w(TAG, "[STATUS] " + status);
        runOnUiThread(() -> {
            if (binding != null) {
                binding.tvChannelName.setText(channelName + " • " + status);
            }
        });
    }

    public static Intent createIntent(Context ctx, String channelId, String channelName,
                                       String serverId, String serverName, boolean micEnabled) {
        Intent intent = new Intent(ctx, VoiceCallActivity.class);
        intent.putExtra(EXTRA_CHANNEL_ID, channelId);
        intent.putExtra(EXTRA_CHANNEL_NAME, channelName);
        intent.putExtra(EXTRA_SERVER_ID, serverId);
        intent.putExtra(EXTRA_SERVER_NAME, serverName);
        intent.putExtra(EXTRA_MIC_ENABLED, micEnabled);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVoiceCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Extract extras
        channelId = getIntent().getStringExtra(EXTRA_CHANNEL_ID);
        channelName = getIntent().getStringExtra(EXTRA_CHANNEL_NAME);
        serverId = getIntent().getStringExtra(EXTRA_SERVER_ID);
        serverName = getIntent().getStringExtra(EXTRA_SERVER_NAME);
        micEnabled = getIntent().getBooleanExtra(EXTRA_MIC_ENABLED, true);

        // User info
        tokenManager = new TokenManager(this);
        UserResponse user = tokenManager.getUser();
        if (user != null) {
            currentUserId = user.getId();
            currentDisplayName = user.getDisplayName();
            currentAvatarUrl = user.getAvatarUrl();
        }

        setupUI();
        setupWebRtc();
        setupStomp();
        registerPipReceiver();
    }

    private void setupUI() {
        binding.tvChannelName.setText(channelName);

        // Top bar buttons
        binding.btnClose.setOnClickListener(v -> minimizeToBackground());
        binding.btnAddUser.setOnClickListener(v ->
                InvitePeopleBottomSheet.newInstance(serverId, serverName)
                        .show(getSupportFragmentManager(), "invite_people"));

        // Audio output toggle (speaker/earpiece)
        binding.btnAudioOutput.setOnClickListener(v -> toggleSpeaker());

        // Participants grid
        adapter = new VoiceParticipantAdapter();
        binding.rvParticipants.setLayoutManager(new GridLayoutManager(this, 3));
        binding.rvParticipants.setAdapter(adapter);

        // Bottom controls
        binding.btnMicToggle.setOnClickListener(v -> toggleMic());
        binding.btnCameraToggle.setOnClickListener(v -> toggleCamera());
        binding.btnEndCall.setOnClickListener(v -> leaveAndFinish());

        // Set initial mic state
        updateMicUI();
        updateCameraUI();
        updateSpeakerUI();

        // Add self to participants list
        if (currentUserId != null) {
            VoiceParticipant self = new VoiceParticipant(currentUserId, currentDisplayName, currentAvatarUrl);
            participants.add(self);
            adapter.submitList(new ArrayList<>(participants));
        }
    }

    private void setupWebRtc() {
        // Set audio mode for VoIP — critical for WebRTC audio routing
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(speakerEnabled);
        }

        webRtcClient = new WebRtcClient(this, new WebRtcClient.Listener() {
            @Override
            public void onIceCandidate(String peerId, IceCandidate candidate) {
                // Post to main thread — WebRTC callbacks fire on signaling thread
                mainHandler.post(() -> {
                    if (voiceStompClient != null) {
                        voiceStompClient.sendIceCandidate(currentUserId, peerId,
                                candidate.sdp, candidate.sdpMid, candidate.sdpMLineIndex);
                    }
                });
            }

            @Override
            public void onLocalOffer(String peerId, SessionDescription sdp) {
                mainHandler.post(() -> {
                    Log.d(TAG, "[WEBRTC] Sending offer to " + peerId);
                    showStatus("Offer ready, sending...");
                    if (voiceStompClient != null) {
                        voiceStompClient.sendOffer(currentUserId, peerId, sdp.description);
                        showStatus("Offer sent to peer");
                    } else {
                        showStatus("ERROR: stompClient null!");
                    }
                });
            }

            @Override
            public void onLocalAnswer(String peerId, SessionDescription sdp) {
                mainHandler.post(() -> {
                    Log.d(TAG, "[WEBRTC] Sending answer to " + peerId);
                    if (voiceStompClient != null) {
                        voiceStompClient.sendAnswer(currentUserId, peerId, sdp.description);
                    }
                });
            }

            @Override
            public void onPeerDisconnected(String peerId) {
                mainHandler.post(() -> removePeerFromUI(peerId));
            }

            @Override
            public void onAudioLevelChanged(String peerId, boolean speaking) {
                mainHandler.post(() -> adapter.updateSpeaking(peerId, speaking));
            }
        });

        webRtcClient.startLocalMedia();
        webRtcClient.setMicEnabled(micEnabled);
        webRtcClient.startAudioLevelMonitoring();
    }

    private void setupStomp() {
        showStatus("Connecting...");
        voiceStompClient = new VoiceStompClient(tokenManager);
        voiceStompClient.connect(channelId, currentUserId, new VoiceStompClient.Listener() {
            @Override
            public void onSignalMessage(VoiceSignalMessage msg) {
                mainHandler.post(() -> handleSignalMessage(msg));
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "STOMP error: " + message);
                showStatus("STOMP: " + message);
            }

            @Override
            public void onConnected() {
                Log.d(TAG, "STOMP connected, sending join after brief delay");
                showStatus("Connected");
                // Small delay to allow SUBSCRIBE frames to be processed by broker
                mainHandler.postDelayed(() -> {
                    showStatus("Joining...");
                    voiceStompClient.sendJoin(currentUserId, currentDisplayName, currentAvatarUrl);
                    showStatus("Joined, finding peers...");
                    // First load after a short delay to give JOIN time to reach the server
                    mainHandler.postDelayed(() -> loadExistingParticipants(), 300);
                    // Retry load in case peers join slightly after us
                    mainHandler.postDelayed(() -> loadExistingParticipants(), 3000);
                }, 500);
            }
        });
    }

    private void handleSignalMessage(VoiceSignalMessage msg) {
        if (msg == null || msg.getType() == null) return;
        String senderId = msg.getUserId();
        Log.d(TAG, "[SIGNAL] Received type=" + msg.getType() + " from=" + senderId
                + " target=" + msg.getTargetUserId());
        if (currentUserId.equals(senderId)) {
            Log.d(TAG, "[SIGNAL] Ignoring own message type=" + msg.getType());
            return;
        }

        switch (msg.getType()) {
            case "join":
                onPeerJoined(msg);
                break;
            case "leave":
                onPeerLeft(msg);
                break;
            case "offer":
                onOfferReceived(msg);
                break;
            case "answer":
                onAnswerReceived(msg);
                break;
            case "ice-candidate":
                onIceCandidateReceived(msg);
                break;
        }
    }

    private void onPeerJoined(VoiceSignalMessage msg) {
        String peerId = msg.getUserId();
        Log.d(TAG, "[PEER] Joined: " + peerId + " displayName=" + msg.getDisplayName());

        // Add to participants list if not already there
        boolean exists = false;
        for (VoiceParticipant p : participants) {
            if (p.getUserId().equals(peerId)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            participants.add(new VoiceParticipant(peerId, msg.getDisplayName(), msg.getAvatarUrl()));
            adapter.submitList(new ArrayList<>(participants));
        }

        // When we receive a JOIN broadcast, DON'T create an offer.
        // The joining user will discover us via REST and send us an offer.
        // We just prepare a PeerConnection to answer.
        Log.d(TAG, "[PEER] Creating PC for " + peerId + " (will wait for their offer)");
        webRtcClient.createPeerConnection(peerId, false);
    }

    private void onPeerLeft(VoiceSignalMessage msg) {
        removePeerFromUI(msg.getUserId());
        webRtcClient.removePeer(msg.getUserId());
    }

    private void onOfferReceived(VoiceSignalMessage msg) {
        Log.d(TAG, "[WEBRTC] Offer received from " + msg.getUserId());
        SessionDescription sdp = new SessionDescription(
                SessionDescription.Type.OFFER, msg.getSdp());
        webRtcClient.setRemoteOffer(msg.getUserId(), sdp);
    }

    private void onAnswerReceived(VoiceSignalMessage msg) {
        Log.d(TAG, "[WEBRTC] Answer received from " + msg.getUserId());
        SessionDescription sdp = new SessionDescription(
                SessionDescription.Type.ANSWER, msg.getSdp());
        webRtcClient.setRemoteAnswer(msg.getUserId(), sdp);
    }

    private void onIceCandidateReceived(VoiceSignalMessage msg) {
        Log.d(TAG, "[WEBRTC] ICE candidate received from " + msg.getUserId());
        IceCandidate candidate = new IceCandidate(
                msg.getSdpMid(),
                msg.getSdpMLineIndex() != null ? msg.getSdpMLineIndex() : 0,
                msg.getCandidate());
        webRtcClient.addIceCandidate(msg.getUserId(), candidate);
    }

    private void removePeerFromUI(String peerId) {
        participants.removeIf(p -> p.getUserId().equals(peerId));
        adapter.submitList(new ArrayList<>(participants));
    }

    private void loadExistingParticipants() {
        String token = "Bearer " + tokenManager.getAccessToken();
        ServerService service = RetrofitClient.getServerService(this);
        service.getVoiceParticipants(token, channelId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<VoiceParticipant>>> call,
                                   @NonNull Response<ApiResponse<List<VoiceParticipant>>> response) {
                if (isFinishing()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    int peerCount = 0;
                    for (VoiceParticipant p : response.body().getResult()) {
                        if (p.getUserId().equals(currentUserId)) continue;
                        boolean exists = false;
                        for (VoiceParticipant existing : participants) {
                            if (existing.getUserId().equals(p.getUserId())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            participants.add(p);
                            peerCount++;
                            // The joining user ALWAYS creates offers to existing participants.
                            // Existing users will answer when they receive the offer.
                            Log.d(TAG, "[REST] Discovered peer " + p.getUserId() + ", creating offer");
                            showStatus("Found peer, creating PC+offer...");
                            try {
                                webRtcClient.createPeerConnection(p.getUserId(), true);
                            } catch (Exception e) {
                                Log.e(TAG, "createPeerConnection crashed", e);
                                showStatus("ERROR: PC creation failed!");
                            }
                        }
                    }
                    if (peerCount == 0) {
                        showStatus("No new peers found");
                    }
                    adapter.submitList(new ArrayList<>(participants));
                } else {
                    showStatus("REST failed: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<VoiceParticipant>>> call,
                                  @NonNull Throwable t) {
                Log.e(TAG, "Failed to load participants", t);
                showStatus("REST error: " + t.getMessage());
            }
        });
    }

    private void toggleMic() {
        micEnabled = !micEnabled;
        webRtcClient.setMicEnabled(micEnabled);
        updateMicUI();
    }

    private void toggleCamera() {
        cameraEnabled = !cameraEnabled;
        if (cameraEnabled) {
            webRtcClient.startCamera(null);
        }
        webRtcClient.toggleCamera();
        updateCameraUI();
    }

    private void toggleSpeaker() {
        speakerEnabled = !speakerEnabled;
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(speakerEnabled);
        }
        updateSpeakerUI();
    }

    private void updateMicUI() {
        if (micEnabled) {
            binding.btnMicToggle.setImageResource(R.drawable.ic_mic_on);
            binding.btnMicToggle.setColorFilter(
                    ContextCompat.getColor(this, R.color.color_text_primary));
        } else {
            binding.btnMicToggle.setImageResource(R.drawable.ic_mic_off);
            binding.btnMicToggle.setColorFilter(
                    ContextCompat.getColor(this, R.color.color_error));
        }
    }

    private void updateCameraUI() {
        if (cameraEnabled) {
            binding.btnCameraToggle.setImageResource(R.drawable.ic_videocam);
            binding.btnCameraToggle.setColorFilter(
                    ContextCompat.getColor(this, R.color.color_text_primary));
        } else {
            binding.btnCameraToggle.setImageResource(R.drawable.ic_videocam_off);
            binding.btnCameraToggle.setColorFilter(
                    ContextCompat.getColor(this, R.color.color_text_primary));
        }
    }

    private void updateSpeakerUI() {
        if (speakerEnabled) {
            binding.btnAudioOutput.setColorFilter(
                    ContextCompat.getColor(this, R.color.color_success));
        } else {
            binding.btnAudioOutput.setColorFilter(
                    ContextCompat.getColor(this, R.color.color_text_primary));
        }
    }

    private void leaveAndFinish() {
        if (voiceStompClient != null && currentUserId != null) {
            voiceStompClient.sendLeave(currentUserId);
        }
        finish();
    }

    // ── Picture-in-Picture ──────────────────────────────────────────────

    private void minimizeToBackground() {
        enterPictureInPictureMode(buildPipParams());
    }

    private PictureInPictureParams buildPipParams() {
        Intent endCallIntent = new Intent(ACTION_PIP_END_CALL).setPackage(getPackageName());
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                this, PIP_REQUEST_CODE, endCallIntent,
                android.app.PendingIntent.FLAG_IMMUTABLE);

        RemoteAction endAction = new RemoteAction(
                Icon.createWithResource(this, R.drawable.ic_call_end),
                getString(R.string.end_call),
                getString(R.string.end_call),
                pendingIntent);

        return new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(3, 4))
                .setActions(Collections.singletonList(endAction))
                .build();
    }

    private void registerPipReceiver() {
        pipReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_PIP_END_CALL.equals(intent.getAction())) {
                    leaveAndFinish();
                }
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_PIP_END_CALL);
        ContextCompat.registerReceiver(this, pipReceiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private void unregisterPipReceiver() {
        if (pipReceiver != null) {
            unregisterReceiver(pipReceiver);
            pipReceiver = null;
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPip, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPip, newConfig);
        int visibility = isInPip ? View.GONE : View.VISIBLE;
        binding.topBar.setVisibility(visibility);
        binding.bottomBar.setVisibility(visibility);

        // Fewer columns + hide names so avatars fit without overlapping in the mini window
        binding.rvParticipants.setLayoutManager(
                new GridLayoutManager(this, isInPip ? 2 : 3));
        adapter.setCompactMode(isInPip);

        // Reduce RecyclerView padding in PiP to use space efficiently
        int padding = isInPip
                ? (int) (4 * getResources().getDisplayMetrics().density)
                : getResources().getDimensionPixelSize(R.dimen.spacing_md);
        binding.rvParticipants.setPadding(padding, padding, padding, padding);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // Auto-enter PiP when user presses Home or switches apps
        enterPictureInPictureMode(buildPipParams());
    }

    @Override
    public void onBackPressed() {
        // Back in full-screen → minimize; Back while in PiP → system handles dismissal
        if (!isInPictureInPictureMode()) {
            minimizeToBackground();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterPipReceiver();
        mainHandler.removeCallbacksAndMessages(null);
        if (voiceStompClient != null) {
            if (currentUserId != null) {
                voiceStompClient.sendLeave(currentUserId);
            }
            voiceStompClient.disconnect();
        }
        if (webRtcClient != null) {
            webRtcClient.dispose();
        }
        // Reset audio
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(false);
        }
    }
}
