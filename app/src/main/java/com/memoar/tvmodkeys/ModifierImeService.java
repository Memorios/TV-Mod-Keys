package com.memoar.tvmodkeys;

import android.hardware.input.InputManager;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.provider.Settings;
import android.text.InputType;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

/**
 * Invisible IME: no on-screen keys, it only translates hardware key events
 * (US layout) with Shift / Caps Lock / Ctrl / Alt into text or edit actions.
 */
public class ModifierImeService extends InputMethodService {

    // keycode -> {normal, shifted}
    private static final SparseArray<String[]> KEYS = new SparseArray<>();
    // Alt (or Ctrl+Alt) + keycode -> text. Edit freely.
    private static final SparseArray<String> ALT_KEYS = new SparseArray<>();

    private static void put(int code, String base, String shifted) {
        KEYS.put(code, new String[]{base, shifted});
    }

    static {
        for (int i = 0; i < 26; i++) {
            String lower = String.valueOf((char) ('a' + i));
            put(KeyEvent.KEYCODE_A + i, lower, lower.toUpperCase());
        }
        String shiftedDigits = ")!@#$%^&*(";
        for (int i = 0; i < 10; i++) {
            put(KeyEvent.KEYCODE_0 + i, String.valueOf(i), String.valueOf(shiftedDigits.charAt(i)));
        }
        put(KeyEvent.KEYCODE_GRAVE, "`", "~");
        put(KeyEvent.KEYCODE_MINUS, "-", "_");
        put(KeyEvent.KEYCODE_EQUALS, "=", "+");
        put(KeyEvent.KEYCODE_LEFT_BRACKET, "[", "{");
        put(KeyEvent.KEYCODE_RIGHT_BRACKET, "]", "}");
        put(KeyEvent.KEYCODE_BACKSLASH, "\\", "|");
        put(KeyEvent.KEYCODE_SEMICOLON, ";", ":");
        put(KeyEvent.KEYCODE_APOSTROPHE, "'", "\"");
        put(KeyEvent.KEYCODE_COMMA, ",", "<");
        put(KeyEvent.KEYCODE_PERIOD, ".", ">");
        put(KeyEvent.KEYCODE_SLASH, "/", "?");
        put(KeyEvent.KEYCODE_SPACE, " ", " ");

        ALT_KEYS.put(KeyEvent.KEYCODE_E, "\u20AC"); // euro
        ALT_KEYS.put(KeyEvent.KEYCODE_L, "\u00A3"); // pound
        ALT_KEYS.put(KeyEvent.KEYCODE_Y, "\u00A5"); // yen
    }

    private final Set<Integer> consumed = new HashSet<>();
    // Shift state tracked from the Shift key events themselves, in case the
    // system does not put META_SHIFT_ON into the key events of this keyboard.
    private boolean shiftDown;

