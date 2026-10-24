package com.elitedarkkaiser.nubialy.xposed;
import android.os.Looper;
import android.graphics.Typeface;
import android.graphics.Paint;
import android.util.TypedValue;
import com.elitedarkkaiser.nubialy.LockClockFonts;
import android.view.*;
import android.widget.*;
import com.elitedarkkaiser.nubialy.SurfaceStyle;
import com.elitedarkkaiser.nubialy.StatusBarStyle;
import de.robv.android.xposed.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;
/** Scoped view transforms only. No tile state, lock-screen security, text or layout model writes. */
final class SurfaceBackend {
    // Only verified cleanup may clear a restoration-only failure; apply/hook failures stay latched.
    private static boolean hardFailed;
    private static final String WIDE="com.zte.qs.tileimpl.QSTileViewWide",CIRCLE="com.zte.qs.tileimpl.QSTileViewCircle",LOCK="cn.nubia.lockscreen.NubiaKeyguardStatusView";
    private static final String CLIP="com.zte.mifavor.keyguard.settings.LockScreenClockClip";
    private static final Set<String> observedClockTypes=new TreeSet<>();
    private static final String LIVE="com.zte.mifavor.keyguard.KeyguardStatusView",LIVE_AOSP="com.android.keyguard.KeyguardStatusView",ITEM="com.zte.mifavor.keyguard.settings.LockScreenClockStyleItem",CONTENT="com.zte.mifavor.keyguard.personalclock.ClockContentView",BASE="com.zte.mifavor.keyguard.settings.BaseLockScreenClock",ART="com.zte.mifavor.keyguard.settings.LockScreenClockArtword",DEFAULT="com.zte.mifavor.keyguard.settings.LockScreenClockDefault";
    private static final String WIDGET="com.zte.feature.keyguardwidget.";
    private static final String[] WIDGET_TYPES={"WeatherWidgetViewTypeOne","WeatherWidgetViewTypeTwo","WeatherWidgetViewTypeThree","AlarmWidgetViewTypeOne","AlarmWidgetViewTypeTwo","BatteryWidgetView","BatteryWidgetViewTypeTwo"};
    private static int widgetGroup(String name){for(int i=0;i<WIDGET_TYPES.length;i++)if(name.equals(WIDGET+WIDGET_TYPES[i]))return i<3?4:i<5?5:6;return -1;}
    private static boolean ownerName(String name){return widgetGroup(name)>=0||name.equals(WIDE)||name.equals(CIRCLE)||name.equals(LOCK)||name.equals(ART)||name.equals(DEFAULT)||name.equals(CLIP)||name.equals(CONTENT)||name.equals(liveHost)||name.equals(ITEM);}
    private static final Map<ViewGroup,Boolean> roots=new WeakHashMap<>();
    private static final Map<View,State> states=new WeakHashMap<>();
    private static final List<XC_MethodHook.Unhook> hooks=new ArrayList<>();
    private static String restorationFailures="none";private static boolean attempted,ready,active,writing,liveKeyguard,liveViaAdapt;private static String liveHost;private static volatile boolean failed;private static long generation;
    private static SurfaceStyle style=SurfaceStyle.STOCK;private static int captured,restored;private static String message="Surface observers unavailable.";
    private record Target(ViewGroup owner,int group){}
    private record Frame(long generation,State state){}
    private static final class State {
        final WeakReference<ViewGroup> owner;final int group,scale,opacity,position;final boolean hide;
        float x,y,a,tx,ty,r;int visibility,fontDepth;Typeface fontStock,fontSelected;float textSize,fontFittedSize;boolean fontPadding;int fontAreaWidth,fontAreaHeight;
        State(View v,Target target){owner=new WeakReference<>(target.owner());group=target.group();int appearance=style.appearance(group);scale=StatusBarStyle.indicatorScale(appearance);opacity=StatusBarStyle.indicatorOpacity(appearance);position=style.position(group);hide=style.hide(group);
            x=v.getScaleX();y=v.getScaleY();a=v.getAlpha();tx=v.getTranslationX();ty=v.getTranslationY();r=v.getRotation();visibility=v.getVisibility();if(group==2&&v instanceof TextView text&&style.clockFont()!=0){fontStock=text.getTypeface();textSize=text.getTextSize();fontPadding=text.getIncludeFontPadding();var face=LockClockFonts.FACES[style.clockFont()];fontSelected=Typeface.create(face.family(),face.style());}}
    }
    /**
     * The live lock-screen clock host differs by firmware. On NX809J (RM11) the ZTE
     * host com.zte.mifavor.keyguard.KeyguardStatusView carries mClockLayout directly.
     * On NX789J (RM10, Android 15) the host is AOSP com.android.keyguard.KeyguardStatusView
     * and mClockLayout lives on its adapter (getAdapt().mClockLayout). Same clock item
     * underneath; only the hop to reach it changes. Sets liveHost and liveViaAdapt.
     */
    private static Class<?> resolveLiveHost(ClassLoader loader,Class<?> item) throws Exception {
        try{
            Class<?> live=XposedHelpers.findClass(LIVE,loader);
            if(live.getSuperclass()==GridLayout.class&&live.getDeclaredField("mClockLayout").getType()==item&&live.getDeclaredField("mDarkAmount").getType()==float.class){liveHost=LIVE;liveViaAdapt=false;return live;}
        }catch(Throwable rm11Absent){/* fall through to the adapter-based host */}
        Class<?> live=XposedHelpers.findClass(LIVE_AOSP,loader);
        Class<?> adapt=live.getDeclaredMethod("getAdapt").getReturnType();
        if(live.getSuperclass()!=GridLayout.class||live.getDeclaredField("mDarkAmount").getType()!=float.class||adapt.getDeclaredField("mClockLayout").getType()!=item)throw new IllegalStateException("Live keyguard signatures");
        liveHost=LIVE_AOSP;liveViaAdapt=true;return live;
    }
    /** mClockLayout for a live host instance: direct field (RM11) or via getAdapt() (RM10). */
    private static Object liveClockLayout(Object view){return liveViaAdapt?XposedHelpers.getObjectField(XposedHelpers.callMethod(view,"getAdapt"),"mClockLayout"):XposedHelpers.getObjectField(view,"mClockLayout");}
    static synchronized void install(ClassLoader loader){QsAppearanceBackend.install(loader);
        if(attempted)return;attempted=true;List<XC_MethodHook.Unhook> observers=new ArrayList<>();
        String stage="class lookup";
        try{
            Class<?> wide=XposedHelpers.findClass(WIDE,loader),circle=XposedHelpers.findClass(CIRCLE,loader),lock=XposedHelpers.findClass(LOCK,loader);
            stage="circle background getter";
            if(circle.getMethod("getColorBackgroundDrawable").getReturnType()!=android.graphics.drawable.GradientDrawable.class||wide.getMethod("getColorBackgroundDrawable").getReturnType()!=android.graphics.drawable.GradientDrawable.class)throw new IllegalStateException("Circle background signature");
            stage="tile label descriptors";
            for(Class<?> tile:new Class<?>[]{wide,circle})if(!tile.getSuperclass().getName().equals("com.zte.qs.tileimpl.QSTileViewBase")||!hasTextGetter(tile,"getLabel")||!hasTextGetter(tile,"getSecondaryLabel"))throw new IllegalStateException("Tile signatures");
            stage="keyguard fields";
            if(lock.getSuperclass()!=GridLayout.class||lock.getDeclaredField("mClockView").getType()!=TextClock.class||!TextView.class.isAssignableFrom(lock.getDeclaredField("mDateView").getType())||lock.getDeclaredField("mDarkAmount").getType()!=float.class||lock.getDeclaredField("mPulsing").getType()!=boolean.class)throw new IllegalStateException("Keyguard signatures");
            stage="personal clock fields";
            Class<?> content=XposedHelpers.findClass(CONTENT,loader),base=XposedHelpers.findClass(BASE,loader),art=XposedHelpers.findClass(ART,loader),normal=XposedHelpers.findClass(DEFAULT,loader);
            if(content.getSuperclass()!=LinearLayout.class||content.getDeclaredField("mScence").getType()!=int.class||content.getDeclaredField("SCENE_NORMAL").getType()!=int.class||art.getSuperclass()!=base||normal.getSuperclass()!=base||!TextView.class.isAssignableFrom(art.getDeclaredField("mDateView").getType())||!TextView.class.isAssignableFrom(normal.getDeclaredField("mDateView").getType())||!TextView.class.isAssignableFrom(normal.getDeclaredField("mClockView").getType()))throw new IllegalStateException("Personal clock signatures");
            for(String field:new String[]{"mImageView1","mImageView2","mImageView3","mImageView4"})if(art.getDeclaredField(field).getType()!=ImageView.class)throw new IllegalStateException("Art clock digit signature");
            Class<?> clip=XposedHelpers.findClass(CLIP,loader);
            if(clip.getSuperclass()!=base||!TextView.class.isAssignableFrom(clip.getDeclaredField("mDateView").getType()))throw new IllegalStateException("Clip date signature");
            for(String field:new String[]{"mClockHourView","mClockMinuteView","mClockMinuteMaskView"})if(!TextView.class.isAssignableFrom(clip.getDeclaredField(field).getType()))throw new IllegalStateException("Clip clock signature");
            stage="live keyguard host fields";
            // resolveLiveHost handles both firmwares: NX809J's ZTE host (direct
            // mClockLayout) and NX789J's AOSP KeyguardStatusView (mClockLayout via
            // getAdapt()). If neither resolves, skip only the live-clock sub-feature;
            // every other surface still installs and applies.
            List<Class<?>> lifecycleOwners=new ArrayList<>(Arrays.asList(wide,circle,lock,content,art,normal,clip));
            try{
                Class<?> item=XposedHelpers.findClass(ITEM,loader);
                if(item.getSuperclass()!=LinearLayout.class||item.getDeclaredField("mScene").getType()!=int.class||!item.getDeclaredField("mClock").getType().getName().equals("com.zte.mifavor.keyguard.settings.LockScreenClockStyleInterface"))throw new IllegalStateException("Live clock item signature");
                Class<?> live=resolveLiveHost(loader,item);
                lifecycleOwners.add(live);lifecycleOwners.add(item);liveKeyguard=true;
            }catch(Throwable liveUnavailable){liveKeyguard=false;liveHost=null;}
            for(String name:WIDGET_TYPES){Class<?> widget=XposedHelpers.findClass(WIDGET+name,loader);if(!LinearLayout.class.isAssignableFrom(widget))throw new IllegalStateException("Widget signature");lifecycleOwners.add(widget);}
            stage="view lifecycle hooks";
            observers.add(XposedBridge.hookMethod(View.class.getDeclaredMethod("onAttachedToWindow"),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(ready&&!p.hasThrowable()&&main()){try{QsAppearanceBackend.observe((View)p.thisObject);NotificationIconBackend.observe((View)p.thisObject);ConnectivityBackend.observe((View)p.thisObject);SystemIconPackBackend.observe((View)p.thisObject);NotificationBackend.observe((View)p.thisObject);BrightnessBackend.observe((View)p.thisObject);target((View)p.thisObject);if(active)track((View)p.thisObject);}catch(Throwable unavailable){if(active)fail(unavailable);}}}}));
            observers.add(XposedBridge.hookMethod(View.class.getDeclaredMethod("onDetachedFromWindow"),new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){if(!active||!main())return;View view=(View)p.thisObject;NotificationBackend.release(view);BrightnessBackend.release(view);restore(view);if(view instanceof ViewGroup root){QsShapeBackend.release(root);QsOutlineBackend.release(root);for(View v:new ArrayList<>(states.keySet()))if(states.get(v).owner.get()==root)restore(v);roots.remove(root);}}}));
            stage="keyguard pulsing hook";
            observers.add(XposedBridge.hookMethod(lock.getDeclaredMethod("setPulsing",boolean.class),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(active&&main())FingerprintBackend.refresh();}}));
            // Observe the actual lifecycle implementations as vendor overrides need not
            // dispatch through View.onAttachedToWindow. Inflation also precedes attachment.
            XC_MethodHook discover=new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!p.hasThrowable()&&main()&&p.thisObject instanceof ViewGroup root&&(ownerName(root.getClass().getName()))){roots.put(root,true);if(active)refreshOwner(root);}}};
            Set<Method> lifecycleMethods=new HashSet<>();
            for(Class<?> owner:lifecycleOwners)for(String name:new String[]{"onFinishInflate","onAttachedToWindow","onLayout","setDarkAmount","setPulsing","loadStyle","setScene","addClockView","updateClockView"}){
                for(Class<?> type=owner;type!=null;type=type.getSuperclass()){
                    boolean found=false;for(Method method:type.getDeclaredMethods())if(method.getName().equals(name)){if(lifecycleMethods.add(method))observers.add(XposedBridge.hookMethod(method,discover));found=true;}if(found)break;
                }
            }
            QsLayoutBackend.install(loader);QsGridBackend.install(loader);QsGridInspection.install(loader);BatteryControlBackend.install(loader);NotificationIconBackend.install(loader);BatteryStyleBackend.install(loader);ConnectivityBackend.install(loader);SystemIconPackBackend.install(loader);PowerMenuBackend.install(loader);NotificationBehaviorBackend.install(loader);ChargingInfoBackend.install(loader);NotificationBackend.install(loader);VolumeBackend.install(loader);BrightnessBackend.install(loader);FingerprintBackend.install(loader);ready=true;message="Surface observers ready.";
        }catch(Throwable unavailable){for(var h:observers)try{h.unhook();}catch(Throwable ignored){}message="Surface source unavailable at "+stage+" ("+unavailable.getClass().getSimpleName()+").";}
    }
    private static boolean hasTextGetter(Class<?> owner,String name){
        // NX809J publishes both View bridge and TextView getter descriptors. Android's
        // name-only getMethod lookup may return the bridge; require the actual descriptor.
        for(Method method:owner.getMethods())if(method.getName().equals(name)&&method.getParameterCount()==0&&method.getReturnType()==TextView.class)return true;
        return false;
    }
    private static boolean main(){return Looper.myLooper()==Looper.getMainLooper();}
    private static Target target(View view){
        View current=view;
        for(int depth=0;depth<24&&current!=null;depth++){
            String name=current.getClass().getName();
            if(current instanceof ViewGroup root&&ownerName(name)){
                roots.put(root,true);
                if(widgetGroup(name)>=0)return current==view&&liveWidget(root)?new Target(root,widgetGroup(name)):null;
                if(name.equals(CONTENT)||name.equals(ITEM)||name.equals(liveHost))return null;
                if(name.equals(ART)||name.equals(DEFAULT)||name.equals(CLIP)){
                    if(!normalPersonalScene(root))return null;
                    if(XposedHelpers.getObjectField(root,"mDateView")==view)return new Target(root,3);
                    if(name.equals(DEFAULT)&&XposedHelpers.getObjectField(root,"mClockView")==view)return new Target(root,2);
                    if(name.equals(CLIP))for(String field:new String[]{"mClockHourView","mClockMinuteView","mClockMinuteMaskView"})if(XposedHelpers.getObjectField(root,field)==view)return new Target(root,2);
                    if(name.equals(ART))for(String field:new String[]{"mImageView1","mImageView2","mImageView3","mImageView4"})if(XposedHelpers.getObjectField(root,field)==view)return new Target(root,2);
                    return null;
                }
                if(name.equals(LOCK)){
                    if(!Boolean.FALSE.equals(XposedHelpers.getObjectField(root,"mPulsing"))||((Number)XposedHelpers.getObjectField(root,"mDarkAmount")).floatValue()!=0)return null;
                    if(XposedHelpers.getObjectField(root,"mClockView")==view)return new Target(root,2);
                    if(XposedHelpers.getObjectField(root,"mDateView")==view)return new Target(root,3);
                }else{
                    if(XposedHelpers.callMethod(root,"getLabel")==view)return new Target(root,0);
                    if(XposedHelpers.callMethod(root,"getSecondaryLabel")==view)return new Target(root,1);
                }return null;
            }
            ViewParent parent=current.getParent();current=parent instanceof View v?v:null;
        }return null;
    }
    private static boolean liveWidget(ViewGroup widget){
        View current=widget;for(int depth=0;depth<24&&current!=null;depth++){
            if(current.getClass().getName().equals(liveHost))return ((Number)XposedHelpers.getObjectField(current,"mDarkAmount")).floatValue()==0;
            if(current.getClass().getName().equals(CONTENT))return false;
            ViewParent parent=current.getParent();current=parent instanceof View v?v:null;
        }return false;
    }
    private static boolean normalPersonalScene(ViewGroup root){
        View current=root;ViewGroup item=null;
        for(int depth=0;depth<24&&current!=null;depth++){
            String name=current.getClass().getName();
            if(name.equals(ITEM)){
                if(((Number)XposedHelpers.getObjectField(current,"mScene")).intValue()!=1||XposedHelpers.getObjectField(current,"mClock")!=root)return false;
                item=(ViewGroup)current;
            }
            if(name.equals(liveHost))return item!=null&&liveClockLayout(current)==item&&((Number)XposedHelpers.getObjectField(current,"mDarkAmount")).floatValue()==0;
            // ClockContentView belongs to the personal-clock chooser path. Do not
            // infer live ownership from its SCENE_NORMAL flag alone.
            if(name.equals(CONTENT))return false;
            ViewParent parent=current.getParent();current=parent instanceof View v?v:null;
        }
        return false;
    }
    private static String liveClockStatus(){
        int hosts=0;Set<String> types=new TreeSet<>();
        for(ViewGroup root:new ArrayList<>(roots.keySet()))if(root.isAttachedToWindow()&&root.getClass().getName().equals(liveHost)){
            hosts++;Object item=liveClockLayout(root);if(item!=null){Object clock=XposedHelpers.getObjectField(item,"mClock");types.add(clock==null?"not created":clock.getClass().getSimpleName());if(clock!=null&&observedClockTypes.size()<8)observedClockTypes.add(clock.getClass().getSimpleName());}
        }
        return " Live keyguard hosts: "+hosts+"; active clock types: "+(types.isEmpty()?"not observed":String.join(", ",types))+"; observed clock types: "+(observedClockTypes.isEmpty()?"none":String.join(", ",observedClockTypes))+".";
    }
    static void begin(SurfaceStyle selection){
        if(selection==null||!selection.valid()||!main())throw new IllegalArgumentException();if(!end())throw new IllegalStateException("Surface restoration incomplete");style=selection;captured=restored=0;if(!style.any()){message="Stock Quick Settings and lock-screen text appearance.";return;}
        if(!ready||failed)throw new IllegalStateException(message);
        XC_MethodHook callback=new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){if(!active||writing)return;State state=states.get(p.thisObject);if(state==null)return;if(!main()){failed=true;hardFailed=true;return;}if(((Method)p.method).getName().equals("setTypeface"))state.fontDepth++;p.setObjectExtra("nubia-ly:surface",new Frame(generation,state));}
            @Override protected void afterHookedMethod(MethodHookParam p){Object extra=p.getObjectExtra("nubia-ly:surface");if(!(extra instanceof Frame frame)||!active||writing||frame.generation()!=generation||states.get(p.thisObject)!=frame.state())return;View v=(View)p.thisObject;State s=frame.state();
                try{if(((Method)p.method).getName().equals("setTypeface")&&--s.fontDepth>0)return;if(p.hasThrowable())throw new IllegalStateException();switch(((Method)p.method).getName()){case "setTextSize"->s.textSize=((TextView)v).getTextSize();case "setIncludeFontPadding"->s.fontPadding=((TextView)v).getIncludeFontPadding();case "setTypeface"->s.fontStock=((TextView)v).getTypeface();case "setScaleX"->s.x=v.getScaleX();case "setScaleY"->s.y=v.getScaleY();case "setAlpha"->s.a=v.getAlpha();case "setTranslationX"->s.tx=v.getTranslationX();case "setTranslationY"->s.ty=v.getTranslationY();case "setRotation"->s.r=v.getRotation();case "setVisibility"->s.visibility=v.getVisibility();}
                    if(s.fontDepth>0)return;Target t=target(v);if(t==null||t.owner()!=s.owner.get()||t.group()!=s.group)restore(v);else paint(v,s);
                }catch(Throwable unavailable){fail(unavailable);}
            }
        };
        try{for(String property:new String[]{"setScaleX","setScaleY","setAlpha","setTranslationX","setTranslationY","setRotation"})hooks.add(XposedHelpers.findAndHookMethod(View.class,property,float.class,callback));hooks.add(XposedHelpers.findAndHookMethod(View.class,"setVisibility",int.class,callback));hooks.add(XposedHelpers.findAndHookMethod(TextView.class,"setTypeface",Typeface.class,callback));hooks.add(XposedHelpers.findAndHookMethod(TextView.class,"setTypeface",Typeface.class,int.class,callback));hooks.add(XposedHelpers.findAndHookMethod(TextView.class,"setTextSize",int.class,float.class,callback));hooks.add(XposedHelpers.findAndHookMethod(TextView.class,"setIncludeFontPadding",boolean.class,callback));FingerprintBackend.begin(style.fingerprintStyle(),style.fingerprintHidden()!=0);QsLayoutBackend.begin(style.qsTopLayout(),style.qsLabelPosition(),style.qsLabelLines(),style.qsBrightnessPosition(),style.qsHideBrightnessIcons());QsGridBackend.begin(style.qsColumns(),style.qsRows(),style.qsLandscapeColumns(),style.qsLandscapeRows());QsShapeBackend.begin(style.tileShape(),style.largeTileShape());QsOutlineBackend.begin(style.qsStyle());IndicatorColorBackend.begin(style.palette());BrightnessBackend.begin(style.brightnessStyle(),style.palette());VolumeBackend.begin(style.volumeStyle());NotificationBackend.begin(style.notificationStyle(),style.notificationAppearance());BatteryControlBackend.begin(style.batteryPercent(),style.batteryCharge());NotificationIconBackend.begin(style.notificationCount(),style.notificationLimit());BatteryStyleBackend.begin(style.batteryStyle(),style.batteryCharge(),style.palette().battery(),style.palette().batteryText());SystemIconPackBackend.begin(style.iconPack(),style.qsIconPack());ConnectivityBackend.begin(style.wifiStyle(),style.signalStyle(),style.iconPack());PowerMenuBackend.begin(style.powerStyle());NotificationBehaviorBackend.begin(style.notificationBehavior());ChargingInfoBackend.begin(style.chargingInfo());QsAppearanceBackend.begin(style.qsTheme());active=true;refresh();if(failed||!QsLayoutBackend.healthy()||!QsGridBackend.healthy()||!QsShapeBackend.healthy()||!QsOutlineBackend.healthy()||!BrightnessBackend.healthy()||!VolumeBackend.healthy()||!NotificationBackend.healthy()||!BatteryControlBackend.healthy()||!NotificationIconBackend.healthy()||!BatteryStyleBackend.healthy()||!ConnectivityBackend.healthy()||!PowerMenuBackend.healthy()||!NotificationBehaviorBackend.healthy()||!ChargingInfoBackend.healthy()||!FingerprintBackend.healthy())throw new IllegalStateException("Surface apply failed");message="Quick Settings and lock-screen styling active.";}
        catch(Throwable unavailable){end();throw new IllegalStateException("Surface activation unavailable",unavailable);}
    }
    private static void fail(Throwable failure){failed=true;hardFailed=true;message="Surface callback failed ("+failure.getClass().getSimpleName()+"); restoration required.";}
    private static boolean scanningOwner;
    private static void track(View view){
        if(!active||writing||failed||!main()||!view.isAttachedToWindow())return;
        try{Target target=target(view);State s=states.get(view);if(target!=null&&!scanningOwner){QsShapeBackend.track(target.owner());QsOutlineBackend.track(target.owner());}if(target==null){if(s!=null)restore(view);return;}
            if(s!=null&&(s.owner.get()!=target.owner()||s.group!=target.group())){if(!restore(view))return;s=null;}
            if(s==null){if(style.appearance(target.group())==0&&style.position(target.group())==0&&!style.hide(target.group())&&!(target.group()==2&&view instanceof TextView&&style.clockFont()!=0))return;s=new State(view,target);states.put(view,s);captured++;}paint(view,s);
        }catch(Throwable unavailable){fail(unavailable);}
    }
    private static void paint(View v,State s){writing=true;try{
        if(s.scale!=100){v.setScaleX(s.x*s.scale/100f);v.setScaleY(s.y*s.scale/100f);}if(s.opacity!=100)v.setAlpha(s.a*s.opacity/100f);
        if(s.position!=0){float density=v.getResources().getDisplayMetrics().density;if(!Float.isFinite(density)||density<=0||!Float.isFinite(s.tx)||!Float.isFinite(s.ty)||!Float.isFinite(s.r))throw new IllegalStateException();if(StatusBarStyle.horizontal(s.position)!=0)v.setTranslationX(s.tx+StatusBarStyle.horizontal(s.position)*density);if(StatusBarStyle.vertical(s.position)!=0)v.setTranslationY(s.ty+StatusBarStyle.vertical(s.position)*density);if(StatusBarStyle.rotation(s.position)!=0)v.setRotation(s.r+StatusBarStyle.rotation(s.position));}if(s.hide)v.setVisibility(View.GONE);if(s.fontSelected!=null)fitFont((TextView)v,s);
    }finally{writing=false;}}
    private static void fitFont(TextView view,State state){
        // Capture the stock layout allocation before changing its font. Do not
        // chase later animation/layout bounds or compound a previous fitted size.
        if(state.fontAreaHeight==0&&view.getHeight()>0&&view.getWidth()>0){
            int h=view.getHeight()-view.getPaddingTop()-view.getPaddingBottom(),w=view.getWidth()-view.getPaddingLeft()-view.getPaddingRight();
            if(h>0&&w>0){state.fontAreaHeight=h;state.fontAreaWidth=w;}
        }
        if(state.fontAreaHeight<=0||state.fontAreaWidth<=0)return;
        Paint measure=new Paint(view.getPaint());measure.setTypeface(state.fontSelected);measure.setTextSize(state.textSize);
        Paint.FontMetrics metrics=measure.getFontMetrics();float height=metrics.bottom-metrics.top;
        // Reserve the widest numeral for every digit, so minute changes cannot
        // resize the clock. Preserve separators and the vendor's time format.
        float digitWidth=0;for(int digit=0;digit<10;digit++)digitWidth=Math.max(digitWidth,measure.measureText(Integer.toString(digit)));
        float width=0;String text=view.getText().toString();for(int i=0;i<text.length();i++){char c=text.charAt(i);width+=Character.isDigit(c)?digitWidth:measure.measureText(String.valueOf(c));}
        if(!Float.isFinite(height)||height<=0||!Float.isFinite(width))throw new IllegalStateException("Clock font metrics unavailable");
        float factor=Math.min(1,state.fontAreaHeight/height);if(width>0)factor=Math.min(factor,state.fontAreaWidth/width);
        float size=state.textSize*factor;state.fontFittedSize=size;if(!Float.isFinite(size)||size<=0)throw new IllegalStateException("Clock font fit unavailable");
        if(!Objects.equals(view.getTypeface(),state.fontSelected))view.setTypeface(state.fontSelected);
        // Full font padding keeps lower strokes inside the fixed allocation.
        // Only text rendering changes: parent dimensions and transforms stay stock.
        if(!view.getIncludeFontPadding())view.setIncludeFontPadding(true);
        if(!near(view.getTextSize(),size))view.setTextSize(TypedValue.COMPLEX_UNIT_PX,size);
    }
    private static boolean near(float a,float b){return Float.isFinite(a)&&Float.isFinite(b)&&Math.abs(a-b)<0.00001f;}
    private static boolean restore(View v){State s=states.get(v);if(s==null)return true;writing=true;try{
        if(s.scale!=100){v.setScaleX(s.x);v.setScaleY(s.y);if(!near(v.getScaleX(),s.x)||!near(v.getScaleY(),s.y))throw new IllegalStateException();}
        if(s.opacity!=100){v.setAlpha(s.a);if(!near(v.getAlpha(),s.a))throw new IllegalStateException();}
        if(StatusBarStyle.horizontal(s.position)!=0){v.setTranslationX(s.tx);if(!near(v.getTranslationX(),s.tx))throw new IllegalStateException();}
        if(StatusBarStyle.vertical(s.position)!=0){v.setTranslationY(s.ty);if(!near(v.getTranslationY(),s.ty))throw new IllegalStateException();}
        if(StatusBarStyle.rotation(s.position)!=0){v.setRotation(s.r);if(!near(v.getRotation(),s.r))throw new IllegalStateException();}
        if(s.fontSelected!=null){((TextView)v).setTypeface(s.fontStock);((TextView)v).setTextSize(TypedValue.COMPLEX_UNIT_PX,s.textSize);((TextView)v).setIncludeFontPadding(s.fontPadding);if(!near(((TextView)v).getTextSize(),s.textSize)||((TextView)v).getIncludeFontPadding()!=s.fontPadding||!Objects.equals(((TextView)v).getTypeface(),s.fontStock))throw new IllegalStateException();}
        if(s.hide){v.setVisibility(s.visibility);if(v.getVisibility()!=s.visibility)throw new IllegalStateException();}states.remove(v);restored++;return true;
    }catch(Throwable unavailable){failed=true;message="Surface restoration incomplete. Disable nubia-ly in Vector and reboot.";return false;}finally{writing=false;}}
    /** Lifecycle callbacks touch only their owner; never scan unrelated QS/volume/fingerprint state per layout. */
    private static void refreshOwner(ViewGroup root){
        if(!active||writing||failed||!main()||!root.isAttachedToWindow())return;
        QsShapeBackend.track(root);QsOutlineBackend.track(root);
        boolean previous=scanningOwner;scanningOwner=true;try{ArrayDeque<View> queue=new ArrayDeque<>();queue.add(root);int count=0;
        while(!queue.isEmpty()&&count++<128){View v=queue.removeFirst();track(v);if(v instanceof ViewGroup group)for(int i=0;i<group.getChildCount()&&queue.size()<128;i++)queue.add(group.getChildAt(i));}}finally{scanningOwner=previous;}
    }
    static void refresh(){if(!active||failed||!main())return;VolumeBackend.refresh();BrightnessBackend.refresh();FingerprintBackend.refresh();for(View v:new ArrayList<>(states.keySet()))track(v);for(ViewGroup root:new ArrayList<>(roots.keySet()))refreshOwner(root);}
    static boolean end(){boolean tintRestored=IndicatorColorBackend.end();QsAppearanceBackend.end();generation++;active=false;for(View v:new ArrayList<>(states.keySet()))restore(v);for(var h:new ArrayList<>(hooks))try{h.unhook();hooks.remove(h);}catch(Throwable unavailable){failed=true;hardFailed=true;}boolean layoutRestored=QsLayoutBackend.end(),gridRestored=QsGridBackend.end(),batteryControlsRestored=BatteryControlBackend.end(),notificationIconsRestored=NotificationIconBackend.end(),batteryRestored=BatteryStyleBackend.end(),connectivityRestored=ConnectivityBackend.end(),iconPacksRestored=SystemIconPackBackend.end(),powerRestored=PowerMenuBackend.end(),behaviorRestored=NotificationBehaviorBackend.end(),chargingRestored=ChargingInfoBackend.end(),notificationRestored=NotificationBackend.end(),volumeRestored=VolumeBackend.end(),brightnessRestored=BrightnessBackend.end(),outlineRestored=QsOutlineBackend.end(),shapeRestored=QsShapeBackend.end(),fingerprintRestored=FingerprintBackend.end();String blocked="";if(!tintRestored)blocked+=" indicator tint;";if(!layoutRestored)blocked+=" QS layout;";if(!gridRestored)blocked+=" QS grid;";if(!batteryControlsRestored)blocked+=" battery controls;";if(!notificationIconsRestored)blocked+=" notification icons;";if(!batteryRestored)blocked+=" battery style;";if(!connectivityRestored)blocked+=" connectivity;";if(!iconPacksRestored)blocked+=" icon packs;";if(!powerRestored)blocked+=" power menu;";if(!behaviorRestored)blocked+=" notification behavior;";if(!chargingRestored)blocked+=" charging info;";if(!notificationRestored)blocked+=" notifications;";if(!volumeRestored)blocked+=" volume;";if(!brightnessRestored)blocked+=" brightness;";if(!outlineRestored)blocked+=" QS outline;";if(!shapeRestored)blocked+=" QS shape;";if(!fingerprintRestored)blocked+=" fingerprint;";if(failed)blocked+=" surface failure latched;";if(!states.isEmpty())blocked+=" surface states remaining;";restorationFailures=blocked.isEmpty()?"none":blocked;if(!hardFailed&&states.isEmpty()&&hooks.isEmpty())failed=false;return tintRestored&&layoutRestored&&gridRestored&&!failed&&states.isEmpty()&&batteryControlsRestored&&notificationIconsRestored&&batteryRestored&&connectivityRestored&&iconPacksRestored&&powerRestored&&behaviorRestored&&chargingRestored&&notificationRestored&&volumeRestored&&brightnessRestored&&outlineRestored&&shapeRestored&&fingerprintRestored;}
    static boolean healthy(){return IndicatorColorBackend.healthy()&&QsAppearanceBackend.healthy()&&!failed&&QsLayoutBackend.healthy()&&QsGridBackend.healthy()&&BatteryControlBackend.healthy()&&NotificationIconBackend.healthy()&&BatteryStyleBackend.healthy()&&ConnectivityBackend.healthy()&&PowerMenuBackend.healthy()&&NotificationBehaviorBackend.healthy()&&ChargingInfoBackend.healthy()&&NotificationBackend.healthy()&&VolumeBackend.healthy()&&BrightnessBackend.healthy()&&QsOutlineBackend.healthy()&&QsShapeBackend.healthy()&&FingerprintBackend.healthy();}
    static String status(){String fit="";int clock=0,date=0,weather=0,alarm=0,battery=0;for(State s:states.values()){if(s.fontSelected!=null)fit=" Font fit: "+s.fontAreaWidth+"x"+s.fontAreaHeight+" px; vendor size: "+s.textSize+" px; fitted size: "+s.fontFittedSize+" px.";if(s.group==2)clock++;if(s.group==3)date++;if(s.group==4)weather++;if(s.group==5)alarm++;if(s.group==6)battery++;}return IndicatorColorBackend.status()+" "+QsAppearanceBackend.status()+" "+message+" Surface restoration failures: "+restorationFailures+". Widgets captured — weather: "+weather+"; alarm: "+alarm+"; battery: "+battery+"."+liveClockStatus()+" Lock-screen clock captured: "+clock+"; date captured: "+date+"; requested hide clock: "+style.hide(2)+"; requested hide date: "+style.hide(3)+"."+" Surface captures: "+captured+"; restored: "+restored+"; remaining: "+states.size()+". "+QsShapeBackend.status()+" "+QsOutlineBackend.status()+" "+QsLayoutBackend.status()+" "+QsGridBackend.status()+" "+QsGridInspection.status()+" "+BrightnessBackend.status()+" "+VolumeBackend.status()+" "+NotificationBackend.status()+" "+BatteryControlBackend.status()+" "+NotificationIconBackend.status()+" "+BatteryStyleBackend.status()+" "+ConnectivityBackend.status()+" "+SystemIconPackBackend.status()+" "+PowerMenuBackend.status()+" "+NotificationBehaviorBackend.status()+" "+ChargingInfoBackend.status()+" "+FingerprintBackend.status()+" Lock-screen font: "+LockClockFonts.FACES[style.clockFont()].name()+" (text clock views only)."+fit;}
}
