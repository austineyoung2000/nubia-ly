# Recovery contract — prerequisites implemented; boot recovery pending

Before any live customization:

1. Identify exact firmware/System UI baseline and limit hooks/resources to that backend.
2. Stage one named feature, record original state and a durable transaction ID; validate without activation where possible.
3. Install a separately disableable companion module and a root-owned recovery mechanism before activation. It must handle app death and must not rely on the modified System UI for access.
4. Test emergency disable with the target module inactive. Document recovery entry and root-manager differences for the actual device.
5. Apply only on an explicit user action. Activation remains pending until health criteria pass. Unknown completion cannot authorize replay or show success.
6. If health fails, disable only nubia-ly changes and preserve stock overlays, Toolbox modules and unrelated user modifications.
7. Escalate incomplete rollback honestly; never promise all crash loops can be recovered by an in-app button.

These requirements are a future design milestone. The foundation contains no apply engine and installs no module or watchdog. It cannot currently demonstrate recovery from a live customization because it cannot apply one.

## Implemented prerequisite in 0.1.1

A private durable emergency-stop latch defaults to stopped and can only be stopped in this milestone. Failed persistence is reported honestly. There is no arm action, apply engine, or System UI customization hook. RecoveryPolicy requires every prerequisite (activation, validated baseline, independently verified recovery, validated feature, no emergency stop). Future apply code must use this gate; currently all features remain unavailable regardless of policy inputs.

The activation probe hooks only nubia-ly's own status method. It does not confirm System UI loading or health. The existing System UI log entry remains the separate loading check.

The latch cannot recover a boot loop, remove Vector scope, unhook a running process, or undo overlays. Manual fallback while the manager is accessible: disable only nubia-ly in Vector, then reboot. Root-manager-specific recovery without a working UI still needs implementation and device testing before live cosmetic work.
