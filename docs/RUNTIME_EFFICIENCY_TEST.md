# Runtime efficiency test

Use the signed APK on NX809J. Guard module unchanged; no permanent root daemon is added. Rendering and vendor window operations require in-process hooks, so a root daemon would add another process without replacing those hooks. Short bounded guard leases and the recovery contract remain authoritative.

Changes:
- Fingerprint renewal is every eight seconds only while a custom icon, hidden icon or custom animation is selected. Native selections and denied authority stop recurring reads. Screen off cancels polling and restores the sensor backend; screen on or explicit editor Apply reads fresh authority. The native AOD/authentication paths remain native.
- System UI enabled renewal changes from five to eight seconds. A successful unchanged renewal updates the deadline and checks health without rescanning/reapplying status-bar views. Existing lifecycle, dark/color, attach and layout hooks still handle view changes. Screen-off bounded validation remains to retain persistent styling safely.
- Custom launcher renewal is eight seconds on Home and during screen-on transitions into other apps. Screen off and stock profiles stop renewal. Sidebar keeps its existing eight-second cached renewal and screen-off suspension to preserve pre-opening appearance.
- Applying an editor page explicitly notifies the fingerprint service, so waking a dormant stock-profile runtime does not rely on polling.

This reduces scheduled reads; it is not a measured battery-life claim. Root-fallback reads retain their integrity checks. Shared rendering, artwork caches, guard-file permissions, fifteen-second deadlines and recovery module scripts are unchanged.

Phone checks:
1. Apply a fingerprint icon/animation from stock without reboot; verify immediately, then lock/wake repeatedly. Restore stock and check normal unlock and app authentication.
2. Keep launcher customization enabled; stay in another app for a minute and return home. Check transition artwork and folder styling before/after screen off.
3. Open sidebar after leaving it closed for a minute, and after lock/wake; appearance must be ready at opening.
4. Check QS, status bar, notification, volume and power styles after rotation, app changes, dark/light changes and wake.
5. Use guard Action/Emergency stop. Enabled styling must lose authority within the existing bounded deadline; stock appearance and restart cutoffs must still work.

No merge before phone testing. For actual energy comparison use matched screen brightness/refresh, identical selected effects and comparable foreground apps; code inspection cannot establish a percentage saving.

## Native host activation follow-up

The phone diagnostic reported “No attached stock status bar” while a native QS header was already known. Activation now seeds attached verified QS headers from their existing scope registry and keeps healthy styling armed if a native window is between detach/attach. Native attachment callbacks register host ancestry even while styling is inactive. Only verified native hosts receive transforms; missing views do not expand scope or bypass guard deadlines. Real mutation/restoration failures remain fatal. Regression tests cover no-host activation followed by native attachment and an already-attached known header with no prior host registration. Test version 117 / 1.2.3-host-activation-test.
