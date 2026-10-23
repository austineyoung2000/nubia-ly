package com.elitedarkkaiser.nubialy.xposed;

import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.os.Looper;
import android.view.ViewGroup;
import com.elitedarkkaiser.nubialy.QsTileEffects;
import de.robv.android.xposed.*;
import java.lang.ref.WeakReference;
import java.util.*;

/** Draw-only decoration for exact inspected NX809J tile owners under SurfaceBackend authority. */
final class QsOutlineBackend {
    private static final String WIDE="com.zte.qs.tileimpl.QSTileViewWide",CIRCLE="com.zte.qs.tileimpl.QSTileViewCircle";
    private static final Map<GradientDrawable,WeakReference<ViewGroup>> owners=new WeakHashMap<>();
    private static XC_MethodHook.Unhook hook;private static boolean active,failed;private static long generation;private static int selectedStyle;
    static void begin(int style){if(style<0||style>7)throw new IllegalArgumentException();if(!end())throw new IllegalStateException("QS outline cleanup failed");selectedStyle=style;if(style==0)return;
        final long ticket=++generation;
        try{hook=XposedHelpers.findAndHookMethod(GradientDrawable.class,"draw",Canvas.class,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                if(!active||failed||Looper.myLooper()!=Looper.getMainLooper()||!(p.thisObject instanceof GradientDrawable drawable))return;
                WeakReference<ViewGroup> ref=owners.get(drawable);ViewGroup owner=ref==null?null:ref.get();
                if(!eligible(owner,drawable))return;
                try{boolean rendered=QsTileEffects.draw(style,(Canvas)p.args[0],drawable,owner.getResources().getDisplayMetrics().density,
                    ()->active&&!failed&&generation==ticket&&eligible(owner,drawable),
                    ()->XposedBridge.invokeOriginalMethod(p.method,p.thisObject,p.args));
                    if(rendered)p.setResult(null);
                }catch(Throwable unavailable){failed=true;invalidateOwners();}
            }
        });active=true;}catch(Throwable unavailable){failed=true;end();throw new IllegalStateException("QS outline hook unavailable",unavailable);}
    }
    private static boolean eligible(ViewGroup owner,GradientDrawable drawable){
        if(owner==null||!owner.isAttachedToWindow()||(!owner.getClass().getName().equals(WIDE)&&!owner.getClass().getName().equals(CIRCLE)))return false;
        WeakReference<ViewGroup> ref=owners.get(drawable);if(ref==null||ref.get()!=owner)return false;
        try{return XposedHelpers.callMethod(owner,"getColorBackgroundDrawable")==drawable;}catch(Throwable unavailable){return false;}
    }
    static void track(ViewGroup owner){if(!active||failed||!owner.isAttachedToWindow()||(!owner.getClass().getName().equals(WIDE)&&!owner.getClass().getName().equals(CIRCLE)))return;
        try{Object value=XposedHelpers.callMethod(owner,"getColorBackgroundDrawable");if(!(value instanceof GradientDrawable drawable))return;
            for(var entry:new ArrayList<>(owners.entrySet()))if(entry.getValue().get()==owner&&entry.getKey()!=drawable)owners.remove(entry.getKey());
            WeakReference<ViewGroup> old=owners.get(drawable);if(old!=null&&old.get()!=null&&old.get()!=owner){failed=true;invalidateOwners();return;}
            if(old==null){owners.put(drawable,new WeakReference<>(owner));owner.invalidate();}
        }catch(Throwable unavailable){failed=true;invalidateOwners();}
    }
    static void release(ViewGroup owner){owners.entrySet().removeIf(e->e.getValue().get()==owner);}
    private static void invalidateOwners(){for(var ref:new ArrayList<>(owners.values())){ViewGroup owner=ref.get();if(owner!=null)try{owner.invalidate();}catch(Throwable unavailable){failed=true;}}}
    static boolean end(){generation++;active=false;invalidateOwners();owners.clear();if(hook!=null)try{hook.unhook();hook=null;}catch(Throwable unavailable){failed=true;}return hook==null;}
    static boolean healthy(){return !failed;}
    static String status(){return "QS tile style: "+selectedStyle+" "+(active?"enabled":"stock")+"; tracked: "+owners.size()+(failed?"; decoration stopped after failure.":".");}
}