    // ---- auto switch back to the previous keyboard when the external one disconnects ----
    private InputManager inputManager;
    private final InputManager.InputDeviceListener deviceListener =
            new InputManager.InputDeviceListener() {
        @Override public void onInputDeviceAdded(int deviceId) { }
        @Override public void onInputDeviceChanged(int deviceId) { }
        @Override public void onInputDeviceRemoved(int deviceId) {
            if (!ImeSwitch.hasExternalKeyboard(deviceId) && ImeSwitch.isOurImeActive(ModifierImeService.this)) {
                switchBack();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        inputManager = (InputManager) getSystemService(INPUT_SERVICE);
        inputManager.registerInputDeviceListener(deviceListener, null);
    }

    @Override
    public void onDestroy() {
        if (inputManager != null) inputManager.unregisterInputDeviceListener(deviceListener);
        super.onDestroy();
    }

    private void switchBack() {
        boolean ok = false;
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                ok = switchToPreviousInputMethod();
                if (!ok) {
                    String prev = ImeSwitch.previousIme(this);
                    if (prev != null && !prev.startsWith(getPackageName() + "/")) {
                        switchInputMethod(prev);
                        ok = true;
                    }
                }
            }
        } catch (Exception ignored) { }
        DebugLog.add("AUTO keyboard disconnected, switched back ok=" + ok);
        if (!ok) {
            Toast.makeText(this, "TV Modifier Keys: could not switch back, choose a keyboard manually",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onEvaluateInputViewShown() {
        return false; // no soft keyboard
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        if (!restarting) shiftDown = false;
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        shiftDown = false;
        consumed.clear();
    }

    private static boolean isShiftKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isShiftKey(keyCode)) shiftDown = true;

        InputConnection ic = getCurrentInputConnection();
        EditorInfo info = getCurrentInputEditorInfo();
        boolean usable = ic != null && info != null
                && info.inputType != InputType.TYPE_NULL
                && !event.isMetaPressed();
        boolean handled = usable && handle(keyCode, event, ic);

        DebugLog.imeEvent();
        if (DebugLog.keyLogging && event.getRepeatCount() == 0) {
            DebugLog.add("IME dn " + DebugLog.name(keyCode)
                    + " meta=0x" + Integer.toHexString(event.getMetaState())
                    + " shift=" + event.isShiftPressed() + "/" + shiftDown
                    + " dev=" + event.getDeviceId()
                    + " ic=" + (ic != null)
                    + " handled=" + handled);
        }

        if (handled) {
            consumed.add(keyCode);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isShiftKey(keyCode)) shiftDown = false;
        if (consumed.remove(keyCode)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private boolean handle(int keyCode, KeyEvent event, InputConnection ic) {
        boolean ctrl = event.isCtrlPressed();
        boolean alt = event.isAltPressed();
        boolean shift = event.isShiftPressed() || shiftDown;

        if (alt) { // Alt or Ctrl+Alt (AltGr style)
            String s = ALT_KEYS.get(keyCode);
            if (s == null) return false;
            ic.commitText(s, 1);
            return true;
        }

        if (ctrl) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_A:
                    return ic.performContextMenuAction(android.R.id.selectAll);
                case KeyEvent.KEYCODE_C:
                    return ic.performContextMenuAction(android.R.id.copy);
                case KeyEvent.KEYCODE_X:
                    return ic.performContextMenuAction(android.R.id.cut);
                case KeyEvent.KEYCODE_V:
                    return ic.performContextMenuAction(android.R.id.paste);
                case KeyEvent.KEYCODE_DEL:
                    deleteWord(ic, true);
                    return true;
                case KeyEvent.KEYCODE_FORWARD_DEL:
                    deleteWord(ic, false);
                    return true;
                default:
                    return false; // let the app see Ctrl+arrows, Ctrl+Z, etc.
            }
        }

        String[] pair = KEYS.get(keyCode);
        if (pair == null) return false;
        boolean isLetter = keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z;
        boolean upper = isLetter ? (shift ^ event.isCapsLockOn()) : shift;
        ic.commitText(pair[upper ? 1 : 0], 1);
        return true;
    }

    private void deleteWord(InputConnection ic, boolean backwards) {
        CharSequence selected = ic.getSelectedText(0);
        if (selected != null && selected.length() > 0) {
            ic.commitText("", 1);
            return;
        }
        if (backwards) {
            CharSequence t = ic.getTextBeforeCursor(64, 0);
            if (t == null || t.length() == 0) return;
            int i = t.length();
            while (i > 0 && Character.isWhitespace(t.charAt(i - 1))) i--;
            while (i > 0 && !Character.isWhitespace(t.charAt(i - 1))) i--;
            ic.deleteSurroundingText(t.length() - i, 0);
        } else {
            CharSequence t = ic.getTextAfterCursor(64, 0);
            if (t == null || t.length() == 0) return;
            int i = 0;
            while (i < t.length() && Character.isWhitespace(t.charAt(i))) i++;
            while (i < t.length() && !Character.isWhitespace(t.charAt(i))) i++;
            ic.deleteSurroundingText(0, i);
        }
    }
}
