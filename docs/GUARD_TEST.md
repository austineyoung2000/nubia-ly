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

## Live protected-broker diagnostic (0.1.4)

Update the signed APK normally, keep Vector enabled with System UI scope, and reboot once to load the diagnostic. The app registers a signature permission; inside NX809J/Android 16 System UI the Vector module installs one Application.attach lifecycle hook to register a read-only dynamic receiver. It uses the platform lifecycle and no Nubia cosmetic methods. Failures leave the check unavailable. No visual hooks are installed.

The receiver is exported so nubia-ly can reach it and requires the app's signature-level GUARD_DIAGNOSTIC permission. Requests target only registered System UI receivers. System UI no longer accesses the guard files: it calls a narrowly exported app ContentProvider, which requires Binder UID equal to the current-user System UI UID resolved by PackageManager and the framework-verified calling package com.android.systemui. It supports only inspect_probe with a fresh nonce and matching version. No caller-supplied paths, root commands, raw files, read queries or mutations are supported; URI grants are disabled. Identity is checked before dropping Binder identity for the app-side read.

Package attribution is verified to belong to the UID; this is Android's UID security boundary and cannot distinguish hostile code already sharing/controlling System UI's UID. Ordinary other application UIDs are rejected. The signature permission protects the diagnostic request; the provider instead uses UID/package validation because System UI is not signed with the nubia-ly key.

The app-side broker validates the existing bounded root-owned probe anew on every request. No cached positive results or polling. Missing, expired or malformed state returns blocked. Broker busy, denied, absent, force-stopped or invalid/version-mismatched replies are unverified. The provider and System UI each admit at most one read at a time. A stalled read stays in flight and is not replayed; the eight-second caller deadline does not prove it stopped. Nonce/version/deadline checks apply to broker replies and end-to-end diagnostic replies. Late results after app closure are ignored.

### Phone check

1. Update to 0.1.4 and reboot. The existing guard module requires no update. Tap **Check System UI guard** before publishing a probe: expect “System UI reached the broker; probe is blocked, expired or unreadable.” This proves broker reachability, not a valid probe.
2. Publish the existing 60-second probe in Termux, then immediately tap **Check System UI guard**: expect “System UI received a fresh valid probe through the protected broker.”
3. Stop with module Action or the existing Termux stop command. Check again: expect broker-blocked. Repeat expiry/reboot invalidation if desired.
4. If broker-unavailable, provide the exact result; no SELinux change or System UI restart should be used as a workaround. Relaunching the app is not required for ordinary checks; the provider can be started on demand, while force-stop/unavailability fails closed.

The original 0.1.3 direct-reader diagnostic was blocked on the tested phone. The broker avoids that read entirely and adds no SELinux rule, relabeling or chmod change. Guard receipt validity remains evidence for this transport test, never cosmetic authorization or verified recovery from future hooks.

The 0.1.5 guard module adds a distinct `preview` permit for the [no-visual-change lifecycle test](PREVIEW_LIFECYCLE.md). Probe diagnostics remain unchanged. Action/boot/uninstall clear both fixed entries; install the updated module before using the new preview command.
