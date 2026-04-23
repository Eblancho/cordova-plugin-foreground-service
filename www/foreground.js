var exec = require('cordova/exec');

function normalizeCallbacks(args, cbIndex) {
  var success = null;
  var error = null;

  if (typeof args[cbIndex] === 'function') {
    success = args[cbIndex];
    if (typeof args[cbIndex + 1] === 'function') {
      error = args[cbIndex + 1];
    }
  }

  return { success: success, error: error };
}

module.exports = {
  // Backward compatible signatures:
  // - start(title, text, icon, importance, notificationId)
  // - start(title, text, icon, importance, notificationId, successCb, errorCb)
  // - start(title, text, icon, successCb, errorCb)   // importance/id omitted
  start: function(title, text, icon, importance, notificationId, successCb, errorCb) {
    var args = Array.prototype.slice.call(arguments);

    // Handle the (title, text, icon, success, error) signature
    if (typeof importance === 'function') {
      successCb = importance;
      errorCb = notificationId;
      importance = "1";
      notificationId = "";
    }

    var callbacks = normalizeCallbacks(args, 5);
    if (callbacks.success || callbacks.error) {
      successCb = callbacks.success;
      errorCb = callbacks.error;
    }

    exec(successCb || null, errorCb || null, "ForegroundPlugin", "start", [
      title || "",
      text || "",
      icon || "",
      importance || "1",
      notificationId || ""
    ]);
  },
  // stop(successCb, errorCb)
  stop: function(successCb, errorCb) {
    exec(successCb || null, errorCb || null, "ForegroundPlugin", "stop", []);
  },
  // requestNotificationPermission(successCb, errorCb)
  // Android 13+ only (POST_NOTIFICATIONS). On older Android versions this is a no-op success.
  requestNotificationPermission: function(successCb, errorCb) {
    exec(successCb || null, errorCb || null, "ForegroundPlugin", "requestNotificationPermission", []);
  },
  // openNotificationSettings(successCb, errorCb)
  // Opens the system notification settings screen for this app.
  openNotificationSettings: function(successCb, errorCb) {
    exec(successCb || null, errorCb || null, "ForegroundPlugin", "openNotificationSettings", []);
  },
  // isIgnoringBatteryOptimizations(successCb, errorCb)
  // Android 6+ (API 23+). successCb receives 1 (true) or 0 (false).
  isIgnoringBatteryOptimizations: function(successCb, errorCb) {
    exec(successCb || null, errorCb || null, "ForegroundPlugin", "isIgnoringBatteryOptimizations", []);
  },
  // requestIgnoreBatteryOptimizations(successCb, errorCb)
  // Opens the system prompt to exclude the app from battery optimizations (Doze) on Android 6+.
  requestIgnoreBatteryOptimizations: function(successCb, errorCb) {
    exec(successCb || null, errorCb || null, "ForegroundPlugin", "requestIgnoreBatteryOptimizations", []);
  },
  // openBatteryOptimizationSettings(successCb, errorCb)
  // Opens the battery optimization settings list screen.
  openBatteryOptimizationSettings: function(successCb, errorCb) {
    exec(successCb || null, errorCb || null, "ForegroundPlugin", "openBatteryOptimizationSettings", []);
  }
};
