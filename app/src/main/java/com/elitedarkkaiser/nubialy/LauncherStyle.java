package com.elitedarkkaiser.nubialy;
/** Cosmetic scale and label treatment only; native layout and targets remain unchanged. */
public record LauncherStyle(int iconScale,boolean hideLabels,int labelColor) {
    public LauncherStyle(int scale,boolean hidden){this(scale,hidden,0);}
    public static final LauncherStyle STOCK=new LauncherStyle(100,false,0);
    public boolean valid(){return iconScale>=70&&iconScale<=100&&(labelColor==0||(labelColor>>>24)==255);}
    public boolean custom(){return iconScale!=100||hideLabels||labelColor!=0;}
    /** Native alpha belongs to launcher transitions/visibility and must be preserved. */
    public static int labelTint(int selected,int original){return selected!=0&&(selected>>>24)==255?(original&0xff000000)|(selected&0x00ffffff):original;}
    public static Integer parseLabelHex(String value){if(value==null)return null;String hex=value.trim();if(hex.startsWith("#"))hex=hex.substring(1);return hex.matches("[0-9a-fA-F]{6}")?(int)(0xff000000L|Long.parseLong(hex,16)):null;}
    public static String labelName(int color){return color==0?"Native":String.format(java.util.Locale.ROOT,"#%06X",color&0xffffff);}
}
