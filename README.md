# nubia-ly

**A standalone customization app for stock Nubia/REDMAGIC firmware, starting with the REDMAGIC 11 Pro (NX809J).**

The goal is an Iconify-style customization experience built around Nubia's own System UI: status-bar appearance, Quick Settings styling, lock-screen clocks and notification visuals. nubia-ly is independent of Redmagic 11 Toolbox, with its own Android package, repository and release lifecycle.

> **Current state: working NX809J clock, compatible-icon and battery-view customization, with another details batch in testing.** Merged clock controls include persistent color, size, bold, seconds, italic, spacing, built-in font families and hiding. The user has confirmed these batches on the phone. Compatible status-icon tint/size/hiding, notification-icon size/hiding and battery-view size/hiding are merged after the user reported more than ten minutes of full-batch testing. The next signed test adds independent icon colors, opacity, individual hiding and clock dates. Root and Vector are required for styling; an independent guard supports revocation and bounded health recovery. Full device boot-loop recovery is not claimed.

Development uses test branches, draft PRs and GitHub Actions signed APKs. Phone testing and explicit user authorization precede merges into `main`.

## Inspiration and technical foundation

| Project / source | Role in nubia-ly |
| --- | --- |
| [Iconify](https://github.com/Mahmud0808/Iconify) | Product and architectural inspiration: an app that organizes Android UI customization through overlays and Xposed features. Iconify's Pixel/AOSP backends are not assumed compatible with Nubia firmware. |
| [Vector by JingMatrix](https://github.com/JingMatrix/Vector) | Selected Xposed framework for future Nubia-specific runtime hooks. Vector supports legacy Xposed and modern libxposed APIs. |
| [Xposed API](https://api.xposed.info/) | The foundation module uses Vector's supported legacy API 82 entry point, as a compile-only dependency supplied by the framework at runtime. |
| User-provided NX809J stock System UI | Device-specific static research baseline for locating Nubia classes, resources and feature gates. The proprietary APK and private device research are not distributed in this repository. |
| Android and Material Components | Native Android application and Material 3 interface. |

This is an original scaffold, not an Iconify fork. No Iconify source, assets or Vector framework implementation are bundled. If code is reused from another project later, its attribution and license requirements must be addressed with that change.

## Initial scope

- Device: **REDMAGIC 11 Pro, NX809J only**.
- Firmware research: stock REDMAGIC Android 16. Build-specific baseline values are optional local configuration, not published source data.
- Android package: **`com.elitedarkkaiser.nubialy`**.
- Vector's recommended module scope: **`com.android.systemui` only**.
- Root: an explicit `su -c 'id -u'` check in this prototype. Root-manager compatibility and the installed Vector/Zygisk combination still require phone testing.
- Other Nubia/REDMAGIC devices: possible future ports, each requiring its own firmware inspection, backend and device verification. They are not supported by this build.

A firmware/hash match identifies an explicitly configured baseline. **It does not certify a customization as safe or enable a feature.** A changed firmware, SDK or System UI APK requires fresh validation.

## What exists now

- Material 3 dark interface with device inspection, feature catalog and recovery planning information.
- Explicit System UI APK hashing outside the UI thread, with an inspection deadline.
- A bounded, user-initiated root check; no root request on startup.
- Vector/Xposed module metadata and `assets/xposed_init` registration.
- An inert module entry point that checks package, main process, model and Android version, then logs loading without installing any hooks.
- Pure compatibility-policy tests and Android unit-test sources.
- GitHub Actions workflow for unit tests, a debug APK, a permanent-key test APK and a signed release APK when signing secrets are configured.

There is no settings write, overlay fabrication, System UI restart, boot receiver, automatic enforcement, framework installer or module installation in the current app. It does not change Toolbox's cooling, lighting, triggers or recovery services.

## Planned customization areas

These are development goals, not currently available features.

| Area | Planned investigations |
| --- | --- |
| Status bar | Icon appearance/visibility, clock styling and battery presentation. |
| Quick Settings | Tile shapes/colors, spacing, header appearance and resource/hook-specific layouts. |
| Lock screen | Clock appearance, positioning and appropriate shortcuts. |
| Notifications | Panel appearance and notification presentation. |
| Profiles | Save and restore only validated customizations with explicit application. |
| Recovery | Scoped emergency disable, recorded original state and failure-aware activation. |

Changes must use proven NX809J paths, classes and firmware behavior. An AOSP feature name or bundled class is not enough to assume a Nubia implementation works.

## Development roadmap

1. **Build and test the foundation.** Establish the GitHub repository, run Android CI, install the app, verify model refusal and firmware/root inspection, then confirm Vector can load the inert entry point.
2. **Implement the recovery foundation.** Prepare an independently disableable customization backend, durable transaction state and a recovery path that does not depend on a healthy modified System UI. Test disabling while cosmetic hooks remain inactive.
3. **Validate one cosmetic feature.** Select a small status-bar appearance change after tracing the exact Nubia implementation. Verify apply, restore, process recreation and recovery before enabling it for users.
4. **Expand one feature at a time.** Add supported status-bar, Quick Settings, lock-screen and notification changes with per-build gates and rollback behavior.
5. **Add profiles and broader device support.** Only after feature-specific behavior is proven; new models need separate testing.

Vector is selected for runtime hooking. Whether particular changes also need resource overlays or a separate root companion remains a feature-specific decision. A working Vector framework alone does not make an arbitrary System UI hook compatible.

## Quick Settings layout validation

Quick Settings grid changes are blocked until the active vendor layout, pagination, animations and recovery behavior have been validated together. A row-count resource by itself is insufficient evidence that a larger grid is supported. No grid override is exposed in this prototype. See [research policy](docs/RESEARCH.md).

## Recovery and change policy

- No activation until the feature's backend and scoped recovery mechanism are implemented and tested.
- Every feature starts disabled and is applied only through an explicit user action.
- Capture original state before changes and retain a durable operation identity.
- Unknown completion cannot authorize automatic replay or be reported as success.
- Recovery must disable only nubia-ly changes, preserving stock overlays and unrelated user modules.
- Do not disable Rescue Party, clear System UI data, replace stock APKs or edit package registries as shortcuts.
- Do not promise that an in-app restore button can recover every crash loop.

The independently revokeable guard, private authorization, freshness watchdog and scoped restoration are implemented for status-bar styling. The broader recovery roadmap remains incomplete; see [recovery design](docs/RECOVERY_DESIGN.md).

## Local compatibility baseline

For device research, optionally create an ignored `local-baseline.properties` with `fingerprint` and `systemUiSha256` values obtained from the build being inspected. Do not commit that file or private device logs. Without this configuration, the app still performs inspection but reports the firmware as unvalidated. A configured match never enables a customization automatically.

## Build and APKs

Requirements: **JDK 17**, Android SDK **platform 35 / build-tools 35.0.0**, and **Gradle 8.7**. Runtime validation targets the observed Android 16 firmware. SDK location belongs in ignored `local.properties`.

```sh
./gradlew testDebugUnitTest assembleDebug
```

The wrapper downloads Gradle 8.7 on first use. Alternatively use an installed Gradle 8.7 with the same task names.

To run compatibility-policy checks without Android SDK:

```sh
sh tests/run-policy-tests.sh
```

GitHub Actions produces `nubia-ly-signed-test-apk` and `nubia-ly-signed-release-apk` using the dedicated persistent key. These repository secrets are configured:

- `RELEASE_KEYSTORE_BASE64`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

The public certificate digest is pinned in `docs/SIGNING_CERT_SHA256.txt`; CI verifies both APKs before publishing. Secretless fork CI produces an explicitly ephemeral debug artifact. Incomplete signing configuration fails. See [signing notes](docs/SIGNING.md).

**Validation at scaffold creation:** pure Java policy smoke tests passed; manifest/resource XML, workflow structure, module registration and source archive were checked. Android compilation, policy checks and unit tests subsequently passed in [GitHub Actions](https://github.com/austineyoung2000/nubia-ly/actions/runs/38456648254) at `6d892d4376fa6420eb50addac79c52996bba5751`; a [debug test APK](https://github.com/austineyoung2000/nubia-ly/actions/runs/38456648254/artifacts/11842337147) is available. [PR1](https://github.com/austineyoung2000/nubia-ly/pull/1) was merged after user phone checks for app activation and emergency stop. Independent System UI loading and boot recovery remain unverified. Permanent signing is configured for the 0.1.2 test branch.

## Phone testing and contribution workflow

- Test branches and draft PRs; no automatic merge.
- The owner tests APKs on their NX809J and explicitly authorizes merges.
- Before editing, inspect current remote HEAD and preserve dirty worktrees.
- Device commands use **Termux and `su -c`**, never adb.
- Commit messages use an area prefix and explain the problem, implementation, behavior and safeguards. Testing evidence belongs in the PR description.
- Do not claim successful phone verification without supplied results.

For the first APK, test app launch, firmware/root inspection and optional inert Vector module loading. There are no cosmetic toggles to test yet. See [Vector integration](docs/VECTOR.md).

## Development history

The following entries describe earlier milestones; the current feature summary is above.

### Recovery foundation update (0.1.1, merged)

The app now shows Vector activation detected by an app-only probe. This does not prove System UI injection; check the separate module log after selecting System UI and rebooting. System UI continues to register zero customization hooks.

A persistent emergency-stop latch defaults to stopped. The app can save the stop, but cannot arm or apply any feature. The prerequisite policy fails closed unless activation, baseline, recovery and feature validation all pass. Full independent boot recovery remains pending; this milestone does not claim boot-loop protection. Disable nubia-ly in Vector and reboot for the manual fallback.

## Independent guard test (0.1.2, draft)

A separate probe-only root-manager ZIP provides an independent Action/Termux stop and boot invalidation. A root-owned atomic probe is bound to the current boot and expires within 60 seconds. Rootless app/System UI readers fail closed on missing, malformed, unsafe or unreadable state. No SELinux policy is changed, and no customization can be activated. See the [phone checklist and limits](docs/GUARD_TEST.md). This is a prerequisite transport test; complete recovery from future cosmetic hooks still requires validation before those features ship.

## System UI guard verification (0.1.4, draft)

**Check System UI guard** sends one signature-protected read-only request to running System UI. System UI asks the app-side protected provider for a fresh guard validation, because direct access may be blocked by the firmware SELinux policy. The provider checks the resolved System UI UID and framework-verified package attribution before reading the one fixed root-owned probe. Fresh version/nonce-matched replies distinguish broker-valid, broker-blocked, broker-busy and broker-unavailable; absent/stale replies stay unverified. No raw file or boot identifier is returned. One diagnostic lifecycle hook registers the receiver, with no cosmetic hooks or root commands. No continuous polling. Update using the existing permanent key and reboot to load the new receiver. The guard module itself is unchanged. See the [live diagnostic phone test](docs/GUARD_TEST.md).

## Preview lifecycle simulation (0.1.5, draft)

An explicit lifecycle-only test uses a separate current-boot permit lasting at most 60 seconds. System UI checks it through the protected broker, expires the session automatically and stops when the permit is revoked/replaced or the broker is unavailable. Late workers cannot restart a stopped attempt. Bounded two-second validation runs only during this simulation, with no permanent polling or root commands. Root module Action and app Emergency stop revoke the test through independent state checks. Restart/reboot never auto-starts it.

This milestone changes no appearance and provides no cosmetic permission. Original visual capture/rollback and crash-loop recovery remain future backend work. Update both the signed APK and guard ZIP, then follow the [phone lifecycle checklist](docs/PREVIEW_LIFECYCLE.md).

## Clock-color preview preparation

The 0.1.6 test build adds a read-only NX809J status-bar clock capture rehearsal. It identifies the vendor-classified top clock and tracks stock color changes through the bounded lifecycle before a future visual override. No clock color changes are enabled. See [phone test and limitations](docs/CLOCK_CAPTURE.md).

## Temporary clock-color test

The 0.1.9 draft adds an explicit fixed lavender status-bar clock preview, bounded to a separate 30-second root permit. End, expiry, guard Action, Emergency stop and screen-off restore the latest stock color with readback. Update both APK and guard module. This is a phone-validation test, not a persistent cosmetic release or verified full boot-loop recovery. See [install and phone checklist](docs/CLOCK_COLOR_PREVIEW.md).

## Status bar clock editor

The 0.1.10 test adds named color presets and a custom Material RGB/hex picker under Status bar. Only your color selection is saved. Explicit previews use the existing 30-second clock permit and stock restoration; saving and reopening never apply automatically. See [editor phone test](docs/CLOCK_EDITOR.md). Keep the 0.1.9 guard module installed.

## Normal clock color enable

The 0.1.12 test replaces the Clock editor's timer-driven preview with Enable clock color / Restore stock color. Enabled color has no duration limit and resumes after wake/reboot while independently authorized by the updated root guard. Root permission is requested only on explicit Enable/Restore. See [install, persistence and recovery tests](docs/CLOCK_ENABLE.md).

### Clock typography test

The next NX809J test adds saved relative clock size (80–120% in 5% steps, plus stock size) and bold weight to the persistent Clock editor. Enable applies the saved style alongside color; Restore stock, guard Action and Emergency stop revoke the entire clock style. See [clock typography](docs/CLOCK_STYLE.md). User confirms multiple clock sizes, colors and bold work on NX809J. The next signed test adds a saved Show seconds switch using the stock clock seconds lifecycle; seconds phone validation is pending.

### Status-bar clock batch — 0.1.16

A single test APK adds italic, relative letter spacing, built-in font family selection (Stock/Sans serif/Serif/Monospace), and hide clock. Combine them with the merged size, bold, color and seconds controls. Persistent Enable/Restore and independent guard recovery apply to the whole clock style. See [batch checklist](docs/CLOCK_BATCH.md); phone validation is pending.

### Larger status-bar batch — 0.1.17

Seven icon and battery controls share the persistent Enable/Restore flow with the working clock editor. Applied icon/battery settings and tint color are frozen separately from draft edits and bound to the enabled root identity. See [one batch checklist and scope](docs/STATUSBAR_BATCH.md). Phone results are pending for this batch.

### Status-bar details batch — 0.1.18

Independent icon RGB/hex color, separate status/notification/battery opacity, live individual icon hiding, date beside the stock clock and an icons/battery draft reset arrive together in one signed test APK. New date/icon settings stay bound to the applied identity snapshot; choices do not apply until Enable. See [combined checklist](docs/STATUSBAR_DETAILS.md). Phone results are pending.
