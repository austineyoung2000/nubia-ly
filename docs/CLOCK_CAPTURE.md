# Clock capture rehearsal — 0.1.6

This is the prerequisite to a temporary clock-color preview. It makes **no appearance change** and does not validate actual color restoration or crash-loop recovery.

Static inspection of the supplied NX809J System UI identified `com.android.systemui.statusbar.policy.Clock` as a direct TextView subclass. Its vendor ClockAdapt exposes an MFVViewType classifier; `isTopStatusBar()` distinguishes the intended clock from other clock instances. `onDarkChanged(ArrayList, float, int)` updates the stock text color. No other-device backend is used, and the uploaded APK, decompiled code, private firmware identifiers and diagnostics are not published.

A read-only observer registers only the four inspected Clock methods: attach, detach, clock update and dark change. Signature/classifier mismatch disables the observer. It never intercepts TextView globally, rewrites method arguments, replaces methods, or calls a view setter. Host exceptions are contained. Clock references are weak. No new timers, root writes or polling are added.

An explicitly started, valid lifecycle session captures ColorStateList for attached top-status-bar clocks. Later stock updates refresh the ledger. Releasing compares the latest stock color with the still-unmodified view and discards the capture. End, expiry, app emergency stop, module Action, a replaced/revoked permit or unavailable broker release captures. Detachment releases that view; reattachment can capture a fresh view while the same session remains valid. System UI recreation starts with empty observations and IDLE; it cannot replay an active session.

The ledger deliberately keeps the latest stock color, not only the initial color. A future override must preserve stock writes and restore the latest full ColorStateList, with its own visual permit and recovery contract. This read-only comparison does not prove those future writes work. All cosmetic apply gates remain closed.

## Phone test

1. Update to the signed 0.1.6 APK. Keep the 0.1.5 guard module installed. Reboot so Vector loads the new code into System UI. Keep System UI in scope.
2. Find **Clock capture rehearsal**. Check clock capture: expect IDLE, zero captures and no appearance change.
3. In Termux publish the existing harmless lifecycle permit:
   ```sh
   su -c 'sh /data/adb/modules/nubia_ly_guard/guardctl.sh preview'
   ```
4. Immediately press **Start clock capture**. Expect ACTIVE, at least one clock capture, and your clock's usual color. Zero captures or backend unavailable is a failed discovery, not permission to force an override.
5. During the remaining session, switch between a light and dark app while watching the stock status-bar clock. Return and check capture. Stock color-change count should increase if the firmware changes its text ColorStateList. If it does not, report that result; don't force theme or resource changes.
6. Press **End clock capture**, then check: STOPPED, verified releases at least one. Clock appearance should remain stock throughout.
7. Publish/start again; use the guard module's root-manager Action, then check: STOPPED with verified releases. Repeat with app Emergency stop.
8. Publish/start again; let 65 seconds pass from publication, then check: EXPIRED or STOPPED, with verified releases. Reboot: IDLE with zero captures; no automatic session.

Send the capture/change/release counts or a screenshot. This test verifies observer discovery and read-only state handling, not live visual rollback. No Quick Settings grid change is available. If the app cannot open, disable only nubia-ly in Vector and reboot; complete boot-loop recovery remains unverified.

## Diagnostic update — 0.1.7

The initial 0.1.6 phone report reached System UI but reported backend unavailable with zero captures. It does not establish which check failed. The diagnostic update preserves the installation/callback stage and exception type in the visible result and Vector log, without recording exception messages, firmware identifiers or user content. A not-yet-initialized adapter/classifier is skipped rather than permanently failing the observer. All visual gates remain closed. Update the APK and reboot, publish the lifecycle permit and Start; send the exact backend diagnostic if it remains blocked.
