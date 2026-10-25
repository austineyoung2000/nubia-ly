package com.elitedarkkaiser.nubialy.xposed;

import android.graphics.drawable.Drawable;
import android.os.*;
import android.view.View;
import android.widget.ImageView;
import com.elitedarkkaiser.nubialy.*;
import de.robv.android.xposed.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;

/** SensorService idle keyguard background only. No authentication, window or touch writes. */
final class SensorFingerprintBackend {
    // Only verified cleanup may clear a restoration-only failure; apply/hook failures stay latched.
    private static boolean hardFailed;
    private static final String BASE="com.fingerprint.sensorservice.",OWNER=BASE+"view.FloatWindowSensorView",SERVICE=BASE+"UdfpsMessageService";
    private static WeakReference<Object> service=new WeakReference<>(null);
    private static final Map<ImageView,State> states=new WeakHashMap<>();
    // Knowledge of an existing idle graphic is not appearance authority. Retain
    // it across draft/style changes, but forget detached/replaced service views.
    private static final Map<ImageView,Boolean> knownGraphics=new WeakHashMap<>();
    private static final ArrayDeque<String> events=new ArrayDeque<>();
    private static String gate="not checked";
    private static void event(String value){if(value.equals(events.peekLast()))return;events.addLast(value);while(events.size()>4)events.removeFirst();}
    private static final List<XC_MethodHook.Unhook> activeHooks=new ArrayList<>();
    private static boolean ready,active,writing,failed,pressed;private static int style,captured,restored;private static boolean hidden;private static long deadline,generation;
    private static String message="Not initialized";
    private static Drawable preparedGlyph;private static int preparedStyle=-1,preparedColor;
    private static final class State {Drawable stock,custom;int depth;State(ImageView view){stock=view.getBackground();}}
    private record Frame(long generation,State state){}
    private static boolean main(){return Looper.myLooper()==Looper.getMainLooper();}
    static boolean install(ClassLoader loader){
        List<XC_MethodHook.Unhook> observers=new ArrayList<>();try{
            Class<?> owner=XposedHelpers.findClass(OWNER,loader),svc=XposedHelpers.findClass(SERVICE,loader),manager=XposedHelpers.findClass(BASE+"view.FloatWindowManager",loader),machine=XposedHelpers.findClass(BASE+"manager.FingerprintStateMachine",loader);
            if(owner.getDeclaredField("mSensorView").getType()!=ImageView.class||owner.getDeclaredField("isQuickShow").getType()!=boolean.class||manager.getDeclaredField("floatWindowAuthenticateSensorView").getType()!=owner||manager.getDeclaredMethod("showFromSystemUI").getReturnType()!=boolean.class||svc.getDeclaredField("mFloatWindowManager").getType()!=manager||svc.getDeclaredField("mIsDoze").getType()!=boolean.class||svc.getDeclaredField("mStateMachine").getType()!=machine||!machine.getDeclaredMethod("getState").getReturnType().isEnum())throw new IllegalStateException("SensorService signatures");
            XC_MethodHook observe=new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){if(main()&&p.thisObject.getClass()==machine&&((Method)p.method).getName().equals("handleTpActionDownState")){pressed=true;event("restore before press");for(ImageView view:new ArrayList<>(states.keySet()))restore(view);}}
                @Override protected void afterHookedMethod(MethodHookParam p){if(!main())return;if(p.thisObject.getClass()==machine&&((Method)p.method).getName().equals("handleTpActionDownState"))pressed=false;if(p.hasThrowable())return;try{if(p.thisObject.getClass().getName().equals(SERVICE)){if(((Method)p.method).getName().equals("onDestroy")){end();SensorAnimationBackend.end();knownGraphics.clear();service.clear();return;}service=new WeakReference<>(p.thisObject);}
                    String method=((Method)p.method).getName();if((method.equals("setSensorViewAlpha")||method.equals("setAuthenticateViewAlpha"))&&p.args.length>0&&p.args[0] instanceof Number alpha&&alpha.floatValue()<=0)event("vendor alpha zero");
                    if(method.equals("setSensorViewVisible")&&p.args.length>0&&Boolean.FALSE.equals(p.args[0]))event("vendor view hidden");
                    if(method.equals("removeSensorView")||method.equals("removeAuthenticateSensorWindow"))event("vendor removes sensor window");refresh();}catch(Throwable failure){fail(failure);}}};
            for(Class<?> type:new Class<?>[]{svc,manager,owner,machine})for(Method method:type.getDeclaredMethods())if(Set.of("onCreate","onDestroy","setState","handleTpActionDownState","handleTpActionUpState","initSensorView","setSensorViewBackground","setSensorViewIconForRedMagic","updateSensorViewIcon","updateSensorViewIconForStyle","setSensorViewVisible","setSensorViewAlpha","setAuthenticateViewAlpha","showSensorViewFloatWindow","removeAuthenticateSensorWindow","removeSensorView","removeAllFloatWindow","startQuickApp","stopQuickApp","tpKeyEventActionDown","tpKeyEventActionUp","tpUp","setAodStart").contains(method.getName()))observers.add(XposedBridge.hookMethod(method,observe));
            observers.add(XposedBridge.hookMethod(View.class.getDeclaredMethod("onAttachedToWindow"),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(ready&&!p.hasThrowable()&&main()&&p.thisObject==image())refresh();}}));
            observers.add(XposedBridge.hookMethod(View.class.getDeclaredMethod("onDetachedFromWindow"),new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){if(main()&&p.thisObject instanceof ImageView view){if(states.containsKey(view))event("sensor view detached");restore(view);knownGraphics.remove(view);}}}));
            ready=true;message="SensorService observers ready";return true;
        }catch(Throwable unavailable){for(var hook:observers)try{hook.unhook();}catch(Throwable ignored){}message="SensorService source unavailable: "+unavailable.getClass().getSimpleName();return false;}
    }
    private static Object owner(){Object svc=service.get();if(svc==null)return null;Object manager=XposedHelpers.getObjectField(svc,"mFloatWindowManager");return manager==null?null:XposedHelpers.getObjectField(manager,"floatWindowAuthenticateSensorView");}
    private static ImageView image(){Object owner=owner();if(owner==null)return null;Object image=XposedHelpers.getObjectField(owner,"mSensorView");return image instanceof ImageView view?view:null;}
    private static boolean eligible(){try{
        Object svc=service.get();if(pressed){gate="press";return false;}if(svc==null){gate="service absent";return false;}
        if(Boolean.TRUE.equals(XposedHelpers.getObjectField(svc,"isFingerprintDisabled"))){gate="vendor disabled";return false;}
        if(Boolean.TRUE.equals(XposedHelpers.getObjectField(svc,"mIsDoze"))){gate="doze";return false;}
        Object power=XposedHelpers.getObjectField(svc,"mPowerManager"),keyguard=XposedHelpers.getObjectField(svc,"mKeyguardManager"),manager=XposedHelpers.getObjectField(svc,"mFloatWindowManager"),machine=XposedHelpers.getObjectField(svc,"mStateMachine"),owner=owner();
        if(power==null||keyguard==null||manager==null||machine==null||owner==null){gate="owner unavailable";return false;}
        if(!Boolean.TRUE.equals(XposedHelpers.callMethod(power,"isInteractive"))){gate="screen off";return false;}
        if(!Boolean.TRUE.equals(XposedHelpers.callMethod(keyguard,"getKeyguardShowing"))){gate="keyguard not showing";return false;}
        if(!Boolean.TRUE.equals(XposedHelpers.callMethod(manager,"showFromSystemUI"))){gate="app authentication";return false;}
        if(Boolean.TRUE.equals(XposedHelpers.getObjectField(owner,"isQuickShow"))){gate="quick app";return false;}
        Object state=XposedHelpers.callMethod(machine,"getState");if(!(state instanceof Enum<?> value)){gate="state unavailable";return false;}
        gate=value.name().equals("INITIALIZED")?"idle":"state "+value.name();return gate.equals("idle");
    }catch(Throwable unavailable){gate="state read unavailable";return false;}}
    static View animationTarget(){if(!main())return null;try{
        Object svc=service.get();if(svc==null||Boolean.TRUE.equals(XposedHelpers.getObjectField(svc,"isFingerprintDisabled"))||Boolean.TRUE.equals(XposedHelpers.getObjectField(svc,"mIsDoze")))return null;
        Object power=XposedHelpers.getObjectField(svc,"mPowerManager"),keyguard=XposedHelpers.getObjectField(svc,"mKeyguardManager"),manager=XposedHelpers.getObjectField(svc,"mFloatWindowManager"),machine=XposedHelpers.getObjectField(svc,"mStateMachine"),owner=owner();
        if(power==null||keyguard==null||manager==null||machine==null||owner==null||!Boolean.TRUE.equals(XposedHelpers.callMethod(power,"isInteractive"))||!Boolean.TRUE.equals(XposedHelpers.callMethod(keyguard,"getKeyguardShowing"))||!Boolean.TRUE.equals(XposedHelpers.callMethod(manager,"showFromSystemUI"))||Boolean.TRUE.equals(XposedHelpers.getObjectField(owner,"isQuickShow")))return null;
        Object state=XposedHelpers.callMethod(machine,"getState");if(!(state instanceof Enum<?> value)||!(value.name().equals("INITIALIZED")||value.name().equals("AUTHENTICATE")))return null;
        Object animation=XposedHelpers.getObjectField(manager,"floatWindowAnimalView");if(animation==null)return null;Object view=XposedHelpers.getObjectField(animation,"authAnimal");return view instanceof View target?target:null;
    }catch(Throwable unavailable){return null;}}
    static boolean apply(int selection,boolean hide,long until){
        if(!main()||!com.elitedarkkaiser.nubialy.FingerprintStyles.valid(selection)||until<=SystemClock.elapsedRealtime()||until-SystemClock.elapsedRealtime()>ClockEnabledPolicy.HEALTH_MS)return false;
        if(failed||!ready)return false;
        if(active&&style==selection&&hidden==hide){deadline=until;refresh();return !failed;}
        if(!end())return false;style=selection;hidden=hide;deadline=until;captured=restored=0;
        if(style==0&&!hidden){message="Stock fingerprint graphic selected";return true;}
        XC_MethodHook callback=new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){if(!active||writing||failed)return;State state=states.get(p.thisObject);if(state!=null){state.depth++;p.setObjectExtra("nubialy:sensor",new Frame(generation,state));}}
            @Override protected void afterHookedMethod(MethodHookParam p){Object extra=p.getObjectExtra("nubialy:sensor");if(!(extra instanceof Frame frame)||frame.generation()!=generation||!active||writing||failed||states.get(p.thisObject)!=frame.state())return;try{if(p.hasThrowable()||!main())throw new IllegalStateException("Sensor background callback");ImageView view=(ImageView)p.thisObject;State state=frame.state();Drawable stockUpdate=view.getBackground();if(stockUpdate!=state.custom||stockUpdate==null)state.stock=stockUpdate;if(--state.depth>0)return;
                if(SystemClock.elapsedRealtime()>=deadline||!eligible()){event("restore: "+gate);restore(view);}else{
                    if(stockUpdate!=null&&stockUpdate!=state.custom)knownGraphics.put(view,true);else if(stockUpdate==null)event("vendor idle background cleared");paint(view,state);
                }}catch(Throwable failure){fail(failure);}}
        };
        try{activeHooks.add(XposedHelpers.findAndHookMethod(View.class,"setBackground",Drawable.class,callback));activeHooks.add(XposedHelpers.findAndHookMethod(View.class,"setBackgroundResource",int.class,callback));activeHooks.add(XposedHelpers.findAndHookMethod(View.class,"setBackgroundDrawable",Drawable.class,callback));active=true;if(!hidden)glyph();message="Fingerprint appearance armed";refresh();return !failed;}
        catch(Throwable unavailable){fail(unavailable);return false;}
    }
    private static void background(ImageView view,Drawable drawable){int l=view.getPaddingLeft(),t=view.getPaddingTop(),r=view.getPaddingRight(),b=view.getPaddingBottom();view.setBackground(drawable);view.setPadding(l,t,r,b);}
    private static void paint(ImageView view,State state){
        // A temporary null stock background is still retained for exact rollback.
        // Once this view has displayed a genuine idle graphic, keep the user's
        // selected replacement while eligible instead of interpreting null as Hide.
        if(!hidden&&!knownGraphics.containsKey(view))return;
        state.custom=hidden?null:glyph();if(view.getBackground()==state.custom)return;writing=true;try{background(view,state.custom);if(view.getBackground()!=state.custom)throw new IllegalStateException("Fingerprint background rejected");}finally{writing=false;}
    }
    private static Drawable glyph(){int color=0xffffffff;Object svc=service.get();if(svc!=null)color=((Number)XposedHelpers.getObjectField(svc,"mKeyguardColor")).intValue();if((color>>>24)==0)color=0xffffffff;if(preparedGlyph==null||preparedStyle!=style||preparedColor!=color){preparedGlyph=new FingerprintGlyphDrawable(style,color);preparedStyle=style;preparedColor=color;}return preparedGlyph;}
    static void refresh(){if(main())SensorAnimationBackend.refresh();if(!main()||!active||failed)return;try{
        if(SystemClock.elapsedRealtime()>=deadline){event("guard expired");end();message="Fingerprint guard expired; stock restored";return;}
        ImageView current=image();boolean allowed=eligible();
        for(ImageView view:new ArrayList<>(states.keySet()))if(view!=current||!view.isAttachedToWindow()||!allowed){event("restore: "+(!allowed?gate:view!=current?"view replaced":"detached"));if(!restore(view))return;if(view!=current||!view.isAttachedToWindow())knownGraphics.remove(view);}
        if(current==null||!current.isAttachedToWindow()||!allowed)return;
        State state=states.get(current);if(state==null){state=new State(current);states.put(current,state);captured++;if(state.stock!=null)knownGraphics.put(current,true);}paint(current,state);
    }catch(Throwable failure){fail(failure);}}
    private static void fail(Throwable failure){failed=true;hardFailed=true;message="Fingerprint appearance stopped: "+failure.getClass().getSimpleName();if(main())end();else new Handler(Looper.getMainLooper()).post(()->end());}
    private static boolean restore(ImageView view){State state=states.get(view);if(state==null)return true;writing=true;try{background(view,state.stock);if(view.getBackground()!=state.stock)throw new IllegalStateException();states.remove(view);restored++;return true;}catch(Throwable unavailable){failed=true;message="Fingerprint restoration incomplete; disable nubia-ly for SensorService in Vector and reboot";return false;}finally{writing=false;}}
    static boolean end(){generation++;active=false;for(ImageView view:new ArrayList<>(states.keySet()))restore(view);for(var hook:new ArrayList<>(activeHooks))try{hook.unhook();activeHooks.remove(hook);}catch(Throwable unavailable){failed=true;hardFailed=true;}if(!hardFailed&&states.isEmpty()&&activeHooks.isEmpty())failed=false;return !failed&&states.isEmpty();}
    static String status(){String visual="";try{ImageView current=image();if(current!=null){float alpha=1;boolean visible=true;View node=current;for(int depth=0;node!=null&&depth<6;depth++){alpha*=node.getAlpha();visible&=node.getVisibility()==View.VISIBLE;node=node.getParent() instanceof View parent?parent:null;}visual=" View: attached="+current.isAttachedToWindow()+", visible="+visible+", tree alpha="+alpha+", background="+(current.getBackground() instanceof FingerprintGlyphDrawable?"custom":current.getBackground()==null?"null":"stock")+".";}}catch(Throwable ignored){}return message+". Requested: "+(hidden?"Hidden":FingerprintGlyphDrawable.NAMES[style])+"; service observed: "+(service.get()!=null)+"; captured: "+captured+"; restored: "+restored+"; remaining: "+states.size()+"; gate: "+gate+"."+visual+" Recent: "+String.join(" → ",events)+".";}
}
