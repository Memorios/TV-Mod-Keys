package com.memoar.tvmodkeys;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * Wakes up when any Bluetooth device connects. If a physical (alphabetic)
 * keyboard shows up within a few seconds, this app's keyboard is selected.
 */
public class KeyboardConnectReceiver extends BroadcastReceiver {
    private static final long TIMEOUT_MS = 9000;
    private static final long POLL_MS = 500;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) return;
        final Context app = context.getApplicationContext();
        if (!ImeSwitch.autoEnable(app) || ImeSwitch.isOurImeActive(app)) return;

        final PendingResult pending = goAsync();
        final Handler handler = new Handler(Looper.getMainLooper());
        final long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ImeSwitch.hasExternalKeyboard(-1)) {
                    ImeSwitch.enableOurIme(app);
                    pending.finish();
                } else if (SystemClock.uptimeMillis() < deadline) {
                    handler.postDelayed(this, POLL_MS);
                } else {
                    pending.finish();
                }
            }
        }, POLL_MS);
    }
}
