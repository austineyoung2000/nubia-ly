"""Run the production override backend against deterministic view/hook/time doubles."""
from pathlib import Path
import tempfile, subprocess
root = Path(__file__).resolve().parents[1]
sources = {
'android/content/res/ColorStateList.java': '''package android.content.res; public record ColorStateList(int color) { public static ColorStateList valueOf(int c){return new ColorStateList(c);} }''',
'android/os/Looper.java': '''package android.os; public class Looper { static final Looper MAIN=new Looper(); public static Looper getMainLooper(){return MAIN;} public static Looper myLooper(){return MAIN;} }''',
'android/os/Handler.java': '''package android.os; public class Handler {public Handler(Looper l){} public boolean post(Runnable r){r.run();return true;} }''',
'android/os/SystemClock.java': '''package android.os; public class SystemClock {public static long time=100000; public static long elapsedRealtime(){return time;} }''',
'de/robv/android/xposed/XC_MethodHook.java': '''package de.robv.android.xposed; public class XC_MethodHook {
 public static class MethodHookParam {public Object thisObject;public Object[] args;public boolean hasThrowable(){return false;}}
 protected void beforeHookedMethod(MethodHookParam p) throws Throwable {} protected void afterHookedMethod(MethodHookParam p) throws Throwable {}
 public void before(MethodHookParam p) throws Throwable {beforeHookedMethod(p);} public void after(MethodHookParam p) throws Throwable {afterHookedMethod(p);}
 public class Unhook {private Runnable r;public Unhook(Runnable r){this.r=r;}public void unhook(){r.run();}}
}''',
'de/robv/android/xposed/XposedHelpers.java': '''package de.robv.android.xposed; import java.util.*;
public class XposedHelpers {public static Map<Class<?>,XC_MethodHook> hooks=new HashMap<>();
 public static XC_MethodHook.Unhook findAndHookMethod(Class<?> c,String n,Object... args){Class<?> type=(Class<?>)args[0]; XC_MethodHook cb=(XC_MethodHook)args[1];hooks.put(type,cb);return cb.new Unhook(()->hooks.remove(type));}
}''',
'android/widget/TextView.java': '''package android.widget; import android.content.res.*;import de.robv.android.xposed.*;
public class TextView {private ColorStateList colors; public boolean failPaint;
 public TextView(int color){colors=ColorStateList.valueOf(color);}public boolean isAttachedToWindow(){return true;}public ColorStateList getTextColors(){return colors;}
 public void setTextColor(int c){write(int.class,c);}public void setTextColor(ColorStateList c){write(ColorStateList.class,c);}
 private void write(Class<?> type,Object value){XC_MethodHook cb=XposedHelpers.hooks.get(type); XC_MethodHook.MethodHookParam p=new XC_MethodHook.MethodHookParam();p.thisObject=this;p.args=new Object[]{value};
 try {if(cb!=null)cb.before(p);if(failPaint && value instanceof Integer i && i==0xffc0a8ff){failPaint=false;throw new RuntimeException("paint failure");} colors=p.args[0] instanceof ColorStateList c?c:ColorStateList.valueOf((Integer)p.args[0]);if(cb!=null)cb.after(p);}catch(RuntimeException e){throw e;}catch(Throwable e){throw new RuntimeException(e);}}
}''',
'com/android/systemui/statusbar/policy/Clock.java': '''package com.android.systemui.statusbar.policy; public class Clock extends android.widget.TextView {public Clock(int c){super(c);}}''',
'com/elitedarkkaiser/nubialy/xposed/ClockCaptureBackend.java': '''package com.elitedarkkaiser.nubialy.xposed;import android.widget.TextView;import java.util.*;class ClockCaptureBackend {static List<TextView> views=new ArrayList<>();static List<TextView> attachedClocks(){return views;}static boolean available(){return true;}}''',
'com/elitedarkkaiser/nubialy/xposed/BackendTest.java': '''package com.elitedarkkaiser.nubialy.xposed;
import android.os.SystemClock;import android.widget.TextView;import android.content.res.ColorStateList;import com.android.systemui.statusbar.policy.Clock;import de.robv.android.xposed.XposedHelpers;
public class BackendTest {static void check(boolean b){if(!b)throw new AssertionError(ClockColorBackend.status());}
 public static void main(String[] args){Clock a=new Clock(1),b=new Clock(2); ClockCaptureBackend.views.add(a);ClockCaptureBackend.views.add(b);
 check(ClockColorBackend.begin(130000));check(a.getTextColors().color()==0xffc0a8ff);
 TextView unrelated=new TextView(3);unrelated.setTextColor(4);check(unrelated.getTextColors().color()==4);
 ColorStateList dark=new ColorStateList(5);a.setTextColor(dark);check(a.getTextColors().color()==0xffc0a8ff);
 ClockColorBackend.end();check(a.getTextColors().equals(dark));check(b.getTextColors().color()==2);check(XposedHelpers.hooks.isEmpty());check(ClockColorBackend.status().contains("remaining: 0"));
 check(ClockColorBackend.begin(130000));ClockColorBackend.detach(a);check(a.getTextColors().equals(dark));ClockColorBackend.end();check(b.getTextColors().color()==2);
 check(ClockColorBackend.begin(130000));SystemClock.time=130000;a.setTextColor(9);check(a.getTextColors().color()==9);check(b.getTextColors().color()==2);check(XposedHelpers.hooks.isEmpty());
 SystemClock.time=140000;a.failPaint=true;check(!ClockColorBackend.begin(170000));check(a.getTextColors().color()==9);check(XposedHelpers.hooks.isEmpty());check(!ClockColorBackend.begin(170000));
 System.out.println("Production clock backend checks passed: apply, unrelated views, stock updates, restore, detach, expiry, partial apply failure and failed-session fence");}}
'''
}
with tempfile.TemporaryDirectory(prefix='nubialy-clock-backend-') as work:
    work=Path(work)
    for name, source in sources.items():
        path=work/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(source)
    files=[str(p) for p in work.rglob('*.java')]
    files += [str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/ColorRestore.java'),str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/xposed/ClockColorBackend.java')]
    subprocess.run(['java','com.sun.tools.javac.Main','-d',str(work/'classes'),*files],check=True)
    subprocess.run(['java','-cp',str(work/'classes'),'com.elitedarkkaiser.nubialy.xposed.BackendTest'],check=True)
