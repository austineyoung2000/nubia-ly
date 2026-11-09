# Notification background styles

The dashboard gains a compact Notifications tile in the column next to Volume,
using the same card width, art height and padding as Quick Settings and Status
bar. It opens its own Notifications page with a local sample preview and Stock,
Duoline, CyberPunk, Fluid, iOS and Layers selection cards. The old coming-later
placeholder is removed. Existing dashboard cards remain as approved.

Reference: https://github.com/RisingOS-Revived/android_vendor_addons/tree/seventeen/themes/notifications
The five source directories were inspected, including notification_material_bg
ripple layers. These are code-native adaptations of their background motifs:
Duoline top/bottom strokes, CyberPunk offset panels, Fluid soft tonal surface,
iOS bottom tiers, Layers inset rim. No upstream XML/assets are copied. Unlike the
ROM overlays, Fluid uses a soft tonal wash over native blur rather than replacing
its transparency, and iOS tiers stay inside existing card bounds. Native card
layout, grouping and rounding remain authoritative; global-actions/footer shapes
and ROM spacing/bool changes from those overlays are not installed.

## Inspected native drawing and scope

SystemUI DEX identifies NotificationBackgroundView directly extending View, with
mBackground Drawable and mCornerRadii float[8]. ActivatableNotificationView owns
mBackgroundNormal of that exact type. ExpandableNotificationRow derives from it.
The legacy onDraw path applies native clip-top/bottom amounts then invokes its
declared draw(Canvas,Drawable), which calculates bounds including expansion and
native blur handling. That exact pass is hooked directly.

Each pass requires the exact attached background view under an exact
ExpandableNotificationRow whose current mBackgroundNormal is that view. The
passed Drawable must equal the view's current mBackground. Other surfaces,
headers, footer buttons and group-container backgrounds are excluded. The newer
hover-dismiss rendering path that bypasses this draw method remains stock.

Original native drawing executes exactly once. Static overlays run afterward in
a separate temporary layer, inside native onDraw's active clipping and the native
corner path. Native blur is drawn directly, never copied/flattened into a bitmap.
Revocation removes only incomplete overlay pixels, retaining the stock pass.
Restore unhooks and invalidates observed backgrounds. Diagnostics report observed
backgrounds and decorated passes without reading/logging notification contents.
No listener permission, notification content access, audio/settings writes,
channel/ranking/grouping changes, app actions, click/swipe handlers, privacy/
lock-screen policy changes, native fields or dimensions are involved. There are
no timers, bitmap caches, disk writes or animation loops in the renderer.

SurfaceStyle appends a twentieth notificationStyle field. Prior encodings migrate
to Stock. Shared guard authority, ordered draft saving and explicit Enable/Restore
remain the same as the other editors.

## Validation and phone test

Production-hook doubles cover exact row/view/drawable identity, unrelated passes,
replacement, detachment, off-thread draws, native draw once, cancellation, failure
and cleanup. Native Android pixels cover five distinct treatments, unchanged
corner arrays, balanced canvas state, and retaining native pixels if an overlay
is canceled. UI tests cover Notifications routing and draft-only selection.

Install/reboot → Notifications → Notification styles → choose → Enable. Open the
shade with real notifications and check collapsed/expanded cards, grouped items,
heads-up and lock-screen presentation where available, text/buttons, tap, swipe,
light/dark mode, dismiss/reopen, then Restore stock. If stock remains visible,
Check customization reports Notification backgrounds/decorated draws. No merging
until phone testing and explicit user authorization. This draft includes the
unmerged Volume PR #23 so the full current dashboard can be reviewed together.

## Card colors

Notifications → Appearance controls offers Native, Solid and Two-tone gradient, with two independent color pickers and native resets. Custom colors are drawn within the existing background clip before the chosen style treatment. Two-tone blends horizontally like the QS tiles; the existing background opacity setting controls transparency. The existing opacity and corner controls still apply. An optional text color applies while compatible notification text is drawn; Native restores system colors. Custom app layouts, images, and input fields may retain their own colors. Contrast is not automatically adjusted, so check readability with light and dark themes. The same native pass exclusions described above still apply.
