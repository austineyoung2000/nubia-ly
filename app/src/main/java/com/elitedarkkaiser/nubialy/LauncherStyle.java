package com.elitedarkkaiser.nubialy;
/** Cosmetic scale only: native cell geometry, targets and databases stay unchanged. */
public record LauncherStyle(int iconScale,boolean hideLabels) {
    public static final LauncherStyle STOCK=new LauncherStyle(100,false);
    public boolean valid(){return iconScale>=70&&iconScale<=100;}
    public boolean custom(){return iconScale!=100||hideLabels;}
}
