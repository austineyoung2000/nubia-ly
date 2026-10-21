package com.elitedarkkaiser.nubialy.xposed;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.view.*;
import android.widget.ImageView;
import com.elitedarkkaiser.nubialy.ConnectivityStyles;
import com.elitedarkkaiser.nubialy.ConnectivityResourcePolicy;
import de.robv.android.xposed.*;
import java.util.*;
/** Native state remains authoritative; only ordinary connected glyphs in PhoneStatusBarView are redrawn. */
final class ConnectivityBackend {
 private record ResourceState(Drawable drawable,int level){}
 private record ArtworkState(boolean wifi,int style,int level,Drawable drawable){}
 private static final Map<ImageView,ArtworkState> artwork=new WeakHashMap<>();
 private static final Map<ImageView,ResourceState> resources=new WeakHashMap<>();
 private static final Map<ImageView,Boolean> observed=new WeakHashMap<>();
 private static final List<XC_MethodHook.Unhook> hooks=new ArrayList<>();
 private static int pack,wifi,signal,draws,wifiDraws,mobileDraws;private static final Set<String> fallbackResources=new TreeSet<>();private static boolean active,ready,failed;
 private static boolean main(){return Looper.myLooper()==Looper.getMainLooper();}
 private static String id(View v){try{return v.getResources().getResourcePackageName(v.getId()).equals("com.android.systemui")?v.getResources().getResourceEntryName(v.getId()):"";}catch(Throwable absent){return "";}}
 private static int category(ImageView v){String id=id(v);if(!id.equals("wifi_signal")&&!id.equals("mobile_signal"))return 0;boolean owner=false;View current=v;
  for(int i=0;i<16&&current!=null;i++){String name=current.getClass().getName();if(name.equals(id.equals("wifi_signal")?"com.android.systemui.statusbar.pipeline.wifi.ui.view.ModernStatusBarWifiView":"com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView"))owner=true;
   if(name.equals("com.android.systemui.statusbar.phone.PhoneStatusBarView"))return owner?(id.equals("wifi_signal")?1:2):0;current=current.getParent() instanceof View next?next:null;}return 0;}
 static void observe(View view){if(main()&&view instanceof ImageView image&&category(image)!=0){observed.put(image,true);if(active)image.invalidate();}}
 static void install(ClassLoader loader){try{
  XposedHelpers.findClass("com.android.systemui.statusbar.pipeline.wifi.ui.view.ModernStatusBarWifiView",loader);XposedHelpers.findClass("com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView",loader);
  XposedBridge.hookMethod(ImageView.class.getDeclaredMethod("setImageResource",int.class),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(main()&&!p.hasThrowable())record((ImageView)p.thisObject,(Integer)p.args[0]);}});
  Class<?> binder=XposedHelpers.findClass("com.android.systemui.common.ui.binder.IconViewBinder",loader),icon=XposedHelpers.findClass("com.android.systemui.common.shared.model.Icon",loader),resource=XposedHelpers.findClass("com.android.systemui.common.shared.model.Icon$Resource",loader);
  if(resource.getDeclaredMethod("getRes").getReturnType()!=int.class||binder.getDeclaredMethod("bind",icon,ImageView.class).getReturnType()!=void.class)throw new IllegalStateException("Icon binding signature");
  XposedBridge.hookMethod(binder.getDeclaredMethod("bind",icon,ImageView.class),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!main()||p.hasThrowable()||!(p.args[1] instanceof ImageView image))return;String name=id(image);if(!name.equals("wifi_signal")&&!name.equals("mobile_signal"))return;resources.remove(image);if(p.args[0]!=null&&p.args[0].getClass()==resource)record(image,((Number)XposedHelpers.callMethod(p.args[0],"getRes")).intValue());}});
  ready=true;
 }catch(Throwable unavailable){ready=false;}}
 private static void record(ImageView image,int resource){String target=id(image);if(!target.equals("wifi_signal")&&!target.equals("mobile_signal"))return;observe(image);resources.remove(image);try{String name=image.getResources().getResourceEntryName(resource);int level=ConnectivityResourcePolicy.level(target.equals("wifi_signal"),name);if(level>=0)resources.put(image,new ResourceState(image.getDrawable(),level));else if(fallbackResources.size()<8)fallbackResources.add(name);}catch(Throwable absent){} }
 static void begin(int wifiStyle,int signalStyle){begin(wifiStyle,signalStyle,0);}
 static void begin(int wifiStyle,int signalStyle,int iconPack){if(!main()||wifiStyle<0||wifiStyle>8||signalStyle<0||signalStyle>8)throw new IllegalArgumentException();if(!end())throw new IllegalStateException();pack=iconPack;wifi=wifiStyle;signal=signalStyle;draws=wifiDraws=mobileDraws=0;if(wifi==0&&signal==0&&pack==0)return;if(!ready||failed)throw new IllegalStateException("Signal styling unavailable");
  try{hooks.add(XposedBridge.hookMethod(ImageView.class.getDeclaredMethod("onDraw",Canvas.class),new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){if(!active||failed||!main())return;ImageView v=(ImageView)p.thisObject;int kind=category(v),style=kind==1?wifi:kind==2?signal:0;if(kind==0||(style==0&&pack==0)||!v.isAttachedToWindow())return;observed.put(v,true);Drawable nativeIcon=v.getDrawable();if(nativeIcon==null)return;
   try{int level;Integer color=null;if(kind==1){ResourceState state=resources.get(v);if(state==null||state.drawable()!=nativeIcon)return;level=state.level();}else if(!nativeIcon.getClass().getName().equals("com.android.settingslib.graph.SignalDrawable")){ResourceState state=resources.get(v);if(state==null||state.drawable()!=nativeIcon)return;level=state.level();}else{int packed=nativeIcon.getLevel();if((packed>>>16)!=0)return;int count=(packed>>>8)&255,raw=packed&255;if(count<2||count>6||raw>=count)return;level=Math.round(raw*4f/(count-1));Object paint=XposedHelpers.getObjectField(nativeIcon,"mForegroundPaint");if(paint instanceof Paint foreground)color=foreground.getColor();}
    ArtworkState cached=artwork.get(v);if(cached==null||cached.wifi()!=(kind==1)||cached.style()!=style||cached.level()!=level){Drawable loaded=style==0?com.elitedarkkaiser.nubialy.SystemIconPacks.artwork(pack,(kind==1?"wifi_":"signal_")+level):ConnectivityStyles.artwork(kind==1,style,level);if(loaded==null)return;cached=new ArtworkState(kind==1,style,level,loaded);artwork.put(v,cached);}Drawable art=cached.drawable();if(v.getImageTintList()!=null)color=v.getImageTintList().getColorForState(v.getDrawableState(),v.getImageTintList().getDefaultColor());ColorFilter filter=v.getColorFilter();if(color==null&&filter==null)filter=nativeIcon.getColorFilter();if(color==null&&filter==null)return;art.setColorFilter(null);if(color!=null)art.setTint(color);else{art.setTintList(null);art.setColorFilter(filter);}
    art.setAlpha(v.getImageAlpha());art.setLayoutDirection(v.getLayoutDirection());Rect bounds=nativeIcon.getBounds();if(bounds.isEmpty())return;art.setBounds(bounds);Canvas c=(Canvas)p.args[0];int save=c.save();try{c.translate(v.getPaddingLeft(),v.getPaddingTop());c.concat(v.getImageMatrix());art.draw(c);}finally{c.restoreToCount(save);}draws++;if(kind==1)wifiDraws++;else mobileDraws++;p.setResult(null);
   }catch(Throwable unavailable){failed=true;v.invalidate();}
  }}));active=true;invalidate();}catch(Throwable unavailable){end();throw new IllegalStateException("Signal styling unavailable",unavailable);}}
 private static void invalidate(){for(ImageView v:new ArrayList<>(observed.keySet()))v.invalidate();}
 static boolean end(){active=false;invalidate();artwork.clear();for(var h:new ArrayList<>(hooks))try{h.unhook();hooks.remove(h);}catch(Throwable unavailable){failed=true;}return !failed;}
 static boolean healthy(){return !failed;}
 static String status(){return "Connectivity styles: Wi-Fi "+wifi+", mobile "+signal+"; glyph draws: "+draws+" (Wi-Fi "+wifiDraws+", mobile "+mobileDraws+"); native fallback resources: "+(fallbackResources.isEmpty()?"none":String.join(", ",fallbackResources))+(ready?".":"; source unavailable.");}
}
