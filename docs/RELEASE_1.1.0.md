# nubia-ly 1.1.0

A more organized personalization editor, new color controls, and expanded launcher customization for stock Nubia/REDMAGIC.

## What's new

- Preview-first editors with visible category tabs, fixed Apply/Restore actions, and Help & diagnostics in the toolbar. The main dashboard stays familiar.
- A distinct charcoal navigation band with clearer selected text and a brighter purple indicator, matching the approved concept.
- Independent status-bar colors for Wi-Fi, mobile signal/data, battery, and other system icons. Notification icons remain separate.
- Quick Settings tile colors and two-tone gradients that compose with tile styles, including Neumorph.
- Brightness track/fill colors and two-tone progress with native clipping and existing slider effects.
- QS header clock/date colors and sizes, font selection, and Native/Digital/Stacked/Outline clocks. Header status artwork follows applied status-bar selections; leave the separate header status tint Native to inherit individual colors.
- Upper QS background images, opacity, dimming and height controls, with dismissal and lock-screen visibility fixes.
- Launcher icon packs, labels, folders and layout controls with compatibility reporting across launcher versions. Report your launcher/OS version and the feature that fails; see the README for the launcher dump command.
- Row and column choices use sliders. Colors use the shared hex/RGB editor. Apply and Restore affect the current area.
- Recovery and lifecycle fixes, with scoped restoration and independent Recovery Guard support.

## Install / update

1. Install **nubia-ly-v1.1.0.apk** over your existing app. The release uses the persistent project signing certificate.
2. New users: install **nubia-ly-guard-v0.1.17.zip** in the supported root manager. Existing users already running Guard 0.1.17 / versionCode 22 can keep it.
3. Enable nubia-ly in Vector for the supported System UI/launcher targets described in the README, grant root, and **reboot** to load the updated hooks.
4. Open an editor, make your selections, then press Apply. Restore returns that area to its native appearance; Recovery Guard Action revokes active customization.

Root and Vector are required for System UI customization. Support targets NX809J stock Android 16 and the NX789J community port on Android 15/16; exact feature compatibility depends on vendor firmware and launcher version. The boot-animation loader has its own supported KernelSU/player requirements.

**Assets:** signed APK, Recovery Guard 0.1.17 ZIP, and SHA256SUMS.txt. No RRO UI-style experiment or OverlayFS mounting module is included or required.

[Installation, screenshots and troubleshooting](https://github.com/austineyoung2000/nubia-ly#installation)
