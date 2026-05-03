package com.example.hubble.data.realtime;

import android.content.Context;
import android.util.Log;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;

import java.util.HashSet;
import java.util.Set;

/**
 * Thin wrapper around the Agora RtcEngine for voice-only calls.
 *
 * All public methods MUST be called from the main thread.
 * Listener callbacks are delivered on the Agora internal thread —
 * callers are responsible for posting to the main thread if needed.
 */
public class AgoraVoiceClient {
    private static final String TAG = "AgoraVoiceClient";

    // ── Listener interface ──────────────────────────────────────────────────

    public interface Listener {
        /** Called when we successfully joined the Agora channel. */
        void onJoinSuccess(int uid, int elapsed);

        /** Called when a remote participant joins the channel. */
        void onParticipantJoined(int uid, int elapsed);

        /** Called when a remote participant leaves the channel. */
        void onParticipantLeft(int uid, int reason);

        /** Called when a remote user's speaking state changes. */
        void onSpeakingChanged(int uid, boolean speaking);

        /** Called when the connection state changes. */
        void onConnectionStateChanged(int state, int reason);

        /** Called when an SDK error occurs. */
        void onError(int errorCode, String description);
    }

    // ── State ──────────────────────────────────────────────────────────────

    private RtcEngine rtcEngine;
    private Listener listener;
    private boolean micEnabled = true;
    private boolean speakerphoneOn = true;
    private boolean inChannel = false;
    // Track which UIDs were speaking in last volume callback to detect silence
    private final Set<Integer> lastSpeakingUids = new HashSet<>();

    // ── Lifecycle ──────────────────────────────────────────────────────────

    /**
     * Initialise the Agora engine. Must be called once before any other method.
     *
     * @param context  Application context
     * @param appId    Agora App ID (received from the backend token endpoint)
     * @param listener Callback listener
     */
    public void initialize(Context context, String appId, Listener listener) {
        this.listener = listener;

        RtcEngineConfig config = new RtcEngineConfig();
        config.mContext = context.getApplicationContext();
        config.mAppId = appId;
        config.mEventHandler = engineEventHandler;

        try {
            rtcEngine = RtcEngine.create(config);
            // Voice-only profile: lower latency, no video overhead
            rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
            // MEETING scenario: low-latency AEC suitable for small group calls.
            // CHATROOM has heavier processing (higher latency); MEETING balances
            // echo cancellation and delay for 1-on-1 / small group voice.
            rtcEngine.setAudioScenario(Constants.AUDIO_SCENARIO_MEETING);
            rtcEngine.enableAudio();
            rtcEngine.disableVideo();
            rtcEngine.setDefaultAudioRoutetoSpeakerphone(true);
            // Enable volume indicator every 200ms, smooth=3 (recommended by Agora).
            // reportVad=true to include local user in speakers array.
            rtcEngine.enableAudioVolumeIndication(200, 3, true);
            Log.d(TAG, "RtcEngine initialized, SDK version=" + RtcEngine.getSdkVersion());
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize RtcEngine", e);
            if (listener != null) {
                listener.onError(-1, "Engine init failed: " + e.getMessage());
            }
        }
    }

    /**
     * Join an Agora voice channel.
     *
     * @param channelId The channel name (same as Hubble channelId)
     * @param uid       The local user's Agora UID (from AgoraUidMapper)
     * @param token     Short-lived token from the backend
     */
    public void joinChannel(String channelId, int uid, String token) {
        if (rtcEngine == null) {
            Log.e(TAG, "joinChannel called before initialize()");
            return;
        }
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        options.publishMicrophoneTrack = true;
        options.autoSubscribeAudio = true;

        int result = rtcEngine.joinChannel(token, channelId, uid, options);
        Log.d(TAG, "joinChannel result=" + result + " channelId=" + channelId + " uid=" + uid
                + (result == 0 ? " [OK - waiting for onJoinChannelSuccess]" : " [FAILED - check error code]"));
    }

