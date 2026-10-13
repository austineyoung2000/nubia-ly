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
