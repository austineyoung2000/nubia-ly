package com.elitedarkkaiser.nubialy.xposed;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Temporary drawing tint confined to text belonging to an attached notification row. */
final class NotificationTextBackend {
    private static final String ROW="com.android.systemui.statusbar.notification.row.ExpandableNotificationRow";
    private static final String EXTRA="nubialy.notification.text.layer";
    private static final Set<View> seen=Collections.newSetFromMap(new WeakHashMap<>());
    private static XC_MethodHook.Unhook hook;
    private static boolean active;
    private static boolean eligible(View v){
        if(!(v instanceof TextView)||v instanceof EditText||!v.isAttachedToWindow())return false;
        for(int depth=0;depth<32&&v.getParent() instanceof View;depth++){
            v=(View)v.getParent();
            if(v.getClass().getName().equals(ROW))return v.isAttachedToWindow();
        }
        return false;
    }
    static void begin(int color){
        if(Looper.myLooper()!=Looper.getMainLooper()||color!=0&&(color>>>24)!=255)throw new IllegalArgumentException();
        if(!end())throw new IllegalStateException("Notification text cleanup incomplete");
        if(color==0)return;
        Paint paint=new Paint();paint.setColorFilter(new PorterDuffColorFilter(color,PorterDuff.Mode.SRC_IN));
        try{
            hook=XposedHelpers.findAndHookMethod(TextView.class,"onDraw",Canvas.class,new XC_MethodHook(1000){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    if(!active||Looper.myLooper()!=Looper.getMainLooper()||!ClockColorBackend.authorized()||!(p.thisObject instanceof View v)||!eligible(v))return;
                    try{Canvas c=(Canvas)p.args[0];int save=c.saveLayer(0,0,v.getWidth(),v.getHeight(),paint);p.setObjectExtra(EXTRA,save);seen.add(v);}catch(Throwable ignored){}
                }
                @Override protected void afterHookedMethod(MethodHookParam p){Object saved=p.getObjectExtra(EXTRA);if(saved instanceof Integer n)try{((Canvas)p.args[0]).restoreToCount(n);}catch(Throwable ignored){}}
            });active=true;
        }catch(Throwable failure){end();throw new IllegalStateException("Notification text unavailable",failure);}
    }
    static boolean end(){active=false;if(hook!=null)try{hook.unhook();hook=null;}catch(Throwable ignored){}
        for(View v:new ArrayList<>(seen))try{v.invalidate();}catch(Throwable ignored){}
        seen.clear();return hook==null;
    }
    static boolean healthy(){return active||hook==null;}
}
