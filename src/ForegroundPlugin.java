package com.davidbriglio.foreground;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

public class ForegroundPlugin extends CordovaPlugin {

    private static final String TAG = "ForegroundPlugin";
    private static final int REQ_POST_NOTIFICATIONS = 10133;
    private static final String CHANNEL_ID = "foreground.service.channel";

    private CallbackContext pendingNotificationPermissionCallback;

    @Override
    @TargetApi(26)
    public boolean execute(final String action, final JSONArray args, final CallbackContext command)
        throws JSONException {

        if (android.os.Build.VERSION.SDK_INT < 26) {
            command.success();
            return true;
        }

        Activity activity = cordova.getActivity();
        if (activity == null) {
            command.error("Activity is null");
            return false;
        }

        Intent intent = new Intent(activity, ForegroundService.class);

        if ("start".equals(action)) {
            if (!isAppInForeground(activity)) {
                Log.w(TAG, "Refusing to start FGS: app not in foreground.");
                command.error("Foreground service start not allowed: app is not in foreground. Call start() while the app is visible (usually after a user action).");
                return true;
            }

            if (!canPostNotifications(activity)) {
                Log.w(TAG, "Refusing to start FGS: notifications are disabled/denied.");
                command.error(
                    "Foreground service requires a visible notification. Notifications are disabled/denied for this app. " +
                    "Grant notification permission (Android 13+) and enable notifications in system settings, then retry start()."
                );
                return true;
            }

            intent.setAction("start");

            intent.putExtra("title", getSafeArg(args, 0, "App active"));
            intent.putExtra("text", getSafeArg(args, 1, "Traitement en cours"));
            intent.putExtra("icon", getSafeArg(args, 2, ""));
            intent.putExtra("importance", getSafeArg(args, 3, "1"));
            intent.putExtra("id", getSafeArg(args, 4, "197812504"));

            activity.getApplicationContext().startForegroundService(intent);
            command.success();
            return true;
        }

        if ("stop".equals(action)) {
            activity.getApplicationContext().stopService(intent);
            command.success();
            return true;
        }

        if ("requestNotificationPermission".equals(action)) {
            if (Build.VERSION.SDK_INT < 33) {
                command.success();
                return true;
            }

            if (activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                command.success();
                return true;
            }

            pendingNotificationPermissionCallback = command;
            cordova.requestPermission(this, REQ_POST_NOTIFICATIONS, Manifest.permission.POST_NOTIFICATIONS);
            return true;
        }

        if ("openNotificationSettings".equals(action)) {
            try {
                android.content.Intent settingsIntent =
                    new android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                settingsIntent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
                settingsIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(settingsIntent);
                command.success();
            } catch (Throwable ex) {
                command.error(ex.getMessage() != null ? ex.getMessage() : "Failed to open notification settings");
            }
            return true;
        }

        if ("isIgnoringBatteryOptimizations".equals(action)) {
            command.success(isIgnoringBatteryOptimizations(activity) ? 1 : 0);
            return true;
        }

        if ("requestIgnoreBatteryOptimizations".equals(action)) {
            try {
                if (Build.VERSION.SDK_INT < 23) {
                    command.success();
                    return true;
                }

                if (isIgnoringBatteryOptimizations(activity)) {
                    command.success();
                    return true;
                }

                Intent requestIntent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                requestIntent.setData(android.net.Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(requestIntent);
                command.success();
            } catch (Throwable ex) {
                command.error(ex.getMessage() != null ? ex.getMessage() : "Failed to request ignore battery optimizations");
            }
            return true;
        }

        if ("openBatteryOptimizationSettings".equals(action)) {
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                activity.startActivity(settingsIntent);
                command.success();
            } catch (Throwable ex) {
                command.error(ex.getMessage() != null ? ex.getMessage() : "Failed to open battery optimization settings");
            }
            return true;
        }

        command.error("Unsupported action: " + action);
        return false;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults)
        throws JSONException {
        if (requestCode != REQ_POST_NOTIFICATIONS) {
            return;
        }

        CallbackContext cb = pendingNotificationPermissionCallback;
        pendingNotificationPermissionCallback = null;

        if (cb == null) {
            return;
        }

        boolean granted = grantResults != null
            && grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (granted) {
            cb.success();
        } else {
            cb.error("POST_NOTIFICATIONS permission denied");
        }
    }

    private boolean isAppInForeground(Activity activity) {
        try {
            ActivityManager activityManager =
                (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager == null) {
                return true;
            }

            String packageName = activity.getPackageName();
            java.util.List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
            if (processes == null) {
                return true;
            }

            for (ActivityManager.RunningAppProcessInfo process : processes) {
                if (process == null) {
                    continue;
                }

                if (!packageName.equals(process.processName)) {
                    continue;
                }

                // IMPORTANCE_FOREGROUND: app visible/interactive
                // IMPORTANCE_VISIBLE: visible (e.g. dialog on top) - acceptable for user-initiated starts
                return process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    || process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
            }
        } catch (Throwable ignored) {
            // If we cannot determine state, don't block.
            return true;
        }

        // If process not found, be conservative and don't block.
        return true;
    }

    private boolean canPostNotifications(Context context) {
        if (context == null) {
            return true;
        }

        // Runtime permission gate on Android 13+.
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            } catch (Throwable ignored) {
                // If check fails, don't hard-block.
            }
        }

        try {
            NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) {
                return true;
            }
            if (!manager.areNotificationsEnabled()) {
                return false;
            }

            // Channel-level block (Android 8+). A blocked channel means no visible notification.
            if (Build.VERSION.SDK_INT >= 26) {
                try {
                    android.app.NotificationChannel channel = manager.getNotificationChannel(CHANNEL_ID);
                    if (channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                        return false;
                    }
                } catch (Throwable ignored) {
                    // ignore
                }
            }

            return true;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private boolean isIgnoringBatteryOptimizations(Context context) {
        if (context == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }

        try {
            PowerManager powerManager =
                (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) {
                return false;
            }
            return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private String getSafeArg(JSONArray args, int index, String defaultValue) {
        if (args == null) {
            return defaultValue;
        }

        if (index < 0 || index >= args.length()) {
            return defaultValue;
        }

        String value = args.optString(index, defaultValue);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value;
    }
}
