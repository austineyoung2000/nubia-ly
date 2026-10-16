# Quick Settings grid editor investigation

## Current stage: read-only phone verification

App 0.1.56-qs-grid-inspection (57) observes the existing native layouts and appends a short **QS grid inspection** summary to Check customization's technical details. No grid editor or layout override is enabled in this batch. The existing guard 0.1.15 is unchanged.

The summary contains only host category, columns, rows, tile-record counts, cell/gap sizes, bounds, orientation and pager counts. It reads no tile labels, SSIDs, notification data or user settings. Sampling follows native layout/measure callbacks, with exact class allowlists, main-thread and attachment checks, bounded weak references and no timers or root commands. Missing/changed signatures disable inspection without disabling the existing cosmetic backend.

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

## Required phone evidence

1. Install the test APK and reboot so the observers load. Leave the current guard installed.
2. Open expanded Quick Settings in portrait; swipe through pages; close it.
3. In nubia-ly, Quick Settings → Show technical details → Check customization, capture the **QS grid inspection** paragraph.
4. Repeat in landscape if the stock panel rotates. Do not change display density for this first sample.

This phase needs no new Enable action; it does not alter layout. Existing enabled styles continue to use their previous authority and recovery.

## Editor implementation gates

The intended entry is **Quick Settings → Rows & columns**, using the approved focused editor and shared draft/Enable/Restore flow. Initial choices must be bounded by measured width/height, full label/touch targets and native animation limits. Stock remains the default. Do not advertise fixed ranges before the evidence supports them.

Before exposing application, verify: native pagination and tile ordering, first/last-page restoration, tile additions/removals and native editing, large font/display sizing, portrait/landscape, RTL, media visibility and expansion/reveal animation. Restoration must recompute the latest native resources and redistribute native records under the same existing guard, including independent stop and failed-apply cleanup. No System UI restart, package-data clearing, settings write or global resource replacement is an acceptable shortcut.

Host-shaped inspection tests verify exact-host sampling, inherited fields, pager counts, detached/off-main exclusions and unchanged layout fields. They do not prove phone geometry, animation behavior or future grid override recovery.
