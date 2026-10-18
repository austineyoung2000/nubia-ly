package com.elitedarkkaiser.nubialy.xposed;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import com.elitedarkkaiser.nubialy.SystemIconPacks;
import com.elitedarkkaiser.nubialy.SystemIconPackPolicy;
import de.robv.android.xposed.*;
import java.util.*;

/** Draw-only, scoped replacement. Native drawables and their state are never mutated. */
final class SystemIconPackBackend {
    private record Source(Drawable drawable,String key){}
    private record Tile(String spec,boolean transientState,String stateKey){}
    private static final Map<Object,String> soundModes=new WeakHashMap<>();
    private static final Map<ImageView,Tile> tiles=new WeakHashMap<>();
    private record Art(String key,Drawable drawable){}
    private static final Map<Drawable,String> drawableKeys=new WeakHashMap<>();
    private static final Map<Drawable.ConstantState,String> stateKeys=new WeakHashMap<>();
    private static final Map<ImageView,Source> sources=new WeakHashMap<>();
    private static final Map<ImageView,Art> artwork=new WeakHashMap<>();
    private static final Map<ImageView,Boolean> targets=new WeakHashMap<>();
    private static final Map<Integer,String> resourceKeys=new HashMap<>();
    private static final List<XC_MethodHook.Unhook> draws=new ArrayList<>();
    private static Class<?> qsIconClass,resourceIconClass,drawableResourceIconClass,statusIconClass;
    private static java.lang.reflect.Method drawMethod;
    private static int pack,qsPack,drawCount,qsBindings,statusBindings,tintSkips;
    private static final Set<String> unmatched=new TreeSet<>();private static boolean ready,active;
    private static boolean main(){return Looper.myLooper()==Looper.getMainLooper();}
    private static String resourceKey(Resources res,int id){
        // Memoize only System UI's stable resource table, never IDs from arbitrary app Resources.
        try{String pkg=res.getResourcePackageName(id);if(!pkg.equals("com.android.systemui")&&!pkg.equals("android"))return null;
            if(resourceKeys.containsKey(id))return resourceKeys.get(id);
            String key=SystemIconPackPolicy.key(res.getResourceEntryName(id));if(resourceKeys.size()<4096)resourceKeys.put(id,key);return key;
        }catch(Throwable unavailable){return null;}
    }
    private static void remember(Drawable d,String key){if(d==null||key==null)return;drawableKeys.put(d,key);Drawable.ConstantState cs=d.getConstantState();if(cs!=null)stateKeys.put(cs,key);}
    private static String key(Drawable d){if(d==null)return null;String k=drawableKeys.get(d);return k!=null?k:stateKeys.get(d.getConstantState());}
    /** Resolve from the actual bound native model, including drawables cached before observers. */
    private static String boundKey(ImageView image){
        try{
            if(statusIconClass!=null&&statusIconClass.isInstance(image)){
                Object model=XposedHelpers.getObjectField(image,"mIcon");
                Object icon=model==null?null:XposedHelpers.getObjectField(model,"icon");
                if(icon!=null&&((Number)XposedHelpers.callMethod(icon,"getType")).intValue()==2){
                    String pkg=(String)XposedHelpers.callMethod(icon,"getResPackage");
                    if(!"com.android.systemui".equals(pkg)&&!"android".equals(pkg))return null;
                    String key=resourceKey(image.getResources(),((Number)XposedHelpers.callMethod(icon,"getResId")).intValue());
                    if(key!=null)statusBindings++;return key;
                }return null;
            }
            View v=image;
            for(int i=0;i<8&&v!=null;i++){
                if(qsIconClass!=null&&qsIconClass.isInstance(v)){
                    Object icon=XposedHelpers.getObjectField(v,"mLastIcon");
                    if(icon==null)return null;
                    // QsInCompose preloads these on a worker: DrawableIconWithRes keeps mId,
                    // not ResourceIcon.mResId. The bound model survives drawable cloning.
                    int res;
                    if(resourceIconClass.isInstance(icon))res=XposedHelpers.getIntField(icon,"mResId");
                    else if(drawableResourceIconClass.isInstance(icon))res=XposedHelpers.getIntField(icon,"mId");
                    else return null;
                    String key=resourceKey(image.getResources(),res);
                    if(key!=null)qsBindings++;return key;
                }
                v=v.getParent() instanceof View parent?parent:null;
            }
        }catch(Throwable unsupported){}
        return null;
    }
    private static Integer boundColor(ImageView image){
        try{
            if(statusIconClass!=null&&statusIconClass.isInstance(image)){int color=XposedHelpers.getIntField(image,"mCurrentSetColor");return color==0?null:color;}
            View v=image;for(int i=0;i<8&&v!=null;i++){if(qsIconClass!=null&&qsIconClass.isInstance(v)){int color=XposedHelpers.getIntField(v,"mTint");return color==0?null:color;}v=v.getParent() instanceof View parent?parent:null;}
        }catch(Throwable unsupported){}return null;
    }
    private static void tileChanged(ImageView image,Object state){
        try{String spec=(String)XposedHelpers.getObjectField(state,"spec");String stateKey; synchronized(soundModes){stateKey=soundModes.get(state);}
            if("rotation".equals(spec))stateKey=(Boolean)XposedHelpers.getObjectField(state,"value")?"rotate":"portrait";
            tiles.put(image,new Tile(spec,(Boolean)XposedHelpers.getObjectField(state,"isTransient"),stateKey));}catch(Throwable absent){tiles.remove(image);}
        boundChanged(image);
    }
    private static String resolvedKey(ImageView image,Drawable nativeIcon){
        String nativeKey=boundKey(image);if(nativeKey==null)nativeKey=key(nativeIcon);
        Tile tile=tiles.get(image);
        return tile==null?nativeKey:SystemIconPackPolicy.tileKey(tile.spec(),tile.transientState(),tile.stateKey()!=null?tile.stateKey():nativeKey);
    }
    private static void boundChanged(ImageView image){sources.remove(image);if(active&&image.isAttachedToWindow())image.invalidate();}
    private static boolean scope(ImageView image){
        try{String id=image.getResources().getResourceEntryName(image.getId());if(id.equals("wifi_signal")||id.equals("mobile_signal"))return false;}catch(Throwable absent){}
        View v=image;boolean qs=false;for(int i=0;i<18&&v!=null;i++){
        String name=v.getClass().getName();if(name.equals("com.android.systemui.statusbar.phone.NotificationIconContainer"))return false;
        if(qsIconClass!=null&&qsIconClass.isInstance(v))qs=true;
        if(name.equals("com.android.systemui.statusbar.phone.PhoneStatusBarView"))return true;
        if(qs)return true;
        v=v.getParent() instanceof View parent?parent:null;
    }return false;}
    static void observe(View view){if(!main()||!(view instanceof ImageView image))return;targets.remove(image);sources.remove(image);artwork.remove(image);if(scope(image)){targets.put(image,true);if(active)image.invalidate();}}
    static void install(ClassLoader loader){try{
        qsIconClass=XposedHelpers.findClass("com.android.systemui.qs.tileimpl.QSIconViewImpl",loader);
        resourceIconClass=XposedHelpers.findClass("com.android.systemui.qs.tileimpl.QSTileImpl$ResourceIcon",loader);
        drawableResourceIconClass=XposedHelpers.findClass("com.android.systemui.qs.tileimpl.QSTileImpl$DrawableIconWithRes",loader);
        statusIconClass=XposedHelpers.findClass("com.android.systemui.statusbar.StatusBarIconView",loader);
        Class<?> state=XposedHelpers.findClass("com.android.systemui.plugins.qs.QSTile$State",loader);
        XposedBridge.hookMethod(qsIconClass.getDeclaredMethod("updateIcon",ImageView.class,state,boolean.class),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(main()&&!p.hasThrowable())tileChanged((ImageView)p.args[0],p.args[1]);}});
        XposedBridge.hookMethod(statusIconClass.getDeclaredMethod("updateDrawable",boolean.class),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(main()&&!p.hasThrowable())boundChanged((ImageView)p.thisObject);}});
        XposedBridge.hookMethod(Resources.class.getDeclaredMethod("getDrawableForDensity",int.class,int.class,Resources.Theme.class),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(main()&&!p.hasThrowable()&&p.getResult() instanceof Drawable d)remember(d,resourceKey((Resources)p.thisObject,(Integer)p.args[0]));}});
        XposedBridge.hookMethod(ImageView.class.getDeclaredMethod("setImageResource",int.class),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!main()||p.hasThrowable())return;ImageView image=(ImageView)p.thisObject;String k=resourceKey(image.getResources(),(Integer)p.args[0]);if(k!=null){remember(image.getDrawable(),k);sources.remove(image);}}});
        try{Class<?> sound=XposedHelpers.findClass("com.zte.qs.tiles.SoundVibrateTile",loader);
            Class<?> booleanState=XposedHelpers.findClass("com.android.systemui.plugins.qs.QSTile$BooleanState",loader);
            XposedBridge.hookMethod(sound.getDeclaredMethod("handleUpdateState",booleanState,Object.class),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){
                if(p.hasThrowable())return;try{int mode=XposedHelpers.getIntField(p.thisObject,"mCurrentMode");synchronized(soundModes){soundModes.put(p.args[0],mode==0?"silent":mode==1?"vibrate":"ring");}}catch(Throwable unsupported){}
            }});
        }catch(Throwable unavailable){/* Other firmware can still resolve verified native ringer aliases. */}
        drawMethod=ImageView.class.getDeclaredMethod("onDraw",Canvas.class);ready=true;
    }catch(Throwable unavailable){ready=false;}}
    static void begin(int value){begin(value,value);}
    static void begin(int value,int quickSettings){if(!main()||(!SystemIconPacks.valid(value)||!SystemIconPacks.valid(quickSettings)))throw new IllegalArgumentException();end();pack=value;qsPack=quickSettings;drawCount=qsBindings=statusBindings=tintSkips=0;unmatched.clear();sources.clear();if(value==0&&quickSettings==0)return;if(!ready)throw new IllegalStateException("Icon pack observer unavailable");
        draws.add(XposedBridge.hookMethod(drawMethod,new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){
            if(!active||!main())return;ImageView image=(ImageView)p.thisObject;if(!image.isAttachedToWindow())return;
            Boolean target=targets.get(image);if(target==null){target=scope(image);targets.put(image,target);}if(!target)return;
            int selectedPack=isQs(image)?qsPack:pack;if(selectedPack==0)return;
            Drawable nativeIcon=image.getDrawable();if(nativeIcon==null)return;
            Source source=sources.get(image);if(source==null||source.drawable()!=nativeIcon){String k=resolvedKey(image,nativeIcon);source=new Source(nativeIcon,k);if(k==null&&unmatched.size()<6)unmatched.add(nativeIcon.getClass().getSimpleName());sources.put(image,source);}if(source.key()==null)return;
            try{Art cached=artwork.get(image);if(cached==null||!cached.key().equals(source.key())){Drawable d=SystemIconPacks.artwork(selectedPack,source.key());if(d==null)return;cached=new Art(source.key(),d);artwork.put(image,cached);}
                Drawable d=cached.drawable();d.setColorFilter(null);if(image.getImageTintList()!=null)d.setTint(image.getImageTintList().getColorForState(image.getDrawableState(),image.getImageTintList().getDefaultColor()));else{ColorFilter filter=image.getColorFilter();if(filter==null)filter=nativeIcon.getColorFilter();if(filter==null){Integer color=boundColor(image);if(color==null){tintSkips++;return;}d.setTint(color);}else{d.setTintList(null);d.setColorFilter(filter);}}
                Rect bounds=nativeIcon.getBounds();if(bounds.isEmpty())return;d.setBounds(bounds);d.setAlpha(image.getImageAlpha());d.setState(image.getDrawableState());d.setLayoutDirection(image.getLayoutDirection());
                Canvas canvas=(Canvas)p.args[0];int save=canvas.save();try{canvas.translate(image.getPaddingLeft(),image.getPaddingTop());canvas.concat(image.getImageMatrix());d.draw(canvas);}finally{canvas.restoreToCount(save);}drawCount++;p.setResult(null);
            }catch(Throwable unsupported){artwork.remove(image);/* Isolated artwork failure keeps the native icon. */}
        }}));active=true;invalidate();
    }
    private static boolean isQs(ImageView image){View v=image;for(int i=0;i<18&&v!=null;i++){if(qsIconClass!=null&&qsIconClass.isInstance(v))return true;v=v.getParent() instanceof View parent?parent:null;}return false;}
    private static void invalidate(){for(ImageView v:new ArrayList<>(targets.keySet()))if(Boolean.TRUE.equals(targets.get(v)))v.invalidate();}
    static boolean end(){active=false;for(var h:draws)h.unhook();draws.clear();artwork.clear();invalidate();return true;}
    static boolean healthy(){return true;}
    static String status(){return "Icon pack: "+SystemIconPacks.NAMES[pack]+"; decorated draws: "+drawCount+"; bindings: QS "+qsBindings+", status "+statusBindings+"; tint skips: "+tintSkips+"; unmatched: "+String.join(",",unmatched)+"; scoped targets: "+targets.values().stream().filter(Boolean.TRUE::equals).count()+".";}
}
