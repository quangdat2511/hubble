package com.example.hubble.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.PowerManager;

public class AudioProximityManager implements SensorEventListener {

    private final AudioManager audioManager;
    private final SensorManager sensorManager;
    private final Sensor proximitySensor;
    private PowerManager.WakeLock wakeLock;
    private boolean isPlaying = false;

    private MediaPlayer mediaPlayer;

    public AudioProximityManager(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null && powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "Hubble:ProximityAudio");
        }
    }

    // [MỚI] Hàm để Adapter truyền MediaPlayer vào
    public void setMediaPlayer(MediaPlayer player) {
        this.mediaPlayer = player;
    }

    public void start() {
        if (isPlaying) return;
        isPlaying = true;

        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(true);

        if (proximitySensor != null) {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stop() {
        if (!isPlaying) return;
        isPlaying = false;
        mediaPlayer = null; // Giải phóng bộ nhớ

        if (proximitySensor != null) {
            sensorManager.unregisterListener(this);
        }

        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(false);

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isPlaying) return;

        float distance = event.values[0];
        boolean isNear = distance < proximitySensor.getMaximumRange() && distance < 5f;

        if (isNear) {
            // TẮT MÀN HÌNH
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire(10 * 60 * 1000L);
            }

            // KỸ THUẬT: PAUSE -> ĐỔI LOA -> PLAY
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                audioManager.setSpeakerphoneOn(false); // Ép ra loa trong
                mediaPlayer.start();
            } else {
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                audioManager.setSpeakerphoneOn(false);
            }
        } else {
            // BẬT LẠI MÀN HÌNH
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }

            // KỸ THUẬT: PAUSE -> ĐỔI LOA -> PLAY
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                audioManager.setMode(AudioManager.MODE_NORMAL);
                audioManager.setSpeakerphoneOn(true); // Ép ra loa ngoài
                mediaPlayer.start();
            } else {
                audioManager.setMode(AudioManager.MODE_NORMAL);
                audioManager.setSpeakerphoneOn(true);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}