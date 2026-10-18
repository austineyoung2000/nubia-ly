# Launcher page and OS11.5 research

The user has this launcher installed and requested a separate Launcher dashboard
tile and page. The initial page opens the verified exported native settings
activity and reports installed version information. It does not claim working
custom launcher hooks. No additional Vector scope is needed for this stage.

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

Next: validate in-process getters and narrowly scoped cosmetic hooks, including
original-state capture, revocation on guard stop, crash/startup limits, and
restoration. Only then expose draft/apply controls for icons and labels. Folder,
grid, gesture and animation findings require separate validation. No launcher
data clearing, stock APK replacement, automatic restart or arbitrary root
commands are introduced by this stage.

## Phone check

Open Launcher from the dashboard, inspect version details, open native settings,
and return with Back. Check large font, rotation, and missing/disabled launcher
handling. This stage has no custom styling to apply or restore. Native settings
changes are owned by the launcher and are outside nubia-ly's global Restore.
