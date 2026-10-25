# Runtime efficiency test

Use the signed APK on NX809J. Guard module unchanged; no permanent root daemon is added. Rendering and vendor window operations require in-process hooks, so a root daemon would add another process without replacing those hooks. Short bounded guard leases and the recovery contract remain authoritative.

Changes:
- Fingerprint optimization has been rolled back; see the rollback section below.
- System UI enabled renewal changes from five to eight seconds. A successful unchanged renewal updates the deadline and checks health without rescanning/reapplying status-bar views. Existing lifecycle, dark/color, attach and layout hooks still handle view changes. Screen-off bounded validation remains to retain persistent styling safely.
- Custom launcher renewal is eight seconds on Home and during screen-on transitions into other apps. Screen off and stock profiles stop renewal. Sidebar keeps its existing eight-second cached renewal and screen-off suspension to preserve pre-opening appearance.
- Fingerprint Apply behavior has been restored to the pre-optimization implementation.

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

## Fingerprint rollback and native indicator update follow-up

Test APK 119 / 1.2.5-fingerprint-rollback-test. Owner reports fingerprint regression began with the efficiency changes. Restore SensorFingerprintRuntime and SensorFingerprintBackend exactly from main fa5cb759d1a1c2907f7ce157e9be9f9979985251, including their regression fixtures, and remove the new Apply-time fingerprint service nudge. This restores the original three-second fingerprint guard checks and original screen lifecycle. No fingerprint wake/cache/polling optimization is retained. This intentionally gives up the proposed fingerprint efficiency saving to recover the previous behavior. System UI and launcher eight-second renewal and no-rescan renewal are retained; sidebar and module remain unchanged.

Re-inspected owner-supplied SystemUI_MFV.apk SHA-256 09cf76ee820ffba3414631c310fdf531c650825ddd1af95c5c927a148f678393. Exact RedMagicFunctionIconFeature fields coolingFanView/liquidCoolingView/rateRefreshView and onStatusBarInflate plus nested init/update methods are signature checked before hook registration. Native updates restore the previous native state before executing and recapture/reapply appearance afterward, including writes bypassing framework setters. No hardware/model/settings writes. Native inflate registers the verified root. Missing signatures leave this optional adapter unavailable and report it. Tests use native-shaped fields, bypassed setters and latest native visibility restoration.

Phone checks: reboot after installing; test the selected fingerprint across repeated short and long lock/wake and native face/app authentication. Apply fan/pump Hide and toggle actual cooling features; icons should remain hidden. Restore must return the latest native visibility without changing hardware. Native update hooks/updates are reported in Help. Do not merge before phone confirmation.
