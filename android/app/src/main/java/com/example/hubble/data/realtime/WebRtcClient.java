package com.example.hubble.data.realtime;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.webrtc.*;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebRtcClient {
    private static final String TAG = "WebRtcClient";

    private static final List<PeerConnection.IceServer> ICE_SERVERS = List.of(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
    );

    private final Context context;
    private final Listener listener;
    private PeerConnectionFactory peerConnectionFactory;
    private EglBase eglBase;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;
    private VideoSource videoSource;
    private VideoTrack localVideoTrack;
    private VideoCapturer videoCapturer;
    private SurfaceTextureHelper surfaceTextureHelper;

    // peerId → PeerConnection
    private final Map<String, PeerConnection> peerConnections = new ConcurrentHashMap<>();
    // peerId → pending ICE candidates (received before remote description set)
    private final Map<String, List<IceCandidate>> pendingCandidates = new ConcurrentHashMap<>();

    private boolean micEnabled = true;
    private boolean cameraEnabled = false;
    private boolean disposed = false;

    // Audio level monitoring
    private static final long AUDIO_LEVEL_POLL_INTERVAL_MS = 300;
    private static final double SPEAKING_THRESHOLD = 0.01;
    private final Handler audioLevelHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Boolean> speakingStates = new ConcurrentHashMap<>();
    private final Runnable audioLevelPoller = this::pollAudioLevels;

    public interface Listener {
        void onIceCandidate(String peerId, IceCandidate candidate);
        void onLocalOffer(String peerId, SessionDescription sdp);
        void onLocalAnswer(String peerId, SessionDescription sdp);
        void onPeerDisconnected(String peerId);
        void onAudioLevelChanged(String peerId, boolean speaking);
    }

    public WebRtcClient(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        initPeerConnectionFactory();
    }

    private void initPeerConnectionFactory() {
        eglBase = EglBase.create();

        PeerConnectionFactory.InitializationOptions initOptions =
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .setEnableInternalTracer(false)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        JavaAudioDeviceModule audioDeviceModule = JavaAudioDeviceModule.builder(context)
                .setUseHardwareAcousticEchoCanceler(true)
                .setUseHardwareNoiseSuppressor(true)
                .createAudioDeviceModule();
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(audioDeviceModule)
                .createPeerConnectionFactory();
    }

    public void startLocalMedia() {
        // Audio — enable echo cancellation, noise suppression, and auto gain
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio0", audioSource);
        localAudioTrack.setEnabled(micEnabled);
    }

    public void startCamera(SurfaceViewRenderer localRenderer) {
        if (videoCapturer != null) return;

        videoCapturer = createCameraCapturer();
        if (videoCapturer == null) {
            Log.w(TAG, "No camera available");
            return;
        }

        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());
        videoCapturer.startCapture(640, 480, 30);

        localVideoTrack = peerConnectionFactory.createVideoTrack("video0", videoSource);
        localVideoTrack.setEnabled(cameraEnabled);

        if (localRenderer != null) {
            localRenderer.init(eglBase.getEglBaseContext(), null);
            localRenderer.setMirror(true);
            localVideoTrack.addSink(localRenderer);
        }
    }

    private VideoCapturer createCameraCapturer() {
        Camera2Enumerator enumerator = new Camera2Enumerator(context);
        for (String name : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(name)) {
                return enumerator.createCapturer(name, null);
            }
        }
        for (String name : enumerator.getDeviceNames()) {
            return enumerator.createCapturer(name, null);
        }
        return null;
    }

    public void createPeerConnection(String peerId, boolean createOffer) {
        if (disposed) {
            Log.w(TAG, "createPeerConnection: disposed, skipping " + peerId);
            return;
        }
        if (peerConnectionFactory == null) {
            Log.e(TAG, "createPeerConnection: factory is null, WebRTC not initialized!");
            return;
        }
        if (peerConnections.containsKey(peerId)) {
            if (createOffer) {
                // PC exists but we need to send an offer — do it on the existing PC
                Log.d(TAG, "createPeerConnection: PC exists for " + peerId + ", creating offer on existing PC");
                PeerConnection existingPc = peerConnections.get(peerId);
                if (existingPc != null && existingPc.signalingState() == PeerConnection.SignalingState.STABLE) {
                    createAndSendOffer(existingPc, peerId);
                }
            } else {
                Log.w(TAG, "createPeerConnection: already exists for " + peerId);
            }
            return;
        }
        Log.d(TAG, "createPeerConnection: peerId=" + peerId + " createOffer=" + createOffer);

        PeerConnection.RTCConfiguration config = new PeerConnection.RTCConfiguration(ICE_SERVERS);
        config.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        PeerConnection pc = peerConnectionFactory.createPeerConnection(config, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState state) {
                Log.d(TAG, "Signaling state for " + peerId + ": " + state);
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
                Log.d(TAG, "ICE state for " + peerId + ": " + state);
                if (state == PeerConnection.IceConnectionState.DISCONNECTED ||
                    state == PeerConnection.IceConnectionState.FAILED ||
                    state == PeerConnection.IceConnectionState.CLOSED) {
                    listener.onPeerDisconnected(peerId);
                }
            }

            @Override
            public void onIceConnectionReceivingChange(boolean receiving) {}

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState state) {
                Log.d(TAG, "ICE gathering for " + peerId + ": " + state);
            }

            @Override
            public void onIceCandidate(IceCandidate candidate) {
                Log.d(TAG, "ICE candidate for " + peerId + ": " + candidate.sdpMid);
                listener.onIceCandidate(peerId, candidate);
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] candidates) {}

            @Override
            public void onAddStream(MediaStream stream) {}

            @Override
            public void onRemoveStream(MediaStream stream) {}

            @Override
            public void onDataChannel(DataChannel channel) {}

            @Override
            public void onRenegotiationNeeded() {}

            @Override
            public void onAddTrack(RtpReceiver receiver, MediaStream[] streams) {}

            @Override
            public void onTrack(RtpTransceiver transceiver) {}
        });

        if (pc == null) {
            Log.e(TAG, "Failed to create PeerConnection for " + peerId);
            return;
        }

        // Add local tracks
        if (localAudioTrack != null) {
            pc.addTrack(localAudioTrack, Collections.singletonList("stream0"));
            Log.d(TAG, "Added local audio track to PC for " + peerId);
        } else {
            Log.w(TAG, "No local audio track to add for " + peerId);
        }
        if (localVideoTrack != null) {
            pc.addTrack(localVideoTrack, Collections.singletonList("stream0"));
        }

        peerConnections.put(peerId, pc);

        if (createOffer) {
            createAndSendOffer(pc, peerId);
        }
    }

    private void createAndSendOffer(PeerConnection pc, String peerId) {
        try {
            Log.d(TAG, "Creating offer for " + peerId + " pcState=" + pc.signalingState());
            MediaConstraints constraints = new MediaConstraints();
            constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
            pc.createOffer(new SdpObserverAdapter() {
                @Override
                public void onCreateSuccess(SessionDescription sdp) {
                    Log.d(TAG, "Offer created for " + peerId + ", setting local description");
                    try {
                        pc.setLocalDescription(new SdpObserverAdapter() {
                            @Override
                            public void onSetSuccess() {
                                Log.d(TAG, "Local description set for " + peerId + ", sending offer");
                                listener.onLocalOffer(peerId, sdp);
                            }
                            @Override
                            public void onSetFailure(String error) {
                                Log.e(TAG, "Failed to set local description for " + peerId + ": " + error);
                            }
                        }, sdp);
                    } catch (Exception e) {
                        Log.e(TAG, "Exception in setLocalDescription for " + peerId, e);
                    }
                }
                @Override
                public void onCreateFailure(String error) {
                    Log.e(TAG, "Failed to create offer for " + peerId + ": " + error);
                }
            }, constraints);
        } catch (Exception e) {
            Log.e(TAG, "Exception in createAndSendOffer for " + peerId, e);
        }
    }

    public void setRemoteOffer(String peerId, SessionDescription sdp) {
        Log.d(TAG, "setRemoteOffer from " + peerId);
        PeerConnection pc = peerConnections.get(peerId);
        if (pc == null) {
            createPeerConnection(peerId, false);
            pc = peerConnections.get(peerId);
        }
        if (pc == null) {
            Log.e(TAG, "setRemoteOffer: PC is null for " + peerId);
            return;
        }

        PeerConnection finalPc = pc;
        pc.setRemoteDescription(new SdpObserverAdapter() {
            @Override
            public void onSetSuccess() {
                Log.d(TAG, "Remote offer set for " + peerId + ", creating answer");
                // Create answer
                MediaConstraints constraints = new MediaConstraints();
                constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
                finalPc.createAnswer(new SdpObserverAdapter() {
                    @Override
                    public void onCreateSuccess(SessionDescription answerSdp) {
                        Log.d(TAG, "Answer created for " + peerId + ", setting local description");
                        finalPc.setLocalDescription(new SdpObserverAdapter() {
                            @Override
                            public void onSetSuccess() {
                                Log.d(TAG, "Local answer set for " + peerId + ", sending answer");
                                listener.onLocalAnswer(peerId, answerSdp);
                            }
                            @Override
                            public void onSetFailure(String error) {
                                Log.e(TAG, "Failed to set local answer for " + peerId + ": " + error);
                            }
                        }, answerSdp);
                    }
                    @Override
                    public void onCreateFailure(String error) {
                        Log.e(TAG, "Failed to create answer for " + peerId + ": " + error);
                    }
                }, constraints);
                // Flush pending candidates
                drainPendingCandidates(peerId);
            }
            @Override
            public void onSetFailure(String error) {
                Log.e(TAG, "Failed to set remote offer for " + peerId + ": " + error);
            }
        }, sdp);
    }

    public void setRemoteAnswer(String peerId, SessionDescription sdp) {
        Log.d(TAG, "setRemoteAnswer from " + peerId);
        PeerConnection pc = peerConnections.get(peerId);
        if (pc == null) {
            Log.e(TAG, "setRemoteAnswer: PC is null for " + peerId);
            return;
        }
        pc.setRemoteDescription(new SdpObserverAdapter() {
            @Override
            public void onSetSuccess() {
                Log.d(TAG, "Remote answer set for " + peerId);
                drainPendingCandidates(peerId);
            }
            @Override
            public void onSetFailure(String error) {
                Log.e(TAG, "Failed to set remote answer for " + peerId + ": " + error);
            }
        }, sdp);
    }

    public void addIceCandidate(String peerId, IceCandidate candidate) {
        PeerConnection pc = peerConnections.get(peerId);
        if (pc != null && pc.getRemoteDescription() != null) {
            Log.d(TAG, "Adding ICE candidate for " + peerId + " mid=" + candidate.sdpMid);
            pc.addIceCandidate(candidate);
        } else {
            // Queue the candidate
            Log.d(TAG, "Queuing ICE candidate for " + peerId + " (remote desc not set yet)");
            pendingCandidates.computeIfAbsent(peerId, k -> new ArrayList<>()).add(candidate);
        }
    }

    private void drainPendingCandidates(String peerId) {
        List<IceCandidate> candidates = pendingCandidates.remove(peerId);
        if (candidates != null) {
            PeerConnection pc = peerConnections.get(peerId);
            if (pc != null) {
                Log.d(TAG, "Draining " + candidates.size() + " pending ICE candidates for " + peerId);
                for (IceCandidate c : candidates) {
                    pc.addIceCandidate(c);
                }
            }
        }
    }

    public void removePeer(String peerId) {
        PeerConnection pc = peerConnections.remove(peerId);
        if (pc != null) {
            pc.close();
        }
        pendingCandidates.remove(peerId);
    }

    public void toggleMic() {
        micEnabled = !micEnabled;
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(micEnabled);
        }
    }

    public boolean isMicEnabled() {
        return micEnabled;
    }

    public void setMicEnabled(boolean enabled) {
        micEnabled = enabled;
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(enabled);
        }
    }

    public void toggleCamera() {
        cameraEnabled = !cameraEnabled;
        if (localVideoTrack != null) {
            localVideoTrack.setEnabled(cameraEnabled);
        }
    }

    public boolean isCameraEnabled() {
        return cameraEnabled;
    }

    public void switchCamera() {
        if (videoCapturer instanceof CameraVideoCapturer) {
            ((CameraVideoCapturer) videoCapturer).switchCamera(null);
        }
    }

    public EglBase getEglBase() {
        return eglBase;
    }

    public void dispose() {
        disposed = true;
        stopAudioLevelMonitoring();
        for (PeerConnection pc : peerConnections.values()) {
            pc.close();
        }
        peerConnections.clear();
        pendingCandidates.clear();

        if (localAudioTrack != null) {
            localAudioTrack.dispose();
            localAudioTrack = null;
        }
        if (localVideoTrack != null) {
            localVideoTrack.dispose();
            localVideoTrack = null;
        }
        if (videoCapturer != null) {
            try { videoCapturer.stopCapture(); } catch (Exception ignored) {}
            videoCapturer.dispose();
            videoCapturer = null;
        }
        if (videoSource != null) {
            videoSource.dispose();
            videoSource = null;
        }
        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.dispose();
            surfaceTextureHelper = null;
        }
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }
        if (eglBase != null) {
            eglBase.release();
            eglBase = null;
        }
    }

    // ── Audio level monitoring ──────────────────────────────────────────

    public void startAudioLevelMonitoring() {
        audioLevelHandler.removeCallbacks(audioLevelPoller);
        audioLevelHandler.post(audioLevelPoller);
    }

    public void stopAudioLevelMonitoring() {
        audioLevelHandler.removeCallbacks(audioLevelPoller);
        speakingStates.clear();
    }

    private void pollAudioLevels() {
        if (disposed) return;

        for (Map.Entry<String, PeerConnection> entry : peerConnections.entrySet()) {
            String peerId = entry.getKey();
            PeerConnection pc = entry.getValue();
            if (pc == null) continue;

            pc.getStats(report -> {
                if (report == null) return;
                boolean speaking = false;
                Map<String, RTCStats> statsMap = report.getStatsMap();
                for (RTCStats stats : statsMap.values()) {
                    String type = stats.getType();
                    Map<String, Object> members = stats.getMembers();

                    // Check inbound-rtp or track stats for remote audio level
                    if ("inbound-rtp".equals(type) || "track".equals(type)) {
                        Object kindObj = members.get("kind");
                        if (kindObj != null && !"audio".equals(String.valueOf(kindObj))) continue;

                        // Try audioLevel (0.0 – 1.0)
                        Object audioLevelObj = members.get("audioLevel");
                        if (audioLevelObj instanceof Number) {
                            double level = ((Number) audioLevelObj).doubleValue();
                            if (level > SPEAKING_THRESHOLD) {
                                speaking = true;
                                break;
                            }
                        }
                    }
                }

                boolean wasSpeaking = Boolean.TRUE.equals(speakingStates.get(peerId));
                if (speaking != wasSpeaking) {
                    speakingStates.put(peerId, speaking);
                    listener.onAudioLevelChanged(peerId, speaking);
                }
            });
        }

        audioLevelHandler.postDelayed(audioLevelPoller, AUDIO_LEVEL_POLL_INTERVAL_MS);
    }

    /** No-op SDP observer — override only the callbacks you care about */
    private static class SdpObserverAdapter implements SdpObserver {
        @Override public void onCreateSuccess(SessionDescription sdp) {}
        @Override public void onSetSuccess() {}
        @Override public void onCreateFailure(String error) {
            Log.e(TAG, "SDP create failure: " + error);
        }
        @Override public void onSetFailure(String error) {
            Log.e(TAG, "SDP set failure: " + error);
        }
    }
}
