# QS colors, Two-Tone and upper background — APK 95

Draft test branch; phone confirmation required before merge.

Three focused Quick Settings pages use the existing ColorPickerDialog (six-digit hex, RGB sliders, preview, Cancel/Use color). Page Enable/Restore applies only its own configuration. QS colors cover active/inactive tile backgrounds and icons, and main/secondary labels; zero follows native colors. Unavailable tiles retain native state colors. Two-Tone supplies two active-background accents and temporarily takes visual priority over decorative tile styles. The upper background supports a solid color or privately imported JPEG, center crop, opacity, dimming and a height percentage slider. Changes remain drafts until Enable.

## Inspected firmware path

The stock APK exposes QSTileViewBase background start/end and label getters, wide-tile label overrides, and MultiThemeFeature.getQSIconViewColor. Return hooks preserve native state/animation processing and are active only under ClockColorBackend's current health deadline. Native updateColors refreshes existing tiles on start and restores current native colors on end; native view properties and tile actions are not replaced.

MfvQSContainerImpl is a FrameLayout with mHeader, brightnessView, getShowQsPanel, setExpansion and a custom dispatchDraw. It applies its native fancy clipping before calling inherited dispatchDraw. The added layer draws at that inherited call only for this exact container and normal QS mode, before children, with bounded top-region clipping and balanced canvas save/restore. It adds no touch target or layout node. Expansion, split shade, landscape, wallpaper blur and dark/light interaction remain phone-test items. This is an adaptation of the two-accent visual idea; it does not install Rising's ROM framework.

## Image and authority handling

Imports run on the app worker, accept at most 16 MB, inspect dimensions before decoding, downsample to at most 1024 per axis and encode at most 256 KiB. Only a SHA-256 ID enters the frozen applied snapshot. The image stays in private app storage. The provider exports only the image named by the currently active, root-authorized configuration, read-only, to the attributed System UI UID/package. It accepts no filesystem paths or arbitrary file names. System UI reads and validates image bytes/hash/dimensions on its existing broker worker, caches one bitmap, and never decodes or reads files per frame. Stop/revocation/expiry disables the draw layer and color return hooks; no old cached image grants authority.

SurfaceStyle keeps legacy encodings unchanged when the new theme is stock and accepts a validated optional theme suffix. Independent status-bar, lock-screen and other QS pages retain applied themes. New production backend tests cover colors, wide labels, Two-Tone ends, initial refresh/native restore, exact container scope, normal-mode gate, clipped drawing and authority expiry. Pure and Android UI tests cover bounded encoding, page partition/restore and the shared hex dialog. Guard remains 0.1.17.

## Phone test

1. Install/reboot. Set a custom active background and icon hex color on QS colors, Enable that page, then open QS and toggle a tile. Check inactive/unavailable states and Restore.
2. Enable Two-Tone with two distinct accents. Verify both large and small tiles, taps/long presses, and Restore bringing back your previous tile decoration.
3. Choose an upper background image. Enable that page. Check crop/opacity/dimming/height, collapsed/expanded shade, scrolling, brightness, rotation, lock/unlock and notifications below QS.
4. Enable only one of the three pages and confirm unapplied drafts elsewhere do not appear. Restore each separately, then test Emergency stop/guard Action. Report native class/signature or QS colors/background diagnostics if the feature is unavailable.
