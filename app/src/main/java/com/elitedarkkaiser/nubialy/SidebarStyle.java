package com.elitedarkkaiser.nubialy;

import java.util.*;
/** Independent sidebar draft. Zero dimensions retain the vendor's layout. */
public record SidebarStyle(int color,int text,int opacity,int blur,int width,int height,int columns,
        int iconSize,int corner,int theme,String iconPack,List<String> apps,int surface) {
    public static final String REFRESH_ACTION="com.elitedarkkaiser.nubialy.SIDEBAR_REFRESH";
    public static final String PACKAGE="com.zte.floatassist", METHOD="inspect_sidebar_enabled";
    public static final String APK_SHA256="b738d16731604914c2b775d0379156d92b7595d8217ae880b6be1e0d5b5f9580";
    public static final SidebarStyle DEFAULT=new SidebarStyle(0xff25232b,0xfff4efff,85,60,0,0,2,100,28,0,"@launcher",List.of());
    public SidebarStyle(int color,int text,int opacity,int blur,int width,int height,int columns,int iconSize,int corner,int theme,String iconPack,List<String> apps){this(color,text,opacity,blur,width,height,columns,iconSize,corner,theme,iconPack,apps,0);}
    public SidebarStyle { apps=apps==null?null:List.copyOf(apps); }
    private static boolean validPackage(String s){return s!=null&&(s.isEmpty()||(s.length()<=200&&s.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")));}
    public boolean valid(){return validSurface(surface)&&(color>>>24)==255&&(text>>>24)==255&&opacity>=0&&opacity<=100&&blur>=0&&blur<=150&&(width==0||width>=160&&width<=400)&&(height==0||height>=240&&height<=800)&&columns>=1&&columns<=4&&iconSize>=70&&iconSize<=130&&corner>=0&&corner<=48&&theme>=0&&theme<=3&&("@launcher".equals(iconPack)||validPackage(iconPack))&&apps!=null&&apps.size()<=100&&new HashSet<>(apps).size()==apps.size()&&apps.stream().allMatch(x->!x.isEmpty()&&validPackage(x));}
    public String encode(){return color+","+text+","+opacity+","+blur+","+width+","+height+","+columns+","+iconSize+","+corner+","+theme+","+surface+"|"+iconPack+"|"+String.join(";",apps);}
    public static SidebarStyle decode(String s){try{if(s==null||s.length()>24000)return null;String[] p=s.split("\\|",-1);if(p.length!=3)return null;String[] n=p[0].split(",",-1);if(n.length!=10&&n.length!=11)return null;int[] v=new int[11];for(int i=0;i<n.length;i++)v[i]=Integer.parseInt(n[i]);SidebarStyle r=new SidebarStyle(v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8],v[9],p[1],p[2].isEmpty()?List.of():Arrays.asList(p[2].split(";",-1)),v[10]);return r.valid()?r:null;}catch(RuntimeException bad){return null;}}
    public SidebarStyle with(int key,int value){int[] n={color,text,opacity,blur,width,height,columns,iconSize,corner,theme};n[key]=value;return new SidebarStyle(n[0],n[1],n[2],n[3],n[4],n[5],n[6],n[7],n[8],n[9],iconPack,apps,surface);}
    public SidebarStyle withPack(String p){return new SidebarStyle(color,text,opacity,blur,width,height,columns,iconSize,corner,theme,p,apps,surface);}
    public SidebarStyle withApps(List<String> p){return new SidebarStyle(color,text,opacity,blur,width,height,columns,iconSize,corner,theme,iconPack,p,surface);}
    public SidebarStyle withSurface(int value){return new SidebarStyle(color,text,opacity,blur,width,height,columns,iconSize,corner,theme,iconPack,apps,value);}
    public static boolean validSurface(int value){return value>=0&&value<=7||value>=101&&value<=105||value>=201&&value<=206||value>=300&&value<=302;}
}
