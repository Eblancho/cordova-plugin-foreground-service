package com.davidbriglio.foreground;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

public class ForegroundPlugin extends CordovaPlugin {

    private static final String TAG = "ForegroundPlugin";

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

        command.error("Unsupported action: " + action);
        return false;
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
