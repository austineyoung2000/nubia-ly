# Sidebar personalization — experimental device test

The sidebar captured on NX809J is FloatControl owned by com.zte.floatassist.
The supplied APK SHA-256 is b738d16731604914c2b775d0379156d92b7595d8217ae880b6be1e0d5b5f9580.
The new adapter checks this exact APK, NX809J, and SDK 36. Unrecognized versions retain stock behavior.
No compatibility claim is made for NX789J or another FloatAssist build.

## Controls

Personalizations → Sidebar has Colors & blur, Layout & size, App icons, and Apps pages.
Color/opacity/blur/roundness are independent. Follow overall UI reads applied Quick Settings inactive-background and primary-label colors. Tonal and Glass set explicit drafts. Panel width/height default to vendor dimensions; custom values are clamped to the display. One column gives a list; 2–4 give a grid. Icon scale is 70–130%.

Icons can follow the pack applied in nubia-ly's Launcher editor, use a separately selected installed standard appfilter pack, or remain stock. For a third-party launcher, choose the same pack manually. Private per-app overrides and launcher-generated themed icons are not imported. Unmapped icons retain stock artwork.

Choose additional apps from enabled launchable packages in the current profile. Stock entries and Data Dock remain. Additional entries exist only in memory while the panel is customized, never in FloatAssist's database. They open a normal app activity; this does not force freeform support. Apps without a launcher entry and other profiles are excluded. Up to 100 additional packages are supported.

## Recovery and lifecycle

The existing Recovery Guard must authorize Apply. The provider checks FloatAssist's verified package AND UID (important because it shares system UID), nonce, version, and global stop. A separate sidebar startup counter bounds repeated new-process failures. Drafts never grant runtime authority.

Reads run on one worker, only while the panel is open. A 3-second visible-panel refresh renews a maximum 15-second permit; unchanged configurations reuse artwork and skip view updates. Close or screen off restores captured dimensions, text color state lists, icon scale, backgrounds, grid columns and stock app entries. Read failure, expiry or explicit Restore does likewise. There is no background loop while closed and no database, vendor APK, SystemUI or launcher mutation.

## Phone test

1. Install the signed test build, add FloatAssist to nubia-ly's Vector scope, and reboot once.
2. Open Sidebar. Adjust a draft and leave without applying: stock sidebar should stay unchanged.
3. Apply a solid panel color first. Open/close the sidebar repeatedly; verify Data Dock and existing app launches.
4. Test blur 0 and 90 with partial opacity. The scene behind the panel should blur; text/icons must stay sharp. ROM blur availability may limit this.
5. Test width/height, each column count, icon scale, portrait/landscape and large text. Check overflow, scroll access and handle operation.
6. Select a pack used by the launcher. Verify mapped and unmapped app icons; test switching back to stock.
7. Add an app absent from the vendor picker. Verify search, selection, normal launch, removal, and disabled/uninstalled app behavior.
8. Restore stock while the panel is open, or use Emergency stop / Guard Action. Confirm restoration within the read/permit deadline and after reopening/rebooting.
9. Repeat screen off/on and multiple panel openings. Check Vector logs for adapter installation or an explicit APK-mismatch message.

Keep the separate activation/stability branch unmerged. This branch starts from main's merged fingerprint-speed commit and does not incorporate that pending fix. Device behavior and rendered UI still require owner testing before merge.
