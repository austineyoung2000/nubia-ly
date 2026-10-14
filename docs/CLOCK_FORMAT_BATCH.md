# Clock date formatting batch: 0.1.20

NX809J only. Update the signed APK over 0.1.19, then reboot so Vector loads the new hooks. Keep nubia-ly scoped to System UI. The existing guard requires no update.

## Included together

- New formats: full weekday, full month and day, day and abbreviated month, day/month, month/day/year, day/month/year, ISO date, full weekday/month/day.
- Independent date position: before or after stock time.
- Independent separator: dot, space, dash or bar.
- Independent letter case: stock, uppercase or lowercase. Only the date changes case.
- Existing date formats, clock color/size/bold/italic/spacing/font/seconds, and icon/battery controls remain available in the same batch.

These choices stay saved but require Enable to apply. Date Off ignores position/separator/case without discarding those choices. Existing date selections retain their previous dot separator and after-time position. Long dates consume width; choose a smaller clock size if needed.

## One combined phone test

1. Pick a clock color and typography you already use. Choose Full weekday, Before time, Bar and Uppercase, then Enable. Expected example: `TUESDAY | 09:21` (weekday/time follow your phone).
2. Try Day/month/year after time with Dash and ISO date before time with Space. Press Enable after each selection. Check seconds both on and off, and 12/24-hour behavior if you normally switch it.
3. Switch apps, pull down the shade, lock/unlock and reopen nubia-ly. Confirm the enabled choices persist and the date does not repeat or leak into the shade clock. Changing a draft alone must not change the enabled style.
4. Choose Date Off and Enable. The time should remain customized with no date. Restore stock status bar and verify the date and all selected appearance overrides are removed.
5. Re-enable a combined clock/icon style, reboot, and confirm persistence. Use the guard Action or Emergency stop and confirm the whole style returns to stock.

Report any clipped text, missing date, failure message or incomplete restoration. No timed session or waiting period is required.

## Validation scope

Automated tests exercise all 288 active date combinations through the production backend, restoration, accessibility suffix deduplication, midnight/timezone rollover, unrelated clocks, stale callbacks and restoration failure fences. Policy tests verify concrete US date outputs, Turkish casing, timezone behavior, legacy IDs and rejection of invalid selections. Phone results are still required; these tests do not establish visual fit on every setting combination.
