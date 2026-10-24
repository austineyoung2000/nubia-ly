package com.elitedarkkaiser.nubialy.xposed;
import android.graphics.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import com.elitedarkkaiser.nubialy.SurfaceStyle.Palette;
import de.robv.android.xposed.*;
import java.util.*;
/** Content-only tint, including replacement artwork. Notification branches never qualify. */
final class IndicatorColorBackend {
 private static Palette palette=Palette.STOCK;private static boolean active;
 private static final List<XC_MethodHook.Unhook> hooks=new ArrayList<>();private static final Set<View> seen=Collections.newSetFromMap(new WeakHashMap<>());private static int draws;
 private static java.lang.reflect.Method notificationMethod;
 private static boolean notification(View v)throws ReflectiveOperationException{if(notificationMethod==null||notificationMethod.getDeclaringClass()!=v.getClass())notificationMethod=v.getClass().getMethod("isNotification");return Boolean.TRUE.equals(notificationMethod.invoke(v));}
 private static String id(View v){try{return v.getResources().getResourceEntryName(v.getId()).toLowerCase(Locale.ROOT);}catch(Exception absent){return "";}}
 private static boolean scoped(View v){for(int n=0;n<24&&v!=null;n++){String name=v.getClass().getName();if(name.equals("com.android.systemui.statusbar.phone.NotificationIconContainer"))return false;if(name.equals("com.android.systemui.statusbar.phone.PhoneStatusBarView")||QsHeaderScope.isRoot(v))return true;v=v.getParent() instanceof View parent?parent:null;}return false;}
 private static int raw(View v){String name=v.getClass().getSimpleName().toLowerCase(Locale.ROOT),id=id(v);
  if(v instanceof ViewGroup){if(name.contains("batterymeterview")||name.equals("mfvbatteryviewlayout"))return 2;if(name.contains("statusbarwifiview"))return 0;if(name.contains("statusbarmobileview"))return 1;}
  if(v instanceof ImageView){if(id.equals("wifi_signal"))return 0;if(id.equals("mobile_signal"))return 1;if(v.getClass().getName().equals("com.android.systemui.statusbar.StatusBarIconView")){try{if(notification(v))return -1;}catch(Throwable absent){return -1;}return 3;}if(Set.of("cooling_fan_view","liquid_cooling_view","refresh_rate_view").contains(id))return 3;}return -1;
 }
 static int role(View v){if(!scoped(v))return -1;int own=raw(v);if(own<0)return -1;View current=v;for(int i=0;i<24&&current.getParent() instanceof View parent;i++){if(raw(parent)>=0)return -1;if(parent.getClass().getName().equals("com.android.systemui.statusbar.phone.PhoneStatusBarView")||QsHeaderScope.isRoot(parent))break;current=parent;}return own;}
 static int layer(View v,Canvas c,Palette colors){int role=role(v);if(role<0)return -1;int color=colors.get(role);if(color==0)return -1;Paint tint=new Paint();tint.setColorFilter(new PorterDuffColorFilter(color,PorterDuff.Mode.SRC_IN));return c.saveLayer(0,0,v.getWidth(),v.getHeight(),tint);}
 static void begin(Palette value){if(value==null||!value.valid())throw new IllegalArgumentException();if(!end())throw new IllegalStateException("Indicator tint cleanup pending");palette=value;draws=0;if(!value.statusAny())return;
  XC_MethodHook callback=new XC_MethodHook(1000){@Override protected void beforeHookedMethod(MethodHookParam p){if(!active||Looper.myLooper()!=Looper.getMainLooper()||!(p.thisObject instanceof View v)||!v.isAttachedToWindow()||role(v)<0||!ClockColorBackend.authorized())return;try{int save=layer(v,(Canvas)p.args[0],palette);if(save>=0){seen.add(v);p.setObjectExtra("nubialy.indicator.tint",save);draws++;}}catch(Throwable ignored){}}
   @Override protected void afterHookedMethod(MethodHookParam p){Object save=p.getObjectExtra("nubialy.indicator.tint");if(save instanceof Integer n)((Canvas)p.args[0]).restoreToCount(n);}};
  try{hooks.add(XposedHelpers.findAndHookMethod(ImageView.class,"onDraw",Canvas.class,callback));hooks.add(XposedHelpers.findAndHookMethod(ViewGroup.class,"dispatchDraw",Canvas.class,callback));active=true;StatusBarBackend.invalidateHeadersAndBars();}catch(Throwable failure){end();throw new IllegalStateException("Indicator tint unavailable",failure);}
 }
 static boolean end(){active=false;for(var hook:new ArrayList<>(hooks))try{hook.unhook();hooks.remove(hook);}catch(Throwable ignored){}for(View v:new ArrayList<>(seen))try{v.invalidate();}catch(Throwable ignored){}return hooks.isEmpty();}
 static boolean healthy(){return active||hooks.isEmpty();}
 static String status(){return "Indicator colors: "+(active?"enabled":"native")+"; draws="+draws+".";}
}
