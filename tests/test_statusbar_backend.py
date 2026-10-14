"""Compile the production status-bar backend with deterministic NX809J-shaped UI doubles."""
from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
sources={
'android/content/res/ColorStateList.java': 'package android.content.res;public record ColorStateList(int color){public static ColorStateList valueOf(int c){return new ColorStateList(c);}public int getDefaultColor(){return color;}}',
'android/os/Looper.java': 'package android.os;public class Looper {static final Looper MAIN=new Looper();public static Looper getMainLooper(){return MAIN;}public static Looper myLooper(){return MAIN;}}',
'android/view/ViewParent.java': 'package android.view;public interface ViewParent {}',
'de/robv/android/xposed/XC_MethodHook.java': '''package de.robv.android.xposed;public class XC_MethodHook {
public static class MethodHookParam {public Object thisObject;public Object[] args;public java.lang.reflect.Member method;private java.util.Map<String,Object> extra=new java.util.HashMap<>();public boolean hasThrowable(){return false;}public void setObjectExtra(String k,Object v){extra.put(k,v);}public Object getObjectExtra(String k){return extra.get(k);}}
protected void beforeHookedMethod(MethodHookParam p)throws Throwable{}protected void afterHookedMethod(MethodHookParam p)throws Throwable{}public class Unhook{Runnable r;public Unhook(Runnable r){this.r=r;}public void unhook(){r.run();}}
}''',
'de/robv/android/xposed/XposedBridge.java': '''package de.robv.android.xposed;import java.util.*;import java.lang.reflect.*;public class XposedBridge {
public static Map<Member,List<XC_MethodHook>> hooks=new HashMap<>();public static XC_MethodHook.Unhook hookMethod(Member m,XC_MethodHook cb){var list=hooks.computeIfAbsent(m,k->new ArrayList<>());list.add(cb);return cb.new Unhook(()->{list.remove(cb);if(list.isEmpty())hooks.remove(m);});}
public static void run(Object o,Class<?> c,String name,Class<?>[] types,Object[] args,Runnable original){try{Method m=c.getDeclaredMethod(name,types);var p=new XC_MethodHook.MethodHookParam();p.thisObject=o;p.method=m;p.args=args;var callbacks=new ArrayList<>(hooks.getOrDefault(m,List.of()));for(var cb:callbacks)cb.beforeHookedMethod(p);original.run();Collections.reverse(callbacks);for(var cb:callbacks)cb.afterHookedMethod(p);}catch(RuntimeException e){throw e;}catch(Throwable e){throw new RuntimeException(e);}}
}''',
'de/robv/android/xposed/XposedHelpers.java': '''package de.robv.android.xposed;public class XposedHelpers {
public static Class<?> findClass(String n,ClassLoader l){try{return Class.forName(n,true,l);}catch(Exception e){throw new RuntimeException(e);}}
public static Object callMethod(Object o,String name,Object... args){try{for(var m:o.getClass().getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length){m.setAccessible(true);return m.invoke(o,args);}throw new RuntimeException("method");}catch(Exception e){throw new RuntimeException(e);}}
public static XC_MethodHook.Unhook findAndHookMethod(Class<?> c,String n,Object... args){try{Class<?>[] types=new Class<?>[args.length-1];for(int i=0;i<types.length;i++)types[i]=(Class<?>)args[i];return XposedBridge.hookMethod(c.getDeclaredMethod(n,types),(XC_MethodHook)args[args.length-1]);}catch(Exception e){throw new RuntimeException(e);}}
}''',
'android/view/View.java': '''package android.view;import de.robv.android.xposed.*;public class View {
public static final int VISIBLE=0,INVISIBLE=4,GONE=8;private float x=1,y=1;private int visibility;public ViewParent parent;public boolean attached,failApply,failRestore;
public ViewParent getParent(){return parent;}public float getScaleX(){return x;}public float getScaleY(){return y;}public int getVisibility(){return visibility;}public boolean isAttachedToWindow(){return attached;}
public void setScaleX(float v){XposedBridge.run(this,View.class,"setScaleX",new Class<?>[]{float.class},new Object[]{v},()->{if((failApply&&v!=1)||(failRestore&&v==1)){failApply=false;throw new RuntimeException("scale failure");}x=v;});}
public void setScaleY(float v){XposedBridge.run(this,View.class,"setScaleY",new Class<?>[]{float.class},new Object[]{v},()->y=v);}
public void setVisibility(int v){XposedBridge.run(this,View.class,"setVisibility",new Class<?>[]{int.class},new Object[]{v},()->visibility=v);}
protected void onAttachedToWindow(){XposedBridge.run(this,View.class,"onAttachedToWindow",new Class<?>[]{},new Object[]{},()->{});}protected void onDetachedFromWindow(){XposedBridge.run(this,View.class,"onDetachedFromWindow",new Class<?>[]{},new Object[]{},()->{});}
public void attach(){attached=true;onAttachedToWindow();}public void detach(){onDetachedFromWindow();attached=false;}
}''',
'android/view/ViewGroup.java': '''package android.view;public class ViewGroup extends View implements ViewParent {private java.util.List<View> children=new java.util.ArrayList<>();public int getChildCount(){return children.size();}public View getChildAt(int n){return children.get(n);}public void add(View v){children.add(v);v.parent=this;if(attached)v.attach();}public void remove(View v){v.detach();children.remove(v);v.parent=null;}}''',
'android/widget/FrameLayout.java': 'package android.widget;public class FrameLayout extends android.view.ViewGroup {}',
'android/widget/LinearLayout.java': 'package android.widget;public class LinearLayout extends android.view.ViewGroup {}',
'android/widget/ImageView.java': '''package android.widget;import android.content.res.*;import de.robv.android.xposed.*;public class ImageView extends android.view.View {private ColorStateList tint;public ColorStateList getImageTintList(){return tint;}public void setImageTintList(ColorStateList c){XposedBridge.run(this,ImageView.class,"setImageTintList",new Class<?>[]{ColorStateList.class},new Object[]{c},()->tint=c);}public void bypassTint(ColorStateList c){tint=c;}}''',
'com/android/systemui/statusbar/AnimatedImageView.java': 'package com.android.systemui.statusbar;public class AnimatedImageView extends android.widget.ImageView {}',
'com/zte/adapt/mifavor/statusbar/StatusBarIconViewAdapt.java': 'package com.zte.adapt.mifavor.statusbar;public class StatusBarIconViewAdapt {public boolean deny;public boolean donotSetIconColor(){return deny;}}',
'com/zte/adapt/mifavor/statusbar/PhoneStatusBarViewAdapt.java': 'package com.zte.adapt.mifavor.statusbar;public class PhoneStatusBarViewAdapt {}',
'com/zte/adapt/mifavor/statusbar/BatteryMeterViewAdapt.java': 'package com.zte.adapt.mifavor.statusbar;public class BatteryMeterViewAdapt {}',
'com/android/systemui/statusbar/StatusBarIconView.java': '''package com.android.systemui.statusbar;import de.robv.android.xposed.*;import android.content.res.*;public class StatusBarIconView extends AnimatedImageView {
public boolean notification,bypass;public com.zte.adapt.mifavor.statusbar.StatusBarIconViewAdapt adapt=new com.zte.adapt.mifavor.statusbar.StatusBarIconViewAdapt();public boolean isNotification(){return notification;}public com.zte.adapt.mifavor.statusbar.StatusBarIconViewAdapt getAdapt(){return adapt;}
public void onDarkChanged(java.util.ArrayList areas,float intensity,int color){XposedBridge.run(this,StatusBarIconView.class,"onDarkChanged",new Class<?>[]{java.util.ArrayList.class,float.class,int.class},new Object[]{areas,intensity,color},()->{if(!adapt.deny){if(bypass)bypassTint(ColorStateList.valueOf(color));else setImageTintList(ColorStateList.valueOf(color));}});}
}''',
'com/android/systemui/battery/BatteryMeterView.java': 'package com.android.systemui.battery;public class BatteryMeterView extends android.widget.LinearLayout {public com.zte.adapt.mifavor.statusbar.BatteryMeterViewAdapt getAdapt(){return new com.zte.adapt.mifavor.statusbar.BatteryMeterViewAdapt();}}',
'com/android/systemui/statusbar/phone/PhoneStatusBarView.java': '''package com.android.systemui.statusbar.phone;import de.robv.android.xposed.*;public class PhoneStatusBarView extends android.widget.FrameLayout {
public com.zte.adapt.mifavor.statusbar.PhoneStatusBarViewAdapt getAdapt(){return new com.zte.adapt.mifavor.statusbar.PhoneStatusBarViewAdapt();}
protected void onAttachedToWindow(){XposedBridge.run(this,PhoneStatusBarView.class,"onAttachedToWindow",new Class<?>[]{},new Object[]{},()->{super.onAttachedToWindow();for(int i=0;i<getChildCount();i++)getChildAt(i).attach();});}
protected void onDetachedFromWindow(){XposedBridge.run(this,PhoneStatusBarView.class,"onDetachedFromWindow",new Class<?>[]{},new Object[]{},()->{super.onDetachedFromWindow();for(int i=0;i<getChildCount();i++)getChildAt(i).detach();});}
}''',
'com/elitedarkkaiser/nubialy/xposed/StatusBarTest.java': '''package com.elitedarkkaiser.nubialy.xposed;
import com.elitedarkkaiser.nubialy.StatusBarStyle;import android.view.*;import android.content.res.ColorStateList;import com.android.systemui.statusbar.*;import com.android.systemui.statusbar.phone.*;import com.android.systemui.battery.*;
public class StatusBarTest {static void check(boolean b){if(!b)throw new AssertionError(StatusBarBackend.status());}static void near(float a,float b){check(Math.abs(a-b)<0.00001f);}public static void main(String[] args){
 StatusBarBackend.install(StatusBarTest.class.getClassLoader());var root=new PhoneStatusBarView();var system=new StatusBarIconView();var notification=new StatusBarIconView();notification.notification=true;var battery=new BatteryMeterView();root.add(system);root.add(notification);root.add(battery);system.setImageTintList(ColorStateList.valueOf(1));root.attach();
 if(args.length>0){if(args[0].equals("apply")){system.failApply=true;try{StatusBarBackend.begin(new StatusBarStyle(0,80,100,100),9);throw new AssertionError();}catch(IllegalStateException expected){}near(system.getScaleX(),1);}else{StatusBarBackend.begin(new StatusBarStyle(0,80,100,100),9);system.failRestore=true;check(!StatusBarBackend.end());}check(!StatusBarBackend.healthy());try{StatusBarBackend.begin(StatusBarStyle.STOCK,9);throw new AssertionError();}catch(IllegalStateException expected){}System.out.println("Status-bar failure fence passed: "+args[0]);return;}
 check(new StatusBarStyle(15,80,120,100).valid());check(!new StatusBarStyle(16,100,100,100).valid());check(!new StatusBarStyle(0,81,100,100).valid());
 StatusBarBackend.begin(new StatusBarStyle(15,120,80,110),9);near(system.getScaleX(),1.2f);near(notification.getScaleY(),0.8f);near(battery.getScaleX(),1.1f);check(system.getVisibility()==8&&notification.getVisibility()==8&&battery.getVisibility()==8);check(system.getImageTintList().color()==9);check(notification.getImageTintList()==null);
 system.setScaleX(0.5f);near(system.getScaleX(),0.6f);system.setVisibility(4);check(system.getVisibility()==8);system.onDarkChanged(new java.util.ArrayList(),0,2);check(system.getImageTintList().color()==9);system.bypass=true;system.onDarkChanged(new java.util.ArrayList(),0,3);check(system.getImageTintList().color()==9);
 var outside=new StatusBarIconView();outside.attach();outside.setImageTintList(ColorStateList.valueOf(4));near(outside.getScaleX(),1);check(outside.getImageTintList().color()==4);
 var later=new StatusBarIconView();later.notification=true;root.add(later);near(later.getScaleX(),0.8f);check(later.getVisibility()==8);root.remove(later);near(later.getScaleX(),1);check(later.getVisibility()==0);
 StatusBarBackend.refresh();near(system.getScaleX(),0.6f);check(StatusBarBackend.end());near(system.getScaleX(),0.5f);near(system.getScaleY(),1);check(system.getVisibility()==4&&battery.getVisibility()==0);check(system.getImageTintList().color()==3);check(StatusBarBackend.status().contains("status-bar remaining: 0"));
 system.setVisibility(0);StatusBarBackend.begin(new StatusBarStyle(1,100,100,100),10);system.adapt.deny=true;system.onDarkChanged(new java.util.ArrayList(),0,5);check(system.getImageTintList().color()==3);system.setImageTintList(ColorStateList.valueOf(6));StatusBarBackend.refresh();check(system.getImageTintList().color()==6);system.adapt.deny=false;StatusBarBackend.refresh();check(system.getImageTintList().color()==10);check(StatusBarBackend.end());check(system.getImageTintList().color()==6);
 StatusBarBackend.begin(new StatusBarStyle(1,120,100,100),11);system.notification=true;StatusBarBackend.refresh();near(system.getScaleX(),0.5f);check(system.getImageTintList().color()==6);check(StatusBarBackend.end());system.notification=false;
 StatusBarBackend.begin(new StatusBarStyle(1,120,80,110),12);root.detach();near(system.getScaleX(),0.5f);near(battery.getScaleX(),1);check(system.getImageTintList().color()==6);check(StatusBarBackend.end());
 StatusBarBackend.begin(StatusBarStyle.STOCK,12);check(StatusBarBackend.end());System.out.println("Production status-bar checks passed: batch, dark updates/bypass, relative stock transforms, new/detached icons, outside-root exclusion, vendor tint opt-out, category changes, root detach and restoration");
}}
'''
}
with tempfile.TemporaryDirectory(prefix='nubialy-statusbar-') as work:
    work=Path(work)
    for name,content in sources.items():
        p=work/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content)
    files=[str(p) for p in work.rglob('*.java')]+[str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/StatusBarStyle.java'),str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/xposed/StatusBarBackend.java')]
    subprocess.run(['java','com.sun.tools.javac.Main','-d',str(work/'classes'),*files],check=True)
    for mode in [[],['apply'],['restore']]:
        subprocess.run(['java','-cp',str(work/'classes'),'com.elitedarkkaiser.nubialy.xposed.StatusBarTest',*mode],check=True)
