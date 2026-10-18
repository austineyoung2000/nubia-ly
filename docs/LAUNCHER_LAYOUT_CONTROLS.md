# Launcher layout expansion

Exact launcher APK gate remains required. The native DesktopGrid class is
`com.android.launcher3.b0`; its static catalogue `s` initially contains rows/cols
6/4 and 6/5. Twenty-three additional entries provide a total of 25 choices,
columns 4..8 and rows 6..10, using its native spacing profile. Nubia-ly changes
the in-memory choice catalogue. The native DesktopGridActivity fragment has
two hard-coded radio rows, so catalogue expansion alone cannot add visible
choices there. Nubia-ly now exposes all 25 choices in its own grid picker.
An explicit signature-protected, bounded request performs a fresh guard read
and invokes H5.T(context).H().e(context, encodedRowsColumns) in the launcher
process. Polling never calls this operation; expired, duplicate, unsupported,
busy or unverified requests cannot initiate a reflow. The launcher owns
preference saving, workspace reflow and widgets.
No launcher database, resource-registry file or stock APK is edited. Removing
the extra catalogue does not rewrite the currently selected native grid.

Folder limits use the inspected FolderGridOrganizer and current DeviceProfile
folder dimensions/capacity. Open-folder rebind creates the matching organizer
through its native constructor, updates cell measurements and lets native
arrangement/paging run. A 50–100% folder icon slider scales only icon drawing
inside open folders, including the Add artwork; labels and touch targets retain
native geometry. Legacy seven-field layouts migrate to 100%. Original profile values are retained and restored with
ownership checks. Close/reopen the folder after changing its layout.

Home page dots use draw-scoped tint/scale/hide adapters. Paint colors and canvas
state are restored after drawing; folder-owned indicators are excluded. Dock
icon scaling is independent and uses the same transition-bound correction as
home icon scaling. Dock and indicator size controls use continuous sliders
with 1% steps; dock also offers inheritance from home-screen sizing. All new settings use page-scoped activation and validated
broker payloads. No extra polling or automatic launcher restart is introduced.

## Dock count: intentionally not enabled

The inspected loader rejects hotseat records with screenId >= F1.e1 (database
hotseat capacity); resource config_num_hotseat_icons sets both visible and
database capacities. The inspected GridSizeMigrationDBController.w returns
false after its state update, so AOSP hotseat migration cannot be assumed.
Raising capacity only while injected could leave entries out of bounds when
injection is disabled or the count is restored. A verified migration/recovery
path is required before exposing additional dock slots. No count toggle or
placeholder is shipped as a working feature.

This branch requires phone testing of grid selection/reflow, widgets, folder
paging/order, gesture return, sleep/reboot and explicit restoration.
