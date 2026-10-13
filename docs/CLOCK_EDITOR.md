# Clock color editor — 0.1.10 test

Open **Status bar → Clock** from the home screen. Choose a named preset or **Custom color**, with Material RGB sliders and a six-digit RGB hex field. The local clock sample shows the chosen color. Use color saves the selection; Cancel discards the custom dialog draft. Presets also save their selection. Save failure is reported. Private preferences contain only a color, never an active session or auto-apply toggle.

The selected preference survives relaunch and reboot without changing System UI. Preview explicitly sends an immutable opaque ARGB value through the existing signature-protected/versioned control request. Missing or transparent values are rejected before session admission. Each session holds its requested color independently of later preference edits. A mismatch in the active reply remains unverified. Stock capture, restoration, guards, expiry, screen-off and process reset behavior remain as tested in 0.1.9.

The same `clock` guard permit and original 30-second window are used; changing color or starting again never renews that permit's deadline. Saving/typing/sliding does not publish a permit, alter private recovery opt-in or paint the status-bar clock. Permanent customization remains unavailable.

## Install

Update the signed 0.1.10 APK and reboot so Vector loads the new color protocol. Keep the existing **0.1.9 guard module**: no module update is needed. Other customization remains blocked.

## Phone test

1. Open Status bar, choose a preset, close/reopen nubia-ly: the selection returns and the stock clock remains unchanged.
2. Custom color: move RGB sliders, enter `#12ABEF`, and Use color. Confirm the local sample/hex update; reopen to confirm persistence. Try invalid hex, such as `FFF` or `GG0000`: Use color must keep the dialog open with an error. Cancel must preserve the previous selection.
3. Publish a fresh permit in Termux:
   ```sh
   su -c 'sh /data/adb/modules/nubia_ly_guard/guardctl.sh clock'
   ```
4. Immediately tap Preview selected color. Confirm the real clock uses the selected color. Restore stock color, then Check preview: restored matching applied, remaining zero, visibly stock color.
5. Fresh permit, select a different color and preview. While active, choose another preset: only the editor sample changes. The active clock retains its original preview color until explicit restore/expiry. A later explicit Preview request may use the remaining time on the same permit; it cannot extend expiry.
6. Confirm expiry (35 seconds from command), guard Action, Emergency stop and screen-off still restore the stock clock. Reboot: the selection persists but the clock is stock and the session IDLE. Start without a fresh permit must block.

No automatic persistent clock styling or boot-loop recovery claim is introduced. For incomplete restoration disable only nubia-ly in Vector and reboot.
