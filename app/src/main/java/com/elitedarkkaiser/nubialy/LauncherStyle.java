package com.elitedarkkaiser.nubialy;
/** Cosmetic scale and label treatment only; native layout and targets remain unchanged. */
public record LauncherStyle(int iconScale,boolean hideLabels,int labelColor,int folderBlur,int folderLabelColor,int folderTitleColor,String iconPack,int iconShape) {
    public LauncherStyle(int scale,boolean hidden,int color,int blur,int labels,int title){this(scale,hidden,color,blur,labels,title,"",0);}
    public LauncherStyle(int scale,boolean hidden,int color){this(scale,hidden,color,-1,0,0);}
    public LauncherStyle(int scale,boolean hidden){this(scale,hidden,0);}
    public static final LauncherStyle STOCK=new LauncherStyle(100,false,0);
    public static LauncherStyle merge(String page,LauncherStyle active,LauncherStyle draft){
        boolean all=page.isEmpty(),icons=all||page.equals("icons"),folders=all||page.equals("folders"),art=all||page.equals("artwork");
        return new LauncherStyle(icons?draft.iconScale():active.iconScale(),icons?draft.hideLabels():active.hideLabels(),icons?draft.labelColor():active.labelColor(),
            folders?draft.folderBlur():active.folderBlur(),folders?draft.folderLabelColor():active.folderLabelColor(),folders?draft.folderTitleColor():active.folderTitleColor(),
            art?draft.iconPack():active.iconPack(),art?draft.iconShape():active.iconShape());
    }
    public boolean valid(){return iconScale>=70&&iconScale<=100&&validColor(labelColor)&&folderBlur>=-1&&folderBlur<=25&&validColor(folderLabelColor)&&validColor(folderTitleColor)&&validIconPack(iconPack)&&iconShape>=0&&iconShape<=4;}
    public boolean custom(){return iconScale!=100||hideLabels||labelColor!=0||folderCustom()||!iconPack.isEmpty()||iconShape!=0;}
    public boolean folderCustom(){return folderBlur!=-1||folderLabelColor!=0||folderTitleColor!=0;}
    public int effectiveIconShape(){return iconPack.isEmpty()?iconShape:0;}
    public boolean iconsCustom(){return !iconPack.isEmpty()||iconShape!=0;}
    private static boolean validIconPack(String id){return id!=null&&(id.isEmpty()||(id.length()<=200&&id.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")));}
    private static boolean validColor(int color){return color==0||(color>>>24)==255;}
    /** Native alpha belongs to launcher transitions/visibility and must be preserved. */
    public static int labelTint(int selected,int original){return selected!=0&&(selected>>>24)==255?(original&0xff000000)|(selected&0x00ffffff):original;}
    public static Integer parseLabelHex(String value){if(value==null)return null;String hex=value.trim();if(hex.startsWith("#"))hex=hex.substring(1);return hex.matches("[0-9a-fA-F]{6}")?(int)(0xff000000L|Long.parseLong(hex,16)):null;}
    public static String labelName(int color){return color==0?"Native":String.format(java.util.Locale.ROOT,"#%06X",color&0xffffff);}
}
