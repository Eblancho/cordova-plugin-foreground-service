package com.davidbriglio.foreground;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

public class ForegroundPlugin extends CordovaPlugin {

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
            intent.setAction("stop");
            activity.getApplicationContext().startService(intent);
            command.success();
            return true;
        }

        command.error("Unsupported action: " + action);
        return false;
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
