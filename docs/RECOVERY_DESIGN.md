# Recovery contract — planned, not implemented

Before any live customization:

1. Identify exact firmware/System UI baseline and limit hooks/resources to that backend.
2. Stage one named feature, record original state and a durable transaction ID; validate without activation where possible.
3. Install a separately disableable companion module and a root-owned recovery mechanism before activation. It must handle app death and must not rely on the modified System UI for access.
4. Test emergency disable with the target module inactive. Document recovery entry and root-manager differences for the actual device.
5. Apply only on an explicit user action. Activation remains pending until health criteria pass. Unknown completion cannot authorize replay or show success.
6. If health fails, disable only nubia-ly changes and preserve stock overlays, Toolbox modules and unrelated user modifications.
7. Escalate incomplete rollback honestly; never promise all crash loops can be recovered by an in-app button.

These requirements are a future design milestone. The foundation contains no apply engine and installs no module or watchdog. It cannot currently demonstrate recovery from a live customization because it cannot apply one.
