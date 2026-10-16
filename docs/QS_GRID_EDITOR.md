# Quick Settings grid editor investigation

## Current stage: bounded editor phone test

App **0.1.59-qs-grid (60)** adds **Quick Settings → Rows & columns** to the existing focused editor. The owner supplied read-only phone evidence confirming the native paged route: 4 columns × 3 rows, 16 records split 12 + 4 across two pages, 210 × 271 px cells and 22 × 28 px field gaps, portrait. This does not establish support for additional rows or narrower cells.

The editor offers Stock / 2 / 3 / 5 columns and Stock / 1 / 2 maximum rows. Duplicate 4-column and 3-row cards were removed because they match the observed stock layout. Previously saved explicit 4/3 drafts display as Stock; opening the editor does not alter the applied snapshot. The backend still accepts those older values for compatibility. Selection saves the shared draft; Enable applies an identity-bound snapshot. Stock is the default. Landscape and fontScale above 1.3 use stock resources; returning to supported portrait reapplies the enabled policy. Row count still respects the native available-height and tile-count limits. Large top tiles are excluded. Guard **0.1.15** is unchanged; its existing Action, emergency stop and Restore end this backend alongside the other surfaces.

The read-only inspector remains available in technical details. It reports only host category, counts, dimensions and orientation; it does not read tile text or identities.

## Scoped backend and restoration

`QsGridBackend` requires the inspected classes/fields/methods. A paged host must be attached under the exact `MfvQSPanel` whose current `mTileLayout` field points to it. Only exact `MfvPagedTileLayout$TilePage` resource outputs are changed: column/resource-column count and a reduced maximum-row cap. The observed native four-column source is required; other native column regimes stay stock. Native cell sizes are unchanged. Two/three columns provide more spacing. Five columns retain native cell width and require the current measure width to fit five cells, both native outer margins and at least 4 dp between columns; narrower widths keep stock columns. The width is part of the native reflow policy key, so resizing rechecks the fit. Four rows remain absent because native row-animation capacity has not been expanded.

Native pager measurement continues to compute row count and propagate it to pages; its existing `mDistributeTiles` path handles page creation/removal, records, adapter and indicator. New pages created during native distribution receive the same bounded policy before records are added. No tile/host model or tile order is written. Native cell measurement, horizontal spacing and RTL placement are retained.

The animator's static and companion dimension getters return the current first-page dimensions only inside that exact pager's `updateAnimators`/`updateViews` call scope, using nested restoration of a thread-local context. All other callers see stock values. Native animation arrays and the three row-delay entries are not enlarged or replaced. A geometry/page-count/orientation change refreshes the existing animator after measurement. No new timer or polling loop.

Restoration first disables policy, calls native `updateResources` on only the known changed pagers (including detached hosts), marks native redistribution and requests layout. This recomputes current stock resources rather than restoring stale captured integers. Next native measure redistributes records and refreshes animation mapping. A failed resource restore retains the host for retry, reports failure and keeps health false. Unmodified/default grids do not request a reflow. Runtime hook failures are visible through the existing surface health/recovery authority. Ordinary feature flags, system settings, overlays and system files are untouched.

33-field SurfaceStyle encoding appends columns/rows; all prior supported formats default them to stock. Existing constructor call sites remain valid, and battery-copy helpers preserve the new fields.

## Inspected vendor boundaries

The owner-supplied NX809J System UI contains both paged and control-center paths. Firmware inputs and full disassembly remain outside the repository.

| Native boundary | Observed behavior | Consequence |
| --- | --- | --- |
| `MfvTileLayout.updateResources()` | Routes columns/rows through `MfvTileLayoutAdapt` and `QSPanelLayoutFeature`; holds cell sizes and spacing. | A single AOSP integer override is insufficient. |
| `QSPanelLayoutFeature.getRows(TileLayoutType,int)` / `getTileColumns(TileLayoutType,int)` | Distinguishes wide and small tiles, consulting `QsDimenUtils`. | Keep the large top tiles independent from the small grid. |
| `MfvPagedTileLayout.onMeasure()` | First page computes rows from height and records; result is copied to other pages. | Row capacity, layout height and page redistribution must agree. |
| `distributeTiles()` / `emptyAndInflateOrRemovePages()` | Uses first-page `maxTiles()` to allocate pages and distribute native records. Updates page indicator/adapter. | Never reduce capacity without native redistribution; never alter tile models. |
| `PagedTileLayoutAdapt.getNumPages(int,int,int)` | Has feature-dependent page-count behavior and decor handling. | Preserve native page decor and last-page restoration. |
| `MfvQSAnimator.getMAX_ROWS()` | Uses `QsDimenUtils`; stores row animation arrays/delay/depth lists. | Animation capacity must be validated alongside layout changes. |
| `ControlCenterTileLayout.onUpdateColumns()` | Explicitly assigns four columns; top panel has its own override. | A separate control-center backend is needed if this is the live host. |
| `ControlCenterTileLayout.onMeasure()` | Computes rows with a large height budget; includes bottom inset and native scroll container behavior. | This route is scrolling rather than an interchangeable paged grid. |
| `ControlCenterPanelAnimator.updateRows()` | Builds animation rows from top/small layout maps. | Refresh animation mapping after any validated geometry change. |

`QsDimenUtils` also branches on orientation, large display density, control-center mode and device form factor. The phone's active route must be demonstrated before selecting a write boundary. Class presence alone does not establish which panel is used.

## Validation and phone batch

Production-hook host tests exercise native-shaped reflow/redistribution, new-page initialization, preserved record order, scoped animator dimensions, stock restoration, current-resource changes, orientation/font suspension, unrelated hosts, failure/retry and default no-op behavior. Preference tests cover legacy decoding, strict bounds, draft/applied separation and copy-helper retention. Android tests cover grid routing, selection persistence across recreation and no recovery-authority changes while editing. Android compilation/tests run only through GitHub Actions. These tests do not replace phone validation.

1. Update APK and reboot; leave Guard 0.1.15 installed.
2. Quick Settings → Rows & columns: choose **3 columns / 2 rows**, then Enable. With 16 small tiles, expect three pages (6 + 6 + 4) when two rows fit. Check every tile is reachable and all tap/long-press actions remain normal.
3. Swipe first/last pages, collapse/reopen, and check expansion/reveal animations. Change native tile ordering/add/remove a tile and verify it remains reachable.
4. Restore stock appearance: expect the native 4 × 3 layout and two pages (12 + 4) for the original 16 records. Re-enable grid and test independent guard Action restores it too.
5. Check rotation, a larger font setting, native media visibility and RTL when available. Unsupported geometry stays stock. Do not change display density as the initial test.
6. Re-enable desired settings and reboot; verify saved enabled choices resume and later draft changes remain unapplied.

No merge until the owner tests and approves. No System UI restart, data clearing or settings-write shortcut is used.

## Additional layout phone test

The owner confirmed 3×2, 4×2 and Restore resetting all styling. App 0.1.59 adds 2-column, 5-column and 1-row choices. Test 5 columns with Stock rows (for 16 tiles, 15 + 1 if three rows fit), then 5×2 (10 + 6), 3×1 and 2×2. Check narrow/rotated panels, every tile/action, swiping and Restore. Duplicates of observed stock 4 columns/3 rows remain removed. No phone success is claimed for these additional choices before owner feedback.
