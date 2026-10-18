package com.elitedarkkaiser.nubialy;
import android.content.Context;
public final class LauncherPreferences {
    private LauncherPreferences(){}
    public static LauncherStyle draft(Context c){return read(c,"draft_");}
    public static LauncherStyle active(Context c){return c.getSharedPreferences("launcher_editor",0).getBoolean("enabled",false)?read(c,"active_"):LauncherStyle.STOCK;}
    private static LauncherStyle read(Context c,String prefix){try{var p=c.getSharedPreferences("launcher_editor",0);var s=new LauncherStyle(p.getInt(prefix+"scale",100),p.getBoolean(prefix+"labels",false));return s.valid()?s:LauncherStyle.STOCK;}catch(ClassCastException invalid){return LauncherStyle.STOCK;}}
    public static boolean save(Context c,LauncherStyle s){return s!=null&&s.valid()&&c.getSharedPreferences("launcher_editor",0).edit().putInt("draft_scale",s.iconScale()).putBoolean("draft_labels",s.hideLabels()).commit();}
    public static boolean activate(Context c,LauncherStyle s){return s!=null&&s.valid()&&c.getSharedPreferences("launcher_editor",0).edit().putInt("active_scale",s.iconScale()).putBoolean("active_labels",s.hideLabels()).putBoolean("enabled",true).commit();}
    public static boolean stop(Context c){return c.getSharedPreferences("launcher_editor",0).edit().putBoolean("enabled",false).commit();}
}
