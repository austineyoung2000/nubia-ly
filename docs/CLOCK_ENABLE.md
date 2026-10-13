# Normal clock color — 0.1.12 test

**Status bar → Clock → Enable clock color** applies the selected saved color with **no customization duration limit**. Restore stock color turns it off. No Termux command is required for normal Enable: the app explicitly runs one fixed `su -c` guard command after you press Enable. Update both APK and guard module, then reboot. The old timed preview remains only in internal test code; the Clock editor presents normal Enable/Restore controls.

## Persistence and independent stop

The root-owned private guard state holds only a durable random enable identity in `/data/adb/nubia_ly_guard/clock_id`, root mode 600 inside root mode 700. The app's private authorization must match that identity. A separate readable boot-scoped receipt binds it to the current boot. Root guard Action/uninstall remove the durable identity and receipt; service only republishes already saved authority, never creates an enabled setting by itself. Restore clears app authorization and asks the guard to stop. Even if the root stop fails, cleared private authorization prevents reapplication and the UI reports incomplete stop.

Screen-off restores stock views and suspends reads while asleep. Screen-on reads enabled authorization and resumes the saved color if it is still permitted. System UI attach and the root boot-service announcement cover startup ordering without a boot polling loop. Repeated resume announcements are coalesced. Saved color is read only for resumed activation; later selection edits apply immediately only after another explicit Enable.

## Recovery

Enabled mode validates the fixed guard authority about every five seconds while awake. A separate 15-second freshness watchdog covers a stuck broker read; it is **not a user-facing customization timer**. Healthy acknowledgements keep a normal customization active indefinitely. A missing/replaced authority, private opt-out or failed read restores stock. Late replies cannot restart a stopped generation. Sleeping does not leave a color override or a polling loop running.

Freshness renewal is restricted to enabled mode and the same identity. Existing temporary preview deadline semantics are unchanged. Full latest-stock ColorStateList capture, scoped inherited-setter hooks, recursion bypass, restore/readback, detach handling and failed-restore fences remain in place.

A private per-boot startup counter allows at most three distinct System UI process startup activations. Another process startup disarms only this app's clock authorization and shows a recovery message. Same-process wake/resume does not consume another attempt. Explicit user Enable resets this guard. This is a scoped restart cutoff, not verified universal boot-loop recovery; legitimate frequent System UI restarts can also trigger it.

The app's root command uses an eight-second caller deadline including its queue. A dispatched command is not replayed after timeout; root work stays quarantined until completion. A late root publication cannot authorize application because its new identity has not been approved privately. No caller-supplied command, path or root payload is accepted.

No stock APK/resources/overlays, Vector internals, SELinux policy, Toolbox or unrelated modules are modified.

## Phone test

1. Install signed 0.1.12 APK and the updated guard's inner ZIP using your root manager. Reboot.
2. Open Status bar → Clock, select a color, press Enable clock color and grant root if asked. Expect the real clock in that color and Enabled status.
3. Use apps normally, close/reopen nubia-ly and leave the screen awake beyond the former 30-second window. Color must stay enabled without a countdown.
4. Lock/unlock. The enabled color should resume on wake. Reboot: the selected enabled color should return after System UI/guard are ready.
5. Restore stock color: visually stock, restored matching applied and remaining zero. Lock/unlock and reboot again: it must remain stock.
6. Enable again, press the guard module Action. Within the next validation (about five seconds), stock should return and remain stock after wake/reboot. Repeat with app Emergency stop.
7. Confirm saved colors and custom RGB/hex selection still work. If root times out, confirm no automatic color application; grant access, wait for the pending command to finish and explicitly Enable again.

Do not deliberately crash System UI to test the startup cutoff. It is covered with local state tests and requires more device recovery evidence before claiming crash-loop recovery. If restoration is incomplete, disable only nubia-ly in Vector and reboot. Phone testing and explicit authorization are required before merge.
