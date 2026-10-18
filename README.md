<div align="center">

<img src=".github/resources/banner.svg" width="100%" alt="nubia-ly — Your REDMAGIC. Your style." />

# nubia-ly

### Personalize stock REDMAGIC, your way.

[![Android CI](https://github.com/austineyoung2000/nubia-ly/actions/workflows/android.yml/badge.svg?branch=main)](https://github.com/austineyoung2000/nubia-ly/actions/workflows/android.yml)
![Device](https://img.shields.io/badge/REDMAGIC_11_Pro-NX809J-bdb1ff?style=flat-square)
![Device](https://img.shields.io/badge/REDMAGIC_10_Pro%20%2F%2010S%20Pro-NX789J-a8e6a0?style=flat-square)
![Android](https://img.shields.io/badge/Android-16-3DDC84?style=flat-square&logo=android&logoColor=white)
![Root](https://img.shields.io/badge/Root-required-ffbe98?style=flat-square)
![Framework](https://img.shields.io/badge/Framework-Vector-bdb1ff?style=flat-square)

[**Releases**](https://github.com/austineyoung2000/nubia-ly/releases) · [**Installation**](#installation) · [**Features**](#features) · [**Report an issue**](https://github.com/austineyoung2000/nubia-ly/issues)

</div>

**nubia-ly is a standalone NX809J/NX789J customization app inspired by Iconify, built for stock Nubia/REDMAGIC System UI with Vector integration**, with a RisingOS-inspired Material interface. Make the status bar, Quick Settings, lock screen, notifications, volume panel and power menu feel like yours—then finish with a systemless boot animation.

Built and phone-tested on the **REDMAGIC 11 Pro (NX809J), running stock Android 16**, with a community port to the **REDMAGIC 10 Pro / 10S Pro (NX789J) on Android 15/16** by **Hammad Yasin**. Each area has its own dashboard entry and focused editor, with visual choices, saved drafts and explicit application.

nubia-ly is a standalone app. [Redmagic 11 Toolbox](https://github.com/austineyoung2000/Redmagic-11-Toolbox) handles hardware, gaming, cooling and lighting; nubia-ly handles personalization. They have separate packages, modules and release lifecycles.

> [!IMPORTANT]
> Support targets **NX809J stock Android 16** (original) and **NX789J / REDMAGIC 10 Pro & 10S Pro on Android 15 and 16** (community port by Hammad Yasin). Other REDMAGIC models, custom ROMs and firmware revisions need separate validation. Root access and Vector are required for System UI customization. The boot-animation loader additionally checks the supported KernelSU setup and exact firmware player.

## Screenshots

<table>
  <tr>
    <th>Icon pack picker</th>
    <th>Quick Settings layout</th>
  </tr>
  <tr>
    <td align="center"><img src=".github/screenshots/icon-packs.jpg" width="260" alt="nubia-ly icon pack picker with visual style cards" /></td>
    <td align="center"><img src=".github/screenshots/quick-settings.jpg" width="260" alt="REDMAGIC Quick Settings with a customized six-large-tile and five-column layout" /></td>
  </tr>
</table>

## Features

| Area | Make it yours |
| --- | --- |
| **Status bar** | Clock fonts, color, size, weight, seconds, spacing and dates; individual icon visibility; icon colors, opacity and placement; Wi-Fi and mobile signal artwork; 16 coordinated Status bar/QS icon packs. |
| **Battery** | Stock plus 24 alternatives, inside/outside/hidden percentage, charging-symbol visibility and adjustable size. |
| **Quick Settings** | Independent portrait/landscape grids, native large-tile arrangements, label position and line limits, brightness placement and sun-icon visibility; separate large/small tile shapes, outline/layered treatments and brightness-slider styles. |
| **Lock screen** | Clock/date appearance, built-in clock fonts, fingerprint icons and animations, plus measured charging diagnostics beneath the stock charging message. |
| **Notifications** | Notification surface styles, corners and opacity, automatic heads-up suppression, overflow counts and notification-icon limits. |
| **Volume** | A dedicated page with visual slider styles, including outline, gradient, neumorphic and layered treatments. |
| **Power menu** | Duoline, CyberPunk, Fluid, iOS and Layers treatments around the native power/restart controls. |
| **Boot animations** | Rising, Cyberpunk, Valorant, Google and Google Monet, with source-frame previews and explicit next-boot application. |

### Designed around your phone

- **One consistent interface:** dark tonal surfaces, rounded cards, visual pickers and dedicated detail pages.
- **Draft first, apply when ready:** browsing and saving ordinary style choices does not enable them. Enable applies the shared saved customization choices.
- **Saved enabled features resume:** the guard wakes the app in the background after first unlock, without opening the dashboard. Later draft edits stay unapplied.
- **Independent recovery controls:** Restore stock, Emergency stop and the Recovery Guard module's Action revoke nubia-ly customization.
- **Systemless boot artwork:** the original animation archive stays untouched. A verified file bind supplies the chosen archive for the next boot.

Tile actions, volume levels, biometric authentication and power actions remain controlled by the native firmware. Quick Settings uses the existing native tile views and pager; tile size and spacing controls are not exposed.

### Shared icon packs

Open **Status bar → Icon packs** to choose Stock or one of 16 RisingOS families: Acherus, Aurora, Circular, Filled, Gradicon, Kai, Lorn, Nostalgic, OxygenOS, Outline, Plumpy, Pavlova UI, Rounded, Sam, Victor and Xperia. Standard Quick Settings tiles are matched by function, including REDMAGIC resource aliases. Data uses the pack’s signal glyph. Quick Share, Device controls and QR scanning are included, plus nubia-ly companion glyphs for Game Space, Light Strip, fan and liquid cooling. Missing upstream artwork uses documented family-weight fallback vectors. Existing independent Wi-Fi and mobile-signal choices stay available and override the pack in the Status bar when a non-stock style is selected.

Selection saves a draft; **Enable customization** applies it. Unsupported resources, connectivity warnings and transitional states keep their native artwork. Pack coverage varies; this does not replace application notification icons, firmware APKs or system files. Restore and the guard revoke draw-time styling, and enabled choices use the existing saved-feature resume flow.

### Quick Settings layouts

- **Portrait grid:** Stock, 2, 3 or 5 columns; Stock, 1, 2, 4, 5 or 6 maximum rows. Duplicate 4-column/3-row choices are represented by Stock.
- **Landscape grid:** independent Stock or 2–6 columns and Stock or 1–6 maximum rows.
- **Large tiles:** Stock, two tiles in one row, six tiles in three rows, or four tiles in a single column. Remaining tiles stay in their original order in the small grid.
- **Labels:** stock placement, above icons or icons only, with stock or 1–3 maximum lines for small tile labels.
- **Brightness:** stock position, above all tiles or below small tiles, with optional sun-icon hiding. The native slider and auto-brightness controls remain in use.

Grid choices preserve native cell dimensions and use available width/height; extra rows and columns apply only when they fit. Expansion animations follow the fitted row count. Large-tile, label and brightness arrangements target the portrait shade; landscape has its own small-grid settings, and larger font configurations retain stock geometry. The compact shade reserves space for its selected large-tile rows so brightness, the drag handle and notifications stay below them.

Selections save a draft until **Enable customization**. **Restore stock appearance** and the guard Action restore the native layout. Layout updates are coalesced, inherited lifecycle observers are deduplicated, and individual layout callbacks refresh only the affected surface. Technical details retain the last layout/grid failure for troubleshooting. See the [layout implementation and validation notes](docs/QS_GRID_EDITOR.md).

## Requirements

| Component | Supported setup |
| --- | --- |
| Phone | REDMAGIC 11 Pro / **NX809J**, REDMAGIC 10 Pro / 10S Pro / **NX789J** |
| Firmware | Validated stock REDMAGIC **Android 16** |
| Root | Root access; current boot-animation support is validated on **KernelSU** |
| Runtime framework | [Vector](https://github.com/JingMatrix/Vector) with a compatible Zygisk environment |
| Companion module | **nubia-ly Recovery Guard**, installed through your root manager |
| App package | `com.elitedarkkaiser.nubialy` |
| Current app / guard | `1.0.0` (70) / `0.1.15` (16) |

Recommended Vector scopes are **System UI** (`com.android.systemui`) and **Fingerprint SensorService** (`com.fingerprint.sensorservice`). The latter is used for the firmware's separate fingerprint renderer. These are the app's declared scopes.

## Installation

1. Download **`nubia-ly-v1.0.0.apk`** and **`nubia-ly-guard-v0.1.15.zip`** from [GitHub Releases](https://github.com/austineyoung2000/nubia-ly/releases). Use the APK and companion module assets; GitHub's source-code archives are not installable modules.
2. Install the APK. Open your root manager's **Modules** page and install the guard ZIP, then enable the module.
3. In **Vector → Modules**, enable **nubia-ly**. Keep **System UI** (`com.android.systemui`) and **Fingerprint SensorService** (`com.fingerprint.sensorservice`) selected in its scope.
4. Reboot, unlock the phone and open nubia-ly. Grant its root request, then check **Setup & device** for the module/runtime status.
5. Choose your styles. Press **Enable customization** to apply the saved choices. The enabled features resume in the background after the first unlock on later boots; the dashboard does not need to stay open.

See the [complete installation and update guide](docs/INSTALLATION.md) for framework prerequisites, upgrades, test builds and recovery. Official release APKs use the persistent project signing key; release assets include `SHA256SUMS.txt` for download verification.

For an existing installation upgraded to the first-unlock resume build, press Enable once to capture the enabled clock settings. Later boots restore the saved enabled state without requiring another Apply.

<details>
<summary><b>Applying a boot animation</b></summary>

Open **Boot animations**, check module compatibility, choose a card and press **Apply on next boot**. Reboot to see it. Selection alone saves a draft; it does not stage the archive.

The loader verifies the firmware player, target file and selected archive. On the validated KernelSU setup it binds only that animation file, without replacing stock files or installing a general mounting metamodule. Unsupported paths or competing fallback archives block application.

**Restore stock on next boot**, or the guard's Action, disarms the animation for the next boot. An existing bind lasts until reboot. Source archives retain their original proportions and may letterbox; dynamic playback colors can differ from the preview.

</details>

<details>
<summary><b>Updating the app and guard</b></summary>

Use APKs signed with the same persistent project key to update in place. Install the matching guard when its version changes, then reboot to load updated hooks and module scripts.

Secretless fork builds use an ephemeral debug key and are not reliable in-place updates to official signed builds. See [signing documentation](docs/SIGNING.md).

</details>

## Recovery

Use **Restore stock appearance** for normal restoration, or **Emergency stop** to revoke active customization. The root-manager **Recovery Guard → Action** provides an independent stop. If the app is unavailable, its fixed Termux command is:

```sh
su -c 'sh /data/adb/modules/nubia_ly_guard/guardctl.sh stop'
```

For manual recovery, disable nubia-ly in Vector and disable its Recovery Guard module through your root manager, then reboot. Boot-animation restoration takes effect after reboot.

The boot loader disarms an unconfirmed animation attempt for the following boot. This is conservative recovery, **not a guarantee against every boot loop**. Keep a working root-manager recovery method available. See [recovery design](docs/RECOVERY_DESIGN.md) and [boot/resume implementation](docs/BOOT_RESUME_AND_ANIMATIONS.md).

## FAQ

<details>
<summary><b>Is this a ROM, an Iconify fork or part of Toolbox?</b></summary>

It is a standalone Android app with Nubia-specific runtime backends. Iconify inspires the customization experience; RisingOS inspires the visual design and supplies credited artwork. Toolbox is a separate companion project for hardware and gaming controls.

</details>

<details>
<summary><b>Will it work on another phone or an AOSP ROM?</b></summary>

NX809J stock Android 16 (original) and NX789J / REDMAGIC 10 Pro on Android 15/16 (port) are supported. An Android version match alone is insufficient: the vendor views, resources, separate fingerprint renderer and boot player must match validated implementations, which the NX789J port resolves per-firmware (e.g. the live lock-screen clock host and the guard receipt read).

</details>

<details>
<summary><b>Does it replace system APKs or boot-animation files?</b></summary>

No stock APK is replaced. Cosmetic features use scoped runtime hooks. Boot artwork uses a systemless file bind while the original archive remains unchanged. Restore revokes runtime styling; boot binds expire on reboot.

</details>

<details>
<summary><b>Why did changing a card not change my phone?</b></summary>

Most cards save the shared editor draft. Press **Enable customization** to apply it. Boot animations have a separate **Apply on next boot** action and require a reboot. Use the setup checks if the framework, guard or firmware is not recognized.

</details>

<details>
<summary><b>Do notification counts show unread messages?</b></summary>

They count eligible notification icon entries represented by the overflow indicator. They are not unread-message counts. Grouping, available space and the native icon maximum still apply.

</details>

<details>
<summary><b>Can I use other customization modules too?</b></summary>

Modules that style the same views or replace the same boot archive can conflict. When investigating a problem, test with overlapping customization disabled and include the active module list in your report.

</details>

## Development & contributions

Bug reports should include the device model, firmware/Android version, app and guard versions, root/framework versions, affected feature, reproduction steps and screenshots where helpful. Remove personal information from logs before sharing.

Development uses **test branches and draft pull requests**. GitHub Actions runs policy/backend tests, guard shell tests, Android unit tests, APK builds and signature verification. Device testing and explicit owner approval precede merging. Android builds run through GitHub Actions; device diagnostics use Termux and `su -c`.

Host policy checks:

```sh
sh tests/run-policy-tests.sh
```

Pinned boot assets are prepared by CI before Android compilation. See [asset provenance](third_party/rising-bootanimations/README.md), [research policy](docs/RESEARCH.md), and the feature implementation notes in [`docs/`](docs/). Older milestone documents record their state at the time; this README summarizes the current merged feature set.

## Credits

- **[RisingOS Revived](https://github.com/RisingOS-Revived)** — Personalizations UI inspiration, fingerprint assets/animations, connectivity and battery artwork, style references and boot animations. See [Personalizations](https://github.com/RisingOS-Revived/android_packages_apps_Personalizations), [vendor addons](https://github.com/RisingOS-Revived/android_vendor_addons) and [vendor rising](https://github.com/RisingOS-Revived/android_vendor_rising).
- **[Iconify / Mahmud0808](https://github.com/Mahmud0808/Iconify)** — inspiration for an organized Android customization experience and this README's presentation.
- **[Redmagic 11 Toolbox](https://github.com/austineyoung2000/Redmagic-11-Toolbox)** — companion project and inspiration for device-specific documentation.
- **[Vector / JingMatrix](https://github.com/JingMatrix/Vector)** and the **[Xposed API](https://api.xposed.info/)** — runtime framework and hook API.
- **[Android Open Source Project](https://source.android.com/)** and **[Material Components](https://github.com/material-components/material-components-android)** — platform and native interface components.
- **EliteDarkKaiser / Austin Young** and device testers — NX809J research, implementation and phone validation.
- **Hammad Yasin** — REDMAGIC 10 Pro / 10S Pro (NX789J) port, Android 15/16: device & SDK gates, SELinux root-assisted guard read, dual-firmware live lock-screen clock host, and Pill / custom-roundness / One UI-style QS tiles. Phone-tested on NX789J.

Upstream notices and asset provenance remain with their respective files in [`third_party/`](third_party/) and the app's bundled notices. Credits do not grant a blanket license to third-party artwork. Proprietary stock firmware/APKs supplied for inspection are not distributed in this repository.

### Apply one page at a time

Enable and Restore act on the current editor page. An area overview applies that area; a detail page applies only its own controls. Other applied pages remain unchanged, and drafts elsewhere stay unapplied. Status-bar and Quick Settings icon packs are independent. Launcher pages follow the same rule. Dashboard Emergency stop and global Restore remain available to stop all customization.
