package com.elitedarkkaiser.nubialy;
import android.content.Context;
public final class SettingsPreferences {
 private SettingsPreferences(){}
 public static SettingsStyle draft(Context c){var s=SettingsStyle.decode(c.getSharedPreferences("settings_editor",0).getString("draft",""));return s==null?SettingsStyle.DEFAULT:s;}
 public static SettingsStyle active(Context c){var p=c.getSharedPreferences("settings_editor",0);return p.getBoolean("enabled",false)?SettingsStyle.decode(p.getString("active","")):null;}
 public static boolean save(Context c,SettingsStyle s){return s.valid()&&c.getSharedPreferences("settings_editor",0).edit().putString("draft",s.encode()).commit();}
 public static boolean activate(Context c,SettingsStyle s){return s.valid()&&c.getSharedPreferences("settings_editor",0).edit().putString("active",s.encode()).putBoolean("enabled",true).commit();}
 public static boolean stop(Context c){return c.getSharedPreferences("settings_editor",0).edit().putBoolean("enabled",false).commit();}
}
