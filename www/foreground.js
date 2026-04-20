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
  }
};
