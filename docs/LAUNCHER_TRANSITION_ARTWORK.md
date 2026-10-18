# Launcher transition artwork

The exact inspected launcher paints workspace custom icons through the scoped
BubbleTextView/FastBitmapDrawable adapter. FloatingIconView builds separate
stock drawable and background-supplier inputs, so app-to-home transitions
previously showed native artwork before the workspace icon appeared.

The optional adapter resolves the inspected original-source View field `r`, the
unique instance void method with `(Drawable, Drawable, Supplier, int)` inputs,
and `setOriginalDrawableBackground(Supplier)`. Decompilation confirms `r` is
assigned the original source view before these methods run. The drawable method
passes artwork into ClipIconView and keeps its separate badge drawable. The
background supplier is used during return-animation clipping.

Both presentation inputs reuse a ready entry from LauncherIconRuntime. Pack
artwork retains its transparency and disables additional shape masks; native
shape overrides use the same shape path as workspace rendering. Known home
icons are primed when the hierarchy is observed. No synchronous package loading,
icon model/database edit, animation timing change or launcher restart is added.

The wrapper checks live authority, icon pack and effective shape at draw time.
Stop, expiry or a changed selection falls back to native artwork. Native bounds,
alpha and color filter are restored in finally, including a failed native draw.
The existing exact APK, model/SDK and recovery gates remain mandatory. Missing
or ambiguous transition signatures keep the existing workspace behavior.

Cold-cache/unmatched/unavailable pack entries intentionally retain native
artwork; the adapter never waits on package loading during an animation.
Tests cover signature rejection, all shapes, native fallback and restoration
after failure. Physical app-to-home gesture testing remains required.
