# Larger status-bar batch — 0.1.17

Seven additions in one signed APK:

| Control | Behavior |
|---|---|
| Tint compatible status icons | Use the color selected in the editor when Enable is pressed. Respect the vendor stock tint opt-out. |
| Status icon size | 80–120% of each current stock view transform, in 5% steps. |
| Hide compatible status icons | Hide recognized system-status icon views in the top status bar. |
| Notification icon size | Separate 80–120% scale for recognized notification icons. |
| Hide notification icons | Hide top-status-bar notification icon views; retain notifications in the shade. |
| Battery size | 80–120% of the stock battery view transform. Preserve charge/warning colors and content. |
| Hide battery | Hide the recognized battery view; release the hide override when disabled and enabled again. |

All clock controls remain. Choose the whole batch and press Enable status-bar style. No customization timer. Restore stock status bar, Emergency stop or guard Action revokes the complete style. Keep guard 0.1.12; install APK 0.1.17 and reboot so Vector loads it.

## NX809J source and scope

The uploaded stock SystemUI_MFV.apk contains PhoneStatusBarView (FrameLayout superclass), StatusBarIconView (AnimatedImageView/ImageView ancestry) and BatteryMeterView (LinearLayout superclass), with the exact vendor getAdapt signatures. StatusBarIconView.isNotification differentiates notification from system icons. Its onDarkChanged writes ImageView tint unless the vendor donotSetIconColor gate opts out. BatteryMeterView has multiple stock drawable paths; this batch changes only its parent-view transform/visibility, never battery drawable/model data or colors.

Scope is a bounded walk of an attached PhoneStatusBarView subtree, at most 512 views and 24 ancestor levels. No shade, lock-screen, Quick Settings or unrelated views are selected. Wi-Fi/mobile indicators rendered by a different implementation are outside this backend. Do not infer that every indicator accepts tint or is represented by StatusBarIconView; phone results establish which recognized views change on this firmware.

Discovery observes attach/detach without styling until authorized. Active temporary setters record each property’s actual stock update before reapplying: scale X/Y, visibility and image tint. Module writes bypass capture. A dark-callback revision distinguishes a hooked tint write from a bypassed write; vendor tint opt-out restores and releases tint ownership. Category changes reclassify the view. New icons are tracked when attached. Detach/root removal restores owned properties and releases state. Stop restores/read backs current stock transforms, visibility and full nullable ColorStateList, removes active setter hooks and fences failed restoration. All reads/writes run on the existing UI actor, with the existing guard freshness watchdog; no extra root processes, polling service or wake lock.

Draft icon/battery settings do not replace the applied snapshot. Explicit Enable creates an identity-bound private applied snapshot, including its tint color, before approving the newly published root identity. Broker reads only the matching applied snapshot. Draft edits, failed commits, late root publication and a different/missing identity cannot authorize a newly edited icon/battery batch. Root stop removes authority independently. Sleeping restores views; valid wake/reboot resumes the applied snapshot. Existing clock selection behavior is retained.

## One combined phone checklist

1. Update APK and reboot. Status bar → Icons & battery: choose a bright color, enable tint, set status icons to 110%, notification icons to 90%, battery to 110%, then Enable status-bar style. Confirm recognized icons change without clipping; battery charge/warning colors stay stock.
2. With a notification present, toggle Hide notification icons and Enable. Its top icon disappears while its shade notification remains. Disable and Enable to restore stock visibility.
3. Toggle system-icon hiding and battery hiding, Enable, then turn them off and Enable. Confirm the recognized views disappear/return under stock visibility rules.
4. Combine with your preferred clock font, size, color, bold/italic and seconds. Open apps, lock/unlock, reopen the editor and reboot. The enabled batch should return. Edit an icon/battery draft without Enable and lock/unlock: the previously applied batch should remain.
5. Restore stock status bar: stock sizes, visibility and tint, with status-bar remaining zero. Re-enable the batch and test guard Action or Emergency stop; wake must not re-enable it. No individual APK per option or timed session is needed.

If any requested item stays stock, send the chosen options and Check status-bar style text; it may not be a recognized compatible view on this firmware. If restoration is incomplete, disable nubia-ly in Vector and reboot. The draft awaits batch phone results and merge authorization. No detailed device battery/performance validation is claimed.
