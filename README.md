# TV Modifier Keys

![TV Modifier Keys](docs/banner.png)

A tiny, invisible Android TV keyboard (input method) that makes **Shift** work on external Bluetooth / USB keyboards on TVs that ignore it.

On affected TVs, holding Shift and pressing `1` types `1` instead of `!`, so capitals and symbols are impossible. This app has no on-screen keys. It only turns the key presses from your physical keyboard into text.

## Features

- Shift + number row, punctuation and letters gives symbols and capitals
- Caps Lock for letters
- Ctrl+A / C / X / V, and Ctrl+Backspace / Delete to delete a word
- Alt + E / L / Y types € £ ¥ (a small table you can edit in `ModifierImeService.java`)
- Selects itself when a Bluetooth keyboard connects and returns to your previous keyboard when it disconnects
- A test screen that shows exactly which key events your TV delivers

**Tested on:** a Xiaomi Android TV (Android 14, MediaTek) with a Xiaomi Mi Portable Dual Mode Keyboard over Bluetooth. Shift + symbols and the automatic switching are confirmed there. Caps Lock, the Ctrl shortcuts and the Alt table are implemented but less tested. Reports for other TVs and keyboards are welcome.

## Is this for you?

Only if Shift (or Caps Lock) does not work on your TV with an external keyboard. Two limits to know about:

- **US layout only.** Every key is typed using a built-in US layout. If you use a non-US keyboard, some symbols will come out wrong.
- **No on-screen keyboard.** While this keyboard is selected, apps that normally pop up the TV keyboard show nothing. Switch back to your normal keyboard (see below) when you want to use the remote.

## Install

1. Enable Developer options and ADB debugging on the TV, then connect: `adb connect <tv-ip>`
2. Install the APK from the [Releases](../../releases) page: `adb install TvModKeys-v1.0.0.apk`
3. Enable and select the keyboard. On the TV this is usually under Settings > Device Preferences > Keyboard (wording varies): enable **TV Modifier Keys**, then choose it as the current keyboard. Or over adb:

```
adb shell ime enable com.memoar.tvmodkeys/.ModifierImeService
adb shell ime set com.memoar.tvmodkeys/.ModifierImeService
```

### Optional: automatic switching

To have the app select itself when your keyboard connects, grant two permissions once over adb:

```
adb shell pm grant com.memoar.tvmodkeys android.permission.WRITE_SECURE_SETTINGS
adb shell pm grant com.memoar.tvmodkeys android.permission.BLUETOOTH_CONNECT
```

Then turn your keyboard on and it is selected; turn it off and your previous keyboard returns. The switch-back needs Android 9 or newer. The automatic selection only reacts to Bluetooth connections, not to USB keyboards or 2.4 GHz dongles (those work fine when you select the keyboard manually). The test screen has a toggle to turn automatic selection off. You may need to grant the permissions again after reinstalling.

## Go back to your normal keyboard

Open the app and use button 2, or over adb:

```
adb shell ime list -s
adb shell ime set <id of your keyboard>
```

## Test screen and troubleshooting

Open the app, click the box, hold Shift and press `1`. The log shows two kinds of lines:

- `RAW` is what the app window receives.
- `IME` is what this keyboard service receives. `handled=true` means it typed the character itself.

| What you see | Meaning |
|---|---|
| `!` is typed | It works |
| `RAW` lines but no `IME` lines | The TV is not passing key events to the keyboard, or this keyboard is not the active one (see the status lines) |
| `IME` lines with `handled=false` or `ic=false` | The service sees the keys but the text field does not accept them |
| `shift=false/false` while holding Shift | Your keyboard is not sending Shift properly |

If it does not work on your TV, open an issue with a screenshot of the test screen.

## Privacy

As an input method, this app sees everything you type on a physical keyboard in text fields, passwords included. It declares **no INTERNET permission**, so it cannot send anything anywhere, and it stores nothing. Key presses are only logged in memory while the test screen is open, and only for what you type into its own test box. The source is in this repository for you to check.

Permissions:

- `WRITE_SECURE_SETTINGS`: lets the app select its own keyboard when yours connects. It can only be granted over adb.
- `BLUETOOTH_CONNECT`: lets Android tell the app that a Bluetooth device connected.

## How it works

The app is an `InputMethodService` with no input view. Some TVs deliver Bluetooth keyboard events with a virtual device id and without the Shift flag, so the system never produces symbols. The service tracks the Shift key itself and commits the right character through the text field's `InputConnection`. A small receiver for `ACL_CONNECTED` selects the keyboard when a physical keyboard appears, and an input device listener hands control back when it disappears.

## Build from source

Requirements: JDK 17, the Android SDK (platform 35 and cmdline-tools, with `ANDROID_HOME` set) and Gradle 9.x. Tested with Gradle 9.9.0, Android Gradle Plugin 9.5.0 and JDK 17.

```
gradle assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. If Gradle complains about `compileSdk`, change it in `app/build.gradle.kts` to a platform you have installed.

## License

MIT, see [LICENSE](LICENSE).
