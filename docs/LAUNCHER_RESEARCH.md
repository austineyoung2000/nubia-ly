# Launcher page and OS11.5 research

The user has this launcher installed and requested a separate Launcher dashboard
tile and page. The page includes an Icons & labels detail editor, a saved draft, explicit Enable
and Restore, and the native settings shortcut. The test build adds the Launcher
Vector scope (`com.zte.mifavor.launcher`); enable it and reboot after updating.
Phone validation of the cosmetic hooks is still required.

## Uploaded research baseline

Archive: REDMAGIC_OS11.5_Launcher_APKS.rar, internal directory `16.5`.
The archive label is not the APK version.

| APK | SHA-256 |
| --- | --- |
| Launcher_MFV.apk | cce32bcfa1fce5a4c208ea510594a1c7d0e6ad614588334a12753f2819cf3f7c |
| LauncherAdapter_MFV.apk | 3c3b1f5129187ceb3f5eef9f0bda929f735f766a1967a7184f249b4275d1e60c |
| LauncherBeautifyAdapter.apk | 38b7fe2ae329960e6ca20679aa8d5c66bde1751f01e2bc30a267c0b516a7d6e2 |
| LauncherResource_MyOSInternal.apk | ea28c3e4b75b45cff8ff597c040e45b66a560ae1bf3a28b554d7fde5a445e741 |

Main package: `com.zte.mifavor.launcher`, version code `160000`, version name
`16.0.010.000.2604151532`, target/compile SDK 36. Version matching alone does
not establish identical APK contents or safe hooks.

Native settings activity `com.android.launcher3.settings.SettingsActivityMFV`
is exported without a component permission in the supplied manifest. Grid and
layout activities require `launchermfv.permission.SET_SUBSCRIPT`; the page does
not try to bypass that protection. It checks installed component enablement,
exported state and permissions before enabling the settings button.

## Findings from JADX 1.5.3

`LauncherCustomizationProvider` delegates through `com.android.launcher3.util.F`
to `n1.n` (source name `GridCustomizationsProxy.java`). Authority:
`com.zte.mifavor.launcher.grid_control`; manifest permission:
`android.permission.BIND_WALLPAPER`.

Update paths include `/set_icon_no_title`, `/set_icon_themed` (boolean_value),
`/set_icon_size_percentage` (float_value), `/default_grid`, and the conditional
concise-clock/shape paths. These are research findings, not supported app APIs.

Crucially, the supplied implementation's query method constructs MatrixCursors
for the getter paths but ends with `return null`. Provider updates also return
zero. Getter strings do not prove a usable readback/restore contract. Do not
ship root `content update` operations against these endpoints based on strings.

Icon size updates call a native layout recalculation path; they are not merely
draw-time scaling. Grid mutation can migrate workspace items. Do not extend
grid bounds or mutate launcher databases in an initial cosmetic implementation.

## Cosmetic test implementation

Only exact APK SHA-256 and NX809J Android 16 pass the app and in-process gates.
`BubbleTextView.onDraw` and its `DoubleShadowBubbleTextView` override establish a
home-screen-only draw scope. Item container/type and Workspace/Hotseat ancestry
exclude folders, all apps, widgets and non-shortcut views. Hook the drawable class
returned by `getIcon` rather than relying on its obfuscated name.

Icon scale is 70–100% of native rendering, around the drawable bounds center.
Canvas save/restore encloses the native icon draw; native bounds, cells, hit
regions, icon positions and databases are never mutated. Labels use a temporary
TextView current-color override during that synchronous draw. Restore the color
and full paint (including shadow state) in the after-hook, even when native draw
throws. Text, accessibility labels and persistent launcher preferences stay intact.

The launcher reads a separate authenticated broker method: resolved launcher UID
plus framework-verified calling package, nonce/version checks, bounded latency,
15-second maximum authority and independent startup restart accounting. It cannot
read System UI or fingerprint broker methods. Guard Action, global emergency
stop/Restore, blocked replies, screen-off, launcher pause and expiry revoke draws.
Poll only while a launcher Activity is resumed and customization is active (or the
broker reports busy); native mode does not continually poll. APK hashing and Binder
reads stay off the main thread. The launcher-only Restore leaves other areas alone.

Launcher Enable reuses current valid global guard authority when possible. If no
valid authority exists it explicitly arms the existing guard; it does not apply
unrelated editor drafts. Global stop also clears the launcher enabled preference.
Drafts remain separate from active launcher selections.

This does not enable themed app icons, folder styles, grid migration, gestures or
animation changes. Those need separate work. No provider writes, launcher data
clearing, APK replacement, automatic restart or arbitrary root commands are used.

## Validation and phone check

Host recovery/broker tests include launcher caller isolation, independent startup
cutoff and global stop. Android tests cover draft/active separation, invalid scale
rejection, temporary text/paint restoration after failure, canvas/bounds restoration,
editor navigation, recreation and native settings permission checks.

1. Install the signed test APK over the existing installation.
2. In Vector retain System UI and Fingerprint SensorService and add Launcher
   (`com.zte.mifavor.launcher`); reboot.
3. Open Launcher → Icons & labels. Try 80% size and hidden labels. Before Enable,
   home-screen appearance should not change.
4. Enable launcher customization and go home. Verify smaller shortcut icons and
   hidden labels; long-press/drag/tap targets and icon positions should be unchanged.
5. App drawer, open folders and widgets should retain native appearance.
6. Reopen nubia-ly → Launcher → Check launcher runtime. Confirm APK/hooks readiness.
7. Restore launcher appearance, go home and verify original native icons/labels.
8. Re-enable and test global Restore/Emergency stop and guard Action separately.
9. Check screen off/on and reboot/first unlock with saved enabled settings.

No guard-module update is required for the launcher-only protocol extension.
