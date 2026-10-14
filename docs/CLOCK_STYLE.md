# Clock size and bold — 0.1.14 test

Status bar → Clock now offers a Material size slider (80–120% of current stock pixels in 5% steps), Use stock size, and Bold clock. Selections save without applying. Enable clock style applies them alongside color; press Enable again after editing. Restore stock clock disables the entire clock customization. No customization duration limit. Keep guard 0.1.12; install only the updated signed APK and reboot for Vector to load it.

The uploaded NX809J Clock directly inherits TextView. Its reloadDimens method uses ClockAdapt.getTextSizeId and FontSizeUtils.updateFontSize, followed by stock padding/font metrics. The hook observes that exact no-argument method alongside the existing vendor-classified top-status-bar callbacks. No dimensions, padding, resources or Quick Settings grids are replaced.

Only guarded tracked Clock views receive temporary typography. Capture actual pixel size, Typeface, fake-bold and skew state; observe inherited size/typeface setters, including nested overloads, before reasserting style. A vendor dimension update bypassing those setters is captured on the clock callback. Scale from the latest stock baseline rather than multiplying an already styled value. Module writes bypass capture. Restore uses pixel units and the latest stock face/paint flags, verifies readback and removes temporary setter hooks. Any incomplete restoration blocks another activation until System UI restarts. The existing authority, health watchdog, startup cutoff and independent stop apply unchanged.

Font size can affect available status-bar space. This test deliberately bounds the scale; do not infer support for larger values or other firmware. No device typography results are claimed before phone testing.

## Phone check

1. Install APK 0.1.14 and reboot. Open Status bar → Clock.
2. Set 80%, enable, then 120%, enable. Confirm the real clock changes, remains readable and does not overlap adjacent icons. Enable Bold, then turn it off and Enable again; stock weight must return.
3. Open several apps, lock/unlock, close/reopen nubia-ly and reboot. The selected enabled size, weight and color should return. Selections themselves must remain saved.
4. Restore stock clock. Size, weight and current stock tint should return, typography remaining zero. Lock/unlock must keep it off. Recheck guard Action and Emergency stop with an enabled style.
5. If you change stock display/font scale while testing, verify the custom percentage follows the new stock baseline and Restore returns to that new stock size. Return your stock setting afterward if desired.

No timed session or deliberate System UI crash is needed. If restoration is incomplete, disable nubia-ly in Vector and reboot. This draft remains unmerged until you report phone results and authorize it.

## Seconds — 0.1.15 test

User phone report for 0.1.14 confirms multiple sizes, colors and bold work. This does not separately establish every recovery/reboot checklist result. Version 0.1.15 adds a saved Show seconds switch; changing it does not apply until Enable. Off returns the clock to its captured stock seconds preference.

The uploaded NX809J Clock exposes isShowSeconds/setShowSeconds, updateShowSeconds, updateClock, mShowSeconds and mSecondsHandler. getSmallTime selects the stock locale/12-hour/24-hour seconds skeleton. Its own seconds handler and screen receiver perform ticking and screen-off handling. The module creates no separate ticker, wake lock, tuner-setting write or clock-format replacement. Signatures are checked only when seconds are requested. Stock seconds setter/tuner updates are recorded for restoration; nested stock callbacks cannot reassert before that capture. Restore before detach lets stock detach clean up its own handler. Stop verifies the latest stock flag and, when stock seconds are off, a cleared seconds handler. Failure blocks another activation until System UI restarts.

Keep guard 0.1.12; install signed APK 0.1.15 and reboot. Enable Show seconds with your preferred style: the real status-bar clock should show changing seconds. Open apps, lock/unlock and reboot; it should return with your saved style. Turn Show seconds off and Enable again: stock format returns. Restore stock, guard Action and Emergency stop should remove the override and report seconds remaining zero. Check that the wider time does not clip or overlap icons. No artificial duration test is required.

Vendor adapter formatting may vary across firmware; phone display validation remains pending. Seconds introduce stock second-level clock updates while awake, so battery/performance effects are not claimed verified.