    /**
     * Leave the current channel. Safe to call even when not in a channel.
     */
    public void leaveChannel() {
        if (rtcEngine != null && inChannel) {
            rtcEngine.leaveChannel();
            inChannel = false;
            Log.d(TAG, "leaveChannel called");
        }
    }

    /**
     * Destroy the engine and release all resources.
     * After this call the object must not be reused.
     */
    public void destroy() {
        leaveChannel();
        if (rtcEngine != null) {
            RtcEngine.destroy();
            rtcEngine = null;
            Log.d(TAG, "RtcEngine destroyed");
        }
        listener = null;
    }

    // ── Audio controls ─────────────────────────────────────────────────────

    public void setMicEnabled(boolean enabled) {
        micEnabled = enabled;
        if (rtcEngine != null) {
            rtcEngine.muteLocalAudioStream(!enabled);
        }
    }

    public boolean isMicEnabled() {
        return micEnabled;
    }

    public void setSpeakerphoneOn(boolean on) {
        speakerphoneOn = on;
        if (rtcEngine != null) {
            rtcEngine.setEnableSpeakerphone(on);
        }
    }

    public boolean isSpeakerphoneOn() {
        return speakerphoneOn;
    }

    // ── Internal event handler ─────────────────────────────────────────────

    private final IRtcEngineEventHandler engineEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            inChannel = true;
            Log.d(TAG, "onJoinChannelSuccess channel=" + channel + " uid=" + uid);
            if (listener != null) listener.onJoinSuccess(uid, elapsed);
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            Log.d(TAG, "onUserJoined uid=" + uid);
            if (listener != null) listener.onParticipantJoined(uid, elapsed);
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            Log.d(TAG, "onUserOffline uid=" + uid + " reason=" + reason);
            if (listener != null) listener.onParticipantLeft(uid, reason);
        }

        @Override
        public void onActiveSpeaker(int uid) {
            if (listener == null) return;
            Log.d(TAG, "onActiveSpeaker: uid=" + uid);
            // Fired by Agora for the loudest remote speaker — more reliable than
            // onAudioVolumeIndication for cross-device detection.
            lastSpeakingUids.add(uid);
            listener.onSpeakingChanged(uid, true);
        }

        @Override
        public void onAudioVolumeIndication(AudioVolumeInfo[] speakers, int totalVolume) {
            if (listener == null) return;
            if (speakers == null) {
                for (int prevUid : lastSpeakingUids) {
                    listener.onSpeakingChanged(prevUid, false);
                }
                lastSpeakingUids.clear();
                return;
            }

            Set<Integer> currentlySpeaking = new HashSet<>();
            for (AudioVolumeInfo info : speakers) {
                // Skip uid=0 when mic is muted — Agora still captures locally
                // for VAD even after muteLocalAudioStream(true).
                if (info.uid == 0 && !micEnabled) continue;

                // Lower threshold for remote users since received volume is typically
                // lower than local capture volume.
                int threshold = (info.uid == 0) ? 20 : 5;
                if (info.volume > threshold) {
                    currentlySpeaking.add(info.uid);
                    listener.onSpeakingChanged(info.uid, true);
                }
            }
            // Any UID that was speaking last frame but not this frame → stopped speaking
            for (int prevUid : lastSpeakingUids) {
                if (!currentlySpeaking.contains(prevUid)) {
                    listener.onSpeakingChanged(prevUid, false);
                }
            }
            lastSpeakingUids.clear();
            lastSpeakingUids.addAll(currentlySpeaking);
        }

        @Override
        public void onConnectionStateChanged(int state, int reason) {
            Log.d(TAG, "onConnectionStateChanged state=" + state + " reason=" + reason);
            if (listener != null) listener.onConnectionStateChanged(state, reason);
        }

        @Override
        public void onError(int err) {
            String desc = RtcEngine.getErrorDescription(err);
            Log.e(TAG, "Agora error " + err + ": " + desc);
            if (listener != null) listener.onError(err, desc);
        }
    };
}
