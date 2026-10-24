# Fingerprint animation speed test

Lock screen → Fingerprint adds a 25–100% speed slider in 5% steps. Set 50% for half speed, then Apply Fingerprint. Picker previews use the draft speed. Stock and None remain unchanged.

The renderer scales custom atlas progress within the native scan animation window. It does not extend that window: a slow effect may end before showing its final frame when the phone dismisses scanning. Native frame counters, bitmaps, scan timing, completion, and unlock behavior are untouched. No timers or additional polling are added.

The optional fourth SurfaceStyle serialization component stores speed. Legacy snapshots default to 100%. Apply and Restore own this field only in the fingerprint scope; other editor tabs retain the applied value.

Device test: update the test APK and reboot to reload Vector. Compare the same effect at 100%, 75%, and 50%. Verify successful unlock, face unlock, cancellation, and screen off dismiss the effect immediately; confirm changing another page does not change the applied speed. Current Guard 22 is unchanged.
