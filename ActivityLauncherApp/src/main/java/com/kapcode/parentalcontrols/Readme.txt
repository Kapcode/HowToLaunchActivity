Reason for INTERNET permission.
<uses-permission android:name="android.permission.INTERNET" />

The AlarmReceiver Class requires this permission to fire if the activity is not in the foreground.

This app does not connect to the internet to send or receive data. its just an annoying nuisance with the Android SDK.
The AlarmReceiver Class is used to make sure the ParentalControl service stays running until the user stops it. Android has Battery saving features that may kill
it, or another app could kill it (Task Killers even though they are old school) Source is on Github :)

List of all manifest permissions used bellow:
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.CHANGE_DEVICE_IDLE_TEMP_WHITELIST"
        tools:ignore="ProtectedPermissions" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT" />
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
    <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions"/>
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.USE_EXACT_ALARM"
        tools:ignore="ExactAlarm" />


        //todo update permissions used in this file before release