# Quick Settings layout editor

## Combined phone test: 0.1.61-qs-layout (62)

The owner confirmed 3×2, 4×2 and 5×4 small grids, stock restoration, and smooth fourth-row expansion after five minutes of phone testing. The next APK adds the remaining requested layout controls together. Tile-size and spacing controls were explicitly excluded. Guard 0.1.15 is unchanged: Restore, its independent Action and emergency stop end both layout backends through the existing surface recovery contract.

| Focused page | Choices |
| --- | --- |
| Portrait grid | Stock / 2 / 3 / 5 columns; Stock / 1 / 2 / 4 / 5 / 6 maximum rows |
| Landscape grid | Stock / 2–6 columns; Stock / 1–6 maximum rows, independent of portrait |
| Large tile layout | Stock; 2 tiles in 2×1; 6 in 2×3; 4 in 1×4 |
| Label layout | Stock below / above / icons only; Stock / 1–3 maximum lines |
| Brightness placement | Stock between groups / above all / below small tiles; show/hide native sun icons |

Explicit portrait 4-column and 3-row cards remain omitted because they duplicate the observed stock layout. Legacy drafts display these as Stock without altering an applied snapshot. Stock always follows the firmware; landscape cards remain explicit because its native geometry differs. Selecting a card saves only the shared draft. Enable freezes the identity-bound applied snapshot. The dashboard remains unchanged.

New top-tile, label and brightness arrangements target the inspected normal portrait shade only. They suspend for landscape and fontScale above 1.3; landscape small grids have their own policy. Native cell sizes, touch controls, slider behavior, accessibility text, host tile order, tap and long-press actions remain in charge. Extra rows are maxima, not a promise that every viewport fits six rows. Five/six columns require enough width for native cells, outer margins and at least 4 dp between columns; otherwise native columns remain.

## Grid and animation ownership

`QsGridBackend` accepts only an attached exact `MfvPagedTileLayout` beneath the exact `MfvQSPanel` whose current `mTileLayout` points to it. Only exact native TilePage resource-column and maximum-row outputs change. Native measurement, distribution, page creation/removal, RTL positioning, indicator and tile record identities remain intact. Width, height budget, orientation and selected policy trigger native resource recomputation. Larger fonts suspend the policy.

Four through six actual rows receive owned native TouchAnimator[row][2] arrays and Float delays. The original first three delays remain; additional delays end at 0.65, leaving a positive interval with the native 0.3 end delay. Dimension overrides apply only within the owning animator's update/progress calls. Geometry changes rebuild native animators. Leaving extended rows restores original arrays only if the fields still reference our arrays; firmware replacements survive stop. Native available-height, cell height and top/vertical margins bound row capacity without shrinking cells.

Stop disables policy first, recomputes current native resources on changed pagers, requests native redistribution and restores owned animation arrays. Detached changed hosts are included. Failed restoration retains ownership for retry and reports unhealthy status. Default stock grids do not reflow.

## Large tiles, labels and brightness

`QsLayoutBackend` requires the inspected MfvQSContent FrameLayout, normal/quick panels, HeaderTileLayout, QSTileViewCircle, adapter and dimension boundaries. Resource names are fixed; no user-supplied class, method or view path is accepted. Shared dimension and quick-count overrides are restricted to nested normal-shade native call scopes. Control center and tile customizer are excluded.

Large-tile count changes use native QuickQSPanel.setMaxTiles and normal QSPanel.setTiles with the same host collection. Matching adapter outputs partition the first N records into quick tiles and every remaining record into the small grid in order. No tile is removed from the host model. Header rows/columns/count and native panel height agree; single-column large tiles are centered at native width. Native row-height differences shift the whole brightness and small-grid blocks; spacing values are not edited. Restore re-reads the current native tuner count rather than assuming the old default of four.

Labels use existing small-tile containers and TextViews. Above moves the original container within the same tile; icons-only makes that container invisible while preserving the cell and accessibility action. Line limits affect only small labels. Firmware max-line and visibility setters update restoration captures. Restore puts back the existing container and native limits.

Brightness placement swaps whole native block bounds after the exact content's native layout: quick/brightness for above, brightness/scroll viewport for below. Parents, sizes and touch views are retained. The operation requires positive, non-overlapping native bounds and visible groups, and is skipped during QS detail. Native layout restores positions. Only the named low/high sun views are hidden; auto brightness stays native.

Fresh shade roots receive one posted, generation-fenced rebuild after layout. Stop fences queued work. There is no polling loop, System UI restart, data clearing, system-file modification or persistent settings-write shortcut.

SurfaceStyle's 34th field packs bounded layout options. Prior 9–33-field supported formats retain stock defaults; existing constructors and battery-copy helpers preserve compatibility and new fields.

## Validation

Host tests execute production hooks against native-shaped measurement/distribution, partition, layout, setter and animation paths. They cover preserved order, extra pages, landscape policy, extended-row progress, width/height fallbacks, current-stock restoration, labels, brightness bounds, unrelated hosts, queued-stop fencing and restoration failure/retry. Preference tests cover legacy/new encoding, strict option bounds, shared draft/applied separation and copy retention. Android tests cover focused routes and every new draft field through recreation without recovery-authority changes. Android builds and Robolectric run only on GitHub Actions.

The owner-supplied NX809J SystemUI APK and disassembly remain outside this repository. Host tests and CI do not establish phone compatibility for the new choices. The read-only inspector reports counts, dimensions and orientation without reading tile content.

## Combined phone check

1. Install the next APK, reboot, retain Guard 0.1.15. Enable a top layout and confirm all tiles still appear across both groups; test each arrangement.
2. Try label-above, icons-only and line limits. Verify tap/long-press and collapse/expand behavior.
3. Try brightness above/below and hidden sun icons; drag brightness and use auto brightness.
4. Try independent landscape columns/rows, then return to portrait. Extra rows apply only when native cells fit; inspect actual rows and check expansion animations.
5. Restore stock, then test guard Action with a custom layout enabled. Check both groups, labels, sun icons, paging and native tile count return. Re-enable desired choices and verify reboot resume.

No merge until owner phone validation and explicit approval.
