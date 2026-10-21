# Styling cleanup audit — APK 93

## Phone evidence

APK 92 diagnostics identify `Surface restoration failures: QS shape` with four shape states still pending during failed activation. Later diagnostics show zero remaining shape states and no surface restoration failures, while the parent remains stopped. This establishes that a child restoration failure propagated into a permanent parent failure even after subsequent cleanup succeeded. It does not establish which vendor write originally rejected shape restoration.

## Changes

Restoration-only failures now remain blocked while tracked state exists. Full cleanup may clear that failure only after successful restoration/read-back and removal of temporary hooks. Installation, application, callback and hook-removal failures retain their separate hard failure latch. No blanket failure reset or forced Enable is used.

| Audited area | Result |
| --- | --- |
| QS shape | Retry retained gradient states; clear restoration-only latch after verified cleanup; include restore exception/read-back failure; capture both radius representations after vendor updates |
| Clock color, typography, seconds, visibility and date | Separate restoration-only failure from permanent backend failure; parent no longer turns a child's temporary cleanup problem into a permanent clock failure |
| Status bar, QS/lock-screen surface views | Clear only restoration-only latch after own view ledger is empty; child cleanup must still succeed |
| System UI and SensorService fingerprint rendering | Same restoration-only recovery rule; apply/installation failures remain blocked |
| Battery controls | Retain changed owners until vendor stock refresh succeeds; cleanup-only latch may clear afterward |
| Charging text | Keep saved formatting until setters/read-back succeed, rather than removing saved state before restoration |
| Temporary hook lists | Remove handles only after successful unhook in affected backends, including connectivity; failed handles remain available for cleanup |
| Draw-only QS outlines, brightness, volume, notifications, battery styles, power menu, icon packs and notification behavior | Reviewed; no saved mutable geometry analogous to QS shape. Existing apply/draw failure behavior retained |
| Launcher | Reviewed separate profile/background and draw-frame restoration paths; no shared System UI latch changed |
| QS layout/grid | Existing cleanup and generation checks retained; grid already clears its failure only after successful restoration |

Persistent System UI sessions can attempt the existing bounded fresh-authority recovery after cleanup-only startup/health failure. Every attempt first restores existing state and must pass startup authorization. Explicit Restore/Stop, identity changes, blocked guard responses, and exhausted retry budget still prevent reapplication. Temporary previews never automatically restart. Guard remains 0.1.17.

## Validation

Production backend regression tests cover unresolved shape and surface restoration blocking, clearing the injected fault, complete stock restoration, then successful fresh application. Existing apply failure fences remain tested separately. Clock, date, runtime authority, status-bar, fingerprint and surface tests run with their production Java sources. Full CI and phone validation status belong in PR40.
