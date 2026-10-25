package com.elitedarkkaiser.nubialy.xposed;

import android.content.res.ColorStateList;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import com.elitedarkkaiser.nubialy.StatusBarStyle;
import de.robv.android.xposed.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;

/** NX809J PhoneStatusBarView subtree only; temporary transforms/tint, no stock model changes. */
final class StatusBarBackend {
    // Only verified cleanup may clear a restoration-only failure; apply/hook failures stay latched.
    private static boolean hardFailed;
    private static final String ROOT="com.android.systemui.statusbar.phone.PhoneStatusBarView";
    private static final String ICON="com.android.systemui.statusbar.StatusBarIconView";
    private static final String BATTERY="com.android.systemui.battery.BatteryMeterView";
    private static final Map<ViewGroup,Boolean> roots=new WeakHashMap<>();
    private static final Map<View,State> states=new WeakHashMap<>();
    private static final List<XC_MethodHook.Unhook> activeHooks=new ArrayList<>();
    private static boolean ready, attempted, active, writing,vendorReady;private static int vendorUpdates;
    private static volatile boolean failed;
    private static long generation;
    private static int color, captured, restored;
    private static String message="Status-bar observer not installed.";
    private static StatusBarStyle style=StatusBarStyle.STOCK;
    private static final class State {
        final WeakReference<ViewGroup> root;
        final String slot; final int scale,opacity,transform; final boolean hide, notification; boolean tint;
        float x,y,a,tx,ty,rotation; int visibility; ColorStateList colors; long tintRevision;
        State(View v, ViewGroup owner, int scale, boolean hide, boolean tint, boolean notification,int opacity,String slot,int transform) {
            root=new WeakReference<>(owner);this.scale=scale;this.hide=hide;this.tint=tint;this.notification=notification;this.opacity=opacity;this.slot=slot;this.transform=transform;
            x=v.getScaleX();y=v.getScaleY();a=v.getAlpha();visibility=v.getVisibility();tx=v.getTranslationX();ty=v.getTranslationY();rotation=v.getRotation();
            if(tint)colors=((ImageView)v).getImageTintList();
        }
    }
    private record Frame(long generation, State state) {}
    private record DarkFrame(long generation, State state, long revision) {}
    static synchronized void install(ClassLoader loader) {
        SurfaceBackend.install(loader);
        if(attempted)return;attempted=true;
        List<XC_MethodHook.Unhook> observers=new ArrayList<>();
        try {
            Class<?> root=XposedHelpers.findClass(ROOT,loader), icon=XposedHelpers.findClass(ICON,loader), battery=XposedHelpers.findClass(BATTERY,loader);
            if(root.getSuperclass()!=FrameLayout.class || !ImageView.class.isAssignableFrom(icon) || battery.getSuperclass()!=LinearLayout.class
                || icon.getDeclaredMethod("isNotification").getReturnType()!=boolean.class || icon.getDeclaredMethod("getSlot").getReturnType()!=String.class
                || !icon.getDeclaredMethod("getAdapt").getReturnType().getName().equals("com.zte.adapt.mifavor.statusbar.StatusBarIconViewAdapt")
                || !root.getDeclaredMethod("getAdapt").getReturnType().getName().equals("com.zte.adapt.mifavor.statusbar.PhoneStatusBarViewAdapt")
                || !battery.getDeclaredMethod("getAdapt").getReturnType().getName().equals("com.zte.adapt.mifavor.statusbar.BatteryMeterViewAdapt")) throw new IllegalStateException();
            for(String name:new String[]{"onAttachedToWindow","onDetachedFromWindow"}) {
                observers.add(XposedBridge.hookMethod(root.getDeclaredMethod(name),new XC_MethodHook(){
                    @Override protected void beforeHookedMethod(MethodHookParam p){
                        if(!ready||!name.equals("onDetachedFromWindow"))return;
                        ViewGroup owner=(ViewGroup)p.thisObject;
                        for(View v:new ArrayList<>(states.keySet()))if(states.get(v).root.get()==owner)restore(v);
                        roots.remove(owner);
                    }
                    @Override protected void afterHookedMethod(MethodHookParam p){
                        if(!ready||p.hasThrowable()||!name.equals("onAttachedToWindow")||Looper.myLooper()!=Looper.getMainLooper())return;
                        ViewGroup owner=(ViewGroup)p.thisObject;roots.put(owner,true);if(active)scan(owner);
                    }
                }));
            }
            observers.add(XposedBridge.hookMethod(View.class.getDeclaredMethod("onAttachedToWindow"),new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){if(ready&&!p.hasThrowable()&&Looper.myLooper()==Looper.getMainLooper()){View view=(View)p.thisObject;ViewGroup owner=owner(view);if(owner!=null&&owner.isAttachedToWindow())roots.put(owner,true);if(active)track(view);}}
            }));
            observers.add(XposedBridge.hookMethod(View.class.getDeclaredMethod("onDetachedFromWindow"),new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){if(active&&Looper.myLooper()==Looper.getMainLooper())restore((View)p.thisObject);}
            }));
            observers.add(XposedBridge.hookMethod(icon.getDeclaredMethod("onDarkChanged",ArrayList.class,float.class,int.class),new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    if(!active||writing||Looper.myLooper()!=Looper.getMainLooper())return;
                    State s=states.get(p.thisObject);if(s==null||!s.tint)return;
                    try {
                        // The inspected vendor callback can return without writing tint.
                        Object adapt=XposedHelpers.callMethod(p.thisObject,"getAdapt");
                        boolean accepts=Boolean.FALSE.equals(XposedHelpers.callMethod(adapt,"donotSetIconColor"));
                        reconcileTint((View)p.thisObject,s,accepts);
                        if(accepts)
                            p.setObjectExtra("nubia-ly:icon-dark",new DarkFrame(generation,s,s.tintRevision));
                    }catch(Throwable unavailable){failed=true;hardFailed=true;}
                }
                @Override protected void afterHookedMethod(MethodHookParam p){
                    Object extra=p.getObjectExtra("nubia-ly:icon-dark");if(!(extra instanceof DarkFrame f))return;
                    View v=(View)p.thisObject;
                    if(!active||writing||f.generation!=generation||states.get(v)!=f.state)return;
                    try {
                        if(p.hasThrowable())throw new IllegalStateException();
                        if(f.revision==f.state.tintRevision)f.state.colors=((ImageView)v).getImageTintList();
                        paint(v,f.state);
                    }catch(Throwable unavailable){failed=true;hardFailed=true;}
                }
            }));
            installVendorObservers(loader,observers);
            ready=true;message="Status-bar observer ready.";
        }catch(Throwable unavailable){
            for(var h:observers)try{h.unhook();}catch(Throwable ignored){}
            ready=false;message="Status-bar source signatures unavailable: "+unavailable.getClass().getSimpleName()+".";
        }
    }
    private static void installVendorObservers(ClassLoader loader,List<XC_MethodHook.Unhook> observers){
        List<XC_MethodHook.Unhook> added=new ArrayList<>();try{
            String name="com.zte.feature.statusbar.RedMagicFunctionIconFeature";Class<?> feature=XposedHelpers.findClass(name,loader),host=XposedHelpers.findClass(ROOT,loader);
            for(String field:new String[]{"coolingFanView","liquidCoolingView","rateRefreshView"})if(feature.getDeclaredField(field).getType()!=ImageView.class)throw new IllegalStateException("Vendor indicator field");
            added.add(XposedBridge.hookMethod(feature.getDeclaredMethod("onStatusBarInflate",host,boolean.class),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!p.hasThrowable()&&mainThread()&&p.args[0] instanceof ViewGroup root){roots.put(root,true);if(active)scan(root);}}}));
            String[][] types={{"CoolingFanIcon","coolingFanView","initCoolingFanView","updateCoolingFansState"},{"LiquidCoolingIcon","liquidCoolingView","initLiquidCoolingView","updateLiquidCoolingState"},{"RefreshRateIcon","rateRefreshView","initRefreshRateIcon","updateRefreshRate"}};
            for(String[] spec:types){Class<?> type=XposedHelpers.findClass(name+"$"+spec[0],loader);if(type.getDeclaredField("this$0").getType()!=feature)throw new IllegalStateException("Vendor feature owner");java.lang.reflect.Field enclosing=type.getDeclaredField("this$0"),indicator=feature.getDeclaredField(spec[1]);enclosing.setAccessible(true);indicator.setAccessible(true);for(int n=2;n<spec.length;n++)added.add(XposedBridge.hookMethod(type.getDeclaredMethod(spec[n]),new XC_MethodHook(){
                private View target(Object nested){try{Object featureOwner=enclosing.get(nested);Object value=indicator.get(featureOwner);return value instanceof View view?view:null;}catch(Throwable absent){return null;}}
                @Override protected void beforeHookedMethod(MethodHookParam p){if(!active||writing||!mainThread())return;View view=target(p.thisObject);if(view!=null&&vendorIndicator(view)!=0&&owner(view)!=null)restore(view);}
                @Override protected void afterHookedMethod(MethodHookParam p){if(p.hasThrowable()||!active||writing||!mainThread())return;View view=target(p.thisObject);if(view!=null&&vendorIndicator(view)!=0&&owner(view)!=null){vendorUpdates++;track(view);}}
            }));}
            observers.addAll(added);vendorReady=true;
        }catch(Throwable unavailable){for(var hook:added)try{hook.unhook();}catch(Throwable ignored){}vendorReady=false;}
    }
    static void begin(StatusBarStyle selection,int selectedColor) {
        if(selection==null||!selection.valid())throw new IllegalArgumentException();
        if(!end())throw new IllegalStateException("Status-bar restoration incomplete");
        style=selection;color=selectedColor;captured=restored=0;
        if(!style.any()){message="Stock icon and battery appearance.";return;}
        if(!ready||failed)throw new IllegalStateException(message);
        XC_MethodHook callback=new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                if(!active||writing)return;
                State s=states.get(p.thisObject);
                // Vendor indicators may be inserted/rebound without an observed root attach.
                // Capture their native state at the first visibility write, then reassert Hide.
                if(s==null&&p.thisObject instanceof View view&&mainThread()&&vendorIndicator(view)!=0){track(view);s=states.get(view);}
                if(s==null)return;
                if(Looper.myLooper()!=Looper.getMainLooper()){failed=true;hardFailed=true;return;}
                p.setObjectExtra("nubia-ly:status-write",new Frame(generation,s));
            }
            @Override protected void afterHookedMethod(MethodHookParam p){
                Object extra=p.getObjectExtra("nubia-ly:status-write");if(!(extra instanceof Frame f))return;
                View v=(View)p.thisObject;
                if(!active||writing||f.generation!=generation||states.get(v)!=f.state)return;
                try {
                    if(p.hasThrowable())throw new IllegalStateException();
                    switch(((Method)p.method).getName()){
                        case "setScaleX" -> f.state.x=v.getScaleX();
                        case "setScaleY" -> f.state.y=v.getScaleY();
                        case "setTranslationX" -> f.state.tx=v.getTranslationX();
                        case "setTranslationY" -> f.state.ty=v.getTranslationY();
                        case "setRotation" -> f.state.rotation=v.getRotation();
                        case "setAlpha" -> f.state.a=v.getAlpha();
                        case "setVisibility", "setTransitionVisibility" -> f.state.visibility=v.getVisibility();
                        case "setImageTintList" -> {if(f.state.tint){f.state.colors=((ImageView)v).getImageTintList();f.state.tintRevision++;}}
                    }
                    paint(v,f.state);
                }catch(Throwable unavailable){failed=true;hardFailed=true;}
            }
        };
        String stage="status-bar property hooks";
        try {
            activeHooks.add(XposedHelpers.findAndHookMethod(View.class,"setScaleX",float.class,callback));
            activeHooks.add(XposedHelpers.findAndHookMethod(View.class,"setScaleY",float.class,callback));
            activeHooks.add(XposedHelpers.findAndHookMethod(View.class,"setTranslationX",float.class,callback));
            activeHooks.add(XposedHelpers.findAndHookMethod(View.class,"setTranslationY",float.class,callback));
            activeHooks.add(XposedHelpers.findAndHookMethod(View.class,"setRotation",float.class,callback));
            activeHooks.add(XposedHelpers.findAndHookMethod(View.class,"setAlpha",float.class,callback));
            activeHooks.add(XposedHelpers.findAndHookMethod(View.class,"setVisibility",int.class,callback));
            activeHooks.add(XposedHelpers.findAndHookMethod(View.class,"setTransitionVisibility",int.class,callback));
            if(style.selected(StatusBarStyle.TINT))activeHooks.add(XposedHelpers.findAndHookMethod(ImageView.class,"setImageTintList",ColorStateList.class,callback));
            stage="surface activation";SurfaceBackend.begin(style.surfaces());stage="status-bar refresh";active=true;refresh();
            if(failed)throw new IllegalStateException("Status-bar property application failed");
            if(!SurfaceBackend.healthy())throw new IllegalStateException("Surface health check failed after status-bar refresh");
            // Hosts may be absent during native window recreation. Keep the bounded
            // session armed; attachment callbacks apply only to verified native hosts.
            message=roots.keySet().stream().anyMatch(View::isAttachedToWindow)?"Status-bar styling active.":"Status-bar styling armed; waiting for attached native host.";
        }catch(Throwable unavailable){end();throw new IllegalStateException("Status-bar activation unavailable at "+stage,unavailable);}
    }
    private static boolean mainThread(){return Looper.myLooper()==Looper.getMainLooper();}
    private static ViewGroup owner(View view){
        View current=view;
        for(int depth=0;depth<24&&current!=null;depth++){
            if((current.getClass().getName().equals(ROOT)||QsHeaderScope.isRoot(current))&&current instanceof ViewGroup group)return group;
            ViewParent parent=current.getParent();current=parent instanceof View v?v:null;
        }return null;
    }
    private static int vendorIndicator(View view){
        // The uploaded NX809J feature resolves these ImageViews under PhoneStatusBarView.
        if(!(view instanceof ImageView)||view.getId()==View.NO_ID)return 0;
        try{
            if(!"com.android.systemui".equals(view.getResources().getResourcePackageName(view.getId())))return 0;
            return switch(view.getResources().getResourceEntryName(view.getId())){
                case "cooling_fan_view"->1;case "liquid_cooling_view"->2;case "refresh_rate_view"->3;default->0;
            };
        }catch(android.content.res.Resources.NotFoundException absent){return 0;}
    }
    private static void scan(ViewGroup root){
        ArrayDeque<View> queue=new ArrayDeque<>();queue.add(root);int count=0;
        while(!queue.isEmpty()&&count++<512){View v=queue.removeFirst();track(v);if(v instanceof ViewGroup g)for(int n=0;n<g.getChildCount()&&queue.size()<512;n++)queue.addLast(g.getChildAt(n));}
    }
    static void invalidateHeadersAndBars(){for(ViewGroup root:new ArrayList<>(roots.keySet()))invalidateTree(root);for(View root:new ArrayList<>(QsHeaderScope.headers))invalidateTree(root);}
    private static void invalidateTree(View v){v.invalidate();if(v instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++)invalidateTree(g.getChildAt(i));}
    static void observeHeader(View header){if(header instanceof ViewGroup root){roots.put(root,true);if(active)scan(root);}}
    static void refresh(){
        if(!active||failed||Looper.myLooper()!=Looper.getMainLooper())return;
        SurfaceBackend.refresh();
        for(View header:new ArrayList<>(QsHeaderScope.headers))if(header instanceof ViewGroup group&&group.isAttachedToWindow())roots.put(group,true);
        for(ViewGroup root:new ArrayList<>(roots.keySet()))if(root.isAttachedToWindow())scan(root);
    }
    private static void track(View v){
        if(!active||writing||failed||!v.isAttachedToWindow())return;
        String name=v.getClass().getName();
        ViewGroup root=owner(v);if(root==null)return;
        int indicator=vendorIndicator(v);
        if(indicator!=0)roots.put(root,true);
        if(!name.equals(ICON)&&!name.equals(BATTERY)&&indicator==0){if(states.containsKey(v))restore(v);return;}
        try {
            boolean notification=name.equals(ICON)&&Boolean.TRUE.equals(XposedHelpers.callMethod(v,"isNotification"));
            String slot=name.equals(ICON)&&!notification?(String)XposedHelpers.callMethod(v,"getSlot"):"";
            // Some stock icons have no named slot. Keep bulk styling, but exclude them
            // from selective hiding and never pass null to immutable-set membership.
            if(!StatusBarStyle.slot(slot))slot="";
            if(indicator!=0)slot="vendor:"+indicator;
            State s=states.get(v);
            if(s!=null&&(s.root.get()!=root||s.notification!=notification||!Objects.equals(s.slot,slot))){if(!restore(v))return;s=null;}
            if(s==null){
                int scale,opacity,transform;boolean hide,tint=false;
                if(indicator!=0){
                    int appearance=indicator==1?style.fanAppearance():indicator==2?style.pumpAppearance():style.refreshAppearance();
                    transform=0;scale=StatusBarStyle.indicatorScale(appearance);opacity=StatusBarStyle.indicatorOpacity(appearance);
                    hide=style.selected(indicator==1?StatusBarStyle.HIDE_FAN:indicator==2?StatusBarStyle.HIDE_PUMP:StatusBarStyle.HIDE_REFRESH);
                }
                else if(name.equals(BATTERY)){transform=style.batteryTransform();scale=style.batteryScale();opacity=style.batteryOpacity();hide=style.selected(StatusBarStyle.HIDE_BATTERY);}
                else {
                    transform=notification?style.notificationTransform():style.systemTransform();
                    scale=notification?style.notificationScale():style.systemScale();opacity=notification?style.notificationOpacity():style.systemOpacity();
                    hide=style.selected(notification?StatusBarStyle.HIDE_NOTIFICATIONS:StatusBarStyle.HIDE_SYSTEM);
                    if(!notification)hide|=style.hiddenSlots().contains(slot);
                    if(!notification&&style.selected(StatusBarStyle.TINT)){
                        Object adapt=XposedHelpers.callMethod(v,"getAdapt");
                        tint=Boolean.FALSE.equals(XposedHelpers.callMethod(adapt,"donotSetIconColor"));
                    }
                }
                if(scale==100&&opacity==100&&!hide&&!tint&&transform==0)return;
                s=new State(v,root,scale,hide,tint,notification,opacity,slot,transform);states.put(v,s);captured++;
            }
            if(name.equals(ICON)&&!notification&&style.selected(StatusBarStyle.TINT)){
                Object adapt=XposedHelpers.callMethod(v,"getAdapt");
                reconcileTint(v,s,Boolean.FALSE.equals(XposedHelpers.callMethod(adapt,"donotSetIconColor")));
            }
            paint(v,s);
        }catch(Throwable unavailable){failed=true;hardFailed=true;message="Status-bar callback failed ("+unavailable.getClass().getSimpleName()+"); restoration required.";}
    }
    private static void reconcileTint(View v,State s,boolean wanted){
        if(s.tint==wanted)return;
        ImageView icon=(ImageView)v;writing=true;
        try {
            if(wanted){s.colors=icon.getImageTintList();s.tintRevision++;s.tint=true;}
            else{icon.setImageTintList(s.colors);if(!Objects.equals(icon.getImageTintList(),s.colors))throw new IllegalStateException();s.tint=false;}
        }finally{writing=false;}
    }
    private static void paint(View v,State s){
        writing=true;
        try {
            if(s.scale!=100){float x=s.x*s.scale/100f,y=s.y*s.scale/100f;if(Math.abs(v.getScaleX()-x)>0.00001f)v.setScaleX(x);if(Math.abs(v.getScaleY()-y)>0.00001f)v.setScaleY(y);}
            if(s.transform!=0){
                float density=v.getResources().getDisplayMetrics().density;
                if(!Float.isFinite(density)||density<=0||!Float.isFinite(s.tx)||!Float.isFinite(s.ty)||!Float.isFinite(s.rotation))throw new IllegalStateException("Invalid stock transform");
                if(StatusBarStyle.horizontal(s.transform)!=0){float target=s.tx+StatusBarStyle.horizontal(s.transform)*density;if(Math.abs(v.getTranslationX()-target)>0.00001f)v.setTranslationX(target);}
                if(StatusBarStyle.vertical(s.transform)!=0){float target=s.ty+StatusBarStyle.vertical(s.transform)*density;if(Math.abs(v.getTranslationY()-target)>0.00001f)v.setTranslationY(target);}
                if(StatusBarStyle.rotation(s.transform)!=0){float target=s.rotation+StatusBarStyle.rotation(s.transform);if(Math.abs(v.getRotation()-target)>0.00001f)v.setRotation(target);}
            }
            if(s.opacity!=100&&Math.abs(v.getAlpha()-s.a*s.opacity/100f)>0.00001f)v.setAlpha(s.a*s.opacity/100f);
            if(s.hide&&v.getVisibility()!=View.GONE)v.setVisibility(View.GONE);
            if(s.tint){ImageView icon=(ImageView)v;if(icon.getImageTintList()==null||icon.getImageTintList().getDefaultColor()!=color)icon.setImageTintList(ColorStateList.valueOf(color));}
        }finally{writing=false;}
    }
    private static boolean restore(View v){
        State s=states.get(v);if(s==null)return true;
        writing=true;
        try {
            if(s.scale!=100){v.setScaleX(s.x);v.setScaleY(s.y);if(Math.abs(v.getScaleX()-s.x)>0.00001f||Math.abs(v.getScaleY()-s.y)>0.00001f)throw new IllegalStateException();}
            if(StatusBarStyle.horizontal(s.transform)!=0){v.setTranslationX(s.tx);if(Math.abs(v.getTranslationX()-s.tx)>0.00001f)throw new IllegalStateException();}
            if(StatusBarStyle.vertical(s.transform)!=0){v.setTranslationY(s.ty);if(Math.abs(v.getTranslationY()-s.ty)>0.00001f)throw new IllegalStateException();}
            if(StatusBarStyle.rotation(s.transform)!=0){v.setRotation(s.rotation);if(Math.abs(v.getRotation()-s.rotation)>0.00001f)throw new IllegalStateException();}
            if(s.opacity!=100){v.setAlpha(s.a);if(Math.abs(v.getAlpha()-s.a)>0.00001f)throw new IllegalStateException();}
            if(s.hide){v.setVisibility(s.visibility);if(v.getVisibility()!=s.visibility)throw new IllegalStateException();}
            if(s.tint){((ImageView)v).setImageTintList(s.colors);if(!Objects.equals(((ImageView)v).getImageTintList(),s.colors))throw new IllegalStateException();}
            states.remove(v);restored++;return true;
        }catch(Throwable unavailable){failed=true;message="Status-bar restoration incomplete. Disable nubia-ly in Vector and reboot.";return false;}
        finally{writing=false;}
    }
    static boolean end(){
        generation++;active=false;
        for(View v:new ArrayList<>(states.keySet()))restore(v);
        for(var h:new ArrayList<>(activeHooks))try{h.unhook();activeHooks.remove(h);}catch(Throwable unavailable){failed=true;hardFailed=true;}boolean surfaceRestored=SurfaceBackend.end();if(!failed&&captured>0)message="Latest stock status-bar properties restored.";if(!hardFailed&&states.isEmpty()&&activeHooks.isEmpty())failed=false;return !failed&&states.isEmpty()&&surfaceRestored;
    }
    static ArrayList<String> inventory(){
        TreeSet<String> slots=new TreeSet<>();if(!ready||Looper.myLooper()!=Looper.getMainLooper())return new ArrayList<>();
        for(ViewGroup root:new ArrayList<>(roots.keySet())){
            if(!root.isAttachedToWindow())continue;ArrayDeque<View> queue=new ArrayDeque<>();queue.add(root);int count=0;
            while(!queue.isEmpty()&&count++<512){View v=queue.removeFirst();
                if(v.getClass().getName().equals(ICON))try{if(!Boolean.TRUE.equals(XposedHelpers.callMethod(v,"isNotification"))){String slot=(String)XposedHelpers.callMethod(v,"getSlot");if(StatusBarStyle.slot(slot)&&slots.size()<32)slots.add(slot);}}catch(Throwable unavailable){/* Unsupported metadata supplies no choice. */}
                if(v instanceof ViewGroup g)for(int n=0;n<g.getChildCount()&&queue.size()<512;n++)queue.addLast(g.getChildAt(n));
            }
        }return new ArrayList<>(slots);
    }
    static boolean healthy(){return !failed&&SurfaceBackend.healthy();}
    static String status(){return message+" Requested vendor hide: fan="+style.selected(StatusBarStyle.HIDE_FAN)+", pump="+style.selected(StatusBarStyle.HIDE_PUMP)+", refresh="+style.selected(StatusBarStyle.HIDE_REFRESH)+"; native update hooks="+vendorReady+", updates="+vendorUpdates+". "+" Captured: "+captured+"; restored: "+restored+"; status-bar remaining: "+states.size()+". "+SurfaceBackend.status();}
}
