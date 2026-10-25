package com.elitedarkkaiser.nubialy;
import android.content.Context;
/** Only explicit Apply grants sidebar authority; drafts never enable hooks. */
public final class SidebarPreferences {
    private SidebarPreferences(){}
    public static SidebarStyle draft(Context c){SidebarStyle s=SidebarStyle.decode(c.getSharedPreferences("sidebar_editor",0).getString("draft",""));return s==null?SidebarStyle.DEFAULT:s;}
    public static SidebarStyle active(Context c){var p=c.getSharedPreferences("sidebar_editor",0);return p.getBoolean("enabled",false)?SidebarStyle.decode(p.getString("active","")):null;}
    public static boolean save(Context c,SidebarStyle s){return s.valid()&&c.getSharedPreferences("sidebar_editor",0).edit().putString("draft",s.encode()).commit();}
    public static boolean activate(Context c,SidebarStyle s){return s.valid()&&c.getSharedPreferences("sidebar_editor",0).edit().putString("active",s.encode()).putBoolean("enabled",true).commit();}
    public static boolean stop(Context c){return c.getSharedPreferences("sidebar_editor",0).edit().putBoolean("enabled",false).commit();}
}
