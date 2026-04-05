package com.example.hubble.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.hubble.R;
import com.example.hubble.data.repository.NotificationRepository;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.view.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class HubbleFcmService extends FirebaseMessagingService {

    private static final String TAG = "HubbleFcmService";
    private static final String CHANNEL_ID = "hubble_notifications";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "onNewToken called with token: " + token);
        NotificationRepository repo = new NotificationRepository(this);
        repo.registerDeviceToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        Log.d(TAG, "onMessageReceived called");
        Log.d(TAG, "Message data: " + message.getData());

        RemoteMessage.Notification notification = message.getNotification();
        if (notification == null) {
            Log.w(TAG, "Notification is null, skipping");
            return;
        }

        String title = notification.getTitle() != null ? notification.getTitle() : getString(R.string.app_name);
        String body = notification.getBody() != null ? notification.getBody() : "";
        Log.d(TAG, "Title: " + title + ", Body: " + body);

        createNotificationChannel();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("fromNotification", true);
        intent.putExtra("navigateTo", "notifications");
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, (int) System.currentTimeMillis(), 
                intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Use app icon as fallback if ic_notifications doesn't exist
        int iconResId = R.drawable.ic_notifications;
        try {
            ContextCompat.getDrawable(this, iconResId);
        } catch (Exception e) {
            Log.w(TAG, "ic_notifications not found, using app icon: " + e.getMessage());
            iconResId = R.drawable.ic_launcher_foreground;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(iconResId)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                .setVibrate(new long[]{0, 500, 250, 500})
                .setLights(0xFF0000FF, 1000, 1000)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            int notifId = (int) System.currentTimeMillis();
            try {
                manager.notify(notifId, builder.build());
                Log.i(TAG, "Notification displayed with ID: " + notifId);
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException displaying notification: " + e.getMessage(), e);
            } catch (Exception e) {
                Log.e(TAG, "Exception displaying notification: " + e.getMessage(), e);
            }
        } else {
            Log.e(TAG, "NotificationManager is null");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(getString(R.string.notification_channel_desc));
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created: " + CHANNEL_ID);
            } else {
                Log.e(TAG, "NotificationManager is null when creating channel");
            }
        }
    }
}
