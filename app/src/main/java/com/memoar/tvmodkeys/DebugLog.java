package com.memoar.tvmodkeys;

import android.view.KeyEvent;

import java.util.ArrayDeque;

/** Tiny in-process log shared by the IME service and the test screen. */
final class DebugLog {
    private static final int MAX = 14;
    private static final ArrayDeque<String> LINES = new ArrayDeque<>();
    private static int count;
    /** True only while the test screen is on screen; key presses are not logged otherwise. */
    static volatile boolean keyLogging;

    private DebugLog() { }

    static synchronized void add(String s) {
        LINES.addFirst(s);
        while (LINES.size() > MAX) LINES.removeLast();
    }

    static synchronized String dump() {
        StringBuilder sb = new StringBuilder();
        for (String l : LINES) sb.append(l).append('\n');
        return sb.toString();
    }

    static synchronized void clear() {
        LINES.clear();
        count = 0;
    }

    static synchronized void imeEvent() { count++; }

    static synchronized int imeEvents() { return count; }

    static String name(int keyCode) {
        return KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "");
    }
}
