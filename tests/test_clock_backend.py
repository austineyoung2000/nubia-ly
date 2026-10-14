"""Run the production override backend against deterministic view/hook/time doubles."""
from pathlib import Path
import tempfile, subprocess
root = Path(__file__).resolve().parents[1]
sources = {
'com/elitedarkkaiser/nubialy/xposed/StatusBarBackend.java': 'package com.elitedarkkaiser.nubialy.xposed;class StatusBarBackend {static void begin(com.elitedarkkaiser.nubialy.StatusBarStyle s,int c){}static void refresh(){}static boolean healthy(){return true;}static boolean end(){return true;}static String status(){return "Stock";}}',

'android/view/View.java': 'package android.view;public class View {public static final int VISIBLE=0,INVISIBLE=4,GONE=8;}',
'android/os/Parcelable.java': 'package android.os;public interface Parcelable {}',
'android/os/Bundle.java': 'package android.os;public class Bundle implements Parcelable {private java.util.Map<String,Object> fields=new java.util.HashMap<>();public Bundle(){}public Bundle(Bundle b){fields.putAll(b.fields);}public void putBoolean(String k,boolean v){fields.put(k,v);}public void putInt(String k,int v){fields.put(k,v);}public int getInt(String k){return (Integer)fields.getOrDefault(k,0);}public boolean getBoolean(String k){return (Boolean)fields.getOrDefault(k,false);}}',

'android/graphics/Typeface.java': 'package android.graphics; public record Typeface(String family,int style) {public Typeface(int s){this("stock",s);}public static final int BOLD=1,ITALIC=2;public int getStyle(){return style;}public static Typeface create(Typeface t,int s){return new Typeface(t==null?"stock":t.family,s);}public static Typeface create(String f,int s){return new Typeface(f==null?"stock":f,s);}}',
'android/graphics/Paint.java': 'package android.graphics; public class Paint {boolean bold;float skew;public boolean isFakeBoldText(){return bold;}public void setFakeBoldText(boolean b){bold=b;}public float getTextSkewX(){return skew;}public void setTextSkewX(float s){skew=s;}}',
'android/util/TypedValue.java': 'package android.util;public class TypedValue {public static final int COMPLEX_UNIT_PX=0;}',

'android/content/res/ColorStateList.java': '''package android.content.res; public record ColorStateList(int color) { public static ColorStateList valueOf(int c){return new ColorStateList(c);} }''',
'android/os/Looper.java': '''package android.os; public class Looper { static final Looper MAIN=new Looper(); public static Looper getMainLooper(){return MAIN;} public static Looper myLooper(){return MAIN;} }''',
'android/os/Handler.java': '''package android.os; public class Handler {public Handler(Looper l){} public boolean post(Runnable r){r.run();return true;} }''',
'android/os/SystemClock.java': '''package android.os; public class SystemClock {public static long time=100000; public static long elapsedRealtime(){return time;} }''',
'de/robv/android/xposed/XC_MethodHook.java': '''package de.robv.android.xposed; public class XC_MethodHook {
 public static class MethodHookParam {public Object thisObject;private Object result;public Object getResult(){return result;}public void setResult(Object r){result=r;}public Object[] args;public java.lang.reflect.Member method;private java.util.Map<String,Object> extra=new java.util.HashMap<>();public void setObjectExtra(String k,Object v){extra.put(k,v);}public Object getObjectExtra(String k){return extra.get(k);}public boolean hasThrowable(){return false;}}
 protected void beforeHookedMethod(MethodHookParam p) throws Throwable {} protected void afterHookedMethod(MethodHookParam p) throws Throwable {}
 public void before(MethodHookParam p) throws Throwable {beforeHookedMethod(p);} public void after(MethodHookParam p) throws Throwable {afterHookedMethod(p);}
 public class Unhook {private Runnable r;public Unhook(Runnable r){this.r=r;}public void unhook(){r.run();}}
}''',
'de/robv/android/xposed/XposedBridge.java': '''package de.robv.android.xposed;public class XposedBridge {
 static java.util.Map<String,java.util.List<XC_MethodHook>> callbacks=new java.util.HashMap<>();
 public static XC_MethodHook.Unhook hookMethod(java.lang.reflect.Member m,XC_MethodHook cb){java.lang.reflect.Method method=(java.lang.reflect.Method)m;String key=XposedHelpers.key(method.getName(),method.getParameterTypes());var list=callbacks.computeIfAbsent(key,k->new java.util.ArrayList<>());list.add(cb);
 XC_MethodHook dispatch=new XC_MethodHook(){protected void beforeHookedMethod(MethodHookParam p)throws Throwable{for(var c:new java.util.ArrayList<>(callbacks.get(key)))c.before(p);}protected void afterHookedMethod(MethodHookParam p)throws Throwable{var all=new java.util.ArrayList<>(callbacks.get(key));java.util.Collections.reverse(all);for(var c:all)c.after(p);}};
 XposedHelpers.hooks.put(key,dispatch);return cb.new Unhook(()->{list.remove(cb);if(list.isEmpty()){callbacks.remove(key);XposedHelpers.hooks.remove(key);}});}}
''',
'de/robv/android/xposed/XposedHelpers.java': '''package de.robv.android.xposed; import java.util.*;
public class XposedHelpers {
 public static Object callMethod(Object o,String name,Object... args){try{for(var m:o.getClass().getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length){m.setAccessible(true);return m.invoke(o,args);}throw new RuntimeException("method");}catch(Exception e){throw new RuntimeException(e);}}
 public static Object getObjectField(Object o,String name){try{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(Exception e){throw new RuntimeException(e);}}
 public static Map<String,XC_MethodHook> hooks=new HashMap<>(); public static String key(String n,Class<?>... types){return n+java.util.Arrays.toString(types);}
 public static XC_MethodHook.Unhook findAndHookMethod(Class<?> c,String n,Object... args){Class<?>[] types=new Class<?>[args.length-1];for(int i=0;i<types.length;i++)types[i]=(Class<?>)args[i];String key=key(n,types); XC_MethodHook cb=(XC_MethodHook)args[args.length-1];hooks.put(key,cb);return cb.new Unhook(()->hooks.remove(key));}
}''',
'android/widget/TextView.java': '''package android.widget; import android.content.res.*;import de.robv.android.xposed.*;
public class TextView {private ColorStateList colors; public boolean failPaint;private int visibility;public int getVisibility(){return visibility;}public void setVisibility(int v){visibility=v;}
 private float spacing;public boolean failSpacing;public float getLetterSpacing(){return spacing;}public void setLetterSpacing(float s){style("setLetterSpacing",new Class<?>[]{float.class},new Object[]{s},()->{if(failSpacing&&s>0){failSpacing=false;throw new RuntimeException("spacing failure");}spacing=s;});}public void spacingBypass(float s){spacing=s;}
 private float size=20;private android.graphics.Typeface face=new android.graphics.Typeface(0);private android.graphics.Paint paint=new android.graphics.Paint();
 public float getTextSize(){return size;}public android.graphics.Typeface getTypeface(){return face;}public android.graphics.Paint getPaint(){return paint;}
 public void setTextSize(float s){style("setTextSize",new Class<?>[]{float.class},new Object[]{s},()->setTextSize(0,s));}
 public void setTextSize(int unit,float s){style("setTextSize",new Class<?>[]{int.class,float.class},new Object[]{unit,s},()->size=s);}
 public void setTypeface(android.graphics.Typeface t){style("setTypeface",new Class<?>[]{android.graphics.Typeface.class},new Object[]{t},()->face=t);}
 public void setTypeface(android.graphics.Typeface t,int weight){style("setTypeface",new Class<?>[]{android.graphics.Typeface.class,int.class},new Object[]{t,weight},()->{setTypeface(android.graphics.Typeface.create(t,weight));paint.setFakeBoldText(true);paint.setTextSkewX(-0.2f);});}
 public void sizeBypass(float s){size=s;}
 private void style(String name,Class<?>[] types,Object[] args,Runnable original){XC_MethodHook cb=XposedHelpers.hooks.get(XposedHelpers.key(name,types));XC_MethodHook.MethodHookParam p=new XC_MethodHook.MethodHookParam();p.thisObject=this;p.args=args;try{p.method=TextView.class.getMethod(name,types);if(cb!=null)cb.before(p);original.run();if(cb!=null)cb.after(p);}catch(Throwable e){throw new RuntimeException(e);}}

 public TextView(int color){colors=ColorStateList.valueOf(color);}public boolean isAttachedToWindow(){return true;}public ColorStateList getTextColors(){return colors;}public int getCurrentTextColor(){return colors.color();}public void stockBypass(ColorStateList c){colors=c;}
 public void setTextColor(int c){write(int.class,c);}public void setTextColor(ColorStateList c){write(ColorStateList.class,c);}
 private void write(Class<?> type,Object value){XC_MethodHook cb=XposedHelpers.hooks.get(XposedHelpers.key("setTextColor",type)); XC_MethodHook.MethodHookParam p=new XC_MethodHook.MethodHookParam();p.thisObject=this;p.args=new Object[]{value};
 try {if(cb!=null)cb.before(p);if(failPaint && value instanceof Integer i && i==0xffc0a8ff){failPaint=false;throw new RuntimeException("paint failure");} colors=p.args[0] instanceof ColorStateList c?c:ColorStateList.valueOf((Integer)p.args[0]);if(cb!=null)cb.after(p);}catch(RuntimeException e){throw e;}catch(Throwable e){throw new RuntimeException(e);}}
}''',
'com/android/systemui/statusbar/policy/Clock.java': '''package com.android.systemui.statusbar.policy;import de.robv.android.xposed.*;public class Clock extends android.widget.TextView {
 public boolean allowVisible=true;public boolean mShowSeconds;public android.os.Handler mSecondsHandler;public int clockUpdates;public boolean failSeconds;
 public Clock(int c){super(c);}public void setVisibility(int v){invoke("setVisibility",new Class<?>[]{int.class},new Object[]{v},()->{if(v!=0||allowVisible)super.setVisibility(v);});}public boolean isShowSeconds(){return mShowSeconds;}
 public void setShowSeconds(boolean b){invoke("setShowSeconds",new Class<?>[]{boolean.class},new Object[]{b},()->mShowSeconds=b);}
 public void updateShowSeconds(){if(failSeconds)throw new RuntimeException("seconds failure");if(mShowSeconds && mSecondsHandler==null)mSecondsHandler=new android.os.Handler(android.os.Looper.getMainLooper());else if(!mShowSeconds)mSecondsHandler=null;}
 public void updateClock(){clockUpdates++;ClockTestAccess.colorReenter(this);}
 public android.os.Parcelable onSaveInstanceState(){var cb=XposedHelpers.hooks.get(XposedHelpers.key("onSaveInstanceState"));var p=new XC_MethodHook.MethodHookParam();p.thisObject=this;p.args=new Object[]{};try{p.method=Clock.class.getDeclaredMethod("onSaveInstanceState");if(cb!=null)cb.before(p);var result=new android.os.Bundle();result.putBoolean("show_seconds",mShowSeconds);result.putBoolean("other",true);result.putInt("visibility",getVisibility());p.setResult(result);if(cb!=null)cb.after(p);return (android.os.Parcelable)p.getResult();}catch(Throwable e){throw new RuntimeException(e);}}
 public void onRestoreInstanceState(android.os.Parcelable b){invoke("onRestoreInstanceState",new Class<?>[]{android.os.Parcelable.class},new Object[]{b},()->{mShowSeconds=((android.os.Bundle)b).getBoolean("show_seconds");super.setVisibility(((android.os.Bundle)b).getInt("visibility"));ClockTestAccess.colorReenter(this);});}

 public void onTuningChanged(String k,String v){invoke("onTuningChanged",new Class<?>[]{String.class,String.class},new Object[]{k,v},()->{if(k.equals("clock_seconds")){mShowSeconds="1".equals(v);updateShowSeconds();updateClock();ClockTestAccess.reenter(this);}});}
 private void invoke(String n,Class<?>[] t,Object[] a,Runnable original){var cb=XposedHelpers.hooks.get(XposedHelpers.key(n,t));var p=new XC_MethodHook.MethodHookParam();p.thisObject=this;p.args=a;try{p.method=Clock.class.getDeclaredMethod(n,t);if(cb!=null)cb.before(p);original.run();if(cb!=null)cb.after(p);}catch(Throwable e){throw new RuntimeException(e);}}
}''',
'com/android/systemui/statusbar/policy/ClockTestAccess.java': '''package com.android.systemui.statusbar.policy;public class ClockTestAccess {public static void reenter(android.widget.TextView v){com.elitedarkkaiser.nubialy.xposed.SecondsTestAccess.track(v);} public static void colorReenter(android.widget.TextView v){com.elitedarkkaiser.nubialy.xposed.SecondsTestAccess.colorTrack(v);}}''',
'com/elitedarkkaiser/nubialy/xposed/SecondsTestAccess.java': '''package com.elitedarkkaiser.nubialy.xposed;public class SecondsTestAccess {public static void track(android.widget.TextView v){ClockSecondsBackend.track(v);}public static void colorTrack(android.widget.TextView v){ClockColorBackend.track(v);}}''',
'com/elitedarkkaiser/nubialy/xposed/ClockCaptureBackend.java': '''package com.elitedarkkaiser.nubialy.xposed;import android.widget.TextView;import java.util.*;class ClockCaptureBackend {static List<TextView> views=new ArrayList<>();static List<TextView> attachedClocks(){return views;}static boolean available(){return true;}}''',
'com/elitedarkkaiser/nubialy/xposed/BackendTest.java': '''package com.elitedarkkaiser.nubialy.xposed;
import android.os.SystemClock;import android.widget.TextView;import android.content.res.ColorStateList;import com.android.systemui.statusbar.policy.Clock;import de.robv.android.xposed.XposedHelpers;
public class BackendTest {static void check(boolean b){if(!b)throw new AssertionError(ClockColorBackend.status());}
 public static void main(String[] args){Clock a=new Clock(1),b=new Clock(2); ClockCaptureBackend.views.add(a);ClockCaptureBackend.views.add(b);
 if(args.length>0){a.failSpacing=true;check(!ClockColorBackend.begin(130000,0xff112233,0,false,false,false,10));check(a.getLetterSpacing()==0);check(XposedHelpers.hooks.isEmpty());check(!ClockColorBackend.begin(130000,0xff112233));System.out.println("Letter-spacing failure rollback and activation fence passed");return;}
 check(ClockColorBackend.begin(130000,0xffc0a8ff));check(a.getTextColors().color()==0xffc0a8ff);
 TextView unrelated=new TextView(3);unrelated.setTextColor(4);check(unrelated.getTextColors().color()==4);
 ColorStateList dark=new ColorStateList(5);var hookedFrame=ClockColorBackend.beforeDark(a);a.setTextColor(dark);ClockColorBackend.afterDark(a,hookedFrame);check(a.getTextColors().color()==0xffc0a8ff);
 ClockColorBackend.end();check(a.getTextColors().equals(dark));check(b.getTextColors().color()==2);check(XposedHelpers.hooks.isEmpty());check(ClockColorBackend.status().contains("remaining: 0"));
 check(ClockColorBackend.begin(130000,0xffc0a8ff));var bypassFrame=ClockColorBackend.beforeDark(a);ColorStateList bypassStock=new ColorStateList(7);a.stockBypass(bypassStock);ClockColorBackend.afterDark(a,bypassFrame);check(a.getCurrentTextColor()==0xffc0a8ff);ClockColorBackend.end();check(a.getTextColors().equals(bypassStock));ClockColorBackend.afterDark(a,bypassFrame);check(a.getTextColors().equals(bypassStock));
 check(ClockColorBackend.begin(130000,0xffc0a8ff));a.stockBypass(new ColorStateList(5));ClockColorBackend.track(a);check(a.getCurrentTextColor()==0xffc0a8ff);ClockColorBackend.detach(a);check(a.getTextColors().equals(dark));ClockColorBackend.end();check(b.getTextColors().color()==2);
 check(ClockColorBackend.begin(130000,0xffc0a8ff));SystemClock.time=130000;a.setTextColor(9);check(a.getTextColors().color()==9);check(b.getTextColors().color()==2);check(XposedHelpers.hooks.isEmpty());
 SystemClock.time=140000;check(ClockColorBackend.begin(170000,0xff112233));check(a.getTextColors().color()==0xff112233);
 check(!ClockColorBackend.begin(170000,0x00112233));check(ClockColorBackend.healthy());ClockColorBackend.end();check(a.getTextColors().color()==9);
 check(ClockColorBackend.begin(155000,0xff112233));SystemClock.time=150000;check(ClockColorBackend.refreshHealth(165000));SystemClock.time=160000;a.setTextColor(10);check(a.getTextColors().color()==0xff112233);
 ClockColorBackend.end();check(a.getTextColors().color()==10);check(!ClockColorBackend.refreshHealth(175000));
 check(ClockColorBackend.begin(175000,0xff112233,120,true));check(a.getTextSize()==24);check(a.getTypeface().getStyle()==1);
 unrelated.setTextSize(30);check(unrelated.getTextSize()==30);
 a.setTextSize(25);check(a.getTextSize()==30);a.setTypeface(new android.graphics.Typeface(2),2);check(a.getTypeface().getStyle()==3);
 ClockColorBackend.end();check(a.getTextSize()==25);check(a.getTypeface().getStyle()==2);check(a.getPaint().isFakeBoldText());check(a.getPaint().getTextSkewX()==-0.2f);check(XposedHelpers.hooks.isEmpty());
 check(ClockColorBackend.begin(175000,0xff112233,80,false));var dim=ClockStyleBackend.beforeDimensions(a);a.sizeBypass(30);ClockStyleBackend.afterDimensions(a,dim);ClockColorBackend.track(a);check(a.getTextSize()==24);ClockColorBackend.detach(a);check(a.getTextSize()==30);ClockColorBackend.end();check(XposedHelpers.hooks.isEmpty());
 check(ClockColorBackend.begin(175000,0xff112233,120,true));var dims=ClockStyleBackend.beforeDimensions(a);float styled=a.getTextSize();a.sizeBypass(styled);ClockStyleBackend.afterDimensions(a,dims);check(a.getTextSize()==styled*1.2f);ClockColorBackend.end();check(a.getTextSize()==styled);ClockStyleBackend.afterDimensions(a,dims);check(a.getTextSize()==styled);
 check(!ClockColorBackend.begin(175000,0xff112233,125,true));
 check(ClockColorBackend.begin(175000,0xff112233,0,false,true));check(a.mShowSeconds&&a.mSecondsHandler!=null);var saved=(android.os.Bundle)a.onSaveInstanceState();check(!saved.getBoolean("show_seconds")&&saved.getBoolean("other"));a.onRestoreInstanceState(saved);check(a.mShowSeconds&&a.mSecondsHandler!=null);a.onTuningChanged("clock_seconds","0");check(a.mShowSeconds&&a.mSecondsHandler!=null);ClockColorBackend.end();check(!a.mShowSeconds&&a.mSecondsHandler==null);check(XposedHelpers.hooks.isEmpty());
 a.setShowSeconds(true);a.updateShowSeconds();check(ClockColorBackend.begin(175000,0xff112233,100,true,true));a.onTuningChanged("other","0");ClockColorBackend.end();check(a.mShowSeconds&&a.mSecondsHandler!=null);a.setShowSeconds(false);a.updateShowSeconds();
 check(ClockColorBackend.begin(175000,0xff112233,0,false,true));ClockColorBackend.detach(a);check(!a.mShowSeconds&&a.mSecondsHandler==null);ClockColorBackend.end();check(XposedHelpers.hooks.isEmpty());
 a.setTypeface(new android.graphics.Typeface(0));a.getPaint().setFakeBoldText(false);a.getPaint().setTextSkewX(0);a.setLetterSpacing(0.02f);
 check(ClockColorBackend.begin(175000,0xff112233,0,false,false,true,0));check(a.getTypeface().getStyle()==2);ClockColorBackend.end();check(a.getTypeface().getStyle()==0);
 check(ClockColorBackend.begin(175000,0xff112233,0,false,false,false,10));check(Math.abs(a.getLetterSpacing()-0.12f)<0.00001f);ClockColorBackend.track(a);check(Math.abs(a.getLetterSpacing()-0.12f)<0.00001f);
 unrelated.setLetterSpacing(0.04f);check(unrelated.getLetterSpacing()==0.04f);a.setLetterSpacing(0.03f);check(Math.abs(a.getLetterSpacing()-0.13f)<0.00001f);ClockColorBackend.end();check(a.getLetterSpacing()==0.03f);
 check(ClockColorBackend.begin(175000,0xff112233,120,true,true,true,-5));check(a.getTypeface().getStyle()==3);check(Math.abs(a.getLetterSpacing()+0.02f)<0.00001f);a.spacingBypass(0.04f);ClockColorBackend.track(a);check(Math.abs(a.getLetterSpacing()+0.01f)<0.00001f);
 check(!ClockColorBackend.begin(175000,0xff112233,0,false,false,false,16));check(ClockColorBackend.healthy());ClockColorBackend.end();check(a.getLetterSpacing()==0.04f);check(a.getTypeface().getStyle()==0);check(!a.mShowSeconds&&a.mSecondsHandler==null);check(XposedHelpers.hooks.isEmpty());
 check(ClockColorBackend.begin(175000,0xff112233,0,false,false,false,0,2,false));check(a.getTypeface().family().equals("serif"));ClockColorBackend.end();check(a.getTypeface().family().equals("stock"));
 check(ClockColorBackend.begin(175000,0xff112233,120,true,true,true,5,3,true));check(a.getTypeface().family().equals("monospace")&&a.getTypeface().getStyle()==3);check(a.getVisibility()==8&&a.mShowSeconds);
 var allSaved=(android.os.Bundle)a.onSaveInstanceState();check(allSaved.getInt("visibility")==0&&!allSaved.getBoolean("show_seconds")&&allSaved.getBoolean("other"));a.onRestoreInstanceState(allSaved);check(a.getVisibility()==8&&a.mShowSeconds);a.setVisibility(4);check(a.getVisibility()==8);ClockColorBackend.end();check(a.getVisibility()==4&&!a.mShowSeconds);check(a.getTypeface().family().equals("stock"));check(XposedHelpers.hooks.isEmpty());
 a.setVisibility(0);check(ClockColorBackend.begin(175000,0xff112233,0,false,false,false,0,0,true));a.setVisibility(0);check(a.getVisibility()==8);unrelated.setVisibility(4);check(unrelated.getVisibility()==4);ClockColorBackend.detach(a);check(a.getVisibility()==0);ClockColorBackend.end();check(XposedHelpers.hooks.isEmpty());
 check(ClockColorBackend.begin(175000,0xff112233,0,false,false,false,0,0,true));a.allowVisible=false;a.setVisibility(0);ClockColorBackend.end();check(a.getVisibility()==8);a.allowVisible=true;a.setVisibility(0);
 check(!ClockColorBackend.begin(175000,0xff112233,0,false,false,false,0,4,false));
 a.failPaint=true;check(!ClockColorBackend.begin(170000,0xffc0a8ff));check(a.getTextColors().color()==10);check(XposedHelpers.hooks.isEmpty());check(!ClockColorBackend.begin(170000,0xffc0a8ff));
 System.out.println("Production clock backend checks passed: apply, hooked/bypassed dark updates, tracked-view reassertion, latest-stock restore, stale dark frames, detach, health and failure fences");}}
'''
}
with tempfile.TemporaryDirectory(prefix='nubialy-clock-backend-') as work:
    work=Path(work)
    for name, source in sources.items():
        path=work/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(source)
    files=[str(p) for p in work.rglob('*.java')]
    files += [str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/StatusBarStyle.java'),str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/ClockStylePolicy.java'),str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/xposed/ClockStyleBackend.java'),str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/xposed/ClockSecondsBackend.java'),str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/xposed/ClockVisibilityBackend.java'),str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/ClockEnabledPolicy.java'),str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/PreviewPolicy.java'),str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/ClockColorPolicy.java'),str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/ColorRestore.java'),str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/xposed/ClockColorBackend.java')]
    subprocess.run(['java','com.sun.tools.javac.Main','-d',str(work/'classes'),*files],check=True)
    subprocess.run(['java','-cp',str(work/'classes'),'com.elitedarkkaiser.nubialy.xposed.BackendTest'],check=True)

    subprocess.run(['java','-cp',str(work/'classes'),'com.elitedarkkaiser.nubialy.xposed.BackendTest','spacing-failure'],check=True)
