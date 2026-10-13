# nubia-ly

**A standalone customization app for stock Nubia/REDMAGIC firmware, starting with the REDMAGIC 11 Pro (NX809J).**

The goal is an Iconify-style customization experience built around Nubia's own System UI: status-bar appearance, Quick Settings styling, lock-screen clocks and notification visuals. nubia-ly is independent of Redmagic 11 Toolbox, with its own Android package, repository and release lifecycle.

> **Current state: foundation prototype.** The app can inspect device compatibility and request a read-only root check. Its Vector module registers one app-only activation probe and **zero System UI customization hooks**. There are no working cosmetic modifications, apply controls, recovery watchdogs or companion-module installers yet.

Prototype source is developed on the [`test/foundation`](https://github.com/austineyoung2000/nubia-ly/tree/test/foundation) branch and reviewed through a draft PR before merging. `main` initially carries this project overview.

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
- GitHub Actions workflow for unit tests, a debug APK, and an optional signed release APK when signing secrets are configured.

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

The current prototype does not implement this recovery engine; it cannot apply cosmetic changes. See the [planned recovery contract](docs/RECOVERY_DESIGN.md).

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

GitHub Actions produces `nubia-ly-debug-apk`. A release build is conditional on these repository secrets:

- `RELEASE_KEYSTORE_BASE64`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

Use a dedicated app signing key. Without signing secrets, CI produces only the debug APK; incomplete release secrets cause the signing step to fail rather than label an unsigned APK as signed.

**Validation at scaffold creation:** pure Java policy smoke tests passed; manifest/resource XML, workflow structure, module registration and source archive were checked. Android compilation, policy checks and unit tests subsequently passed in [GitHub Actions](https://github.com/austineyoung2000/nubia-ly/actions/runs/38456648254) at `6d892d4376fa6420eb50addac79c52996bba5751`; a [debug test APK](https://github.com/austineyoung2000/nubia-ly/actions/runs/38456648254/artifacts/11842337147) is available. Release signing is not configured. Vector loading and phone testing remain pending. App source remains in [draft PR1](https://github.com/austineyoung2000/nubia-ly/pull/1), not merged.

## Phone testing and contribution workflow

- Test branches and draft PRs; no automatic merge.
- The owner tests APKs on their NX809J and explicitly authorizes merges.
- Before editing, inspect current remote HEAD and preserve dirty worktrees.
- Device commands use **Termux and `su -c`**, never adb.
- Commit messages use an area prefix and explain the problem, implementation, behavior and safeguards. Testing evidence belongs in the PR description.
- Do not claim successful phone verification without supplied results.

For the first APK, test app launch, firmware/root inspection and optional inert Vector module loading. There are no cosmetic toggles to test yet. See [Vector integration](docs/VECTOR.md).

## Recovery foundation update (0.1.1, draft)

The app now shows Vector activation detected by an app-only probe. This does not prove System UI injection; check the separate module log after selecting System UI and rebooting. System UI continues to register zero customization hooks.

A persistent emergency-stop latch defaults to stopped. The app can save the stop, but cannot arm or apply any feature. The prerequisite policy fails closed unless activation, baseline, recovery and feature validation all pass. Full independent boot recovery remains pending; this milestone does not claim boot-loop protection. Disable nubia-ly in Vector and reboot for the manual fallback.
