"""Production draft/applied preferences: enable identity binding and failed-commit behavior."""
from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
sources={
'android/content/Context.java': '''package android.content;public class Context {public static final int MODE_PRIVATE=0;public final Prefs prefs=new Prefs();public Prefs getSharedPreferences(String n,int m){return prefs;}
 public static class Prefs {public java.util.Map<String,Object> values=new java.util.HashMap<>();public boolean fail;public int getInt(String k,int d){return (Integer)values.getOrDefault(k,d);}public String getString(String k,String d){return (String)values.getOrDefault(k,d);}public Editor edit(){return new Editor(this);}}
 public static class Editor {Prefs p;java.util.Map<String,Object> writes=new java.util.HashMap<>();Editor(Prefs p){this.p=p;}public Editor putInt(String k,int v){writes.put(k,v);return this;}public Editor putString(String k,String v){writes.put(k,v);return this;}public boolean commit(){if(p.fail)return false;p.values.putAll(writes);return true;}}
}''',
'com/elitedarkkaiser/nubialy/PreferencesTest.java': '''package com.elitedarkkaiser.nubialy;public class PreferencesTest {static void check(boolean b){if(!b)throw new AssertionError();}public static void main(String[] args){
 var c=new android.content.Context();String id="aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",other="bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";var selected=new StatusBarStyle(15,120,80,110);
 check(StatusBarPreferences.read(c).equals(StatusBarStyle.STOCK));check(StatusBarPreferences.save(c,selected));check(StatusBarPreferences.active(c,id).equals(StatusBarStyle.STOCK));
 check(StatusBarPreferences.activate(c,id,selected,0xff112233));check(StatusBarPreferences.active(c,id).equals(selected));check(StatusBarPreferences.activeColor(c,id)==0xff112233);check(StatusBarPreferences.active(c,other).equals(StatusBarStyle.STOCK));
 check(StatusBarPreferences.save(c,StatusBarStyle.STOCK));check(StatusBarPreferences.read(c).equals(StatusBarStyle.STOCK));check(StatusBarPreferences.active(c,id).equals(selected));
 c.prefs.fail=true;check(!StatusBarPreferences.activate(c,other,StatusBarStyle.STOCK,0xff445566));check(StatusBarPreferences.active(c,id).equals(selected));check(StatusBarPreferences.active(c,other).equals(StatusBarStyle.STOCK));c.prefs.fail=false;
 check(!StatusBarPreferences.activate(c,"bad",selected,0xff112233));check(!StatusBarPreferences.activate(c,id,new StatusBarStyle(16,100,100,100),0xff112233));check(!StatusBarPreferences.activate(c,id,selected,0x00112233));
 c.prefs.values.put("active_mask","corrupt");check(StatusBarPreferences.active(c,id).equals(StatusBarStyle.STOCK));c.prefs.values.put("mask",99);check(StatusBarPreferences.read(c).equals(StatusBarStyle.STOCK));c.prefs.values.put("active_color",0);check(StatusBarPreferences.activeColor(c,id)==ClockColorPolicy.DEFAULT);
 System.out.println("Status-bar preferences passed: draft/applied separation, identity binding, frozen color, failed commit and corruption fallback");}}
'''
}
with tempfile.TemporaryDirectory(prefix='nubialy-status-prefs-') as work:
    work=Path(work)
    for name,content in sources.items():
        p=work/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content)
    files=[str(p) for p in work.rglob('*.java')]+[str(root/'app/src/main/java/com/elitedarkkaiser/nubialy'/n) for n in ['StatusBarPreferences.java','StatusBarStyle.java','ClockColorPolicy.java','PreviewPolicy.java']]
    subprocess.run(['java','com.sun.tools.javac.Main','-d',str(work/'classes'),*files],check=True)
    subprocess.run(['java','-cp',str(work/'classes'),'com.elitedarkkaiser.nubialy.PreferencesTest'],check=True)
