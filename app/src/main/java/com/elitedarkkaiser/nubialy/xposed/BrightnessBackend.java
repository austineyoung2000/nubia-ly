package com.elitedarkkaiser.nubialy.xposed;

import android.graphics.Canvas;
import android.graphics.drawable.*;
import android.os.Looper;
import android.view.*;
import android.widget.SeekBar;
import com.elitedarkkaiser.nubialy.BrightnessEffects;
import com.elitedarkkaiser.nubialy.SurfaceStyle.Palette;
import de.robv.android.xposed.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;

/** Exact NX809J ToggleSliderView ownership, native LayerDrawable/ClipDrawable progress retained. */
final class BrightnessBackend {
    private static final String OWNER="com.zte.controlcenter.view.ToggleSliderView",SLIDER="com.android.systemui.settings.brightness.ToggleSeekBar",CCSLIDER="com.zte.controlcenter.view.CCToggleSeekBar",CCOWNER="com.zte.controlcenter.view.CCBrightnessToggleSliderView";
    private record Entry(WeakReference<SeekBar> slider,WeakReference<Drawable> root){}
    private static final Map<GradientDrawable,Entry> leaves=new WeakHashMap<>();
    private static final Map<SeekBar,Boolean> observed=new WeakHashMap<>();
    private static final List<XC_MethodHook.Unhook> hooks=new ArrayList<>();
    private static ClassLoader loader;private static boolean active,failed;private static long generation;private static int selection;private static int drawCallbacks,decoratedDraws;private static String lastRoot="not observed";
    static void install(ClassLoader value){loader=value;}
    static void observe(View view){if(main()&&view instanceof SeekBar slider&&sliderName(view.getClass().getName())){observed.put(slider,true);if(active&&!failed){try{track(slider);if(owned(slider))slider.invalidate();}catch(Throwable unavailable){failed=true;invalidate();}}}}
    private static boolean sliderName(String name){return name.equals(SLIDER)||name.equals(CCSLIDER);}
    static void release(View view){if(view instanceof SeekBar slider){observed.remove(slider);leaves.entrySet().removeIf(e->e.getValue().slider().get()==slider);}}
    private static boolean main(){return Looper.myLooper()==Looper.getMainLooper();}
    private static Palette palette=Palette.STOCK;
    static void begin(int style){begin(style,Palette.STOCK);}
    static void begin(int style,Palette colors){
        if(style<0||style>3||!main())throw new IllegalArgumentException();if(!end())throw new IllegalStateException("Brightness cleanup incomplete");selection=style;palette=colors;if(style==0&&!colors.brightnessAny())return;
        drawCallbacks=decoratedDraws=0;lastRoot="not observed";final long ticket=++generation;
        try{
            Class<?> slider=XposedHelpers.findClass(SLIDER,loader),owner=XposedHelpers.findClass(OWNER,loader);
            if(slider.getSuperclass()!=SeekBar.class||owner.getMethod("getSlider").getReturnType()!=slider)throw new IllegalStateException("Brightness signatures");
            Class<?> ccSlider=XposedHelpers.findClass(CCSLIDER,loader),ccOwner=XposedHelpers.findClass(CCOWNER,loader);
            if(ccSlider.getSuperclass()!=slider||!ccOwner.getSuperclass().getName().equals("com.zte.controlcenter.view.CCToggleSliderView")||ccOwner.getMethod("getSlider").getReturnType()!=ccSlider)throw new IllegalStateException("Control Center brightness signatures");
            Method draw=null;for(Class<?> type=slider;type!=null;type=type.getSuperclass())try{draw=type.getDeclaredMethod("onDraw",Canvas.class);break;}catch(NoSuchMethodException absent){}
            if(draw==null)throw new IllegalStateException("Brightness draw unavailable");
            hooks.add(XposedBridge.hookMethod(draw,new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){
                if(!active||failed||!main()||!sliderName(p.thisObject.getClass().getName()))return;
                try{drawCallbacks++;observed.put((SeekBar)p.thisObject,true);track((SeekBar)p.thisObject);}catch(Throwable unavailable){failed=true;invalidate();}
            }}));
            hooks.add(XposedHelpers.findAndHookMethod(GradientDrawable.class,"draw",Canvas.class,new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){
                if(!active||failed||!main()||!(p.thisObject instanceof GradientDrawable d))return;Entry entry=leaves.get(d);if(!eligible(entry,d))return;
                SeekBar view=entry.slider().get();
                try{GradientDrawable[] parts=layers(view.getProgressDrawable());boolean fill=parts!=null&&parts[1]==d;GradientDrawable colored=com.elitedarkkaiser.nubialy.BrightnessColors.copy(d,fill,palette);GradientDrawable artwork=colored==null?d:colored;com.elitedarkkaiser.nubialy.QsOutlineRenderer.NativeDraw original=colored==null?()->XposedBridge.invokeOriginalMethod(p.method,p.thisObject,p.args):()->artwork.draw((Canvas)p.args[0]);boolean rendered=BrightnessEffects.draw(style,(Canvas)p.args[0],artwork,view.getResources().getDisplayMetrics().density,()->active&&!failed&&generation==ticket&&eligible(entry,d),original);if(!rendered&&colored!=null){original.draw();rendered=true;}if(rendered){decoratedDraws++;p.setResult(null);}}
                catch(Throwable unavailable){failed=true;invalidate();}
            }}));active=true;refresh();invalidate();
        }catch(Throwable unavailable){failed=true;end();throw new IllegalStateException("Brightness styling unavailable",unavailable);}
    }
    static void refresh(){if(!active||failed||!main())return;try{for(SeekBar slider:new ArrayList<>(observed.keySet()))track(slider);}catch(Throwable unavailable){failed=true;invalidate();}}
    private static boolean owned(SeekBar slider){
        if(slider==null||!slider.isAttachedToWindow()||!sliderName(slider.getClass().getName()))return false;
        ViewParent parent=slider.getParent();for(int depth=0;depth<8&&parent instanceof View view;depth++,parent=view.getParent()){
            String name=view.getClass().getName();boolean expected=name.equals(OWNER)||(slider.getClass().getName().equals(CCSLIDER)&&name.equals(CCOWNER));
            if(expected)return XposedHelpers.callMethod(view,"getSlider")==slider;
        }return false;
    }
    private static GradientDrawable[] layers(Drawable root){
        if(!(root instanceof LayerDrawable layer)||layer.getNumberOfLayers()!=2)return null;
        Drawable background=layer.findDrawableByLayerId(android.R.id.background),progress=layer.findDrawableByLayerId(android.R.id.progress);
        if(!(background instanceof GradientDrawable track)||!(progress instanceof ClipDrawable clip)||!(clip.getDrawable() instanceof GradientDrawable fill)||track==fill)return null;
        if(track.getShape()!=GradientDrawable.RECTANGLE||fill.getShape()!=GradientDrawable.RECTANGLE)return null;
        return new GradientDrawable[]{track,fill};
    }
    private static boolean eligible(Entry entry,GradientDrawable drawable){
        if(entry==null)return false;try{SeekBar slider=entry.slider().get();Drawable root=entry.root().get();if(!owned(slider)||root==null||slider.getProgressDrawable()!=root)return false;GradientDrawable[] parts=layers(root);return parts!=null&&(parts[0]==drawable||parts[1]==drawable);}catch(Throwable unavailable){return false;}
    }
    private static void track(SeekBar slider){
        leaves.entrySet().removeIf(e->e.getValue().slider().get()==null||e.getValue().slider().get()==slider);
        if(!owned(slider)){lastRoot="owner excluded: "+slider.getClass().getSimpleName();return;}Drawable root=slider.getProgressDrawable();lastRoot=root==null?"null":root.getClass().getSimpleName();GradientDrawable[] parts=layers(root);if(parts==null)return;
        Entry entry=new Entry(new WeakReference<>(slider),new WeakReference<>(root));for(GradientDrawable part:parts){Entry other=leaves.get(part);if(other!=null&&other.slider().get()!=slider)throw new IllegalStateException("Shared brightness drawable");leaves.put(part,entry);}
    }
    private static void invalidate(){Set<SeekBar> views=new HashSet<>(observed.keySet());for(Entry entry:new ArrayList<>(leaves.values())){SeekBar view=entry.slider().get();if(view!=null)views.add(view);}for(SeekBar view:views)try{if(owned(view))view.invalidate();}catch(Throwable unavailable){failed=true;}}
    static boolean end(){generation++;active=false;invalidate();leaves.clear();boolean removed=true;for(var hook:new ArrayList<>(hooks))try{hook.unhook();hooks.remove(hook);}catch(Throwable unavailable){removed=false;failed=true;}return removed;}
    static boolean healthy(){return !failed;}
    static String status(){return "Brightness style: "+selection+"; tracked layers: "+leaves.size()+"; observed sliders: "+observed.size()+"; decorated draws: "+decoratedDraws+"; draw callbacks: "+drawCallbacks+"; last root: "+lastRoot+(failed?"; styling stopped.":".");}
}
