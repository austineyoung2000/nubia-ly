package com.elitedarkkaiser.nubialy;
import android.content.Context;
public final class LauncherPreferences {
    private LauncherPreferences(){}
    public static LauncherStyle draft(Context c){return read(c,"draft_");}
    public static LauncherStyle active(Context c){return c.getSharedPreferences("launcher_editor",0).getBoolean("enabled",false)?read(c,"active_"):LauncherStyle.STOCK;}
    private static LauncherStyle read(Context c,String prefix){try{var p=c.getSharedPreferences("launcher_editor",0);var s=new LauncherStyle(p.getInt(prefix+"scale",100),p.getBoolean(prefix+"labels",false),p.getInt(prefix+"color",0),p.getInt(prefix+"folder_blur",-1),p.getInt(prefix+"folder_labels",0),p.getInt(prefix+"folder_title",0));return s.valid()?s:LauncherStyle.STOCK;}catch(ClassCastException invalid){return LauncherStyle.STOCK;}}
    public static boolean save(Context c,LauncherStyle s){return s!=null&&s.valid()&&c.getSharedPreferences("launcher_editor",0).edit().putInt("draft_scale",s.iconScale()).putBoolean("draft_labels",s.hideLabels()).putInt("draft_color",s.labelColor()).putInt("draft_folder_blur",s.folderBlur()).putInt("draft_folder_labels",s.folderLabelColor()).putInt("draft_folder_title",s.folderTitleColor()).commit();}
    public static boolean activate(Context c,LauncherStyle s){return s!=null&&s.valid()&&c.getSharedPreferences("launcher_editor",0).edit().putInt("active_scale",s.iconScale()).putBoolean("active_labels",s.hideLabels()).putInt("active_color",s.labelColor()).putInt("active_folder_blur",s.folderBlur()).putInt("active_folder_labels",s.folderLabelColor()).putInt("active_folder_title",s.folderTitleColor()).putBoolean("enabled",true).commit();}
    public static boolean stop(Context c){return c.getSharedPreferences("launcher_editor",0).edit().putBoolean("enabled",false).commit();}
}
