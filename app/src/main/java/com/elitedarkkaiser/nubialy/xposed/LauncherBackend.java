package com.elitedarkkaiser.nubialy.xposed;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.elitedarkkaiser.nubialy.LauncherStyle;
import com.elitedarkkaiser.nubialy.LauncherDrawState;
import com.elitedarkkaiser.nubialy.LauncherTransitionBounds;
import de.robv.android.xposed.*;
import java.lang.reflect.Field;
import java.util.*;

/** Draw-scoped only. Canvas and text paint are restored after every draw, including exceptions. */
final class LauncherBackend {
    private static final Map<TextView,Boolean> seen=new WeakHashMap<>();
    private static final ThreadLocal<Frame> drawing=new ThreadLocal<>();
    private static LauncherStyle style=LauncherStyle.STOCK;
    private static long deadline;
    private static boolean installed;
    private static long transitionCorrections;
    private static Field currentTextColor;
    private static final class Frame {
        Frame previous;TextView view;Drawable icon;LauncherDrawState.TextState text;
    }
    static boolean install(ClassLoader loader){
        if(installed)return true;
        List<XC_MethodHook.Unhook> added=new ArrayList<>();
        try{
            Class<?> bubble=XposedHelpers.findClass("com.android.launcher3.BubbleTextView",loader);
            var getter=bubble.getDeclaredMethod("getIcon");
            var draw=getter.getReturnType().getDeclaredMethod("draw",Canvas.class);
            currentTextColor=XposedHelpers.findField(TextView.class,"mCurTextColor");
            XC_MethodHook viewDraw=new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    TextView view=(TextView)p.thisObject;
                    if(!homeShortcut(view))return;
                    seen.put(view,true);
                    if(!active())return;
                    Frame f=new Frame();f.previous=drawing.get();f.view=view;
                    p.setObjectExtra("launcher_frame",f);
                    try{
                        f.icon=(Drawable)getter.invoke(view);
                        if(style.hideLabels()){
                            f.text=LauncherDrawState.hide(view,currentTextColor);
                        }
                        drawing.set(f);
                    }catch(Throwable unavailable){restore(f);p.setObjectExtra("launcher_frame",null);end();}
                }
                @Override protected void afterHookedMethod(MethodHookParam p){Object f=p.getObjectExtra("launcher_frame");if(f instanceof Frame frame)restore(frame);}
            };
            added.add(XposedBridge.hookMethod(bubble.getDeclaredMethod("onDraw",Canvas.class),viewDraw));
            Class<?> shadow=XposedHelpers.findClass("com.android.launcher3.views.DoubleShadowBubbleTextView",loader);
            added.add(XposedBridge.hookMethod(shadow.getDeclaredMethod("onDraw",Canvas.class),viewDraw));
            added.add(XposedBridge.hookMethod(draw,new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    Frame f=drawing.get();if(f==null||f.icon!=p.thisObject||!active()||style.iconScale()==100)return;
                    Canvas c=(Canvas)p.args[0];
                    try{p.setObjectExtra("launcher_canvas",LauncherDrawState.scale(c,(Drawable)p.thisObject,style.iconScale()));}
                    catch(Throwable unavailable){p.setObjectExtra("launcher_canvas",null);end();}
                }
                @Override protected void afterHookedMethod(MethodHookParam p){Object save=p.getObjectExtra("launcher_canvas");if(save instanceof Integer count)((Canvas)p.args[0]).restoreToCount(count);}
            }));
            var bounds=LauncherTransitionBounds.resolve(XposedHelpers.findClass("com.android.launcher3.views.FloatingIconView",loader));
            added.add(XposedBridge.hookMethod(bounds,new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){
                    if(p.hasThrowable()||!active()||!(p.args[1] instanceof TextView icon)||!homeShortcut(icon))return;
                    try{if(LauncherTransitionBounds.adjust((android.graphics.RectF)p.args[3],style.iconScale()))transitionCorrections++;}
                    catch(Throwable unavailable){end();}
                }
            }));
            installed=true;return true;
        }catch(Throwable unavailable){for(var hook:added)hook.unhook();end();return false;}
    }
    static void observe(View root){
        if(root instanceof TextView label&&homeShortcut(label)){seen.put(label,true);label.invalidate();}
        if(root instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)observe(group.getChildAt(i));
    }
    private static boolean homeShortcut(TextView view){
        try{
            Object info=view.getTag();if(info==null)return false;
            int container=XposedHelpers.getIntField(info,"container"),type=XposedHelpers.getIntField(info,"itemType");
            if((container!=-100&&container!=-101)||(type!=0&&type!=1&&type!=6))return false;
            for(var parent=view.getParent();parent!=null;parent=parent.getParent()){
                String name=parent.getClass().getName();
                if(name.contains("Folder")||name.contains("AllApps"))return false;
                if(name.equals("com.android.launcher3.Workspace")||name.equals("com.android.launcher3.Hotseat"))return true;
            }
        }catch(Throwable unavailable){}
        return false;
    }
    private static void restore(Frame f){
        try{if(f.text!=null)f.text.restore();}catch(Throwable unavailable){style=LauncherStyle.STOCK;deadline=0;}
        finally{if(f.previous==null)drawing.remove();else drawing.set(f.previous);}
    }
    private static boolean active(){return installed&&style.custom()&&SystemClock.elapsedRealtime()<deadline;}
    static void apply(LauncherStyle selected,long until){if(!installed||selected==null||!selected.valid()||until<=SystemClock.elapsedRealtime()){end();return;}boolean changed=!selected.equals(style);style=selected;deadline=until;if(changed)refresh();}
    static void end(){boolean changed=style.custom();style=LauncherStyle.STOCK;deadline=0;if(changed)refresh();}
    private static void refresh(){for(TextView view:new ArrayList<>(seen.keySet()))if(view!=null)view.invalidate();}
    static String status(){return "Launcher hooks: "+(installed?"ready":"unavailable")+"; appearance: "+(active()?style.iconScale()+"% / labels "+(style.hideLabels()?"hidden":"native"):"native")+"; observed shortcuts: "+seen.size()+"; animation bounds: "+transitionCorrections+".";}
}
