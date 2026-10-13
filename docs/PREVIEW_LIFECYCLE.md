# Preview lifecycle simulation — 0.1.5

This milestone tests session control in System UI without changing any appearance, setting, view, resource or cosmetic hook. It does not capture/restore an original visual state yet; that belongs to a validated future visual backend. The diagnostic probe can never start this simulation.

## Separate scoped permit

The updated optional `nubia_ly_guard` module provides `guardctl.sh preview`. It publishes `/data/local/tmp/nubia-ly-guard/preview` atomically, with its own exact `nubia-ly-preview-v1` protocol, kernel boot ID, absolute uptime deadline of at most 60 seconds, `lifecycle-only` purpose and random permit ID. Root ownership, directory/file modes, regular-file type, canonical paths and bounded size are checked by the app reader. A probe token, wrong purpose, malformed receipt, old boot or expired deadline is rejected. The two fixed broker read methods accept no caller-chosen path.

Start admission and activation must both occur before the original eight-second caller deadline, including System UI queue time. A queued stale Start is rejected without changing an existing session.

Start is explicit: the app validates the fresh lifecycle permit and saves a separate lifecycle-test opt-in, while the cosmetic emergency-stop latch remains stopped. System UI validates through the UID/package-protected app broker and starts only the simulation. There is no auto-start, saved active session, boot enforcement or replay after System UI restart.

The active session retains the original permit ID/deadline. Replacing the file cannot extend or switch the running session: the next validation stops it. Another Start is a new explicit attempt, still bounded by the permit's original deadline.

## Expiry and stop

System UI schedules the original expiry and rechecks the broker about every two seconds only while this explicit session is active (at most about 30 automatic validations for a full permit). There is no permanent monitoring or root polling. A fresh status request validates an active permit too. Missing/revoked/malformed/expired permits, private opt-in disarm, busy/unavailable broker, or stale replies stop the simulation. At most one worker read is admitted; a stalled read is not replayed. Caller timeout does not prove IPC/filesystem work stopped.

Generation tickets prevent a worker completing after Stop, expiry or a newer Start from reactivating the old session. Timer callbacks are canceled on termination. Status also checks elapsed realtime before reporting active: Android Handler delivery can be delayed during sleep, but an expired permit cannot be reported as a current active session. This simulation takes no wakelock and performs no visual work to restore.

The root module's Action, boot service and uninstall now remove both probe and lifecycle files from this fixed namespace. Action is reflected at the next bounded validation (normally within two seconds), or immediately through a fresh status check. App Emergency stop saves the stopped cosmetic latch, clears lifecycle opt-in and requests System UI cancellation. End lifecycle test does the same. A missing acknowledgment stays unverified; the fresh broker disarm check and original deadline remain independent fences. No stock resources, Vector registry, SELinux rules/labels/permissions, Toolbox or other modules are touched.

## Phone checklist

1. Update to the permanently signed 0.1.5 APK. Install the updated **inner** `nubia-ly-guard-test.zip` from the Actions artifact through your existing root manager. Reboot once to load both updated components.
2. Tap **Check lifecycle status**: expect `IDLE` (a newly loaded System UI never starts automatically). Start without a lifecycle permit must be blocked, even if a diagnostic probe exists.
3. In Termux publish a separate lifecycle-only permit:

```sh
su -c 'sh /data/adb/modules/nubia_ly_guard/guardctl.sh preview'
```

4. Immediately tap **Start lifecycle test**: expect System UI lifecycle `ACTIVE`, with no appearance change. Press **End lifecycle test**, then check status: expect `STOPPED`.
5. Publish again and start. Press the root manager guard module's **Action**, then check lifecycle status: expect `STOPPED` (allow up to two seconds if an automatic validation is in flight).
6. Publish again and start. Wait over 60 seconds, then check status: expect `EXPIRED`, or `STOPPED` if a final broker validation observes expiry first. No visual change should occur at any stage.
7. Publish/start again, reboot, then check: expect `IDLE`; Start must fail until a new current-boot permit is published. This checks no automatic replay and root boot invalidation for the simulation, not recovery from actual cosmetic hooks.
8. Emergency stop while active should report stopped through the System UI reply; on reopening, the test must not auto-start.

Termux independent stop fallback:

```sh
su -c 'sh /data/adb/modules/nubia_ly_guard/guardctl.sh stop'
```

If a check is unavailable, leave cosmetics disabled and provide the exact status. No forced System UI restart, SELinux change or registry edit should be used as a workaround. Full visual rollback and crash-loop recovery remain separate, unverified milestones.
