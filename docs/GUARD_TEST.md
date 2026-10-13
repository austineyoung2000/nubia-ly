# Independent recovery guard — probe-only phone test

This milestone adds a separately installable root-manager module, `nubia_ly_guard`. It provides a stop path that does not need nubia-ly or System UI. It does not alter Vector's registry, stock overlays, Rescue Party, APKs, system properties, SELinux or other modules. No root manager is installed/configured by the app.

The only state is `/data/local/tmp/nubia-ly-guard/probe`, inside a root-owned mode-755 directory. A probe is published atomically and lasts at most 60 seconds for the current kernel boot ID. Rootless readers require regular root-owned non-writable paths, an exact protocol, the same boot ID and a bounded expiry. Missing/unreadable/expired/malformed state fails closed. SELinux restrictions are not relaxed; phone testing must establish whether this NX809J firmware permits reads from the app and System UI.

`service.sh` removes the probe on boot. A previous-boot probe is invalid even before that script runs. The manager's Action button and uninstall script remove only the guard's receipt. There is no always-running watchdog, no automatic replay and no auto-arm. This token is explicitly `probe-only`: it cannot authorize a cosmetic change, and every feature remains blocked.

## Phone test

1. Install the permanently signed test APK (one final uninstall of the old ephemeral-debug version may be needed). Enable nubia-ly/System UI in Vector and reboot.
2. Install the guard ZIP through the existing root manager and reboot. Do not enable any customization. The app does not install the ZIP itself.
3. Tap **Check recovery guard**. Expect blocked/missing; no probe exists by default.
4. In Termux publish a harmless 60-second transport probe:

```sh
su -c 'sh /data/adb/modules/nubia_ly_guard/guardctl.sh probe'
```

5. Immediately tap **Check recovery guard**. Expect “Recovery probe readable for this boot.” If unreadable, report it; do not change SELinux policy.
6. Press the root manager module's **Action**, then check again. Expect blocked. This works independently of the app. Termux fallback:

```sh
su -c 'sh /data/adb/modules/nubia_ly_guard/guardctl.sh stop'
```

7. Publish again, wait over 60 seconds, then check. Expect expired/blocked.
8. Publish again and reboot. Expect blocked afterwards. Inspect module status if needed:

```sh
su -c 'sh /data/adb/modules/nubia_ly_guard/guardctl.sh status'
```

Do not regard the app probe as proof that System UI can read it. The System UI entry independently logs its guard status on process load; no cosmetic hooks are installed. System UI readability, root-manager execution and reboot behavior need actual phone evidence.

## Limits and next gate

This is an independent stop/boot-invalidation transport test, not complete boot-loop recovery. Deleting a receipt cannot undo hooks already installed in a running process. Current System UI hooks are inert. Before future cosmetics, implement a bounded preview transaction and ensure all feature entry paths recheck a validated guard; reboot with an invalid receipt must skip those hooks. Validate recovery without the app/UI on the actual firmware/root environment. If normal manager access is available, disabling only nubia-ly in Vector and rebooting remains the manual fallback. Do not guess Vector database or root-manager safe-mode paths.

Magisk/KernelSU module entry points follow their official module guides. Their behavior on this phone and APatch behavior are not yet claimed as verified. Removing/disabling the guard does not remove Vector or Toolbox.

## Live System UI diagnostic (0.1.3)

Update the signed APK normally, keep Vector enabled with System UI scope, and reboot once to load the diagnostic. The app registers a signature permission; inside NX809J/Android 16 System UI the Vector module installs one Application.attach lifecycle hook to register a read-only dynamic receiver. This uses the platform lifecycle, not a guessed Nubia cosmetic method. Receiver registration failure is caught and leaves the check unavailable. No visual hooks are installed.

The receiver is exported only so nubia-ly can reach it, and requires the app's signature-level GUARD_DIAGNOSTIC permission. Requests target com.android.systemui; only a version-matched response with the current random request nonce and within eight seconds is displayed as evidence. It reads only the existing bounded root-owned probe. At most one background read may be in flight; a stalled worker is not replayed and cannot queue more work. A caller timeout does not claim that filesystem work stopped. Late replies and callbacks after app closure are ignored. No automatic check, heartbeat, root grant, polling, stock resources or Vector registry changes.

After reboot tap **Check System UI guard** before publishing a probe. Expect “System UI replied, but its guard probe is blocked or unreadable.” This proves the diagnostic replied, not guard readability.

Publish the existing 60-second probe in Termux, then immediately tap **Check System UI guard**. Expect “System UI read a valid current-boot probe.” Stop using module Action or the existing stop command, then check again. Expect a System UI reply reporting blocked. Repeat for expiry and reboot as desired. If there is no reply, check System UI scope and Vector logs; do not restart/kill System UI or change SELinux.

The existing guard module is unchanged and does not need reinstallation. Probe readability is diagnostic evidence, not permission to apply cosmetics or proof of future boot-loop recovery.
