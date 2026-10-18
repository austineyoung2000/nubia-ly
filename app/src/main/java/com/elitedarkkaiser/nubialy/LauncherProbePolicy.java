package com.elitedarkkaiser.nubialy;
/** Temporary compatibility experiment; never merge this switch into the normal release. */
public final class LauncherProbePolicy {
    public static final boolean ENABLED=true;
    private LauncherProbePolicy(){}
    public static LauncherStyle restrict(LauncherStyle style){
        return ENABLED?new LauncherStyle(style.iconScale(),style.hideLabels(),style.labelColor()):style;
    }
    public static boolean pageAllowed(String page){return !ENABLED||"icons".equals(page);}
}
