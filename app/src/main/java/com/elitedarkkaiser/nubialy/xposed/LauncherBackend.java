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
import com.elitedarkkaiser.nubialy.LauncherIconShapes;
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
    private static long transitionCorrections,artworkCorrections;
    private static boolean transitionArtworkReady;
    private static Field currentTextColor;
    private static final class Frame {
        Frame previous;TextView view;Drawable icon;LauncherDrawState.TextState text;boolean home,folder;android.content.ComponentName component;
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
                    boolean folder=folderShortcut(view),home=homeShortcut(view);android.content.ComponentName component=active()&&style.iconsCustom()?appComponent(view):null;if(!home&&!folder&&component==null)return;
                    seen.put(view,true);
                    if(!active())return;
                    Frame f=new Frame();f.previous=drawing.get();f.view=view;f.home=home;f.folder=folder;f.component=component;
                    p.setObjectExtra("launcher_frame",f);
                    try{
                        f.icon=(Drawable)getter.invoke(view);
                        if(home&&(style.hideLabels()||style.labelColor()!=0)){
                            int color=style.hideLabels()?0:LauncherStyle.labelTint(style.labelColor(),currentTextColor.getInt(view));
                            f.text=LauncherDrawState.tint(view,currentTextColor,color);
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
                    Frame f=drawing.get();if(f==null||f.icon!=p.thisObject||!active()||!(f.home||f.folder)||scaleFor(f.view)==100)return;
                    Canvas c=(Canvas)p.args[0];
                    try{p.setObjectExtra("launcher_canvas",LauncherDrawState.scale(c,(Drawable)p.thisObject,scaleFor(f.view)));}
                    catch(Throwable unavailable){p.setObjectExtra("launcher_canvas",null);end();}
                }
                @Override protected void afterHookedMethod(MethodHookParam p){Object save=p.getObjectExtra("launcher_canvas");if(save instanceof Integer count)((Canvas)p.args[0]).restoreToCount(count);}
            }));
            if(!com.elitedarkkaiser.nubialy.LauncherProbePolicy.ENABLED){
            var inner=getter.getReturnType().getDeclaredMethod("c",Canvas.class,android.graphics.Rect.class);
            var paintField=java.util.Arrays.stream(getter.getReturnType().getDeclaredFields()).filter(field->field.getType()==Paint.class).findFirst().orElseThrow();paintField.setAccessible(true);
            added.add(XposedBridge.hookMethod(inner,new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){Frame f=drawing.get();if(f==null||f.icon!=p.thisObject||f.component==null||!active()||!style.iconsCustom())return;
                    android.graphics.Bitmap image=LauncherIconRuntime.image(f.view,f.component);if(image==null)return;
                    try{LauncherIconShapes.draw((Canvas)p.args[0],image,(android.graphics.Rect)p.args[1],(Paint)paintField.get(p.thisObject),style.effectiveIconShape());p.setResult(null);}catch(Throwable unavailable){end();}
                }
            }));
            var bounds=LauncherTransitionBounds.resolve(XposedHelpers.findClass("com.android.launcher3.views.FloatingIconView",loader));
            added.add(XposedBridge.hookMethod(bounds,new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){
                    if(p.hasThrowable()||!active()||!(p.args[1] instanceof TextView icon)||(!homeShortcut(icon)&&!folderShortcut(icon)))return;
                    try{if(LauncherTransitionBounds.adjust((android.graphics.RectF)p.args[3],scaleFor(icon)))transitionCorrections++;}
                    catch(Throwable unavailable){end();}
                }
            }));
            // Optional exact-APK adapter: a missing transition hook keeps normal icons working.
            try{
                var target=com.elitedarkkaiser.nubialy.LauncherTransitionArtwork.resolve(XposedHelpers.findClass("com.android.launcher3.views.FloatingIconView",loader));
                added.add(XposedBridge.hookMethod(target.setIcon(),new XC_MethodHook(){
                    @Override protected void beforeHookedMethod(MethodHookParam p){
                        try{Object source=target.source().get(p.thisObject);if(source instanceof TextView icon&&p.args[0] instanceof Drawable original){Drawable custom=transitionIcon(icon,original);if(custom!=original){p.args[0]=custom;artworkCorrections++;}}}catch(Throwable ignored){}
                    }
                }));
                added.add(XposedBridge.hookMethod(target.setBackground(),new XC_MethodHook(){
                    @Override protected void beforeHookedMethod(MethodHookParam p){
                        try{Object source=target.source().get(p.thisObject);if(source instanceof TextView icon&&p.args[0] instanceof java.util.function.Supplier<?> supplier&&active()&&style.iconsCustom()){
                            p.args[0]=(java.util.function.Supplier<Drawable>)()->{Object original=supplier.get();return original instanceof Drawable drawable?transitionIcon(icon,drawable):null;};
                        }}catch(Throwable ignored){}
                    }
                }));transitionArtworkReady=true;
            }catch(Throwable unavailable){transitionArtworkReady=false;}
            LauncherLayoutBackend.install(loader);LauncherFolderBackend.install(loader);}
            installed=true;return true;
        }catch(Throwable unavailable){for(var hook:added)hook.unhook();end();return false;}
    }
    private static Drawable transitionIcon(TextView icon,Drawable original){
        if(android.os.Looper.myLooper()!=android.os.Looper.getMainLooper()||!active()||!style.iconsCustom())return original;
        var component=appComponent(icon);if(component==null)return original;
        var bitmap=LauncherIconRuntime.image(icon,component);if(bitmap==null)return original;
        String pack=style.iconPack();int shape=style.effectiveIconShape();
        return com.elitedarkkaiser.nubialy.LauncherTransitionArtwork.wrap(original,bitmap,shape,
            ()->active()&&style.iconsCustom()&&pack.equals(style.iconPack())&&shape==style.effectiveIconShape());
    }
    static void observe(View root){
        LauncherLayoutBackend.observe(root);LauncherFolderBackend.observe(root);
        if(root instanceof TextView label&&homeShortcut(label)){seen.put(label,true);if(active()&&style.iconsCustom())LauncherIconRuntime.image(label,appComponent(label));label.invalidate();}
        if(root instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)observe(group.getChildAt(i));
    }
    private static android.content.ComponentName appComponent(TextView view){
        try{Object info=view.getTag();if(info==null||XposedHelpers.getIntField(info,"itemType")!=0||XposedHelpers.getIntField(info,"container")==Integer.MAX_VALUE)return null;
            boolean location=false;for(var parent=view.getParent();parent!=null;parent=parent.getParent()){String name=parent.getClass().getName();if(name.equals("com.android.launcher3.Workspace")||name.equals("com.android.launcher3.Hotseat")||name.equals("com.android.launcher3.folder.Folder")||name.contains("AllAppsContainerView")){location=true;break;}}
            if(!location)return null;Object target=XposedHelpers.callMethod(info,"getTargetComponent");return target instanceof android.content.ComponentName component&&!component.getPackageName().equals("theme.folder.add")?component:null;
        }catch(Throwable unavailable){return null;}
    }
    private static boolean folderShortcut(TextView view){for(var parent=view.getParent();parent!=null;parent=parent.getParent())if(parent.getClass().getName().equals("com.android.launcher3.folder.Folder"))return true;return false;}
    private static int scaleFor(TextView view){if(folderShortcut(view))return style.layout().folderScale();try{Object info=view.getTag();if(info!=null&&XposedHelpers.getIntField(info,"container")==-101&&style.layout().dockScale()!=0)return style.layout().dockScale();}catch(Throwable ignored){}return style.iconScale();}
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
    static void apply(LauncherStyle selected,long until){if(!installed||selected==null||!selected.valid()||until<=SystemClock.elapsedRealtime()){end();return;}selected=com.elitedarkkaiser.nubialy.LauncherProbePolicy.restrict(selected);boolean changed=!selected.equals(style);style=selected;deadline=until;LauncherLayoutBackend.apply(selected.layout(),until);LauncherFolderBackend.apply(selected,until);LauncherIconRuntime.apply(selected,until);if(changed)refresh();}
    static void end(){LauncherIconRuntime.end();LauncherLayoutBackend.end();LauncherFolderBackend.end();boolean changed=style.custom();style=LauncherStyle.STOCK;deadline=0;if(changed)refresh();}
    private static void refresh(){for(TextView view:new ArrayList<>(seen.keySet()))if(view!=null)view.invalidate();}
    static String status(){return "Launcher hooks: "+(installed?"ready":"unavailable")+"; appearance: "+(active()?style.iconScale()+"% / labels "+(style.hideLabels()?"hidden":LauncherStyle.labelName(style.labelColor())):"native")+"; observed shortcuts: "+seen.size()+"; animation bounds: "+transitionCorrections+"; animation artwork: "+(transitionArtworkReady?"ready":"native fallback")+" / "+artworkCorrections+". "+LauncherFolderBackend.status()+" "+LauncherIconRuntime.status()+" "+LauncherLayoutBackend.status();}
}
