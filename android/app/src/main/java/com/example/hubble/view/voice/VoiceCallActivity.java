package com.example.hubble.view.voice;

import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Rational;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.hubble.R;
import com.example.hubble.adapter.voice.VoiceParticipantAdapter;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.api.ServerService;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.model.voice.AgoraTokenResponse;
import com.example.hubble.data.model.voice.VoiceParticipant;
import com.example.hubble.data.realtime.AgoraVoiceClient;
import com.example.hubble.databinding.ActivityVoiceCallBinding;
import com.example.hubble.utils.AgoraUidMapper;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.view.server.InvitePeopleBottomSheet;

import io.agora.rtc2.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private AgoraVoiceClient agoraVoiceClient;
    private TokenManager tokenManager;

    private String channelId;
    private String channelName;
    private String serverId;
    private String serverName;
    private String currentUserId;
    private String currentDisplayName;
    private String currentAvatarUrl;

    private boolean micEnabled;
    private boolean speakerEnabled = true;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<VoiceParticipant> participants = new ArrayList<>();

    // Debounce: delay turning off the speaking ring for remote users to smooth out
    // network jitter (Agora volume indication can briefly drop → ring would flicker).
    private static final int REMOTE_SPEAKING_HOLD_MS = 600;
    private final Map<String, Runnable> pendingSpeakingOff = new HashMap<>();

    // Heartbeat: keep the backend participant list alive; the server removes any
    // participant whose heartbeat is older than 30 s (handles crash / process kill).
    private static final int HEARTBEAT_INTERVAL_MS = 15_000;
    private final Runnable heartbeatRunnable = this::sendHeartbeat;
    /** Prevents calling the leave API twice (once from leaveAndFinish, once from onDestroy). */
    private boolean hasLeft = false;

    // PiP
    private static final String ACTION_PIP_END_CALL = "com.example.hubble.pip.END_CALL";
    private static final int PIP_REQUEST_CODE = 201;
    private BroadcastReceiver pipReceiver;

    /** Log connection state changes without touching the channel name UI. */
    private void showStatus(String status) {
        Log.w(TAG, "[STATUS] " + status);
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
        fetchTokenAndJoinAgora();
        registerPipReceiver();
    }

    private void setupUI() {
        binding.tvChannelName.setText(channelName);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!isInPictureInPictureMode()) {
                    minimizeToBackground();
                    return;
                }

                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        });

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
        binding.btnCameraToggle.setOnClickListener(v -> {
            // Camera not supported in voice-only mode; show a hint
            Toast.makeText(this, R.string.camera_not_supported, Toast.LENGTH_SHORT).show();
        });
        binding.btnEndCall.setOnClickListener(v -> leaveAndFinish());

        // Set initial state
        updateMicUI();
        updateSpeakerUI();

        // Add self to participants list immediately
        if (currentUserId != null) {
            VoiceParticipant self = new VoiceParticipant(
                    currentUserId, currentDisplayName, currentAvatarUrl);
            participants.add(self);
            adapter.submitList(new ArrayList<>(participants));
        }
    }

    // ── Agora flow ─────────────────────────────────────────────────────────

    private void fetchTokenAndJoinAgora() {
        showStatus("Connecting...");
        String authHeader = "Bearer " + tokenManager.getAccessToken();
        ServerService service = RetrofitClient.getServerService(this);

        service.getAgoraToken(authHeader, channelId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AgoraTokenResponse>> call,
                                   @NonNull Response<ApiResponse<AgoraTokenResponse>> response) {
                if (isFinishing()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    AgoraTokenResponse tokenResp = response.body().getResult();
                    setupAgoraClient(tokenResp.getAppId(), tokenResp.getToken(), tokenResp.getUid());
                } else {
                    showStatus("Token fetch failed: " + response.code());
                    Log.e(TAG, "Agora token error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AgoraTokenResponse>> call,
                                  @NonNull Throwable t) {
                if (isFinishing()) return;
                showStatus("Network error");
                Log.e(TAG, "Agora token network error", t);
            }
        });
    }

    private void setupAgoraClient(String appId, String token, int uid) {
        agoraVoiceClient = new AgoraVoiceClient();
        agoraVoiceClient.initialize(this, appId, new AgoraVoiceClient.Listener() {

            @Override
            public void onJoinSuccess(int uid, int elapsed) {
                mainHandler.post(() -> {
                    Log.d(TAG, "Agora joined successfully uid=" + uid);
                    showStatus("Connected");
                    // Load server-side participant list to populate the grid
                    loadExistingParticipants();
                    // Start periodic heartbeat so the backend knows we're alive
                    mainHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS);
                });
            }

            @Override
            public void onParticipantJoined(int agoraUid, int elapsed) {
                mainHandler.post(() -> {
                    Log.d(TAG, "Participant joined agoraUid=" + agoraUid);
                    // Refresh participant list from backend — it has display names/avatars
                    loadExistingParticipants();
                });
            }

            @Override
            public void onParticipantLeft(int agoraUid, int reason) {
                mainHandler.post(() -> {
                    Log.d(TAG, "Participant left agoraUid=" + agoraUid + " reason=" + reason);
                    // Remove by matching Agora UID back to a userId
                    participants.removeIf(p -> AgoraUidMapper.toAgoraUid(p.getUserId()) == agoraUid);
                    adapter.submitList(new ArrayList<>(participants));
                });
            }

            @Override
            public void onSpeakingChanged(int agoraUid, boolean speaking) {
                mainHandler.post(() -> {
                    if (agoraUid == 0) {
                        // Local user — no debounce needed, local mic data is reliable
                        adapter.updateSpeaking(currentUserId, speaking);
                    } else {
                        // Remote user — find userId first
                        String remoteUserId = null;
                        for (VoiceParticipant p : participants) {
                            if (AgoraUidMapper.toAgoraUid(p.getUserId()) == agoraUid) {
                                remoteUserId = p.getUserId();
                                break;
                            }
                        }
                        if (remoteUserId == null) return;

                        final String userId = remoteUserId;
                        if (speaking) {
                            // Cancel any pending "stop speaking" — keep ring ON immediately
                            Runnable pending = pendingSpeakingOff.remove(userId);
                            if (pending != null) mainHandler.removeCallbacks(pending);
                            adapter.updateSpeaking(userId, true);
                        } else {
                            // Delay turning off to absorb network jitter gaps
                            if (!pendingSpeakingOff.containsKey(userId)) {
                                Runnable off = () -> {
                                    pendingSpeakingOff.remove(userId);
                                    adapter.updateSpeaking(userId, false);
                                };
                                pendingSpeakingOff.put(userId, off);
                                mainHandler.postDelayed(off, REMOTE_SPEAKING_HOLD_MS);
                            }
                        }
                    }
                });
            }

            @Override
            public void onConnectionStateChanged(int state, int reason) {
                mainHandler.post(() -> {
                    if (state == Constants.CONNECTION_STATE_FAILED) {
                        showStatus("Connection failed");
                    } else if (state == Constants.CONNECTION_STATE_RECONNECTING) {
                        showStatus("Reconnecting...");
                    } else if (state == Constants.CONNECTION_STATE_CONNECTED) {
                        showStatus(channelName);
                    }
                });
            }

            @Override
            public void onError(int errorCode, String description) {
                mainHandler.post(() -> {
                    Log.e(TAG, "Agora error " + errorCode + ": " + description);
                    showStatus("Error " + errorCode);
                });
            }
        });

        agoraVoiceClient.setMicEnabled(micEnabled);
        agoraVoiceClient.setSpeakerphoneOn(speakerEnabled);
        agoraVoiceClient.joinChannel(channelId, uid, token);
    }

    private void loadExistingParticipants() {
        String authHeader = "Bearer " + tokenManager.getAccessToken();
        ServerService service = RetrofitClient.getServerService(this);
        service.getVoiceParticipants(authHeader, channelId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<VoiceParticipant>>> call,
                                   @NonNull Response<ApiResponse<List<VoiceParticipant>>> response) {
                if (isFinishing()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    List<VoiceParticipant> serverList = response.body().getResult();
                    // Merge server list into local list, keeping the "self" entry
                    participants.clear();
                    if (currentUserId != null) {
                        participants.add(new VoiceParticipant(
                                currentUserId, currentDisplayName, currentAvatarUrl));
                    }
                    for (VoiceParticipant p : serverList) {
                        if (!p.getUserId().equals(currentUserId)) {
                            participants.add(p);
                        }
                    }
                    adapter.submitList(new ArrayList<>(participants));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<VoiceParticipant>>> call,
                                  @NonNull Throwable t) {
                Log.e(TAG, "Failed to load participants", t);
            }
        });
    }

    // ── Controls ───────────────────────────────────────────────────────────

    private void toggleMic() {
        micEnabled = !micEnabled;
        if (agoraVoiceClient != null) {
            agoraVoiceClient.setMicEnabled(micEnabled);
        }
        updateMicUI();
    }

    private void toggleSpeaker() {
        speakerEnabled = !speakerEnabled;
        if (agoraVoiceClient != null) {
            agoraVoiceClient.setSpeakerphoneOn(speakerEnabled);
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
        notifyBackendLeave();
        if (agoraVoiceClient != null) {
            agoraVoiceClient.leaveChannel();
        }
        finish();
    }

    /** Sends the leave API call once, idempotent via hasLeft flag. */
    private void notifyBackendLeave() {
        if (hasLeft || channelId == null || tokenManager == null) return;
        hasLeft = true;
        // Stop heartbeat immediately so the scheduler won't see us as active
        mainHandler.removeCallbacks(heartbeatRunnable);
        String authHeader = "Bearer " + tokenManager.getAccessToken();
        RetrofitClient.getServerService(this)
                .leaveVoiceChannel(authHeader, channelId)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> response) {
                        Log.d(TAG, "Leave voice channel notified");
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call,
                                          @NonNull Throwable t) {
                        Log.w(TAG, "Leave voice channel notify failed", t);
                    }
                });
    }

    /** Sends a keep-alive heartbeat and reschedules itself. */
    private void sendHeartbeat() {
        if (hasLeft || channelId == null || tokenManager == null) return;
        String authHeader = "Bearer " + tokenManager.getAccessToken();
        RetrofitClient.getServerService(this)
                .heartbeatVoiceChannel(authHeader, channelId)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> response) {
                        if (!hasLeft) mainHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS);
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                        if (!hasLeft) mainHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS);
                    }
                });
    }

    // ── Picture-in-Picture ──────────────────────────────────────────────────

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

        binding.rvParticipants.setLayoutManager(
                new GridLayoutManager(this, isInPip ? 2 : 3));
        adapter.setCompactMode(isInPip);

        int padding = isInPip
                ? (int) (4 * getResources().getDisplayMetrics().density)
                : getResources().getDimensionPixelSize(R.dimen.spacing_md);
        binding.rvParticipants.setPadding(padding, padding, padding, padding);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        enterPictureInPictureMode(buildPipParams());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterPipReceiver();
        // Best-effort leave: covers back-press, swipe from recents, and low-memory kills.
        // (Hard crashes / SIGKILL are handled by the backend's 30 s stale-participant cleanup.)
        notifyBackendLeave();
        mainHandler.removeCallbacksAndMessages(null);
        if (agoraVoiceClient != null) {
            agoraVoiceClient.leaveChannel();
            agoraVoiceClient.destroy();
            agoraVoiceClient = null;
        }
    }
}
