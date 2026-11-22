package com.memoar.tvmodkeys;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView status;
    private TextView log;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            status.setText(buildStatus());
            log.setText(DebugLog.dump());
            handler.postDelayed(this, 400);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (32 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView info = new TextView(this);
        info.setTextSize(18);
        info.setText(R.string.instructions);
        root.addView(info);

        // RAW = what the window receives before the keyboard service (onKeyPreIme).
        EditText field = new EditText(this) {
            @Override
            public boolean onKeyPreIme(int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                    DebugLog.add("RAW dn " + DebugLog.name(keyCode)
                            + " meta=0x" + Integer.toHexString(event.getMetaState())
                            + " shift=" + event.isShiftPressed()
                            + " dev=" + event.getDeviceId());
                }
                return super.onKeyPreIme(keyCode, event);
            }
        };
        field.setHint("Click here, then type on your keyboard");
        field.setTextSize(22);
        root.addView(field);

        log = new TextView(this);
        log.setTypeface(Typeface.MONOSPACE);
        log.setTextSize(15);
        root.addView(log);

        status = new TextView(this);
        status.setTypeface(Typeface.MONOSPACE);
        status.setTextSize(15);
        root.addView(status);

        Button enable = new Button(this);
        enable.setText(R.string.btn_enable);
        enable.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "Settings screen not available - use the adb commands", Toast.LENGTH_LONG).show();
            }
        });
        root.addView(enable);

        Button pick = new Button(this);
        pick.setText(R.string.btn_pick);
        pick.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.showInputMethodPicker();
        });
        root.addView(pick);

        Button clear = new Button(this);
        clear.setText("Clear log");
        clear.setOnClickListener(v -> DebugLog.clear());
        root.addView(clear);

        Button bt = new Button(this);
        bt.setText("3. Allow Bluetooth (to detect the keyboard connecting)");
        bt.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 31) {
                requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT"}, 1);
            }
        });
        root.addView(bt);

        Button auto = new Button(this);
        auto.setText(autoLabel());
        auto.setOnClickListener(v -> {
            ImeSwitch.setAutoEnable(this, !ImeSwitch.autoEnable(this));
            auto.setText(autoLabel());
        });
        root.addView(auto);

        scroll.addView(root);
        setContentView(scroll);
        field.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        DebugLog.keyLogging = true;
        handler.post(refresh);
    }

    @Override
    protected void onPause() {
        DebugLog.keyLogging = false;
        handler.removeCallbacks(refresh);
        super.onPause();
    }

    private String autoLabel() {
        return "Select this keyboard when the keyboard connects: "
                + (ImeSwitch.autoEnable(this) ? "ON" : "OFF");
    }

    private boolean granted(String perm) {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED;
    }

    private String buildStatus() {
        String def = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        boolean ours = def != null && def.startsWith(getPackageName() + "/");
        StringBuilder sb = new StringBuilder();
        sb.append("\nActive keyboard: ").append(def).append(ours ? "  (this app)" : "  (NOT this app)").append('\n');
        sb.append("Key events seen by the keyboard service: ").append(DebugLog.imeEvents()).append('\n');
        sb.append("WRITE_SECURE_SETTINGS: ")
          .append(granted(Manifest.permission.WRITE_SECURE_SETTINGS) ? "granted" : "MISSING (adb pm grant)").append('\n');
        sb.append("BLUETOOTH_CONNECT: ")
          .append(Build.VERSION.SDK_INT < 31 || granted("android.permission.BLUETOOTH_CONNECT") ? "granted" : "MISSING").append('\n');
        sb.append("Physical keyboards:\n");
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d == null || d.isVirtual()) continue;
            if ((d.getSources() & InputDevice.SOURCE_KEYBOARD) != InputDevice.SOURCE_KEYBOARD) continue;
            sb.append("  ").append(d.getName())
              .append(" id=").append(id)
              .append(" vid:pid=").append(Integer.toHexString(d.getVendorId()))
              .append(':').append(Integer.toHexString(d.getProductId()))
              .append(d.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC ? " alpha" : " non-alpha")
              .append('\n');
        }
        return sb.toString();
    }
}
