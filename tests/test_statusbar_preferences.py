"""Production draft/applied preferences: enable identity binding and failed-commit behavior."""
from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
sources={
'com/elitedarkkaiser/nubialy/xposed/SurfaceBackend.java':'package com.elitedarkkaiser.nubialy.xposed;final class SurfaceBackend {static void install(ClassLoader l){}static void begin(com.elitedarkkaiser.nubialy.SurfaceStyle s){}static void refresh(){}static boolean end(){return true;}static boolean healthy(){return true;}static String status(){return "";}}',
'android/content/Context.java': '''package android.content;public class Context {public static final int MODE_PRIVATE=0;public final Prefs prefs=new Prefs();public Prefs getSharedPreferences(String n,int m){return prefs;}
 public static class Prefs {public java.util.Map<String,Object> values=new java.util.HashMap<>();public boolean fail;public int getInt(String k,int d){return (Integer)values.getOrDefault(k,d);}public String getString(String k,String d){return (String)values.getOrDefault(k,d);}public Editor edit(){return new Editor(this);}}
 public static class Editor {Prefs p;java.util.Map<String,Object> writes=new java.util.HashMap<>();Editor(Prefs p){this.p=p;}public Editor putInt(String k,int v){writes.put(k,v);return this;}public Editor putString(String k,String v){writes.put(k,v);return this;}public boolean commit(){if(p.fail)return false;p.values.putAll(writes);return true;}}
}''',
'com/elitedarkkaiser/nubialy/PreferencesTest.java': '''package com.elitedarkkaiser.nubialy;public class PreferencesTest {static void check(boolean b){if(!b)throw new AssertionError();}public static void main(String[] args){
 var c=new android.content.Context();String id="aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",other="bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";var selected=new StatusBarStyle(15,120,80,110);
 check(StatusBarPreferences.read(c).equals(StatusBarStyle.STOCK));check(StatusBarPreferences.save(c,selected));check(StatusBarPreferences.active(c,id).equals(StatusBarStyle.STOCK));
 check(StatusBarPreferences.activate(c,id,selected,0xff112233));check(StatusBarPreferences.active(c,id).equals(selected));check(StatusBarPreferences.activeColor(c,id)==0xff112233);check(StatusBarPreferences.active(c,other).equals(StatusBarStyle.STOCK));
 check(StatusBarPreferences.save(c,StatusBarStyle.STOCK));check(StatusBarPreferences.read(c).equals(StatusBarStyle.STOCK));check(StatusBarPreferences.active(c,id).equals(selected));
 var details=new StatusBarStyle(113,110,90,100,50,60,70,java.util.Set.of("alarm_clock","bluetooth"),StatusBarStyle.transform(-8,6,20),StatusBarStyle.transform(2,-4,-10),StatusBarStyle.transform(0,4,5),StatusBarStyle.appearance(120,50),StatusBarStyle.appearance(80,75),StatusBarStyle.appearance(110,25));check(StatusBarPreferences.save(c,details));check(StatusBarPreferences.read(c).equals(details));check(StatusBarPreferences.saveColor(c,0xffabcdef));check(StatusBarPreferences.saveDate(c,2));check(StatusBarPreferences.active(c,id).equals(selected));check(StatusBarPreferences.activeDate(c,id)==0);
 check(StatusBarPreferences.activate(c,id,details,0xffabcdef,2));check(StatusBarPreferences.active(c,id).equals(details));check(StatusBarPreferences.activeDate(c,id)==2);check(StatusBarPreferences.activeDate(c,other)==0);check(StatusBarPreferences.color(c)==0xffabcdef);check(StatusBarPreferences.activeColor(c,id)==0xffabcdef);
 check(StatusBarPreferences.save(c,StatusBarStyle.STOCK));check(StatusBarPreferences.active(c,id).equals(details));
 c.prefs.values.put("active_system_transform",1);check(StatusBarPreferences.active(c,id).equals(StatusBarStyle.STOCK));
 var surfaces=new SurfaceStyle(8,StatusBarStyle.appearance(110,75),0,StatusBarStyle.appearance(120,50),0,0,0,StatusBarStyle.transform(2,-2,5),0,3,2,7,4,1,25);var full=details.withSurfaces(surfaces);
 check(StatusBarPreferences.save(c,full));check(StatusBarPreferences.read(c).equals(full));check(StatusBarPreferences.activate(c,id,full,0xffabcdef,2));check(StatusBarPreferences.active(c,id).equals(full));check(StatusBarPreferences.save(c,selected));check(StatusBarPreferences.active(c,id).equals(full));
 String legacy=String.join(",",java.util.Arrays.copyOf(surfaces.encode().split(","),31));
 var grid=SurfaceStyle.decode(legacy+",3,2");check(grid!=null&&grid.qsColumns()==3&&grid.qsRows()==2&&grid.qsOptions()==0);check(grid.withBatteryControls(2,1).qsColumns()==3);check(grid.withBatteryStyle(4).qsRows()==2);
 check(StatusBarPreferences.save(c,details.withSurfaces(grid)));check(StatusBarPreferences.read(c).surfaces().equals(grid));check(StatusBarPreferences.activate(c,id,details.withSurfaces(grid),0xffabcdef,2));check(StatusBarPreferences.save(c,StatusBarStyle.STOCK));check(StatusBarPreferences.active(c,id).surfaces().equals(grid));
 var dense=SurfaceStyle.decode(legacy+",5,6");check(dense!=null&&dense.qsColumns()==5&&dense.qsRows()==6);
 int options=2|(1<<2)|(3<<4)|(2<<6)|256|(6<<9)|(5<<12);var layout=SurfaceStyle.decode(legacy+",5,4,"+options);check(layout!=null&&layout.qsTopLayout()==2&&layout.qsLabelPosition()==1&&layout.qsLabelLines()==3&&layout.qsBrightnessPosition()==2&&layout.qsHideBrightnessIcons()&&layout.qsLandscapeColumns()==6&&layout.qsLandscapeRows()==5);check(layout.equals(SurfaceStyle.decode(layout.encode())));check(layout.withBatteryStyle(4).qsOptions()==options&&layout.withBatteryControls(1,0).qsOptions()==options);
 for(int invalid:new int[]{-1,32768,12,192,512,3584,28672})check(!SurfaceStyle.qsOptionsValid(invalid));
 check(SurfaceStyle.decode(legacy+",5,7")==null);check(SurfaceStyle.decode(legacy).qsColumns()==0);
 c.prefs.values.put("active_surfaces","bad");check(StatusBarPreferences.active(c,id).equals(StatusBarStyle.STOCK));
 check(StatusBarPreferences.activate(c,id,selected,0xff112233));
 c.prefs.fail=true;check(!StatusBarPreferences.activate(c,other,StatusBarStyle.STOCK,0xff445566));check(StatusBarPreferences.active(c,id).equals(selected));check(StatusBarPreferences.active(c,other).equals(StatusBarStyle.STOCK));c.prefs.fail=false;
 var clock=new ClockSnapshot(0xff445566,110,true,true,true,-5,2,true);check(StatusBarPreferences.activate(c,id,details,0xffabcdef,2,clock));check(StatusBarPreferences.activeClock(c,id).equals(clock));check(StatusBarPreferences.save(c,selected));check(StatusBarPreferences.activeClock(c,id).equals(clock));check(StatusBarPreferences.activeClock(c,other).equals(ClockSnapshot.STOCK));c.prefs.values.put("active_clock","corrupt");check(StatusBarPreferences.activeClock(c,id).equals(ClockSnapshot.STOCK));check(ClockSnapshot.decode(clock.encode()).equals(clock));check(ClockSnapshot.decode("1,0,0,0,0")==null);
 check(!StatusBarPreferences.activate(c,"bad",selected,0xff112233));check(!StatusBarPreferences.activate(c,id,new StatusBarStyle(128,100,100,100),0xff112233));check(!StatusBarPreferences.activate(c,id,selected,0x00112233));
 c.prefs.values.put("active_mask","corrupt");check(StatusBarPreferences.active(c,id).equals(StatusBarStyle.STOCK));c.prefs.values.put("mask",128);check(StatusBarPreferences.read(c).equals(StatusBarStyle.STOCK));c.prefs.values.put("active_color",0);check(StatusBarPreferences.activeColor(c,id)==ClockColorPolicy.DEFAULT);
 System.out.println("Status-bar preferences passed: draft/applied separation, identity binding, frozen color, failed commit and corruption fallback");}}
'''
}
with tempfile.TemporaryDirectory(prefix='nubialy-status-prefs-') as work:
    work=Path(work)
    for name,content in sources.items():
        p=work/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content)
    files=[str(p) for p in work.rglob('*.java')]+[str(root/'app/src/main/java/com/elitedarkkaiser/nubialy'/n) for n in ['StatusBarPreferences.java','StatusBarStyle.java','SurfaceStyle.java','NotificationControls.java','LockClockFonts.java','ClockColorPolicy.java','ClockDatePolicy.java','PreviewPolicy.java','ClockSnapshot.java','ClockStylePolicy.java']]
    files.append(str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/FingerprintStyles.java'))
    files.append(str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/FingerprintAnimations.java'))
    subprocess.run(['java','com.sun.tools.javac.Main','-d',str(work/'classes'),*files],check=True)
    subprocess.run(['java','-cp',str(work/'classes'),'com.elitedarkkaiser.nubialy.PreferencesTest'],check=True)
