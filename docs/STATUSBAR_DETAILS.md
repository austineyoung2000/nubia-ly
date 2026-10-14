# Status-bar details batch — 0.1.18

Seven additions in one APK:

- Independent compatible status-icon color: presets and custom opaque RGB/hex, separate from clock color.
- Status-icon opacity: 25–100% of current stock alpha, in 5% steps.
- Notification-icon opacity: separate 25–100% multiplier.
- Battery opacity: separate 25–100% multiplier, retaining stock charge/warning colors.
- Individual status-icon hiding: Load individual status icons reads current stock slot names; check the ones to hide. Saved absent slots remain visible in the manager so they can be removed. Bulk hiding still applies if enabled.
- Date beside clock: Off, day of week, month/day, day/date or numeric month/day. Preserve the original stock time and spans; append the date using its calendar, timezone and locale.
- Reset icons & battery selections: reset draft icon/battery switches, size, opacity and individual hiding to stock. Retain chosen color and clock settings; press Enable to apply the reset.

Keep guard 0.1.12. Install signed APK 0.1.18 and reboot to load the new Vector code. One Enable status-bar style freezes the combined applied settings and color/date snapshot under the new root identity. Restore stock, guard Action and Emergency stop revoke the complete style. No customization duration limit or separate ticker.

## Implementation and scope

Use the same inspected NX809J top-status-bar subtree backend. New opacity observes View.setAlpha stock writes and applies the multiplier once to the latest stock baseline, preserving stock zero-alpha and animation decisions. Restore/readback includes the original alpha. Individual hiding uses StatusBarIconView.getSlot from the uploaded stock APK, only for non-notification icons. No other-device slot list, arbitrary resource names or commands are accepted. At most 32 slot names, each a validated 1–64-character identifier, are serialized under bounded data-only keys. Reclassify when slot/notification identity changes.

The live inventory is a read-only, signature-protected operation on the existing receiver. It uses the same bounded subtree walk and request admission deadline, without starting/stopping styling or reading root authority. The app verifies response code, nonce, app version, elapsed deadline, count and every string before publishing choices. Cancellation and stale replies cannot update the picker. It reveals no notification package/content list.

The exact inspected Clock.getSmallTime formatter and mCalendar/mLocale signatures are checked only when date is enabled. Preserve the stock result in a SpannableStringBuilder; use the existing stock minute/seconds ticks. Cache only the current local date/locale, refreshing on rollover. Accessibility retains the stock clock description and appends the date once. Stop/detach restores owned text/description, asks the stock clock to refresh and removes the formatter hook. Generation/state frames fence a late formatter result. Failed restoration blocks another activation until System UI restarts. Shade/lock-screen clocks remain outside the existing vendor-classified top-clock scope.

Draft color, opacity, individual hiding and date selections are separate from the applied identity-bound snapshot. Changing the draft, reopening, wake or reboot does not apply those edited values until explicit Enable. Old 0.1.17 snapshots default new properties to stock, preserving the tested prior batch on update. Existing clock selection behavior is retained.

## One batch phone checklist

1. Update APK and reboot. Set clock to one color and status icons to another using presets or Custom status icon color. Enable tint and press Enable. Confirm compatible icons and clock use their own colors.
2. Set status/notification/battery opacity to 60/80/70%, Enable. Check the expected dimming while stock charging colors remain. Return all to 100% and Enable.
3. Load individual status icons. Hide one listed icon and Enable; other recognized icons should stay unchanged unless bulk hiding is selected. Uncheck and Enable to restore it. The icon must be present/active to observe this visually.
4. Try a clock date format with your preferred font/color/size/seconds. Enable; confirm the stock time still advances, date appears once, and the wider text does not clip or overlap icons. Off + Enable restores time-only stock formatting.
5. Combine the batch, open apps, lock/unlock, reopen and reboot. The applied combination should return. Edit a draft without Enable and wake: previously applied settings remain.
6. Reset icons & battery selections, then Enable: stock icon/battery appearance while clock choices stay selected. Restore stock status bar: all remaining counts zero. Re-enable a combination and check guard Action or Emergency stop once against the full batch.

No separate APK per feature, timed session or deliberate crash test. If a requested icon stays stock, send its listed name/options and Check status-bar style text; compatible coverage remains specific to this firmware. If restoration is incomplete, disable nubia-ly in Vector and reboot. Phone validation and explicit merge authorization remain pending; battery/performance measurements are not claimed.
