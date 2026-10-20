package com.elitedarkkaiser.nubialy.xposed;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import de.robv.android.xposed.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.*;
/** Appearance of inspected in-process images only; never change biometric service commands. */
final class FingerprintBackend {
    // Only verified cleanup may clear a restoration-only failure; apply/hook failures stay latched.
    private static boolean hardFailed;
    private static final String OWNER="com.zte.feature.fingerprintlcd.FloatWindowSensorView",FEATURE="com.zte.feature.fingerprintlcd.FingerprintUnderLcdFeature";
    private static final Map<Object,WeakReference<Object>> owners=new WeakHashMap<>();
    private static final Map<ImageView,State> states=new WeakHashMap<>();
    private static final List<XC_MethodHook.Unhook> hooks=new ArrayList<>();
    private static Field registry,managerInstance,managerFeature,managerOwner;
    private static String discovery="not checked";
    private static Field field(Class<?> type,String name)throws ReflectiveOperationException{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    private static boolean attempted,ready,active,writing,failed;private static int style,captured,restored;private static boolean hidden;private static long generation;
    private static final class State {final WeakReference<Object> owner;final boolean direct;Drawable stock,stockImage,custom;int updateDepth;State(Object owner,ImageView image){this.owner=new WeakReference<>(owner);direct=owner.getClass().getName().equals(FEATURE);stock=image.getBackground();stockImage=image.getDrawable();}}
    private record Frame(long generation,State state){}
    static void install(ClassLoader loader){if(attempted)return;attempted=true;List<XC_MethodHook.Unhook> pending=new ArrayList<>();try{
        Class<?> owner=XposedHelpers.findClass(OWNER,loader),feature=XposedHelpers.findClass(FEATURE,loader);
        if(owner.getSuperclass()!=View.class||owner.getDeclaredField("sensorView").getType()!=ImageView.class||owner.getDeclaredField("enrollState").getType()!=boolean.class||feature.getDeclaredMethod("isSensorUIKeyguard").getReturnType()!=boolean.class||feature.getDeclaredMethod("isSensorUIAODStatus").getReturnType()!=boolean.class||feature.getDeclaredMethod("getKeyguardColor").getReturnType()!=int.class)throw new IllegalStateException();
        if(feature.getDeclaredField("mFingerprintIcon").getType()!=ImageView.class)throw new IllegalStateException("Lock-screen fingerprint image signature");
        Class<?> base=XposedHelpers.findClass("com.zte.base.Feature",loader),manager=XposedHelpers.findClass("com.zte.feature.fingerprintlcd.FloatWindowManager",loader);
        registry=field(base,"sFeatures");managerInstance=field(manager,"floatWindowManager");managerFeature=field(manager,"mFingerprintUnderLcdFeature");managerOwner=field(manager,"floatWindowAuthenticateSensorView");
        if(!Map.class.isAssignableFrom(registry.getType())||managerInstance.getType()!=manager||managerFeature.getType()!=feature||managerOwner.getType()!=owner)throw new IllegalStateException("Fingerprint registry signature");
        XC_MethodHook directObserver=new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(p.hasThrowable()||!main())return;owners.put(p.thisObject,new WeakReference<>(p.thisObject));if(active)refresh();}};
        for(Method method:feature.getDeclaredMethods())if(Set.of("tryInflateFingerprintIcon","handleShowFingerprintIcon","refreshFillLightLayoutFingerColor","setFingerprintIconVisibility").contains(method.getName()))pending.add(XposedBridge.hookMethod(method,directObserver));
        XC_MethodHook observer=new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(p.hasThrowable()||!main())return;try{View root=(View)p.thisObject;for(Object arg:p.args)if(arg!=null&&arg.getClass().getName().equals(FEATURE))owners.put(root,new WeakReference<>(arg));if(active)refresh();}catch(Throwable failure){if(active)failed=true;hardFailed=true;}}};
        for(Method m:owner.getDeclaredMethods())if(Set.of("initSensorView","setSensorViewByStatus","setSensorViewIconForRedmagic","setSensorViewVisible","setEnrollState").contains(m.getName()))pending.add(XposedBridge.hookMethod(m,observer));
        // Sensor image is in a separate window. Its owner is not an ancestor.
        pending.add(XposedBridge.hookMethod(View.class.getDeclaredMethod("onDetachedFromWindow"),new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){if(main()&&p.thisObject instanceof ImageView image)restore(image);}}));
        ready=true;
    }catch(Throwable failure){for(var h:pending)try{h.unhook();}catch(Throwable ignored){}ready=false;}}
    private static boolean main(){return Looper.myLooper()==Looper.getMainLooper();}
    private static ImageView image(Object owner){Object value=XposedHelpers.getObjectField(owner,owner.getClass().getName().equals(FEATURE)?"mFingerprintIcon":"sensorView");return value instanceof ImageView view?view:null;}
    private static boolean eligible(Object owner){
        WeakReference<Object> ref=owners.get(owner);Object feature=ref==null?null:ref.get();
        // Modern firmware delegates drawing to a separate SensorService process.
        // Its graphics cannot safely be replaced through legacy System UI images.
        if(feature!=null&&XposedHelpers.getObjectField(feature,"mSensorUIService")!=null)return false;
        if(feature==null||Boolean.TRUE.equals(XposedHelpers.callMethod(feature,"isSensorUIAODStatus")))return false;
        if(owner.getClass().getName().equals(FEATURE)){
            Object bar=XposedHelpers.getObjectField(feature,"mStatusBar");if(bar==null||!Boolean.TRUE.equals(XposedHelpers.callMethod(bar,"isKeyguardShowing")))return false;
            Object monitor=XposedHelpers.callMethod(feature,"getKeyguardUpdateMonitor");return monitor!=null&&Boolean.TRUE.equals(XposedHelpers.callMethod(monitor,"isDeviceInteractive"));
        }
        return Boolean.FALSE.equals(XposedHelpers.getObjectField(owner,"enrollState"))&&Boolean.TRUE.equals(XposedHelpers.callMethod(feature,"isSensorUIKeyguard"));
    }
    static void begin(int selection,boolean hide){if(!com.elitedarkkaiser.nubialy.FingerprintStyles.valid(selection)||!main())throw new IllegalArgumentException();if(!end())throw new IllegalStateException("Fingerprint restoration incomplete");style=selection;hidden=hide;captured=restored=0;if(style==0&&!hidden)return;if(!ready||failed)throw new IllegalStateException("Fingerprint source unavailable");
        XC_MethodHook background=new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){if(!active||writing)return;State state=states.get(p.thisObject);if(state!=null){state.updateDepth++;p.setObjectExtra("nubia-ly:fingerprint",new Frame(generation,state));}}
            @Override protected void afterHookedMethod(MethodHookParam p){Object extra=p.getObjectExtra("nubia-ly:fingerprint");if(!(extra instanceof Frame frame)||!active||writing||frame.generation()!=generation||states.get(p.thisObject)!=frame.state())return;try{if(p.hasThrowable()||!main())throw new IllegalStateException();ImageView image=(ImageView)p.thisObject;State state=frame.state();String method=((Method)p.method).getName();if(method.equals("setBackground"))state.stock=image.getBackground();else state.stockImage=image.getDrawable();if(--state.updateDepth>0)return;Object owner=frame.state().owner.get();if(owner==null||!eligible(owner))restore(image);else paint(image,frame.state());}catch(Throwable failure){failed=true;hardFailed=true;}}
        };
        try{hooks.add(XposedHelpers.findAndHookMethod(View.class,"setBackground",Drawable.class,background));hooks.add(XposedHelpers.findAndHookMethod(ImageView.class,"setImageDrawable",Drawable.class,background));hooks.add(XposedHelpers.findAndHookMethod(ImageView.class,"setImageResource",int.class,background));active=true;refresh();if(failed)throw new IllegalStateException();}catch(Throwable failure){end();throw new IllegalStateException("Fingerprint appearance unavailable",failure);}}
    private static void background(ImageView image,Drawable value){int l=image.getPaddingLeft(),t=image.getPaddingTop(),r=image.getPaddingRight(),b=image.getPaddingBottom();image.setBackground(value);image.setPadding(l,t,r,b);}
    private static void paint(ImageView image,State state){writing=true;try{Drawable selected=hidden?null:state.direct?state.stock:state.custom;background(image,selected);if(state.direct){image.setImageDrawable(hidden?null:style==0?state.stockImage:state.custom);if(image.getDrawable()!=(hidden?null:style==0?state.stockImage:state.custom))throw new IllegalStateException("Fingerprint image apply incomplete");}if(image.getBackground()!=selected)throw new IllegalStateException("Fingerprint background apply incomplete");}finally{writing=false;}}
    // Read existing vendor objects. Never call the manager factory or create a sensor window.
    private static void discover()throws IllegalAccessException{
        Object existing=registry.get(null);int features=0;boolean external=false;
        if(existing instanceof Map<?,?> values)for(Object feature:new ArrayList<>(values.values()))if(feature!=null&&feature.getClass().getName().equals(FEATURE)){owners.put(feature,new WeakReference<>(feature));features++;if(XposedHelpers.getObjectField(feature,"mSensorUIService")!=null)external=true;}
        Object manager=managerInstance.get(null);boolean floating=false;
        if(manager!=null){Object feature=managerFeature.get(manager),owner=managerOwner.get(manager);if(feature!=null&&feature.getClass().getName().equals(FEATURE)){owners.put(feature,new WeakReference<>(feature));if(owner!=null&&owner.getClass().getName().equals(OWNER)){owners.put(owner,new WeakReference<>(feature));floating=true;}}}
        discovery="registry features: "+features+"; floating window found: "+floating+(external?"; external SensorService connected: use Check fingerprint service for renderer status":"");
    }
    static void refresh(){if(!active||failed||!main())return;try{
        discover();
        for(ImageView image:new ArrayList<>(states.keySet())){Object owner=states.get(image).owner.get();if(owner==null||!image.isAttachedToWindow()||!eligible(owner)||image(owner)!=image)restore(image);}
        for(Object owner:new ArrayList<>(owners.keySet())){ImageView image=image(owner);if(image==null||!image.isAttachedToWindow()||!eligible(owner))continue;State state=states.get(image);if(state!=null&&state.owner.get()!=owner)throw new IllegalStateException("Shared sensor image");if(state==null){state=new State(owner,image);states.put(image,state);captured++;}Object feature=owners.get(owner).get();int color=((Number)XposedHelpers.callMethod(feature,"getKeyguardColor")).intValue();state.custom=style==0?state.stock:new FingerprintGlyphDrawable(style,color);paint(image,state);}
    }catch(Throwable failure){failed=true;hardFailed=true;}}
    private static boolean restore(ImageView image){State state=states.get(image);if(state==null)return true;writing=true;try{background(image,state.stock);if(state.direct)image.setImageDrawable(state.stockImage);if(image.getBackground()!=state.stock||(state.direct&&image.getDrawable()!=state.stockImage))throw new IllegalStateException();states.remove(image);restored++;return true;}catch(Throwable failure){failed=true;return false;}finally{writing=false;}}
    static boolean end(){generation++;active=false;for(ImageView image:new ArrayList<>(states.keySet()))restore(image);for(var hook:new ArrayList<>(hooks))try{hook.unhook();hooks.remove(hook);}catch(Throwable failure){failed=true;hardFailed=true;}if(!hardFailed&&states.isEmpty()&&hooks.isEmpty())failed=false;return !failed&&states.isEmpty();}
    static boolean healthy(){return !failed;}
    static String status(){return "Fingerprint: "+(hidden?"Hidden":FingerprintGlyphDrawable.NAMES[style])+"; "+discovery+"; observed owners: "+owners.size()+"; captured: "+captured+"; restored: "+restored+"; remaining: "+states.size()+(failed?"; restoration required.":".");}
}
