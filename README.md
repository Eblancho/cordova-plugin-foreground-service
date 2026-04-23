# cordova-plugin-foreground-service [![npm version](https://badge.fury.io/js/cordova-plugin-foreground-service.svg)](https://badge.fury.io/js/cordova-plugin-foreground-service)

***NOTE***: This plugin does not on its own allow the user to execute javascript while the app is in the background. This must be accompanied by another plugin that will create a background process that will give the application background cycles. This plugin mainly exists to solve the problem outlined [here](https://developer.android.com/about/versions/oreo/background#services). An app will have restricted background processing if it is not considered a "foreground app" for android API 26+ and will prevent background pluggins from functioning properly. With this plugin your application will be a foreground app and let your background services run properly. This plugin may also help with [android doze](https://developer.android.com/training/monitoring-device-state/doze-standby#understand_app_standby) where an application may not run in the background unless it also has a foreground process. Do not expect this plugin on its own to allow your app to execute javascript while in the background.

---

This plugin allows for android devices to continue running services in the background, using a foreground ongoing notification. This is targeted towards use with plugins such as 'cordova-geolocation' that will not run while the app is in the background on android API 26+.

---

## Requirements

- cordova-android version >= 7.0.0
- android-sdk api >= 26

***NOTE: Using cordova version >= 8.0.0 is recommended.***

### Android 12+ (API 31+) background start limits

On Android 12+, the system can throw `ForegroundServiceStartNotAllowedException` if you try to start a foreground service while your app is in the background (not user-visible). Make sure you call `cordova.plugins.foregroundService.start(...)` while the app is in the foreground (typically right after a user action). If started while restricted, the service will stop itself and log a warning instead of crashing the whole app process.

### Android 14+ (targetSdk 34) foreground service types

This plugin declares `android:foregroundServiceType="dataSync"` and adds `android.permission.FOREGROUND_SERVICE_DATA_SYNC` in the manifest via `plugin.xml`. If you change the service type in your app, ensure the matching `FOREGROUND_SERVICE_*` permission is also declared.

### Android 13+ notifications permission (API 33+)

If your app targets Android 13+ and notifications are disabled/denied, the foreground service notification may not appear. Ensure the user grants the runtime notification permission (`POST_NOTIFICATIONS`) using a permissions plugin.

---

## Setup and Usage

### Install

```shell
cordova plugin add cordova-plugin-foreground-service

#or

cordova plugin add https://github.com/Eblancho/cordova-plugin-foreground-service
```

### Start Method

To enable the foreground service, call the `start` method:

```javascript
cordova.plugins.foregroundService.start('Notification Title', 'Notification Text', 'drawable-icon-name', [notification importance], [notification id]);

// High priority with notification id 10
// ('myicon.png' is in the 'res/drawable' folder)
cordova.plugins.foregroundService.start('GPS Running', 'Background Service', 'myicon', 3, 10);

// Notification id is optional
cordova.plugins.foregroundService.start('GPS Running', 'Background Service', 'myicon', 3);

// Notification importance is optional, the default is 1 - Low (no sound or vibration)
cordova.plugins.foregroundService.start('GPS Running', 'Background Service', 'myicon');

// Icon is optional, but will use a 'star' icon as the default
cordova.plugins.foregroundService.start('GPS Running', 'Background Service');
```

### Android 13+ (API 33+) Notification Permission

If your app targets Android 13+ and the user denies notifications, a foreground service **cannot** reliably show its ongoing notification.
This plugin will refuse to start and return an error until notifications are allowed.

Also note: on Android 8+ a user can disable the specific notification channel. If the channel is blocked, the plugin will also refuse to start.

You can request the runtime permission (Android 13+) and/or open the system notification settings:

```javascript
cordova.plugins.foregroundService.requestNotificationPermission(
  () => console.log('Notifications granted'),
  (err) => console.warn('Notifications denied', err)
);

cordova.plugins.foregroundService.openNotificationSettings();
```

### Battery optimizations / Doze (Android 6+)

On some OEM devices (notably Samsung), aggressive battery optimizations can kill your app process while it is waiting for results from external activities (camera intents, file chooser, etc.). A foreground service helps, but you should also ask the user to exclude the app from battery optimizations.

This plugin exposes helpers:

```javascript
cordova.plugins.foregroundService.isIgnoringBatteryOptimizations(
  (v) => console.log('ignoringBatteryOptimizations=', String(v)),
  console.warn
);

cordova.plugins.foregroundService.requestIgnoreBatteryOptimizations();
// Or, if the prompt cannot be shown:
cordova.plugins.foregroundService.openBatteryOptimizationSettings();
```

### Stop Method

To disable the foreground service, call the `stop` method:

```javascript
cordova.plugins.foregroundService.stop();
```

---

## Parameters

### **Icon**

The drawable icon can be any drawable image that exists in the resource folder. This means you must copy the icon you want to use into the `platforms/android/app/src/main/res/drawable` folder set. If no icon can be found using the icon name parameter, a default star icon will be used.

**NOTE:** The normal android drawable (`R.drawable`) icons are not available for use as icons. You cannot reference these icons through the `start` icon parameter. Include your icon manually in the folders mentioned above.

### **Notification ID**

The notification id is a customizable integer that is used to reference the notification that will be launched. This is customizable to avoid conflicting with any other notifications. If this is not included, a default id will be used.

### **Notification Importance**

Notification importance dictates how the notification is initially presented:

Value | Importance | Description
--- | --- | ---
1|IMPORTANCE_LOW|Does not make a sound or heads-up display. (Default)
2|IMPORTANCE_DEFAULT|Makes a sound, but no heads-up display.
3|IMPORTANCE_HIGH|Makes a sound and heads-up display.

---

## Questions?

Please feel free to open an issue or make a pull request!

---

## License

MIT - Please see the LICENSE file for details.

---

David Briglio (2018)
