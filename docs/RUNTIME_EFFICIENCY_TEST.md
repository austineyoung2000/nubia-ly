# Runtime efficiency test

Use the signed APK on NX809J. Guard module unchanged; no permanent root daemon is added. Rendering and vendor window operations require in-process hooks, so a root daemon would add another process without replacing those hooks. Short bounded guard leases and the recovery contract remain authoritative.

Changes:
- Fingerprint renewal is every eight seconds only while a custom icon, hidden icon or custom animation is selected. Native selections and denied authority stop recurring reads. Screen off cancels polling and restores only the ineligible visible graphic, retaining the prepared backend under its unchanged deadline; screen on refreshes it synchronously and reads fresh authority. Expiry still tears down authority. Explicit editor Apply reads fresh authority. The native AOD/authentication paths remain native.
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

## Fingerprint wake and vendor indicator follow-up

Test APK 118 / 1.2.4-wake-indicator-test. Screen off no longer destroys prepared fingerprint hooks/artwork while their existing lease remains valid. It pauses renewal and refreshes eligibility, leaving native AOD unchanged. Native sensor view attachment immediately reasserts the selected glyph when eligible. A single prepared glyph is reused for unchanged style/color instead of decoding a replacement for each refresh. Expired leases still require a fresh authenticated response; no artwork cache grants authority.

If a wake request arrives during an outstanding check, one follow-up is coalesced and replayed after a stale completion, so expiry/sleep/wake cannot silently lose the request. Tests retain lease expiry, reject stale replies, verify glyph reuse and native reattachment, and exercise wake during an in-flight request.

Vendor fan/pump hide now discovers an untracked indicator during its first native visibility write and handles both standard and transition visibility writes. Resource package, exact IDs, attached ancestry and latest-stock rollback are still required. Diagnostics expose the applied fan/pump/refresh hide flags. Test both hide switches with actual fan/pump activity, lock/wake, shade expansion and Restore; hardware operation must remain unchanged.
