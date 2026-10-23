package com.elitedarkkaiser.nubialy.xposed;
import android.graphics.*;
import android.os.Looper;
import android.view.*;
import com.elitedarkkaiser.nubialy.SurfaceStyle.QsTheme;
import de.robv.android.xposed.*;
import java.util.*;
/** Draw-time header-only transforms. No native view properties or visibility are rewritten. */
final class QsHeaderAppearance {
 private static final Set<View> headers=Collections.newSetFromMap(new WeakHashMap<>());
 private static final ThreadLocal<Integer> drawing=ThreadLocal.withInitial(()->0);
 private static QsTheme theme=QsTheme.STOCK;private static boolean installed;private static int observed;
 static void install(){if(installed)return;XposedHelpers.findAndHookMethod(View.class,"draw",Canvas.class,new XC_MethodHook(){
  @Override protected void beforeHookedMethod(MethodHookParam p){
   if(!custom()||Looper.myLooper()!=Looper.getMainLooper()||!(p.thisObject instanceof View v)||!v.isAttachedToWindow())return;
   if(headers.contains(v)){drawing.set(drawing.get()+1);p.setObjectExtra("nubialy.qs.header.root",true);}
   if(drawing.get()==0||!ClockColorBackend.authorized())return;
   int role=role(v);if(role<0)return;int tint=role==0?theme.clockColor():role==1?theme.dateColor():theme.statusColor();int scale=role==0?theme.clockScale():role==1?theme.dateScale():theme.statusScale();if(tint==0&&scale==100)return;
   Canvas c=(Canvas)p.args[0];int save=c.save();try{float factor=scale/100f;c.scale(factor,factor,v.getWidth()/2f,v.getHeight()/2f);if(tint!=0){Paint paint=new Paint();paint.setColorFilter(new PorterDuffColorFilter(tint,PorterDuff.Mode.SRC_IN));c.saveLayer(0,0,v.getWidth(),v.getHeight(),paint);}p.setObjectExtra("nubialy.qs.header.canvas",save);observed++;}catch(Throwable failure){c.restoreToCount(save);}
  }
  @Override protected void afterHookedMethod(MethodHookParam p){Object save=p.getObjectExtra("nubialy.qs.header.canvas");if(save instanceof Integer n)((Canvas)p.args[0]).restoreToCount(n);if(Boolean.TRUE.equals(p.getObjectExtra("nubialy.qs.header.root")))drawing.set(Math.max(0,drawing.get()-1));}
 });installed=true;}
 private static boolean custom(){return theme.clockColor()!=0||theme.dateColor()!=0||theme.statusColor()!=0||theme.clockScale()!=100||theme.dateScale()!=100||theme.statusScale()!=100;}
 private static String id(View v){try{return v.getResources().getResourceEntryName(v.getId()).toLowerCase(java.util.Locale.ROOT);}catch(Exception missing){return "";}}
 private static int rawRole(View v){String id=id(v),name=v.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);if(v instanceof android.widget.TextView){if(id.contains("date")||name.contains("dateview"))return 1;if(id.contains("clock")||name.equals("clock"))return 0;}if(id.equals("statusicons")||id.equals("status_icons")||id.equals("system_icons")||id.equals("battery")||name.contains("statusiconcontainer")||name.contains("batterymeterview")||name.contains("signalclusterview"))return 2;return -1;}
 private static int role(View v){int role=rawRole(v);if(role<0)return -1;View current=v;for(int depth=0;depth<16;depth++){if(headers.contains(current))return role;if(!(current.getParent() instanceof View parent))return -1;current=parent;if(rawRole(current)>=0)return -1;}return -1;}
 static void observe(View container){try{Object header=XposedHelpers.getObjectField(container,"mHeader");if(header instanceof View v)headers.add(v);}catch(Throwable ignored){}}
 static void begin(QsTheme next){theme=next;observed=0;invalidate();}
 static void end(){theme=QsTheme.STOCK;invalidate();}
 private static void invalidate(){for(View v:new ArrayList<>(headers))if(v!=null)v.invalidate();}
 static String status(){return "QS header: "+headers.size()+" hosts; styled draws="+observed+".";}
}
