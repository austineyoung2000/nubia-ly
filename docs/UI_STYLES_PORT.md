# Nubia UI styles: experimental native resource port

This branch ports RisingOS UiStyles across Android (`android`), Nubia framework
(`androidzte`), Settings and System UI. It is not a QS preset. The additional
`zpub.res` framework is included when linking references but is not overlaid:
inspection did not establish a UiStyles surface that requires replacing it.

Nine styles ship as 36 resource-only, initially disabled RRO APKs. Native parent
styles are preserved for Nubia Settings. Sources retain their upstream notices:
https://github.com/RisingOS-Revived/android_vendor_addons/tree/seventeen/themes/UiStyles
The source snapshot is `ui-styles/upstream-resources.json`; native APKs are not
included. `scripts/build-ui-styles.py` needs aapt2, lxml, androguard 4.1.4 and the
four inspected native APKs plus framework-res-zpub.apk in a private input folder:

```sh
python3 scripts/build-ui-styles.py --input-dir PRIVATE_APK_FOLDER --aapt2 AAPT2
```

The checked-in unsigned overlays reproduce byte-for-byte from those inputs.
Catalog SHA-256 values validate them before CI signs each overlay. The RRO
resource package ID is 0x80, separate from target apps at 0x7f; native parent
references remain native references. Explicit resource maps expose only names
present on the inspected target; upstream helper resources remain private.

## Inspection findings

- Framework palettes: 24 neutral, 36 accent and 156 dynamic fabricated mappings
  were active. Adapted surface colors address generated surface resources too.
  OEM/Monet overlays are never disabled; our selected RRO receives highest
  priority. Wallpaper/Monet regeneration priority needs phone testing.
- Nubia framework: `zte_materialColorSurface*` and `zte_materialColorOnSurface*`.
- Settings homepage: `Theme.SettingsSinkHome` -> `Theme.SettingsSink` ->
  `Theme.ZTE.Light.Base.Sink.Card`. Its background points to `bg_card` /
  `mfvc_card_bg_color`, with separate light/dark resource configurations.
  List/card surfaces and native parent themes are adapted directly.
- System UI: matching upstream resources plus Nubia-specific notification and
  volume surfaces. Existing runtime customization pages remain independent and
  may take visual precedence over resource-level colors/shapes.
- Common overlays change six Settings configuration/text resources and three
  System UI configuration resources. Nubia's enabled ZTEColor/ZTE idmap dumps
  returned internal errors; their complete effective values are not established.

## Test flow

Install the signed APK and the UI test guard, then reboot once. Installing the
guard registers the disabled overlays; it enables no theme. Open UI styles,
choose a style and Preview. The guard validates firmware fingerprint and all four
native APK hashes, arms independent expiry before changing resources, enables
and verifies all four targets, and rolls back partial activation. Existing OEM
selections are untouched.

Reopen Settings if its existing windows do not refresh. Check the homepage,
subpages, dialogs, notifications, shade and volume in both light and dark mode.
Return and Keep within 90 seconds, or the preview restores stock. Keep lasts for
this boot only. Restore, Guard Action and uninstall disable this namespace. A
bounded service reset restores test overlays after OverlayManager becomes
available on reboot. No registry XML, stock APK, Rescue Party or Vector data is
edited.

This is not a complete boot-loop recovery guarantee: shell restoration requires
OverlayManager to respond. Framework style overrides need device testing before
release or any persistent startup behavior. Compile/link success and mocked
transaction tests do not establish live compatibility or exact visual parity.

## Port boundaries

Rising-only resources such as xd toolbar names do not exist on Nubia. Their helper
resources are retained but are not falsely exposed as working native targets.
Native Settings windows/card colors are adapted; further native widget mappings
may be needed after visual comparison. Transparent profiles and window behavior
need particular testing. Preview swatches are samples, not rendered stock apps.
The first binary set is bound to the supplied NX809J Android 16 APKs. Other
firmware/device profiles require rebuilding against their resources. Launcher
compatibility remains unchanged.
