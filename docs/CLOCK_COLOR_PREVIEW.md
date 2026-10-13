# Temporary clock-color preview — 0.1.9 test

This NX809J / Android 16 test adds a fixed lavender status-bar clock preview lasting **at most 30 seconds from root permit publication**. It is separate from the read-only clock rehearsal and permanent cosmetic features. It has not yet passed visual phone testing.

## Scoped recovery contract

The updated guard publishes a distinct `nubia-ly-clock-v1` / `clock-color-only` receipt, with current boot ID, absolute expiry and random transaction identity. Probe and lifecycle-only receipts cannot authorize a visual change. Explicit app Start commits only that permit identity as the private opt-in; the signature/version protected System UI controls and authenticated broker retain fresh reads. Replacement, expiry, opt-out or broker failure cannot extend/replay a session. Only one rehearsal/visual session exists; starting another first restores the previous one.

The phone-validated Clock backend identifies attached top-status-bar Clock instances with inspected vendor signatures and classifier. It captures their complete stock ColorStateList before applying lavender. During the active session, two TextView setter hooks are registered because these setters are inherited: callbacks immediately filter the exact Clock class, then the tracked instance map; unrelated views keep their original behavior and arguments. Original stock writes still execute, update the saved stock ColorStateList and are followed by the temporary color. Module apply/restore writes use a recursion bypass and never replace the saved stock target.

End, app Emergency stop, guard revocation/broker failure, original expiry, screen-off and detach restore the latest full stock ColorStateList and verify it by view readback. Setter hooks are removed when the session ends. Stock changes are preserved instead of restoring an obsolete initial theme color. Partial apply errors restore already captured views, block further previews and report counts; incomplete restoration is visible and does not become success. Off-main target writes abort through the main actor with generation fencing. The original eight-second Start deadline and session generation fences remain intact.

Guard Action is independent of this app and removes all three receipts. Revocation is detected at the next bounded active validation (about two seconds); unavailable/hung broker calls fail closed on subsequent validation or absolute expiry. No permanent polling or wakelock. The root permit is the durable transaction record; original view colors are intentionally in-memory because no persistent stock state is altered. System UI recreation starts IDLE with empty override state; hooks/permits cannot auto-start. Boot service removes all permits. This scoped process-reset behavior is not a guarantee of full boot-loop recovery.

No stock APK/resource/overlay, Vector registry, SELinux policy, Toolbox feature or unrelated module is modified. Permanent feature gates remain closed. Runtime signature discovery is a limited backend check, not certification of every NX809J firmware. This draft is for the tested device/firmware first.

## Install

Update the signed 0.1.9 APK and **install the updated guard module** through your root manager. Extract the inner `nubia-ly-guard-test.zip` from the GitHub Actions artifact and install that inner ZIP. Reboot to reload Vector and the guard. Keep System UI and nubia-ly's automatic own scope enabled.

## Phone test

Use the new **Temporary clock color** card, not Clock capture rehearsal.

1. Before a permit, press Preview lavender clock: it must remain blocked.
2. In Termux:
   ```sh
   su -c 'sh /data/adb/modules/nubia_ly_guard/guardctl.sh clock'
   ```
3. Immediately press **Preview lavender clock**. Expect ACTIVE, the status-bar clock becoming lavender, Applied greater than zero and remaining equal to applied. The permit window began with the command, not the button.
4. Press **Restore clock color**, then **Check clock preview**. Expect STOPPED, stock color restored visually, restored matching applied and remaining zero. Compare with the clock before starting; the readback count alone is insufficient.
5. Publish/start again, switch between light/dark apps if stock changes its clock color, then Restore. Verify it returns to the current app's normal stock color.
6. Fresh publish/start for each: guard module Action, app Emergency stop, and 35 seconds from publication without manually ending. Each must visibly restore stock colors and report remaining zero when checked. Expect STOPPED or EXPIRED.
7. Fresh publish/start then turn the screen off. Unlock: stock clock, stopped session and remaining zero. A new permit/Start is required for another preview.
8. Reboot: IDLE, stock clock and no automatic preview. Start without a new clock permit must be blocked.

If remaining is nonzero or the color fails to restore, disable only nubia-ly in Vector and reboot, then report the exact result. Do not clear System UI data or change SELinux. Do not merge until phone results and explicit authorization.
