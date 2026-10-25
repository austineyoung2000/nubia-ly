package com.elitedarkkaiser.nubialy.xposed;
import android.graphics.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import com.elitedarkkaiser.nubialy.BatteryRenderer;
import com.elitedarkkaiser.nubialy.BatteryStyles;
import de.robv.android.xposed.*;
import java.util.*;
/** Current vendor battery container only; draw substitution never writes stock battery or view state. */
final class BatteryStyleBackend {
 private static final String OWNER="com.zte.mifavor.views.MFVBatteryViewLayout",ICON="com.zte.mifavor.views.MFVBatteryMeterView",TYPE="com.zte.mifavor.views.MFVViewType",ROOT="com.android.systemui.statusbar.phone.PhoneStatusBarView";
 private static final Map<ViewGroup,Boolean> owners=new WeakHashMap<>();private static XC_MethodHook.Unhook hook;private static boolean ready,active,failed;private static int batteryColor,textColor,hideCharge,style,draws,fallbacks;private static long generation;
 private record State(ViewGroup owner,FrameLayout container,ImageView icon,int level,int foreground,int frame,boolean charging,boolean saver,boolean percent){}
 private static boolean main(){return Looper.myLooper()==Looper.getMainLooper();}
 static void install(ClassLoader loader){try{Class<?> owner=XposedHelpers.findClass(OWNER,loader),icon=XposedHelpers.findClass(ICON,loader),type=XposedHelpers.findClass(TYPE,loader);if(owner.getSuperclass()!=RelativeLayout.class||icon.getSuperclass()!=ImageView.class||owner.getDeclaredField("mBatteryContainer").getType()!=FrameLayout.class||owner.getDeclaredField("mBatteryIconView").getType()!=icon||icon.getDeclaredField("mType").getType()!=type||type.getDeclaredMethod("isStatusBar").getReturnType()!=boolean.class)throw new IllegalStateException("Battery ownership");
  for(String name:new String[]{"mBateryLevel","mContentColor","mStatusBarColor","mContentResId","mOemChargeType","mChargingSeparation"})if(icon.getDeclaredField(name).getType()!=int.class)throw new IllegalStateException("Battery state");for(String name:new String[]{"mIsCharing","mIsPowerSave"})if(icon.getDeclaredField(name).getType()!=boolean.class)throw new IllegalStateException("Battery flag");if(icon.getDeclaredField("ZTE_BATTARY_CONTENT_RES_IDS").getType()!=int[][].class)throw new IllegalStateException("Battery artwork");
  XC_MethodHook observe=new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(main()&&!p.hasThrowable()&&p.thisObject instanceof ViewGroup view){owners.put(view,true);if(active)view.invalidate();}}};
  for(var method:owner.getDeclaredMethods())if(Set.of("onFinishInflate","onAttachedToWindow","updateBatteryLayout","onBatteryLevelChanged","onPowerSaveChanged").contains(method.getName()))XposedBridge.hookMethod(method,observe);
  ready=true;
 }catch(Throwable unavailable){ready=false;}}
 private static boolean eligible(ViewGroup owner){if(!owner.getClass().getName().equals(OWNER)||!owner.isAttachedToWindow())return false;View current=owner;for(int depth=0;depth<16&&current!=null;depth++){if(current.getClass().getName().equals(ROOT)||QsHeaderScope.isRoot(current))return true;current=current.getParent() instanceof View v?v:null;}return false;}
 private static State state(ViewGroup owner){if(!eligible(owner))return null;Object raw=XposedHelpers.getObjectField(owner,"mBatteryIconView"),box=XposedHelpers.getObjectField(owner,"mBatteryContainer");if(!(raw instanceof ImageView icon)||!icon.getClass().getName().equals(ICON)||!(box instanceof FrameLayout container)||container.getParent()!=owner||icon.getParent()!=container||icon.getVisibility()!=View.VISIBLE)return null;
  Object type=XposedHelpers.getObjectField(icon,"mType");if(type==null||(!Boolean.TRUE.equals(XposedHelpers.callMethod(type,"isStatusBar"))&&!QsHeaderScope.contains(owner)))return null;int level=((Number)XposedHelpers.getObjectField(icon,"mBateryLevel")).intValue();if(level<0||level>100||((Number)XposedHelpers.getObjectField(icon,"mChargingSeparation")).intValue()!=0||((Number)XposedHelpers.getObjectField(icon,"mOemChargeType")).intValue()==128)return null;
  int id=((Number)XposedHelpers.getObjectField(icon,"mContentResId")).intValue();Object ids=XposedHelpers.getObjectField(icon,"ZTE_BATTARY_CONTENT_RES_IDS");boolean known=false;if(ids instanceof int[][] arrays)for(int[] row:arrays)if(row!=null)for(int value:row)if(value!=0&&value==id)known=true;if(!known)return null;
  boolean percent=visibleField(owner,"mBatteryLevelInsideView",container),charging=hideCharge==0&&Boolean.TRUE.equals(XposedHelpers.getObjectField(icon,"mIsCharing"))&&(style==BatteryStyles.TEXT||visibleField(owner,"mInsideChargeView",container)),saver=Boolean.TRUE.equals(XposedHelpers.getObjectField(icon,"mIsPowerSave"));int foreground=level<=20?0xe5ff0000:((Number)XposedHelpers.getObjectField(icon,"mContentColor")).intValue(),frame=((Number)XposedHelpers.getObjectField(icon,"mStatusBarColor")).intValue();return new State(owner,container,icon,level,foreground,frame,charging,saver,percent);
 }
 private static boolean visibleField(ViewGroup owner,String name,ViewGroup parent){return XposedHelpers.getObjectField(owner,name) instanceof View v&&v.getParent()==parent&&v.getVisibility()==View.VISIBLE;}
 static void begin(int value){begin(value,0);}
 static void begin(int value,int charge){begin(value,charge,0,0);}
 static void begin(int value,int charge,int fill,int text){batteryColor=fill;textColor=text;hideCharge=charge;if(!BatteryStyles.valid(value)||!main())throw new IllegalArgumentException();if(!end())throw new IllegalStateException("Battery cleanup incomplete");style=value;draws=fallbacks=0;if(style==0)return;if(!ready||failed)throw new IllegalStateException("Battery renderer unavailable");final long ticket=++generation;
  try{hook=XposedBridge.hookMethod(ViewGroup.class.getDeclaredMethod("dispatchDraw",Canvas.class),new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){if(!active||failed||!main()||!(p.thisObject instanceof ViewGroup view))return;try{ViewGroup owner;if(view.getClass().getName().equals(OWNER)){owner=view;if(!eligible(owner))return;owners.put(owner,true);if(style==BatteryStyles.HIDDEN){draws++;p.setResult(null);return;}if(style!=BatteryStyles.TEXT)return;}else if(view.getClass()==FrameLayout.class&&view.getParent() instanceof ViewGroup parent&&parent.getClass().getName().equals(OWNER)){owner=parent;if(style==BatteryStyles.TEXT||style==BatteryStyles.HIDDEN)return;}else return;
   State state=state(owner);if(state==null||view!=owner&&state.container()!=view){fallbacks++;return;}owners.put(owner,true);ImageView icon=state.icon();Canvas canvas=(Canvas)p.args[0];boolean text=view==owner;float alpha=text?1:icon.getAlpha();if(!Float.isFinite(alpha)||alpha<0||alpha>1)return;int save=canvas.saveLayerAlpha(0,0,view.getWidth(),view.getHeight(),Math.round(alpha*255));
   try{RectF bounds;if(text){float top=state.container().getTop()+icon.getTop();bounds=new RectF(0,top,owner.getWidth(),top+icon.getHeight());}else{canvas.translate(icon.getLeft(),icon.getTop());canvas.concat(icon.getMatrix());bounds=new RectF(0,0,icon.getWidth(),icon.getHeight());}
    int headerColor=QsHeaderAppearance.statusTint(icon),fillColor=batteryColor==0?headerColor:batteryColor,labelColor=QsHeaderAppearance.batteryTextTint(icon)!=0?QsHeaderAppearance.batteryTextTint(icon):textColor==0?headerColor:textColor;
    if(BatteryRenderer.draw(style,canvas,bounds,state.level(),state.charging(),state.saver(),state.percent(),fillColor==0?state.foreground():fillColor,fillColor==0?state.frame():fillColor,labelColor,()->active&&!failed&&generation==ticket&&eligible(owner)&&XposedHelpers.getObjectField(owner,"mBatteryIconView")==icon&&XposedHelpers.getObjectField(owner,"mBatteryContainer")==state.container())){draws++;p.setResult(null);}
   }finally{canvas.restoreToCount(save);}
  }catch(Throwable unavailable){failed=true;invalidate();}}});active=true;invalidate();}catch(Throwable unavailable){end();throw new IllegalStateException("Battery styling unavailable",unavailable);}
 }
 private static void invalidate(){for(ViewGroup owner:new ArrayList<>(owners.keySet()))try{owner.invalidate();if(XposedHelpers.getObjectField(owner,"mBatteryContainer") instanceof View view)view.invalidate();}catch(Throwable unavailable){failed=true;}}
 static boolean end(){generation++;active=false;invalidate();if(hook!=null)try{hook.unhook();hook=null;}catch(Throwable unavailable){failed=true;}return hook==null;}
 static boolean healthy(){return !failed;}
 static String status(){return "Battery style: "+BatteryStyles.NAMES[style]+"; owners: "+owners.size()+"; draws: "+draws+"; stock fallbacks: "+fallbacks+(ready?".":"; source unavailable.");}
}
