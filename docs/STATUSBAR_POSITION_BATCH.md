# Status-bar position batch: 0.1.21

NX809J only. Install over the previous signed build and reboot to load the updated Vector hooks. The existing guard needs no update.

## Ten controls in one APK

| Category | Horizontal offset | Vertical offset | Rotation |
|---|---|---|---|
| Compatible status icons | -8 to +8 dp | -8 to +8 dp | -20 to +20° |
| Notification icons | -8 to +8 dp | -8 to +8 dp | -20 to +20° |
| Battery | -8 to +8 dp | -8 to +8 dp | -20 to +20° |

A separate **Hide cooling pump icon** switch targets only the inspected stock `com.android.systemui:id/liquid_cooling_view` inside PhoneStatusBarView. It keeps the indicator hidden as stock cooling state updates and restores the latest stock visibility when stopped. It does not write cooling settings, hardware nodes or Quick Settings tile state. The normal individual status-icon list does not contain this separate vendor ImageView.

Offset steps are 2 dp; rotation steps are 5°. Zero preserves stock. Positive horizontal moves right, positive vertical moves down, and positive rotation turns clockwise around the existing stock pivot. These are view transforms: layout space remains stock, so combined extreme values can clip or overlap. Recognized icons are scoped to the inspected PhoneStatusBarView subtree; modern network indicator implementations may keep stock appearance.

Changes save as drafts; press Enable to apply. Wake/reboot uses the enabled snapshot, not newer drafts. Reset icons & battery clears these draft controls too; Enable applies the reset while keeping clock choices. No timed session is required.

## Combined phone test

1. Pick a working clock/date style. Set status icons to horizontal +2 dp, vertical -2 dp and rotation +10°. Set notification icons to -2 dp, +2 dp and -10°. Set battery to 0 dp, +2 dp and +5°. Enable and check the three groups independently.
2. Try opposite directions and combine with existing icon size, opacity, tint and individual hiding. Enable after edits. Inspect the home screen, light/dark apps and shade; watch for overlap, clipping or unexpected icon movement.
3. Add or dismiss a notification, switch apps and lock/unlock. Check that offsets remain steady rather than accumulating. Draft changes alone must not apply.
4. Restore stock and confirm original position/angle/color/size/opacity return. Re-enable, reboot and confirm persistence. Guard Action or Emergency stop should restore the entire enabled style.
5. Enable Hide cooling pump icon while the pump indicator is visible. Confirm it disappears while cooling and its Quick Settings tile remain unchanged. Toggle cooling through your normal controls: the indicator should stay hidden. Restore stock while cooling is on and confirm it returns; restore while cooling is off and confirm it stays absent.
6. Reset icons & battery selections, then Enable. Confirm icon/battery appearance returns to stock while clock choices remain.

Report any callback failure, clipping or incomplete restoration. Phone validation is pending.
