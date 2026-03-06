package com.davidbriglio.foreground;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class ForegroundService extends Service {

    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";
    private static final String CHANNEL_ID = "foreground.service.channel";
    private static final String CHANNEL_NAME = "Background Services";
    private static final String CHANNEL_DESCRIPTION = "Enables background processing.";
    private static final int DEFAULT_NOTIFICATION_ID = 197812504;
    private static final int DEFAULT_ICON_RES_ID = 17301514; // android.R.drawable.btn_star

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Important: Android peut relancer un service sticky avec intent == null.
        // On ne veut jamais crasher ici.
        if (intent == null) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            startPluginForegroundService(intent.getExtras());
            return START_NOT_STICKY;
        }

        if (ACTION_STOP.equals(action)) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        // Action inconnue : on stoppe proprement, sans crash.
        stopForeground(true);
        stopSelf();
        return START_NOT_STICKY;
    }

    @TargetApi(26)
    private void startPluginForegroundService(Bundle extras) {
        Context context = getApplicationContext();

        if (extras == null) {
            extras = new Bundle();
        }

        NotificationManager manager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager == null) {
            stopSelf();
            return;
        }

        int importance = parseImportance(extras.getString("importance"));

        // Ne pas supprimer le channel existant.
        // On le crée simplement s'il n'existe pas déjà.
        NotificationChannel existingChannel = manager.getNotificationChannel(CHANNEL_ID);
        if (existingChannel == null) {
            NotificationChannel channel =
                new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
            channel.setDescription(CHANNEL_DESCRIPTION);
            manager.createNotificationChannel(channel);
        }

        String title = extras.getString("title");
        if (title == null || title.trim().isEmpty()) {
            title = "App active";
        }

        String text = extras.getString("text");
        if (text == null || text.trim().isEmpty()) {
            text = "Traitement en cours";
        }

        String iconName = extras.getString("icon");
        int icon = 0;
        if (iconName != null && !iconName.trim().isEmpty()) {
            icon = getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        }
        if (icon == 0) {
            icon = DEFAULT_ICON_RES_ID;
        }

        int notificationId = parseNotificationId(extras.getString("id"));

        Notification notification = new Notification.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setSmallIcon(icon)
            .build();

        // Le FGS doit entrer en foreground immédiatement avec une notif valide.
        startForeground(notificationId, notification);
    }

    private int parseImportance(String value) {
        int rawValue = 1;
        try {
            if (value != null) {
                rawValue = Integer.parseInt(value);
            }
        } catch (NumberFormatException ignored) {
            rawValue = 1;
        }

        switch (rawValue) {
            case 2:
                return NotificationManager.IMPORTANCE_DEFAULT;
            case 3:
                return NotificationManager.IMPORTANCE_HIGH;
            default:
                return NotificationManager.IMPORTANCE_LOW;
        }
    }

    private int parseNotificationId(String value) {
        int id = DEFAULT_NOTIFICATION_ID;
        try {
            if (value != null) {
                id = Integer.parseInt(value);
            }
        } catch (NumberFormatException ignored) {
            id = DEFAULT_NOTIFICATION_ID;
        }

        if (id == 0) {
            return DEFAULT_NOTIFICATION_ID;
        }

        return id;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
