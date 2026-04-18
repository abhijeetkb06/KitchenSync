package com.kitchensync.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.kitchensync.R;
import com.kitchensync.ui.roleselection.RoleSelectionActivity;

/**
 * Foreground service that keeps the P2P sync connection alive when the app
 * is backgrounded.  Android aggressively kills background processes and
 * restricts their network access (Doze mode).  A foreground service with
 * a persistent notification tells the OS "this app is doing important work"
 * and prevents it from being killed or network-throttled.
 */
public class P2PSyncService extends Service {
    private static final String TAG = "P2PSyncService";
    private static final String CHANNEL_ID = "kitchensync_p2p";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        Log.i(TAG, "P2P sync service started (foreground)");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "P2P sync service stopped");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "P2P Sync",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Keeps peer-to-peer sync active in the background");
        channel.setShowBadge(false);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        Intent tapIntent = new Intent(this, RoleSelectionActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Kitchen Sync")
                .setContentText("P2P sync active")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
}
