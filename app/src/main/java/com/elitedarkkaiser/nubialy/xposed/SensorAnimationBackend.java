package com.elitedarkkaiser.nubialy.xposed;

import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
import com.elitedarkkaiser.nubialy.*;
import de.robv.android.xposed.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Substitutes one verified decorative bitmap draw; native surface/control/completion remain intact. */
final class SensorAnimationBackend {
    private record Authority(int id,int speed,long deadline,long generation,WeakReference<View> target,FingerprintAnimationArtwork.Frames frames){}
    private record Draw(long generation,View target,Bitmap original,Paint paint,Bitmap replacement,Rect source){}
    private static final ThreadLocal<Draw> draw=new ThreadLocal<>();
    private static volatile Authority authority;
    private static long generation;
    private static Object resourceLock;
    private static boolean ready;private static volatile boolean failed;
    private static final AtomicLong rendered=new AtomicLong();
    private static String message="Stock animation";
    static void install(ClassLoader loader){List<XC_MethodHook.Unhook> hooks=new ArrayList<>();try{
        Class<?> type=XposedHelpers.findClass("com.fingerprint.sensorservice.view.AnimationView",loader);
        if(!android.view.SurfaceView.class.isAssignableFrom(type)||type.getDeclaredField("mCurrentIndex").getType()!=int.class||type.getDeclaredField("totalCount").getType()!=int.class||type.getDeclaredField("isDestroyed").getType()!=boolean.class||type.getDeclaredField("isStart").getType()!=boolean.class||type.getDeclaredField("mPaint").getType()!=Paint.class||type.getDeclaredField("mBitmaps").getType()!=ArrayList.class)throw new IllegalStateException();
        java.lang.reflect.Field lockField=type.getDeclaredField("M_RESOURCE_LOCK");lockField.setAccessible(true);resourceLock=lockField.get(null);if(resourceLock==null)throw new IllegalStateException();
        hooks.add(XposedBridge.hookMethod(type.getDeclaredMethod("drawView"),new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){draw.remove();Authority a=authority;if(failed||a==null||SystemClock.elapsedRealtime()>=a.deadline()||p.thisObject!=a.target().get())return;try{
                View view=(View)p.thisObject;if(!view.isAttachedToWindow()||view.getVisibility()!=View.VISIBLE||Boolean.TRUE.equals(XposedHelpers.getObjectField(view,"isDestroyed"))||!Boolean.TRUE.equals(XposedHelpers.getObjectField(view,"isStart")))return;
                int index=((Number)XposedHelpers.getObjectField(view,"mCurrentIndex")).intValue(),count=((Number)XposedHelpers.getObjectField(view,"totalCount")).intValue();
                int frame=FingerprintAnimations.mappedFrame(a.id(),index,count,a.speed());if(frame<0)return;
                Bitmap original;
                // Match vendor resource synchronization, releasing it before native holder locking.
                synchronized(resourceLock){Object bitmaps=XposedHelpers.getObjectField(view,"mBitmaps");if(!(bitmaps instanceof ArrayList<?> list)||index<0||index>=list.size()||!(list.get(index) instanceof Bitmap bitmap)||bitmap.isRecycled())return;original=bitmap;}
                Object paint=XposedHelpers.getObjectField(view,"mPaint");if(!(paint instanceof Paint pen))return;
                draw.set(new Draw(a.generation(),view,original,pen,a.frames().bitmap(),a.frames().source(frame)));
            }catch(Throwable unavailable){failed=true;authority=null;}}
            @Override protected void afterHookedMethod(MethodHookParam p){draw.remove();}
        }));
        hooks.add(XposedHelpers.findAndHookMethod(Canvas.class,"drawBitmap",Bitmap.class,Rect.class,Rect.class,Paint.class,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){Draw d=draw.get();Authority a=authority;if(d==null||a==null||failed||a.generation()!=d.generation()||a.target().get()!=d.target()||SystemClock.elapsedRealtime()>=a.deadline()||p.args[0]!=d.original()||p.args[3]!=d.paint()||d.replacement().isRecycled())return;
                if(!d.target().isAttachedToWindow()||d.target().getVisibility()!=View.VISIBLE||!Boolean.TRUE.equals(XposedHelpers.getObjectField(d.target(),"isStart"))||Boolean.TRUE.equals(XposedHelpers.getObjectField(d.target(),"isDestroyed")))return;
                // Do not mutate stock bitmaps, rects or paint; only this invocation's source changes.
                p.args[1]=d.source();p.args[0]=d.replacement();rendered.incrementAndGet();
            }
        }));ready=true;
    }catch(Throwable unavailable){for(var hook:hooks)try{hook.unhook();}catch(Throwable ignored){}message="Animation renderer unavailable";}}
    static boolean apply(int id,long until,FingerprintAnimationArtwork.Frames frames){return apply(id,until,frames,100);}
    static boolean apply(int id,long until,FingerprintAnimationArtwork.Frames frames,int speed){
        if(!FingerprintAnimations.validSpeed(speed)||!FingerprintAnimations.valid(id)||until<=SystemClock.elapsedRealtime()||until-SystemClock.elapsedRealtime()>ClockEnabledPolicy.HEALTH_MS)return false;
        if(id==0){end();return true;}if(!ready||failed||frames==null||frames.id()!=id)return false;
        Authority old=authority;long epoch=old!=null&&old.id()==id?old.generation():++generation;
        authority=new Authority(id,speed,until,epoch,new WeakReference<>(old!=null&&old.id()==id?old.target().get():null),frames);refresh();message="Animation armed: "+FingerprintAnimations.NAMES[id];return true;
    }
    static void refresh(){Authority a=authority;if(a==null)return;if(SystemClock.elapsedRealtime()>=a.deadline()){end();return;}View target=SensorFingerprintBackend.animationTarget();View old=a.target().get();if(old!=target)authority=new Authority(a.id(),a.speed(),a.deadline(),++generation,new WeakReference<>(target),a.frames());}
    static void end(){authority=null;generation++;message="Stock animation";FingerprintAnimationArtwork.clear();}
    static String status(){Authority a=authority;return "Animation: "+(failed?"renderer failed; stock":message)+"; eligible="+(a!=null&&a.target().get()!=null)+"; frames="+rendered.get()+".";}
}
