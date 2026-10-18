package com.elitedarkkaiser.nubialy;
/** Bounded launcher styling and explicit native layout choices. */
public record LauncherStyle(int iconScale,boolean hideLabels,int labelColor,int folderBlur,int folderLabelColor,int folderTitleColor,String iconPack,int iconShape,Layout layout) {
    public record Layout(int folderRows,int folderColumns,boolean hideIndicator,int indicatorColor,int indicatorScale,int dockScale,boolean expandedGrid,int folderScale){
        public Layout(int rows,int columns,boolean hide,int color,int indicator,int dock,boolean grid){this(rows,columns,hide,color,indicator,dock,grid,100);}
        public static final Layout STOCK=new Layout(0,0,false,0,100,0,false);
        public boolean valid(){return folderScale>=50&&folderScale<=100&&dimension(folderRows)&&dimension(folderColumns)&&validColor(indicatorColor)&&indicatorScale>=50&&indicatorScale<=150&&(dockScale==0||dockScale>=70&&dockScale<=100);}
        private static boolean dimension(int n){return n==0||n>=2&&n<=6;}
        public String encode(){return folderRows+","+folderColumns+","+(hideIndicator?1:0)+","+indicatorColor+","+indicatorScale+","+dockScale+","+(expandedGrid?1:0)+","+folderScale;}
        public static Layout decode(String text){if(text==null||text.length()>90)return null;String[] a=text.split(",",-1);if(a.length!=7&&a.length!=8)return null;try{int[] n=new int[a.length];for(int i=0;i<a.length;i++){if(!a[i].matches("-?[0-9]{1,10}"))return null;n[i]=Integer.parseInt(a[i]);}if(n[2]<0||n[2]>1||n[6]<0||n[6]>1)return null;Layout l=new Layout(n[0],n[1],n[2]==1,n[3],n[4],n[5],n[6]==1,a.length==8?n[7]:100);return l.valid()?l:null;}catch(NumberFormatException e){return null;}}
        public static Layout merge(String page,Layout a,Layout d){boolean all=page.isEmpty(),f=all||page.equals("folders"),p=all||page.equals("indicator"),dock=all||page.equals("dock"),grid=all||page.equals("grid");return new Layout(f?d.folderRows:a.folderRows,f?d.folderColumns:a.folderColumns,p?d.hideIndicator:a.hideIndicator,p?d.indicatorColor:a.indicatorColor,p?d.indicatorScale:a.indicatorScale,dock?d.dockScale:a.dockScale,grid?d.expandedGrid:a.expandedGrid,f?d.folderScale:a.folderScale);}
    }
    public LauncherStyle(int scale,boolean hidden,int color,int blur,int labels,int title,String pack,int shape){this(scale,hidden,color,blur,labels,title,pack,shape,Layout.STOCK);}
    public LauncherStyle(int scale,boolean hidden,int color,int blur,int labels,int title){this(scale,hidden,color,blur,labels,title,"",0);}
    public LauncherStyle(int scale,boolean hidden,int color){this(scale,hidden,color,-1,0,0);}
    public LauncherStyle(int scale,boolean hidden){this(scale,hidden,0);}
    public static final LauncherStyle STOCK=new LauncherStyle(100,false,0);
    public static LauncherStyle merge(String page,LauncherStyle active,LauncherStyle draft){
        boolean all=page.isEmpty(),icons=all||page.equals("icons"),folders=all||page.equals("folders"),art=all||page.equals("artwork");
        return new LauncherStyle(icons?draft.iconScale():active.iconScale(),icons?draft.hideLabels():active.hideLabels(),icons?draft.labelColor():active.labelColor(),
            folders?draft.folderBlur():active.folderBlur(),folders?draft.folderLabelColor():active.folderLabelColor(),folders?draft.folderTitleColor():active.folderTitleColor(),
            art?draft.iconPack():active.iconPack(),art?draft.iconShape():active.iconShape(),Layout.merge(page,active.layout(),draft.layout()));
    }
    public boolean valid(){return layout!=null&&layout.valid()&&iconScale>=70&&iconScale<=100&&validColor(labelColor)&&folderBlur>=-1&&folderBlur<=25&&validColor(folderLabelColor)&&validColor(folderTitleColor)&&validIconPack(iconPack)&&iconShape>=0&&iconShape<=4;}
    public boolean custom(){return !layout.equals(Layout.STOCK)||iconScale!=100||hideLabels||labelColor!=0||folderCustom()||!iconPack.isEmpty()||iconShape!=0;}
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
