<div align="center">

<img src=".github/resources/banner.svg" width="100%" alt="nubia-ly — Your REDMAGIC. Your style." />

# nubia-ly

### Personalize stock REDMAGIC, your way.

[![Android CI](https://github.com/austineyoung2000/nubia-ly/actions/workflows/android.yml/badge.svg?branch=main)](https://github.com/austineyoung2000/nubia-ly/actions/workflows/android.yml)
![Device](https://img.shields.io/badge/REDMAGIC_11_Pro-NX809J-bdb1ff?style=flat-square)
![Android](https://img.shields.io/badge/Android-16-3DDC84?style=flat-square&logo=android&logoColor=white)
![Root](https://img.shields.io/badge/Root-required-ffbe98?style=flat-square)
![Framework](https://img.shields.io/badge/Framework-Vector-bdb1ff?style=flat-square)

[**Download builds**](https://github.com/austineyoung2000/nubia-ly/actions/workflows/android.yml) · [**Installation**](#installation) · [**Features**](#features) · [**Report an issue**](https://github.com/austineyoung2000/nubia-ly/issues)

</div>

**nubia-ly brings an Iconify-inspired customization experience to stock Nubia/REDMAGIC System UI**, with a RisingOS-inspired Material interface. Make the status bar, Quick Settings, lock screen, notifications, volume panel and power menu feel like yours—then finish with a systemless boot animation.

Built and phone-tested on the **REDMAGIC 11 Pro (NX809J), running stock Android 16**. Each area has its own dashboard entry and focused editor, with visual choices, saved drafts and explicit application.

nubia-ly is a standalone app. [Redmagic 11 Toolbox](https://github.com/austineyoung2000/Redmagic-11-Toolbox) handles hardware, gaming, cooling and lighting; nubia-ly handles personalization. They have separate packages, modules and release lifecycles.

> [!IMPORTANT]
> Support currently targets **NX809J stock Android 16 only**. Other REDMAGIC models, custom ROMs and firmware revisions need separate validation. Root access and Vector are required for System UI customization. The boot-animation loader additionally checks the supported KernelSU setup and exact firmware player.

## Features

| Area | Make it yours |
| --- | --- |
| **Status bar** | Clock fonts, color, size, weight, seconds, spacing and dates; individual icon visibility; icon colors, opacity and placement; Wi-Fi and mobile signal artwork. |
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
| Phone | REDMAGIC 11 Pro / **NX809J** |
| Firmware | Validated stock REDMAGIC **Android 16** |
| Root | Root access; current boot-animation support is validated on **KernelSU** |
| Runtime framework | [Vector](https://github.com/JingMatrix/Vector) with a compatible Zygisk environment |
| Companion module | **nubia-ly Recovery Guard**, installed through your root manager |
| App package | `com.elitedarkkaiser.nubialy` |
| Current app / guard | `0.1.65-lifecycle` (66) / `0.1.15` (16) |

Recommended Vector scopes are **System UI** (`com.android.systemui`) and **Fingerprint SensorService** (`com.fingerprint.sensorservice`). The latter is used for the firmware's separate fingerprint renderer. These are the app's declared scopes.

## Installation

1. Open [GitHub Actions](https://github.com/austineyoung2000/nubia-ly/actions/workflows/android.yml) and select a **successful build from `main`**.
2. Download `nubia-ly-signed-test-apk` or `nubia-ly-signed-release-apk`, plus `nubia-ly-guard-test-module` from that run. GitHub may require sign-in to download artifacts.
3. Extract the downloaded artifact archives. Install the **APK**, then install the **inner guard module ZIP** through your root manager. Do not flash GitHub's outer artifact ZIP.
4. Enable nubia-ly in Vector, keep its declared scopes selected, grant root access in your root manager and **reboot**.
5. Open nubia-ly and check **Setup & device**. Choose styles, then press **Enable customization** to apply them.

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

Only NX809J stock Android 16 is currently supported. An Android version match alone is insufficient: the vendor views, resources, separate fingerprint renderer and boot player must match validated implementations.

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

Upstream notices and asset provenance remain with their respective files in [`third_party/`](third_party/) and the app's bundled notices. Credits do not grant a blanket license to third-party artwork. Proprietary stock firmware/APKs supplied for inspection are not distributed in this repository.
