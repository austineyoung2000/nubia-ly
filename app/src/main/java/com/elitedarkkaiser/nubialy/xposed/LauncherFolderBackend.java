package com.elitedarkkaiser.nubialy.xposed;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.*;
import android.view.View;
import android.widget.TextView;
import com.elitedarkkaiser.nubialy.*;
import de.robv.android.xposed.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/** Folder-only draw colors and a separate blurred wallpaper copy; native cache is never modified. */
final class LauncherFolderBackend {
    private static final String FOLDER="com.android.launcher3.folder.Folder";
    private static final Map<TextView,Boolean> seen=new WeakHashMap<>();
    private static final ThreadLocal<Integer> opening=ThreadLocal.withInitial(()->0);
    private static final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"nubia-folder-blur");t.setDaemon(true);return t;});
    private static volatile LauncherStyle style=LauncherStyle.STOCK;
    private static volatile long deadline,epoch;
    private static volatile Bitmap blurred;
    private static volatile int readyRadius=-1;
    private static volatile String blurState="native";
    private static volatile java.lang.ref.WeakReference<Object> manager=new java.lang.ref.WeakReference<>(null);
    private static int[] raw;private static int width,height; // worker confined
    private static Method readWallpaper;
    private static Field backgroundView;
    private static final ThreadLocal<Bitmap> nativeCopy=new ThreadLocal<>();
    private static final ThreadLocal<Boolean> mirroring=ThreadLocal.withInitial(()->false);
    private static final LauncherFolderBackgrounds backgrounds=new LauncherFolderBackgrounds();
    private static Field currentColor;
    private static boolean installed;
    static void install(ClassLoader loader){
        List<XC_MethodHook.Unhook> hooks=new ArrayList<>();
        try{
            Class<?> folder=XposedHelpers.findClass(FOLDER,loader),wallpaper=XposedHelpers.findClass("com.android.launcher3.resource.G1",loader);
            Method background=folder.getDeclaredMethod("B0"),cache=wallpaper.getDeclaredMethod("k"),capture=wallpaper.getDeclaredMethod("v",Bitmap.class);
            backgroundView=java.util.Arrays.stream(folder.getDeclaredFields()).filter(f->f.getType().getName().equals(FOLDER+"$l")).findFirst().orElseThrow();backgroundView.setAccessible(true);
            readWallpaper=wallpaper.getDeclaredMethod("r");readWallpaper.setAccessible(true);
            if(background.getReturnType()!=void.class||cache.getReturnType()!=Bitmap.class||readWallpaper.getReturnType()!=Bitmap.class)throw new IllegalStateException("Folder signatures changed");
            currentColor=XposedHelpers.findField(TextView.class,"mCurTextColor");
            XC_MethodHook textDraw=new XC_MethodHook(){
                protected void beforeHookedMethod(MethodHookParam p){
                    TextView view=(TextView)p.thisObject;int kind=kind(view);if(kind==0)return;seen.put(view,true);if(!active())return;
                    int color=kind==2?style.folderTitleColor():style.folderLabelColor();if(color==0)return;
                    try{p.setObjectExtra("folder_text",LauncherDrawState.tint(view,currentColor,LauncherStyle.labelTint(color,currentColor.getInt(view))));}catch(Throwable failure){end();}
                }
                protected void afterHookedMethod(MethodHookParam p){Object state=p.getObjectExtra("folder_text");if(state instanceof LauncherDrawState.TextState text)try{text.restore();}catch(Throwable failure){end();}}
            };
            hooks.add(XposedBridge.hookMethod(TextView.class.getDeclaredMethod("onDraw",Canvas.class),textDraw));
            hooks.add(XposedBridge.hookMethod(XposedHelpers.findClass("com.android.launcher3.BubbleTextView",loader).getDeclaredMethod("onDraw",Canvas.class),textDraw));
            hooks.add(XposedBridge.hookMethod(XposedHelpers.findClass("com.android.launcher3.views.DoubleShadowBubbleTextView",loader).getDeclaredMethod("onDraw",Canvas.class),textDraw));
            hooks.add(XposedBridge.hookMethod(background,new XC_MethodHook(){
                protected void beforeHookedMethod(MethodHookParam p){opening.set(opening.get()+1);if(opening.get()==1)nativeCopy.remove();}
                protected void afterHookedMethod(MethodHookParam p){
                    try{Bitmap original=nativeCopy.get();if(!p.hasThrowable()&&original!=null){View view=(View)backgroundView.get(p.thisObject);if(view!=null&&view.getBackground() instanceof android.graphics.drawable.BitmapDrawable custom){
                        backgrounds.remember(view,original);if(!active())restoreBackgrounds();
                    }}}catch(Throwable failure){end();}finally{int depth=opening.get()-1;if(depth==0){opening.remove();nativeCopy.remove();}else opening.set(depth);}
                }
            }));
            hooks.add(XposedBridge.hookMethod(Canvas.class.getDeclaredMethod("drawColor",int.class),new XC_MethodHook(){
                protected void beforeHookedMethod(MethodHookParam p){Bitmap original=nativeCopy.get();if(opening.get()==0||original==null||mirroring.get())return;try{mirroring.set(true);new Canvas(original).drawColor((Integer)p.args[0]);}catch(Throwable failure){blurState="native tint unavailable";}finally{mirroring.remove();}}
            }));
            hooks.add(XposedBridge.hookMethod(capture,new XC_MethodHook(){
                protected void beforeHookedMethod(MethodHookParam p){
                    manager=new java.lang.ref.WeakReference<>(p.thisObject);blurred=null;readyRadius=-1;
                    Bitmap input=(Bitmap)p.args[0];if(input==null||input.isRecycled()||(long)input.getWidth()*input.getHeight()>524288)return;
                    try{int w=input.getWidth(),h=input.getHeight();int[] copy=new int[w*h];input.getPixels(copy,0,w,0,0,w,h);worker.execute(()->{raw=copy;width=w;height=h;rebuild(epoch);});}catch(Throwable failure){blurState="native: wallpaper copy unavailable";}
                }
            }));
            hooks.add(XposedBridge.hookMethod(cache,new XC_MethodHook(){
                protected void afterHookedMethod(MethodHookParam p){
                    if(p.hasThrowable()||opening.get()==0||!active()||style.folderBlur()<0)return;
                    manager=new java.lang.ref.WeakReference<>(p.thisObject);
                    Bitmap ready=blurred;
                    if(ready!=null&&readyRadius==style.folderBlur()&&!ready.isRecycled()){
                        try{Bitmap copy=ready.copy(Bitmap.Config.ARGB_8888,true);if(copy!=null&&p.getResult() instanceof Bitmap original){nativeCopy.set(original);p.setResult(copy);}}catch(Throwable failure){blurState="native: copy unavailable";}
                    }else prepare();
                }
            }));
            installed=true;
        }catch(Throwable unavailable){for(var hook:hooks)hook.unhook();installed=false;blurState="folder hooks unavailable";}
    }
    static int kind(TextView view){
        boolean title=view.getClass().getName().equals("com.android.launcher3.folder.FolderNameEditText");
        boolean bubble=false;for(Class<?> c=view.getClass();c!=null;c=c.getSuperclass())if(c.getName().equals("com.android.launcher3.BubbleTextView")){bubble=true;break;}
        if(!title&&!bubble)return 0;
        java.util.List<String> ancestors=new ArrayList<>();for(var parent=view.getParent();parent!=null;parent=parent.getParent())ancestors.add(parent.getClass().getName());
        int container=-1,type=-1;if(bubble)try{Object info=view.getTag();container=XposedHelpers.getIntField(info,"container");type=XposedHelpers.getIntField(info,"itemType");}catch(Throwable ignored){}
        return LauncherFolderScope.kind(title,bubble,ancestors,container,type);
    }
    static void observe(View view){if(view instanceof TextView text&&kind(text)!=0){seen.put(text,true);text.invalidate();}}
    private static boolean active(){return installed&&style.folderCustom()&&SystemClock.elapsedRealtime()<deadline;}
    static void apply(LauncherStyle selected,long until){boolean changed=!selected.equals(style);style=selected;deadline=until;if(changed){restoreBackgrounds();epoch++;blurred=null;readyRadius=-1;prepare();refresh();}}
    static void end(){restoreBackgrounds();style=LauncherStyle.STOCK;deadline=0;epoch++;blurred=null;readyRadius=-1;blurState="native";refresh();}
    private static synchronized void prepare(){
        if(!active()||style.folderBlur()<0)return;long ticket=epoch;
        // Worker serializes reads and computation; native cached wallpaper is never borrowed or recycled.
        worker.execute(()->{
            if(ticket!=epoch||!active())return;
            if(raw==null){Object owner=manager.get();if(owner==null){blurState="waiting for folder wallpaper";return;}
                try{Bitmap bitmap=(Bitmap)readWallpaper.invoke(owner);if(bitmap==null||bitmap.isRecycled()||(long)bitmap.getWidth()*bitmap.getHeight()>524288){blurState="native: unsupported wallpaper copy";return;}
                    width=bitmap.getWidth();height=bitmap.getHeight();raw=new int[width*height];bitmap.getPixels(raw,0,width,0,0,width,height);
                    // Native may return a shared thumbnail. Do not recycle it.
                }catch(Throwable failure){blurState="native: wallpaper unavailable";return;}}
            rebuild(ticket);
        });
    }
    private static void rebuild(long ticket){
        if(ticket!=epoch||!active()||style.folderBlur()<0||raw==null)return;int radius=style.folderBlur();blurState="preparing; reopen folder when ready";
        try{int[] pixels=FolderBlur.pixels(raw,width,height,radius);Bitmap result=Bitmap.createBitmap(pixels,width,height,Bitmap.Config.ARGB_8888);
            if(ticket!=epoch||!active()||radius!=style.folderBlur())return;readyRadius=radius;blurred=result;blurState="ready, radius "+radius+"; reopen folder to apply";
        }catch(Throwable failure){blurState="native: custom blur unavailable";}
    }
    private static void restoreBackgrounds(){backgrounds.restore();}
    private static void refresh(){for(TextView text:new ArrayList<>(seen.keySet()))if(text!=null)text.invalidate();}
    static String status(){return "Folders: "+(installed?"ready":"unavailable")+"; blur: "+blurState+".";}
}
