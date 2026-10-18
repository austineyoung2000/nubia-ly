package com.elitedarkkaiser.nubialy.xposed;
import android.os.SystemClock;
import android.view.SurfaceControl;
import com.elitedarkkaiser.nubialy.*;
import de.robv.android.xposed.*;
import java.lang.reflect.*;
import java.util.*;

/** Scales only native depth-controller transactions belonging to the currently open folder. */
final class LauncherFolderSurface {
    private static final ThreadLocal<Frame> applying=new ThreadLocal<>();
    private record Frame(Frame previous,Object controller,Object folder,Object base,Object blur){}
    private static final Map<Object,java.lang.ref.WeakReference<Object>> lastFolders=new WeakHashMap<>();
    private static final Set<Object> closing=Collections.newSetFromMap(new WeakHashMap<>());
    private static final FolderSurfaceState overrides=new FolderSurfaceState();
    private static volatile LauncherStyle style=LauncherStyle.STOCK;private static volatile long deadline;
    private static Method openFolder,setRadius;private static Field launcher,baseSurface,blurSurface;
    private static boolean installed;private static long writes;private static String report="live surface unavailable";
    static void install(ClassLoader loader){
        List<XC_MethodHook.Unhook> hooks=new ArrayList<>();
        try{
            Class<?> depth=XposedHelpers.findClass("com.android.quickstep.util.BaseDepthController",loader),folder=XposedHelpers.findClass("com.android.launcher3.folder.Folder",loader);
            openFolder=Arrays.stream(folder.getDeclaredMethods()).filter(m->m.getName().equals("i1")&&Modifier.isStatic(m.getModifiers())&&m.getParameterCount()==1&&m.getReturnType()==folder).findFirst().orElseThrow();openFolder.setAccessible(true);
            launcher=XposedHelpers.findField(depth,"mLauncher");baseSurface=XposedHelpers.findField(depth,"mBaseSurface");blurSurface=XposedHelpers.findField(depth,"mBlurSurface");
            // This lookup executes only in the verified launcher process through the Xposed module.
            setRadius=XposedHelpers.findMethodExact(SurfaceControl.Transaction.class,"setBackgroundBlurRadius",SurfaceControl.class,int.class);
            XC_MethodHook scope=new XC_MethodHook(){
                protected void beforeHookedMethod(MethodHookParam p){
                    Frame previous=applying.get();Object folder=active()?folder(p.thisObject):null;
                    Frame next=new Frame(previous,p.thisObject,folder,field(baseSurface,p.thisObject),field(blurSurface,p.thisObject));applying.set(next);p.setObjectExtra("folder_surface_frame",next);
                }
                protected void afterHookedMethod(MethodHookParam p){Object value=p.getObjectExtra("folder_surface_frame");if(value instanceof Frame frame){if(frame.previous()==null)applying.remove();else applying.set(frame.previous());}}
            };
            hooks.add(XposedBridge.hookMethod(folder.getDeclaredMethod("handleClose",boolean.class),new XC_MethodHook(){
                protected void beforeHookedMethod(MethodHookParam p){if(active()&&attached(p.thisObject))closing.add(p.thisObject);}
                protected void afterHookedMethod(MethodHookParam p){try{if(Boolean.TRUE.equals(XposedHelpers.callMethod(p.thisObject,"isOpen"))||!attached(p.thisObject))closing.remove(p.thisObject);}catch(Throwable unavailable){closing.remove(p.thisObject);}}
            }));
            hooks.add(XposedBridge.hookMethod(folder.getDeclaredMethod("closeComplete",boolean.class),new XC_MethodHook(){
                protected void afterHookedMethod(MethodHookParam p){if(!attached(p.thisObject))closing.remove(p.thisObject);}
            }));
            hooks.add(XposedBridge.hookMethod(depth.getDeclaredMethod("setWallpaperBlur",int.class),scope));
            hooks.add(XposedBridge.hookMethod(depth.getDeclaredMethod("applyDepthAndBlur",boolean.class,boolean.class,boolean.class),scope));
            hooks.add(XposedBridge.hookMethod(setRadius,new XC_MethodHook(){
                protected void beforeHookedMethod(MethodHookParam p){
                    Object surface=p.args[0];Frame frame=applying.get();int original=(Integer)p.args[1];
                    if(frame==null||frame.folder()==null||!active()||(surface!=frame.base()&&surface!=frame.blur())){overrides.nativeWrite(surface);return;}
                    int adjusted=FolderSurfaceState.radius(original,style.folderBlur(),true,true);
                    overrides.remember(surface,frame.controller(),frame.folder(),original,adjusted);p.args[1]=adjusted;writes++;report="live surface: "+style.folderBlur()+" / 25 (native "+original+" → "+adjusted+")";
                }
            }));
            installed=true;report="live surface ready; open a folder";
        }catch(Throwable failure){for(var hook:hooks)hook.unhook();installed=false;report="live surface hooks unavailable";}
    }
    private static Object field(Field f,Object o){try{return f.get(o);}catch(Throwable unavailable){return null;}}
    private static boolean attached(Object folder){try{return XposedHelpers.callMethod(folder,"getParent")!=null;}catch(Throwable unavailable){return false;}}
    private static Object folder(Object controller){
        try{Object current=openFolder.invoke(null,launcher.get(controller));
            if(current!=null){lastFolders.put(controller,new java.lang.ref.WeakReference<>(current));return current;}
            var remembered=lastFolders.get(controller);Object previous=remembered==null?null:remembered.get();
            if(previous!=null&&closing.contains(previous)&&attached(previous))return previous;
            lastFolders.remove(controller);if(previous!=null)closing.remove(previous);
        }catch(Throwable unavailable){}return null;
    }
    private static boolean active(){return installed&&style.folderBlur()>=0&&SystemClock.elapsedRealtime()<deadline;}
    static void apply(LauncherStyle selected,long until){boolean changed=style.folderBlur()!=selected.folderBlur();if(changed)end();style=selected;deadline=until;}
    static void end(){style=LauncherStyle.STOCK;deadline=0;
        for(var original:overrides.drain()){
            // A later native transaction retires ownership. Never restore a previous folder session.
            if(folder(original.controller())!=original.folder())continue;
            SurfaceControl.Transaction transaction=null;
            try{if(!(Boolean)XposedHelpers.callMethod(original.surface(),"isValid"))continue;transaction=new SurfaceControl.Transaction();setRadius.invoke(transaction,original.surface(),original.nativeRadius());
                Object owner=launcher.get(original.controller()),root=XposedHelpers.callMethod(owner,"getRootView"),attached=XposedHelpers.callMethod(root,"getRootSurfaceControl");
                boolean queued=attached!=null&&Boolean.TRUE.equals(XposedHelpers.callMethod(attached,"applyTransactionOnDraw",transaction));if(!queued)transaction.apply();XposedHelpers.callMethod(root,"invalidate");
            }catch(Throwable unavailable){}finally{if(transaction!=null)transaction.close();}
        }
    }
    static String status(){return report+"; writes: "+writes+".";}
}
