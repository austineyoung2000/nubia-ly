package com.elitedarkkaiser.nubialy.xposed;
import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import com.elitedarkkaiser.nubialy.*;
import com.elitedarkkaiser.nubialy.SurfaceStyle.QsTheme;
import de.robv.android.xposed.*;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;
/** Scoped return-value colors and a non-interactive draw layer. Native view state stays untouched. */
final class QsAppearanceBackend {
 private static final String ROOT="com.zte.mifavor.qs.MfvQSContainerImpl";
 private static final List<XC_MethodHook.Unhook> hooks=new ArrayList<>();
 private static final Set<View> roots=Collections.newSetFromMap(new WeakHashMap<>());
 private static final Set<View> dismissing=Collections.newSetFromMap(new WeakHashMap<>());
 private static final Map<View,Float> panelFractions=new WeakHashMap<>();
 private static final Set<View> tiles=Collections.newSetFromMap(new WeakHashMap<>());private static long generation;
 private static boolean attempted,ready,active,failed,restorePending;private static QsTheme theme=QsTheme.STOCK;
 private static volatile Bitmap image;private static volatile String imageHash="";private static volatile long imageGeneration;
 private static String message="Not installed";private static int draws;
 static void install(ClassLoader loader){if(attempted)return;attempted=true;try{
  Class<?> root=XposedHelpers.findClass(ROOT,loader);if(root.getSuperclass()!=android.widget.FrameLayout.class||root.getDeclaredMethod("getShowQsPanel").getReturnType()!=boolean.class)throw new IllegalStateException("QS container signature");
  root.getDeclaredField("mHeader");root.getDeclaredField("brightnessView");root.getDeclaredMethod("setExpansion",float.class);
  // Panel fraction is distinct from QS expansion: compact QS can be visible at expansion zero.
  Class<?> fragment=XposedHelpers.findClass("com.zte.mifavor.qs.MfvQSFragment",loader);
  java.lang.reflect.Field container=fragment.getDeclaredField("qsContainer");container.setAccessible(true);
  hooks.add(XposedBridge.hookMethod(fragment.getDeclaredMethod("setQsExpansion",float.class,float.class,float.class,float.class),new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p)throws Throwable{
   if(Looper.myLooper()!=Looper.getMainLooper())return;
   Object target=container.get(p.thisObject);if(!(target instanceof View v)||!v.getClass().getName().equals(ROOT))return;
   float fraction=((Number)p.args[1]).floatValue();panelFractions.put(v,Float.isFinite(fraction)?Math.max(0f,Math.min(1f,fraction)):0f);roots.add(v);v.invalidate();
  }}));
  // Nubia can dismiss by fading children while the container/panel fraction remains visible.
  // Latch dismissal at its start; stale expansion frames must not resurrect the background.
  XC_MethodHook appearance=new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p)throws Throwable{
   if(Looper.myLooper()!=Looper.getMainLooper())return;
   Object target=container.get(p.thisObject);if(!(target instanceof View v)||!v.getClass().getName().equals(ROOT))return;
   if(Boolean.TRUE.equals(p.args[0]))dismissing.remove(v);else dismissing.add(v);
   roots.add(v);v.invalidate();
  }};
  hooks.add(XposedBridge.hookMethod(fragment.getDeclaredMethod("animateAppearDisapper",boolean.class),appearance));
  hooks.add(XposedBridge.hookMethod(fragment.getDeclaredMethod("animateHeaderShow",boolean.class,Runnable.class),appearance));
  hooks.add(XposedBridge.hookMethod(fragment.getDeclaredMethod("animateHeaderSlidingOut"),new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p)throws Throwable{
   if(Looper.myLooper()!=Looper.getMainLooper())return;
   Object target=container.get(p.thisObject);if(target instanceof View v&&v.getClass().getName().equals(ROOT)){dismissing.add(v);roots.add(v);v.invalidate();}
  }}));
  Class<?> base=XposedHelpers.findClass("com.zte.qs.tileimpl.QSTileViewBase",loader),wide=XposedHelpers.findClass("com.zte.qs.tileimpl.QSTileViewWide",loader);
  for(Class<?> c:new Class<?>[]{base,wide})for(String name:new String[]{"getBackgroundColorForState","getBackgroundStartColorForState","getBackgroundEndColorForState","getLabelColorForState","getSecondaryLabelColorForState"}){
   for(Method m:c.getDeclaredMethods())if(m.getName().equals(name)&&m.getReturnType()==int.class&&m.getParameterTypes().length>0&&m.getParameterTypes()[0]==int.class)hooks.add(XposedBridge.hookMethod(m,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(Looper.myLooper()==Looper.getMainLooper()&&p.thisObject instanceof View v)tiles.add(v);if(!allowed()||p.hasThrowable())return;int state=(Integer)p.args[0];if(state==0)return;int value=color(name,state);if(value!=0)p.setResult(value);}}));
  }
  Class<?> multi=XposedHelpers.findClass("com.zte.feature.qs.theme.MultiThemeFeature",loader);
  hooks.add(XposedBridge.hookMethod(multi.getDeclaredMethod("getQSIconViewColor",int.class,Context.class,int.class,boolean.class,String.class),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!allowed()||p.hasThrowable())return;int state=(Integer)p.args[2];if(state==0)return;int value=state==2?theme.activeIcon():theme.inactiveIcon();if(value!=0)p.setResult(value);}}));
  // Invoked by the vendor's dispatchDraw after it applies native clipping, before child tiles draw.
  hooks.add(XposedHelpers.findAndHookMethod(ViewGroup.class,"dispatchDraw",Canvas.class,new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){if(Looper.myLooper()!=Looper.getMainLooper()||!p.thisObject.getClass().getName().equals(ROOT))return;ViewGroup v=(ViewGroup)p.thisObject;roots.add(v);if(!allowed()||theme.header()==0||dismissing.contains(v)||!v.isAttachedToWindow())return;try{if(!Boolean.TRUE.equals(XposedHelpers.callMethod(v,"getShowQsPanel")))return;float fraction=panelFractions.getOrDefault(v,0f);if(fraction<=0f)return;draw((Canvas)p.args[0],v,fraction);}catch(Throwable error){failed=true;message="QS background draw stopped: "+error.getClass().getSimpleName();}}}));
  ready=true;message="Ready";
 }catch(Throwable error){for(var h:new ArrayList<>(hooks))try{h.unhook();hooks.remove(h);}catch(Throwable ignored){}failed=true;message="QS appearance unavailable: "+error.getClass().getSimpleName()+" "+error.getMessage();}}
 private static boolean allowed(){return active&&ready&&!failed&&Looper.myLooper()==Looper.getMainLooper()&&ClockColorBackend.authorized();}
 private static int color(String name,int state){boolean on=state==2;return switch(name){case "getLabelColorForState"->theme.primaryLabel();case "getSecondaryLabelColorForState"->theme.secondaryLabel();case "getBackgroundEndColorForState"->on&&theme.twoTone()!=0?theme.secondAccent():on?theme.activeBackground():theme.inactiveBackground();default->on&&theme.twoTone()!=0?theme.accent():on?theme.activeBackground():theme.inactiveBackground();};}
 static void prepare(Context c,QsTheme next)throws Exception {
  if(next.header()==0||next.image().isEmpty())return;
  if(next.image().equals(imageHash)&&image!=null)return;long ticket=imageGeneration;
  byte[] bytes;try(InputStream in=c.getContentResolver().openInputStream(Uri.parse("content://"+GuardBrokerPolicy.AUTHORITY+"/qs-image/"+next.image()))){if(in==null)throw new java.io.IOException("QS image unavailable");bytes=in.readNBytes(QsImageStore.MAX_BYTES+1);}
  if(bytes.length>QsImageStore.MAX_BYTES||!next.image().equals(QsImageStore.digest(bytes)))throw new java.io.IOException("QS image validation failed");
  BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
  if(bounds.outWidth<1||bounds.outHeight<1||bounds.outWidth>1024||bounds.outHeight>1024)throw new java.io.IOException("QS image dimensions rejected");
  Bitmap decoded=BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(decoded==null)throw new java.io.IOException("QS image decode failed");
  if(ticket!=imageGeneration){decoded.recycle();return;}image=decoded;imageHash=next.image();
 }
 private static void draw(Canvas canvas,View root,float fraction){float w=root.getWidth(),h=root.getHeight()*theme.height()/100f;if(w<=0||h<=0)return;int save=canvas.save();try{canvas.clipRect(0,0,w,h);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);p.setColor(theme.headerColor());p.setAlpha(Math.round(theme.opacity()*255/100f*fraction));Bitmap b=image;if(!theme.image().isEmpty()&&theme.image().equals(imageHash)&&b!=null){float scale=Math.max(w/b.getWidth(),h/b.getHeight());float bw=b.getWidth()*scale,bh=b.getHeight()*scale;canvas.drawBitmap(b,null,new RectF((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2),p);}else canvas.drawRect(0,0,w,h,p);p.setColor(Color.BLACK);p.setAlpha(Math.round(theme.dim()*theme.opacity()*255/10000f*fraction));canvas.drawRect(0,0,w,h,p);draws++;}finally{canvas.restoreToCount(save);}}
 static void begin(QsTheme value){if(!end())throw new IllegalStateException("QS color restoration pending");if(!value.any())return;if(!ready||failed)throw new IllegalStateException(message);theme=value;active=true;long ticket=++generation;new Handler(Looper.getMainLooper()).post(()->{if(active&&ticket==generation&&ClockColorBackend.authorized())refreshColors();});invalidate();}
 private static void invalidate(){for(View v:new ArrayList<>(roots))if(v!=null)v.invalidate();}
 static void observe(View view){String n=view.getClass().getName();if(n.equals("com.zte.qs.tileimpl.QSTileViewWide")||n.equals("com.zte.qs.tileimpl.QSTileViewCircle"))tiles.add(view);}
 private static boolean refreshColors(){boolean ok=true;for(View tile:new ArrayList<>(tiles))if(tile!=null&&tile.isAttachedToWindow())try{XposedHelpers.callMethod(tile,"updateColors");tile.invalidate();}catch(Throwable error){ok=false;message="Tile color refresh failed: "+error.getClass().getSimpleName();}return ok;}
 static boolean end(){generation++;restorePending|=active;active=false;theme=QsTheme.STOCK;boolean ok=!restorePending||refreshColors();if(ok)restorePending=false;invalidate();return ok;}
 static boolean twoTone(){return allowed()&&theme.twoTone()!=0;}
 static boolean healthy(){return !active||!failed;}
 static String status(){return "QS colors/background: "+message+"; "+(active?"enabled":"stock")+"; image="+(imageHash.isEmpty()?"none":"cached")+"; draws="+draws+".";}
}
