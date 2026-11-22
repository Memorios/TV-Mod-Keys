package com.memoar.tvmodkeys;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.InputDevice;

/** Helpers shared by the keyboard service and the Bluetooth receiver. */
final class ImeSwitch {
    private static final String PREFS = "tvmodkeys";
    private static final String KEY_PREV = "prev_ime";
    private static final String KEY_AUTO = "auto_enable";

    private ImeSwitch() { }

    static boolean hasExternalKeyboard(int ignoreId) {
        for (int id : InputDevice.getDeviceIds()) {
            if (id == ignoreId) continue;
            InputDevice d = InputDevice.getDevice(id);
            if (d != null && !d.isVirtual()
                    && d.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC
                    && (d.getSources() & InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD) {
                return true;
            }
        }
        return false;
    }

    static String currentIme(Context c) {
        return Settings.Secure.getString(c.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
    }

    static boolean isOurImeActive(Context c) {
        String def = currentIme(c);
        return def != null && def.startsWith(c.getPackageName() + "/");
    }

    static String ourImeId(Context c) {
        return new ComponentName(c, ModifierImeService.class).flattenToShortString();
    }

    /** Needs WRITE_SECURE_SETTINGS (granted once via adb). Returns true if the keyboard was changed. */
    static boolean enableOurIme(Context c) {
        if (isOurImeActive(c)) return false;
        String prev = currentIme(c);
        try {
            Settings.Secure.putString(c.getContentResolver(),
                    Settings.Secure.DEFAULT_INPUT_METHOD, ourImeId(c));
            prefs(c).edit().putString(KEY_PREV, prev).apply();
            DebugLog.add("AUTO keyboard connected, selected this keyboard (was " + prev + ")");
            return true;
        } catch (SecurityException e) {
            DebugLog.add("AUTO cannot select keyboard: WRITE_SECURE_SETTINGS not granted");
            return false;
        }
    }

    static String previousIme(Context c) {
        return prefs(c).getString(KEY_PREV, null);
    }

    static boolean autoEnable(Context c) {
        return prefs(c).getBoolean(KEY_AUTO, true);
    }

    static void setAutoEnable(Context c, boolean on) {
        prefs(c).edit().putBoolean(KEY_AUTO, on).apply();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
