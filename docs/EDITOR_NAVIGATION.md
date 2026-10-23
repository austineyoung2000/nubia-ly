# Tabbed personalization editors

The existing dashboard is unchanged. Status bar, QS, Lock screen, Notifications,
Volume, Power menu and Launcher open directly to an editor with visible scrollable
tabs below a compact collapsible preview, and fixed outlined Restore / filled Apply actions. Back returns to the
dashboard. Tabs retain their draft and scroll position for the activity lifetime;
the selected tab survives activity recreation. Help and runtime diagnostics are
available from the toolbar. A preview title toggles its artwork.

`EditorNavigation` defines each tab's underlying scopes. `AppearanceScope` applies
or restores the union of those scopes against the last applied state. It does
not apply unrelated drafts. Launcher retains its existing individual page scopes.
Existing artwork/font pickers return to the same editor.

Preview canvases scale uniformly and use their natural aspect ratio; slider tracks are thin with circular thumbs. Cards use consistent 16 dp content margins. Tile styles precede shapes and icon packs.

Named color chips are removed; the established hex/RGB picker remains. QS
portrait/landscape dimensions and large-tile arrangements use discrete sliders.
Launcher/folder grid sliders retain their current limits (home 7 by 8 maximum).
Two-tone no longer disables tile decoration: native gradient rendering is the base
for the selected shading/rim effect.

QS Header adds separate clock/date/status colors and 70–130% visual sizes. The
hook tracks the inspected QS container's `mHeader` only. Styling uses balanced
canvas saves/layers during drawing, without changing view colors, visibility,
layout, or touch bounds. Restoring authority or the Header tab stops it. Unmatched
vendor header views stay native; runtime diagnostics report observed styled draws.
This requires device testing, especially battery rendering and large-text bounds.
Legacy 15-field QS theme drafts decode with stock header settings; new drafts use
21 fields. This branch does not contain the abandoned RRO UI style experiment and
requires only the existing guard 22, not a mounting metamodule.
