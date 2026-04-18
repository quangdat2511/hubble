package com.example.hubble.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.PowerManager;

public class AudioProximityManager implements SensorEventListener {

    private final AudioManager audioManager;
    private final SensorManager sensorManager;
    private final Sensor proximitySensor;
    private PowerManager.WakeLock wakeLock;
    private boolean isPlaying = false;

    public AudioProximityManager(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null && powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            // Khởi tạo WakeLock để tắt màn hình khi áp tai
            wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "Hubble:ProximityAudio");
        }
    }

    // Bắt đầu lắng nghe cảm biến khi bấm Play
    public void start() {
        if (isPlaying) return;
        isPlaying = true;

        // Khóa hệ thống ở chế độ Giao tiếp (Thoại) và ép mở loa ngoài từ đầu
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);

        if (proximitySensor != null) {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    // Dừng lắng nghe khi bấm Pause hoặc hết Audio
    public void stop() {
        if (!isPlaying) return;
        isPlaying = false;

        if (proximitySensor != null) {
            sensorManager.unregisterListener(this);
        }

        // Trả hệ thống về mặc định (Loa ngoài bình thường)
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(false);

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release(); // Bật lại màn hình nếu đang tắt
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isPlaying) return;

        float distance = event.values[0];
        // Khoảng cách < 5cm nghĩa là đang áp tai vào
        boolean isNear = distance < proximitySensor.getMaximumRange() && distance < 5f;

        if (isNear) {
            // 1. Tắt màn hình
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire(10 * 60 * 1000L);
            }
            // 2. Chuyển sang loa trong (Earpiece) bằng cách tắt loa ngoài
            audioManager.setSpeakerphoneOn(false);
        } else {
            // 1. Bật lại màn hình
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
            // 2. Chuyển lại loa ngoài (Loudspeaker)
            audioManager.setSpeakerphoneOn(true);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}