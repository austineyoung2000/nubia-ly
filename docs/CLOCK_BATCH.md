# Status-bar clock batch — 0.1.16

This single NX809J test build contains four additions:

- Italic clock: combine with bold or use alone; retain the stock typeface/paint baseline for Restore.
- Letter spacing: offset the current stock spacing by −0.05 to +0.15 em in 0.01 steps. Zero means stock. Repeated updates do not add the offset repeatedly.
- Font family: Stock, Sans serif, Serif or Monospace using platform built-in families. No downloaded or injected font files. Combine with size, weight and color.
- Hide status-bar clock: hide only the vendor-classified top-status-bar Clock. Turning the switch off then Enable releases the hide override; stock policy still decides whether the clock can be visible.

Selections save without applying. Enable clock style freezes and applies the complete saved selection, with no customization duration limit. Restore stock clock, Emergency stop and guard Action revoke the complete style. Keep guard 0.1.12. Install signed APK 0.1.16 and reboot to load the new Vector code.

## Implementation and restoration

Use the uploaded NX809J Clock's direct TextView superclass and already inspected callbacks. Typography captures the latest stock pixel size, Typeface, fake-bold/skew and letter spacing. Stock setter callbacks record their actual result before repaint; module writes bypass capture. Relative spacing is applied once from the stock baseline. Restore verifies spacing and typeface/paint readback and removes temporary hooks. Unexpected failures fence further activation.

Hide uses the inspected Clock.setVisibility(int), preserving its shouldBeVisible policy and vendor adapter. Observe stock visibility decisions after setter/state-restoration callbacks, re-hide only tracked clocks, and restore the latest stock decision through that same setter. Do not alter visible-by-user or visible-by-policy fields, tuner settings, layouts, resources or icon configuration. A refused visible restoration is reported as incomplete rather than bypassing stock policy.

The inspected Clock saves visibility in a Bundle. Replace only that saved visibility entry with the captured stock value, preserving every other entry, including the seconds backend's stock show_seconds entry. Observe recreation before reapplying. Hidden clock does not disable an explicitly enabled stock seconds handler; seconds remains an independent saved option. Guard revocation restores all owned properties and stock seconds together.

Built-in font appearance and fit still need phone confirmation. These changes are scoped to the status-bar clock; battery, network icons, Quick Settings and lock-screen clocks are outside this batch.

## One combined phone checklist

1. Install and reboot. In Status bar → Clock, leave Hide off. Try Serif with italic, then Monospace with bold; press Enable after each selection. Verify a visible font/weight change.
2. Try letter spacing +0.05 and −0.03. Press Enable each time. Confirm wider/narrower gaps without clipping or icon overlap. Set zero and Stock font to verify stock spacing/family returns while your chosen color remains.
3. Combine your preferred font, italic, spacing, size, bold, color and seconds. Open apps, lock/unlock, reopen the editor and reboot. Saved enabled styling should return; seconds should still advance.
4. Enable Hide clock and press Enable. Only the status-bar clock disappears. Turn Hide off and press Enable: clock returns when stock policy allows it.
5. With several options enabled, Restore stock: stock font, spacing, weight, size, tint and visibility; all remaining counts zero. Re-enable and test guard Action or Emergency stop once against the combined style. Wake must not re-enable a stopped style.

No timed test or separate APK per control. Report any visual problem with the chosen combination and Check clock style status. If restoration is incomplete, disable nubia-ly in Vector and reboot. This draft remains unmerged pending batch phone results and authorization.
