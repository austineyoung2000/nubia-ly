package com.elitedarkkaiser.nubialy.xposed;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.view.*;
import com.elitedarkkaiser.nubialy.NotificationEffects;
import com.elitedarkkaiser.nubialy.NotificationControls;
import de.robv.android.xposed.*;
import java.util.*;
/** Exact native card-background pass, beneath notification content and its interactions. */
final class NotificationBackend {
    private static final String BACKGROUND="com.android.systemui.statusbar.notification.row.NotificationBackgroundView",ROW="com.android.systemui.statusbar.notification.row.ExpandableNotificationRow",BASE="com.android.systemui.statusbar.notification.row.ActivatableNotificationView";
    private static final Map<View,Boolean> observed=new WeakHashMap<>();
    private static Class<?> background;private static XC_MethodHook.Unhook hook;private static boolean ready,active,failed;private static long generation;private static int selection,appearance,draws;private static String message="not initialized";
    private static boolean main(){return Looper.myLooper()==Looper.getMainLooper();}
    static void install(ClassLoader loader){try{background=XposedHelpers.findClass(BACKGROUND,loader);Class<?> base=XposedHelpers.findClass(BASE,loader),row=XposedHelpers.findClass(ROW,loader);
        if(background.getSuperclass()!=View.class||!base.isAssignableFrom(row)||base.getDeclaredField("mBackgroundNormal").getType()!=background||background.getDeclaredField("mBackground").getType()!=Drawable.class||background.getDeclaredField("mCornerRadii").getType()!=float[].class)throw new IllegalStateException("Notification signatures");
        background.getDeclaredMethod("draw",Canvas.class,Drawable.class);ready=true;message="ready";
    }catch(Throwable unavailable){message="source unavailable: "+unavailable.getClass().getSimpleName();}}
    static void observe(View view){if(main()&&view.getClass().getName().equals(BACKGROUND)){observed.put(view,true);if(active&&eligible(view))view.invalidate();}}
    static void release(View view){observed.remove(view);}
    private static boolean eligible(View view){if(view==null||!view.isAttachedToWindow()||!view.getClass().getName().equals(BACKGROUND))return false;ViewParent parent=view.getParent();
        for(int depth=0;depth<12&&parent instanceof View owner;depth++,parent=owner.getParent())if(owner.getClass().getName().equals(ROW))try{return XposedHelpers.getObjectField(owner,"mBackgroundNormal")==view;}catch(Throwable unavailable){return false;}return false;
    }
    static void begin(int style){begin(style,0);}
    static void begin(int style,int controls){begin(style,controls,0,0,0);}
    static void begin(int style,int controls,int first,int second,int mode){if(mode<0||mode>3)throw new IllegalArgumentException();if(!NotificationControls.valid(controls))throw new IllegalArgumentException();appearance=controls;if(style<0||style>5||!main())throw new IllegalArgumentException();if(!end())throw new IllegalStateException("Notification cleanup incomplete");selection=style;draws=0;if(style==0&&appearance==0&&mode==0)return;if(!ready||failed)throw new IllegalStateException(message);final long ticket=++generation;
        try{hook=XposedBridge.hookMethod(background.getDeclaredMethod("draw",Canvas.class,Drawable.class),new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){
            if(!active||failed||!main()||!(p.thisObject instanceof View view)||!eligible(view))return;observed.put(view,true);boolean nativeDrawn=false;
            try{if(p.args[1]==null||XposedHelpers.getObjectField(view,"mBackground")!=p.args[1]||!(p.args[1] instanceof Drawable nativeBg))return;
                Canvas canvas=(Canvas)p.args[0];int opacity=NotificationControls.opacity(appearance);int saved=canvas.saveLayerAlpha(0,0,view.getWidth(),view.getHeight(),opacity==0?255:opacity*255/100);
                try{XposedBridge.invokeOriginalMethod(p.method,p.thisObject,p.args);nativeDrawn=true;
                    if(active&&!failed&&generation==ticket&&eligible(view)){
                        RectF bounds=new RectF(nativeBg.getBounds());float[] radii=(float[])XposedHelpers.getObjectField(view,"mCornerRadii");
                        if(mode!=0)NotificationEffects.color(canvas,bounds,radii,first,second,mode);
                        if(NotificationEffects.draw(style,canvas,bounds,radii,view.getResources().getDisplayMetrics().density,()->active&&!failed&&generation==ticket&&eligible(view)&&XposedHelpers.getObjectField(view,"mBackground")==nativeBg,()->{}))draws++;
                        int radius=NotificationControls.radius(appearance);if(radius!=0&&!bounds.isEmpty()){
                            Path path=new Path();path.addRoundRect(bounds,radius*view.getResources().getDisplayMetrics().density,radius*view.getResources().getDisplayMetrics().density,Path.Direction.CW);
                            // Clear outside the extra corner clip on this background-only layer.
                            int clip=canvas.save();try{canvas.clipOutPath(path);canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);}finally{canvas.restoreToCount(clip);}
                        }
                    }
                }finally{canvas.restoreToCount(saved);}
                p.setResult(null);
            }catch(Throwable unavailable){failed=true;invalidate();if(nativeDrawn)p.setResult(null);}
        }});active=true;invalidate();}catch(Throwable unavailable){failed=true;end();throw new IllegalStateException("Notification styling unavailable",unavailable);}
    }
    private static void invalidate(){for(View view:new ArrayList<>(observed.keySet()))try{if(eligible(view))view.invalidate();}catch(Throwable unavailable){failed=true;}}
    static boolean end(){generation++;active=false;invalidate();if(hook!=null)try{hook.unhook();hook=null;}catch(Throwable unavailable){failed=true;}return hook==null;}
    static boolean healthy(){return !failed;}
    static String status(){return "Notification style: "+selection+"; observed backgrounds: "+observed.size()+"; decorated draws: "+draws+"; "+message+(failed?"; styling stopped.":".");}
}
