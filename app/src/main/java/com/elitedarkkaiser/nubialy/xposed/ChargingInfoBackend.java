package com.elitedarkkaiser.nubialy.xposed;
import android.content.*;
import android.os.*;
import android.text.method.TransformationMethod;
import android.widget.TextView;
import com.elitedarkkaiser.nubialy.ChargingReadout;
import de.robv.android.xposed.*;
import java.util.*;
/** Adds measured battery data only to a native charging indication. No polling or sysfs writes. */
final class ChargingInfoBackend {
    // Only verified cleanup may clear a restoration-only failure; apply/hook failures stay latched.
    private static boolean hardFailed;
 private static final String OWNER="com.android.systemui.statusbar.KeyguardIndicationController",TEXT="com.android.systemui.statusbar.phone.KeyguardIndicationTextView";
 private record TextState(int maxLines,TransformationMethod transformation){}
 private static final Map<Object,Boolean> controllers=new WeakHashMap<>();private static final Map<TextView,TextState> changed=new WeakHashMap<>();private static final Map<TextView,String> appended=new WeakHashMap<>();private static final List<XC_MethodHook.Unhook> hooks=new ArrayList<>();private static Class<?> owner;private static boolean ready,active,failed,writing;
 private static boolean main(){return Looper.myLooper()==Looper.getMainLooper();}
 static void install(ClassLoader loader){try{owner=XposedHelpers.findClass(OWNER,loader);if(owner.getDeclaredMethod("computePowerIndication").getReturnType()!=String.class||owner.getDeclaredField("mContext").getType()!=Context.class||owner.getDeclaredField("mDozing").getType()!=boolean.class||owner.getDeclaredField("mPowerPluggedIn").getType()!=boolean.class||!owner.getDeclaredField("mLockScreenIndicationView").getType().getName().equals(TEXT))throw new IllegalStateException();owner.getDeclaredMethod("updateIndicationPublic",boolean.class);
  XposedBridge.hookAllConstructors(owner,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(main()&&!p.hasThrowable())controllers.put(p.thisObject,true);}});ready=true;
 }catch(Throwable unavailable){ready=false;}}
 static void begin(int value){if(value<0||value>1||!main())throw new IllegalArgumentException();if(!end())throw new IllegalStateException();if(value==0)return;if(!ready||failed)throw new IllegalStateException("Charging indication unavailable");try{
  hooks.add(XposedBridge.hookMethod(owner.getDeclaredMethod("computePowerIndication"),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!active||!main()||p.hasThrowable()||!(p.getResult() instanceof String stock)||stock.isBlank())return;try{Object controller=p.thisObject;controllers.put(controller,true);if(!Boolean.FALSE.equals(XposedHelpers.getObjectField(controller,"mDozing"))||!Boolean.TRUE.equals(XposedHelpers.getObjectField(controller,"mPowerPluggedIn")))return;Context c=(Context)XposedHelpers.getObjectField(controller,"mContext");Intent battery=c.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(battery==null||battery.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)==0)return;BatteryManager manager=c.getSystemService(BatteryManager.class);int current=manager==null?Integer.MIN_VALUE:manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);String diagnostic=ChargingReadout.format(battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0),current,battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,Integer.MIN_VALUE));String text=stock+"\n"+diagnostic;for(String field:new String[]{"mLockScreenIndicationView","mTopIndicationView"})if(XposedHelpers.getObjectField(controller,field) instanceof TextView view)appended.put(view,text);p.setResult(text);}catch(Throwable unavailable){failed=true;hardFailed=true;}}}));
  hooks.add(XposedHelpers.findAndHookMethod(TextView.class,"setText",CharSequence.class,TextView.BufferType.class,new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){if(!active||writing||!main()||!(p.thisObject instanceof TextView v)||!v.getClass().getName().equals(TEXT))return;String expected=appended.get(v);if(expected!=null&&expected.contentEquals(p.args[0] instanceof CharSequence text?text:"")){changed.computeIfAbsent(v,key->new TextState(key.getMaxLines(),key.getTransformationMethod()));writing=true;try{v.setTransformationMethod(null);v.setMaxLines(Math.max(2,v.getMaxLines()));}finally{writing=false;}}else restore(v);}}));active=true;refresh();
 }catch(Throwable unavailable){end();throw new IllegalStateException("Charging diagnostics unavailable",unavailable);}}
 private static void restore(TextView v){TextState state=changed.get(v);if(state==null)return;writing=true;try{v.setMaxLines(state.maxLines());v.setTransformationMethod(state.transformation());if(v.getMaxLines()!=state.maxLines()||v.getTransformationMethod()!=state.transformation())throw new IllegalStateException("Charging text restoration rejected");changed.remove(v);}catch(Throwable unavailable){failed=true;}finally{writing=false;}}
 private static void refresh(){for(Object c:new ArrayList<>(controllers.keySet()))try{XposedHelpers.callMethod(c,"updateIndicationPublic",false);}catch(Throwable unavailable){failed=true;hardFailed=true;}}
 static boolean end(){boolean wasActive=active;active=false;for(var h:new ArrayList<>(hooks))try{h.unhook();hooks.remove(h);}catch(Throwable unavailable){failed=true;hardFailed=true;}for(TextView v:new ArrayList<>(changed.keySet()))restore(v);appended.clear();if(wasActive)refresh();if(!hardFailed&&changed.isEmpty()&&hooks.isEmpty())failed=false;return !failed&&changed.isEmpty();}
 static boolean healthy(){return !failed;}
 static String status(){return "Charging diagnostics: "+(active?"enabled":"stock")+(ready?".":"; source unavailable.");}
}
