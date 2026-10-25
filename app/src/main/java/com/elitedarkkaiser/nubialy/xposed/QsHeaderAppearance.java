package com.elitedarkkaiser.nubialy.xposed;
import android.graphics.*;
import android.os.Looper;
import android.view.*;
import android.widget.TextView;
import android.widget.ImageView;
import com.elitedarkkaiser.nubialy.SurfaceStyle.QsTheme;
import com.elitedarkkaiser.nubialy.QsClockRenderer;
import de.robv.android.xposed.*;
import java.util.*;
/** Hooks content draw entry points used by hardware rendering; ancestry establishes scope. */
final class QsHeaderAppearance {
 private static final Set<View> headers=QsHeaderScope.headers;
 private static QsTheme theme=QsTheme.STOCK;private static boolean installed;private static int observed,textDraws,statusDraws;private static String targets="";
 static void install(){if(installed)return;
  XC_MethodHook callback=new XC_MethodHook(2000){
   @Override protected void beforeHookedMethod(MethodHookParam p){
    if(!custom()||Looper.myLooper()!=Looper.getMainLooper()||!(p.thisObject instanceof View v)||!v.isAttachedToWindow()||!ClockColorBackend.authorized())return;
    Content frame=content(v,(Canvas)p.args[0],theme);if(frame==null)return;p.setObjectExtra("nubialy.qs.header.canvas",frame.save());if(frame.replace())p.setResult(null);
   }
   @Override protected void afterHookedMethod(MethodHookParam p){Object save=p.getObjectExtra("nubialy.qs.header.canvas");if(save instanceof Integer n)((Canvas)p.args[0]).restoreToCount(n);}
  };
  XposedHelpers.findAndHookMethod(ImageView.class,"onDraw",Canvas.class,callback);
  XposedHelpers.findAndHookMethod(TextView.class,"onDraw",Canvas.class,callback);
  XposedHelpers.findAndHookMethod(ViewGroup.class,"dispatchDraw",Canvas.class,callback);installed=true;
 }
 record Content(int save,boolean replace){}
 static Content content(View v,Canvas c,QsTheme theme){
  if((v instanceof ImageView||v instanceof TextView)&&statusContent(v)){int tint=IndicatorColorBackend.headerTint(v,theme.statusColor());if(tint==0)return null;int save=c.save();Paint paint=new Paint();paint.setColorFilter(new PorterDuffColorFilter(tint,PorterDuff.Mode.SRC_IN));c.saveLayer(0,0,v.getWidth(),v.getHeight(),paint);statusDraws++;return new Content(save,false);}
  int role=role(v);if(role<0||role<2&&!(v instanceof TextView)||role==2&&!(v instanceof ViewGroup))return null;
  int save=c.save();boolean replace=false;
  try{int tint=role==0?theme.clockColor():role==1?theme.dateColor():theme.statusColor();int scale=role==0?theme.clockScale():role==1?theme.dateScale():theme.statusScale();float factor=scale/100f;c.scale(factor,factor,v.getWidth()/2f,v.getHeight()/2f);
   if(role==0&&(theme.clockFont()!=0||theme.clockStyle()!=0)){TextView clock=(TextView)v;QsClockRenderer.draw(c,new RectF(clock.getPaddingLeft(),clock.getPaddingTop(),clock.getWidth()-clock.getPaddingRight(),clock.getHeight()-clock.getPaddingBottom()),clock.getText(),clock.getPaint(),theme.clockFont(),theme.clockStyle(),tint==0?clock.getCurrentTextColor():tint);replace=true;}
   else if(role!=2&&tint!=0){Paint paint=new Paint();paint.setColorFilter(new PorterDuffColorFilter(tint,PorterDuff.Mode.SRC_IN));c.saveLayer(0,0,v.getWidth(),v.getHeight(),paint);}
   observed++;if(role==2)statusDraws++;else textDraws++;
   return new Content(save,replace);
  }catch(Throwable failure){c.restoreToCount(save);return null;}
 }
 private static boolean custom(){return theme.clockColor()!=0||theme.dateColor()!=0||theme.statusColor()!=0||theme.clockScale()!=100||theme.dateScale()!=100||theme.statusScale()!=100||theme.clockFont()!=0||theme.clockStyle()!=0;}
 private static String id(View v){try{return v.getResources().getResourceEntryName(v.getId()).toLowerCase(java.util.Locale.ROOT);}catch(Exception missing){return "";}}
 private static int rawRole(View v){String id=id(v),name=v.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);if(v instanceof TextView){if(id.contains("date")||name.contains("dateview"))return 1;if(id.contains("clock")||id.contains("time")||name.contains("clock"))return 0;}if(v instanceof ViewGroup&&(id.equals("statusicons")||id.equals("status_icons")||id.equals("system_icons")||id.equals("battery")||name.contains("statusiconcontainer")||name.contains("batterymeterview")||name.contains("mfvbatteryviewlayout")||name.contains("signalclusterview")))return 2;return -1;}
 private static int role(View v){int role=rawRole(v);if(role<0)return -1;View current=v;for(int depth=0;depth<24;depth++){if(headers.contains(current))return role;if(!(current.getParent() instanceof View parent))return -1;current=parent;if(rawRole(current)>=0)return -1;}return -1;}
 // Tint status content at leaf draw points, so a group layer cannot recolor battery text.
 private static boolean statusContent(View view){View v=view;for(int n=0;n<24&&v.getParent() instanceof View parent;n++){v=parent;if(rawRole(v)==2&&QsHeaderScope.contains(v))return true;if(headers.contains(v))break;}return false;}
 static int statusTint(View view){return statusContent(view)?theme.statusColor():0;}
 static void observe(View container){try{Object header=XposedHelpers.getObjectField(container,"mHeader");if(header instanceof View v&&headers.add(v)){v.post(()->{discover(v);StatusBarBackend.observeHeader(v);BatteryControlBackend.refreshHeader();invalidateTree(v);});}}catch(Throwable ignored){}}
 private static void discover(View root){StringBuilder found=new StringBuilder();ArrayDeque<View> queue=new ArrayDeque<>();queue.add(root);int count=0;while(!queue.isEmpty()&&count++<256){View v=queue.removeFirst();if(v instanceof TextView||rawRole(v)==2){if(found.length()<700)found.append(v.getClass().getSimpleName()).append('/').append(id(v)).append('=').append(role(v)).append(';');}ConnectivityBackend.observe(v);SystemIconPackBackend.observe(v);if(v instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)queue.add(group.getChildAt(i));}targets=found.toString();}
 static void begin(QsTheme next){theme=next;observed=textDraws=statusDraws=0;invalidate();}
 static void end(){theme=QsTheme.STOCK;invalidate();}
 private static void invalidate(){for(View v:new ArrayList<>(headers))if(v!=null)invalidateTree(v);}
 private static void invalidateTree(View v){v.invalidate();if(v instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)invalidateTree(group.getChildAt(i));}
 static String status(){return "QS header: "+headers.size()+" hosts; styled draws="+observed+" (text "+textDraws+", icons "+statusDraws+"); targets: "+targets+".";}
}
