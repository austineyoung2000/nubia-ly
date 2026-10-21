# Stock launcher compatibility investigation

Input supplied from the user's original REDMAGIC OS 11.0.18MR1_GB launcher:

- Package: `com.zte.mifavor.launcher`
- Version: `16.0.020.000.2603171131`, versionCode `160000`
- APK SHA-256: `d893e696b0418ea08301ede6014902876e3630580a13cb7c386e430a4b6e6248`
- APK size: 24,827,940 bytes

The prior 11.5 dump baseline remains `16.0.010.000.2604151532`, code 160000,
SHA-256 `cce32bcfa1fce5a4c208ea510594a1c7d0e6ad614588334a12753f2819cf3f7c`.
Matching versionCode alone does not establish compatible code.

| Adapter | 11.5 baseline | Supplied stock APK |
| --- | --- | --- |
| Desktop grid class / catalogue | `b0.s` | `Z.s` |
| Resource manager factory / grid controller | `H5.T(context).H()` | `z5.N(context).C()` |
| Native grid apply / read | `e(context, packed)` / `j(context)` | `d(context, packed)` / `i(context)` |
| Folder organizer / field | `folder.A` / `FolderPagedView.m` | `folder.B` / `FolderPagedView.j` |
| Rebind / cell measurement | `X(List)` / `k0()` | `S(List)` / `f0()` |
| Invariant folder row / column arrays | `J0` / `K0` | `A0` / `B0` |
| Invariant folder capacity | `U` | `T` |
| Folder background method / view | `B0()` / `Folder$l` | `A0()` / `Folder$m` |
| Wallpaper manager | `resource.G1` | `resource.u1` |
| Open folder getter | `i1(...)` | `g1(...)` |
| Themed icon drawable | `u1.c` | `s1.c` |

The stock APK also contains a class named `b0`, but it is DevicePaddings.
Grid selection therefore validates the static map and native 4×6/5×6 entries,
rather than accepting a matching class name alone.

Both folder profiles use four-entry row/column arrays. Overrides preserve their
length, and restoration checks ownership before restoring saved arrays/capacity.
Native grid changes remain explicit one-shot operations with the existing
7-column/8-row limits, fresh guard authority and native preference/reflow path.

The home icons in the screenshots are themed. Their drawable overrides the
internal bitmap `c(Canvas, Rect)` method without invoking its superclass. The
original bitmap hook could replace drawer icons but missed those home icons.
The new resolver hooks the themed override only when it belongs to the actual
bitmap drawable hierarchy. Existing view/component ownership, alpha/paint,
label, badge and missing-pack fallback behavior remains in place.

Validation adds both field/profile restoration mappings, the stock b0 collision,
native grid calls/receipts and non-replay on refresh, both open-folder getters,
and themed-method resolution excluding unrelated classes. Phone validation is
still required for colors, blur/open-close transitions, icon packs/shapes,
folder paging, layout migration and restoration on both APKs.

This is APK 90 on the temporary full-feature compatibility branch. The requested
APK identity bypass remains enabled; this document does not certify arbitrary
launcher versions for release. Main and PR39 remain unchanged.
