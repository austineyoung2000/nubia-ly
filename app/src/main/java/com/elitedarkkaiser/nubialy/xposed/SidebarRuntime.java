package com.elitedarkkaiser.nubialy.xposed;

import android.app.Application;
import android.content.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.elitedarkkaiser.nubialy.*;
import de.robv.android.xposed.*;
import java.util.*;
import java.util.concurrent.*;

/** Exact-APK experimental adapter. No database writes, replacement APK or SystemUI hooks. */
final class SidebarRuntime {
    private final Application app;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private Object controller;
    private SidebarStyle style;
    private boolean visible,busy,dirty=true,screenOff,probeComplete,applying;
    private SidebarStyle cachedStyle;
    private Map<String,Entry> cachedEntries=Map.of();
    private Set<String> cachedPackages=Set.of();
    private final List<java.util.function.Consumer<Boolean>> replies=new ArrayList<>();
    private Runnable deferredShow;
    private final Runnable openTimeout=()->resumeDeferred();
    private Set<String> lastPackages=Set.of();
    private volatile long epoch;
    private long deadline;
    private final Map<View,Snapshot> views=new WeakHashMap<>();
    private final Set<View> attachmentListeners=Collections.newSetFromMap(new WeakHashMap<>());
    private final Map<Object,Drawable> images=new IdentityHashMap<>();
    private final Set<Object> added=Collections.newSetFromMap(new IdentityHashMap<>());
    private Map<String,Entry> entries=Map.of();
    private static final String[] APP_LISTS={"mList","mEditList","mCtlEditAdapterList","mOriginEditList","mRecentList"};
    private static final String[] ADAPTERS={"mListAdapter","mEditListAdapter"};
    private final Runnable poll=()->read();
    private final Runnable expire=()->{epoch++;busy=false;revoke();probeComplete=false;if(!screenOff)read();};
    private record Entry(String label,Drawable icon,Intent launch){}
    private record Read(SidebarStyle style,long deadline,Map<String,Entry> entries){}
    private static final class Snapshot {
        Drawable background;final int width,height;final android.content.res.ColorStateList color;final float sx,sy;
        Snapshot(View v){background=v.getBackground();ViewGroup.LayoutParams p=v.getLayoutParams();width=p==null?0:p.width;height=p==null?0:p.height;color=v instanceof TextView t?t.getTextColors():null;sx=v.getScaleX();sy=v.getScaleY();}
        void restore(View v){v.setBackground(background);ViewGroup.LayoutParams p=v.getLayoutParams();if(p!=null){p.width=width;p.height=height;v.setLayoutParams(p);}if(v instanceof TextView t)t.setTextColor(color);v.setScaleX(sx);v.setScaleY(sy);}
    }
    private SidebarRuntime(Application a){app=a;}
    static void install(Application a){SidebarRuntime r=new SidebarRuntime(a);r.worker.execute(()->{boolean match=r.matches();r.main.post(()->{if(match)r.hooks();else XposedBridge.log("nubia-ly: sidebar APK mismatch; stock retained");});});}
    private boolean matches(){return SidebarCompatibility.supported(app);}
    private void hooks(){try{Class<?> ctl=XposedHelpers.findClass("com.zte.floatassist.Controller",app.getClassLoader());
        for(String field:new String[]{"mCtlListView","mEditLay","mLLSmallBar","mList","mListAdapter","mGridLayout"})XposedHelpers.findField(ctl,field);
        XposedBridge.hookAllConstructors(ctl,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!p.hasThrowable()){controller=p.thisObject;applyCached();}}});
        XposedBridge.hookAllMethods(ctl,"initView",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!p.hasThrowable()){controller=p.thisObject;applyCached();}}});
        XposedBridge.hookAllMethods(ctl,"showInner",new XC_MethodHook(){
            private final Set<MethodHookParam> deferred=Collections.newSetFromMap(new IdentityHashMap<>());
            @Override protected void beforeHookedMethod(MethodHookParam p){controller=p.thisObject;
                if(!active()&&(!probeComplete||cachedStyle!=null)){
                    Object target=p.thisObject;Object[] args=p.args.clone();java.lang.reflect.Member method=p.method;deferred.add(p);p.setResult(null);
                    deferredShow=()->{try{applyCached();XposedBridge.invokeOriginalMethod(method,target,args);onShown(target);}catch(Throwable failure){revoke();}};
                    main.removeCallbacks(openTimeout);main.postDelayed(openTimeout,6500);read();
                }else applyCached();
            }
            @Override protected void afterHookedMethod(MethodHookParam p){if(deferred.remove(p)||p.hasThrowable())return;onShown(p.thisObject);}
        });
        XposedBridge.hookAllMethods(ctl,"hideInner",new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){onHidden();}@Override protected void afterHookedMethod(MethodHookParam p){if(!p.hasThrowable())prepareHiddenSurfaces();}});
        for(String method:new String[]{"initListAdapter","initEditListAdapter","showEditInitListView","showAllApps"})XposedBridge.hookAllMethods(ctl,method,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!p.hasThrowable()&&active()){controller=p.thisObject;dirty=true;applyCached();}}});
        XposedBridge.hookAllMethods(ctl,"updateCtlLay",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!p.hasThrowable()&&active()&&!applying){controller=p.thisObject;applying=true;try{surfaces();}catch(Throwable failure){revoke();}finally{applying=false;}}}});
        XposedBridge.hookAllMethods(ctl,"setBottomBlurBg",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!p.hasThrowable()&&active()&&p.args.length==1&&p.args[0] instanceof View v){if(!(v.getBackground() instanceof SidebarSurfaceDrawable))views.put(v,new Snapshot(v));if(v==fieldView("mCtlListView")||v==fieldView("mEditLay"))background(v);else if(v==fieldView("mLLSmallBar"))v.setBackground(null);}}});
        XposedBridge.hookAllMethods(ctl,"getAppIcon",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(active()&&p.args.length==2&&p.args[1] instanceof Integer user&&user==userId()){Entry e=entries.get(p.args[0]);if(e!=null&&e.icon()!=null)p.setResult(e.icon().getConstantState()==null?e.icon():e.icon().getConstantState().newDrawable(app.getResources()));}}});
        Class<?> adapter=XposedHelpers.findClass("com.zte.floatassist.Controller$FloatingControllerAdapter",app.getClassLoader());
        XposedBridge.hookAllMethods(adapter,"onBindViewHolder",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(p.hasThrowable()||!active()||p.args.length!=2)return;try{View item=(View)XposedHelpers.getObjectField(p.args[0],"itemView");decorate(item);Object list=XposedHelpers.getObjectField(p.thisObject,"mAdapterList");int position=(Integer)p.args[1];if(list instanceof List<?> l&&position>=0&&position<l.size()&&added.contains(l.get(position))){String pkg=(String)XposedHelpers.callMethod(l.get(position),"getPkgName");Entry entry=entries.get(pkg);if(entry!=null)item.setOnClickListener(v->{try{app.startActivity(new Intent(entry.launch()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));XposedHelpers.callMethod(controller,"hide");}catch(Throwable failure){Toast.makeText(app,"App could not be opened",Toast.LENGTH_SHORT).show();}});}}catch(Throwable ignored){}}});
        Class<?> editAdapter=XposedHelpers.findClass("com.zte.floatassist.Controller$FloatingCtlEditAdapter",app.getClassLoader());
        XposedBridge.hookAllMethods(editAdapter,"onBindViewHolder",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!p.hasThrowable()&&active()&&p.args.length>=2)try{decorate((View)XposedHelpers.getObjectField(p.args[0],"itemView"));}catch(Throwable ignored){}}});
        IntentFilter screen=new IntentFilter(Intent.ACTION_SCREEN_OFF);screen.addAction(Intent.ACTION_SCREEN_ON);
        app.registerReceiver(new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){screenOff=Intent.ACTION_SCREEN_OFF.equals(i.getAction());if(screenOff){onHidden();main.removeCallbacks(poll);}else read();}},screen,Context.RECEIVER_NOT_EXPORTED);
        app.registerReceiver(new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){
            String nonce=i.getStringExtra("nonce");if(!isOrderedBroadcast()||!PreviewPolicy.uuid(nonce)||i.getIntExtra("version",-1)!=BuildConfig.VERSION_CODE||!SystemUiGuardProtocol.admitted(i.getLongExtra("request_deadline_ms",0),SystemClock.elapsedRealtime()))return;
            PendingResult result=goAsync();boolean[] done={false};Runnable[] timeout={null};java.util.function.Consumer<Boolean> finish=ready->{if(done[0])return;done[0]=true;main.removeCallbacks(timeout[0]);Bundle out=new Bundle();out.putString("nonce",nonce);out.putInt("version",BuildConfig.VERSION_CODE);out.putBoolean("ready",ready);result.setResultExtras(out);result.setResultCode(SystemUiGuardProtocol.RESPONSE);result.finish();};
            timeout[0]=()->finish.accept(false);main.postDelayed(timeout[0],7500);epoch++;busy=false;read(finish);
        }},new IntentFilter(SidebarStyle.REFRESH_ACTION),SystemUiGuardProtocol.PERMISSION,main,Context.RECEIVER_EXPORTED);
        try{controller=XposedHelpers.getStaticObjectField(ctl,"INSTANCE");}catch(Throwable absent){}
        read();
        XposedBridge.log("nubia-ly: sidebar adapter installed; saved appearance warming before open");
    }catch(Throwable e){revoke();XposedBridge.log("nubia-ly: sidebar hooks unavailable; stock retained");}}
    private boolean active(){return style!=null&&SystemClock.elapsedRealtime()<deadline;}
    private void onShown(Object target){controller=target;visible=true;applyCached();if(!active())read();}
    private void onHidden(){visible=false;deferredShow=null;main.removeCallbacks(openTimeout);}
    private void prepareHiddenSurfaces(){if(controller==null)return;for(String field:new String[]{"mCtlListView","mEditLay"}){View v=fieldView(field);if(v==null)continue;Snapshot original=views.get(v);if(original!=null)original.background=null;if(active())v.setBackground(new SidebarSurfaceDrawable(style,app.getResources().getDisplayMetrics().density,null,this::active));}}
    private void resumeDeferred(){main.removeCallbacks(openTimeout);Runnable show=deferredShow;deferredShow=null;if(show!=null&&!screenOff)show.run();}
    private void applyCached(){if(active()&&controller!=null&&!applying){applying=true;try{apply();dirty=false;}catch(Throwable failure){revoke();XposedBridge.log("nubia-ly: sidebar cached appearance failed; stock restored");}finally{applying=false;}}}
    private void finishReplies(boolean ready){List<java.util.function.Consumer<Boolean>> pending=new ArrayList<>(replies);replies.clear();for(var reply:pending)reply.accept(ready);}
    private void read(){read(null);}
    private void read(java.util.function.Consumer<Boolean> reply){
        if(reply!=null)replies.add(reply);if(busy||screenOff)return;
        busy=true;long request=++epoch;Set<String> packages=new LinkedHashSet<>();for(Object data:appData())try{String pkg=(String)XposedHelpers.callMethod(data,"getPkgName");if(pkg!=null&&!pkg.isEmpty()&&LauncherIconPacks.validPackage(pkg))packages.add(pkg);}catch(Throwable ignored){}
        SidebarStyle cache=cachedStyle;Map<String,Entry> art=cachedEntries;Set<String> known=cachedPackages;
        Runnable timeout=()->{if(epoch==request){epoch++;busy=false;probeComplete=true;revoke();finishReplies(false);resumeDeferred();}};main.postDelayed(timeout,6500);
        worker.execute(()->{
            Read result=null;Set<String> loadedPackages=new LinkedHashSet<>(packages);
            try{
                String nonce=UUID.randomUUID().toString();Bundle input=new Bundle();input.putString("nonce",nonce);input.putInt("version",BuildConfig.VERSION_CODE);
                Bundle out=app.getContentResolver().call(android.net.Uri.parse("content://"+GuardBrokerPolicy.AUTHORITY),SidebarStyle.METHOD,null,input);
                boolean verified=out!=null&&nonce.equals(out.getString("nonce"))&&out.getInt("version",-1)==BuildConfig.VERSION_CODE;
                if(verified&&out.getInt("status",-1)==GuardBrokerPolicy.BUSY)result=new Read(null,-1,Map.of());
                long until=out==null?0:out.getLong("deadline_ms",0);long now=SystemClock.elapsedRealtime();
                if(verified&&out.getInt("status",-1)==GuardBrokerPolicy.READABLE&&PreviewPolicy.uuid(out.getString("permit_id"))&&until>now&&until-now<=ClockEnabledPolicy.HEALTH_MS){
                    SidebarStyle next=SidebarStyle.decode(out.getString("sidebar_style"));
                    if(next==null)result=new Read(null,until,Map.of());
                    else{
                        loadedPackages.addAll(next.apps());
                        if(next.equals(cache)&&known.containsAll(loadedPackages))result=new Read(next,until,art);
                        else{
                            var pm=app.getPackageManager();
                            // Warm all launchable artwork once, before the first drawer opening.
                            if(!next.iconPack().isEmpty())for(var info:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0))if(info.activityInfo!=null&&info.activityInfo.enabled&&info.activityInfo.applicationInfo.enabled)loadedPackages.add(info.activityInfo.packageName);
                            SidebarIconPack pack=null;try{if(!next.iconPack().isEmpty())pack=SidebarIconPack.load(app,next.iconPack());}catch(Exception unavailable){}
                            Map<String,Entry> loaded=new HashMap<>();
                            for(String pkg:loadedPackages){if(request!=epoch)break;Intent launch=pm.getLaunchIntentForPackage(pkg);if(launch==null||launch.getComponent()==null)continue;
                                try{var info=pm.getApplicationInfo(pkg,0);Drawable icon=pack==null?null:pack.icon(launch.getComponent(),pm.getApplicationIcon(info));if(icon==null&&next.apps().contains(pkg))icon=pm.getApplicationIcon(info);loaded.put(pkg,new Entry(pm.getApplicationLabel(info).toString(),icon,launch));}catch(Exception unavailable){}
                            }
                            result=new Read(next,until,Map.copyOf(loaded));
                        }
                    }
                }
            }catch(Throwable unavailable){}
            Read answer=result;main.post(()->{
                main.removeCallbacks(timeout);if(request!=epoch)return;busy=false;
                if(answer!=null&&answer.deadline()==-1){main.removeCallbacks(poll);if(!screenOff)main.postDelayed(poll,1000);return;}
                probeComplete=true;
                if(answer==null||answer.style()==null){revoke();cachedStyle=null;cachedEntries=Map.of();cachedPackages=Set.of();finishReplies(answer!=null);resumeDeferred();return;}
                boolean changed=dirty||!answer.style().equals(style)||answer.entries()!=entries;
                cachedStyle=answer.style();cachedEntries=answer.entries();cachedPackages=Set.copyOf(loadedPackages);
                style=answer.style();entries=answer.entries();deadline=answer.deadline();lastPackages=cachedPackages;
                if(changed)applyCached();main.removeCallbacks(expire);main.postDelayed(expire,Math.max(0,deadline-SystemClock.elapsedRealtime()));
                main.removeCallbacks(poll);if(!screenOff)main.postDelayed(poll,8000);
                finishReplies(active());resumeDeferred();
            });
        });
    }
    private void remember(View v){views.computeIfAbsent(v,Snapshot::new);}
    private void decorate(View v){remember(v);if(v instanceof TextView t&&t.getCurrentTextColor()!=style.text())t.setTextColor(style.text());if(v instanceof ImageView){float scale=style.iconSize()/100f;if(v.getScaleX()!=scale)v.setScaleX(scale);if(v.getScaleY()!=scale)v.setScaleY(scale);}if(v instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)decorate(group.getChildAt(i));}
    private int userId(){return ((Number)XposedHelpers.callMethod(android.os.Process.myUserHandle(),"getIdentifier")).intValue();}
    private int dp(int n){return Math.round(n*app.getResources().getDisplayMetrics().density);}
    private View fieldView(String name){try{return (View)XposedHelpers.getObjectField(controller,name);}catch(Throwable absent){return null;}}
    private java.util.List<Object> appData(){Set<Object> result=Collections.newSetFromMap(new IdentityHashMap<>());if(controller!=null)for(String field:APP_LISTS)try{Object list=XposedHelpers.getObjectField(controller,field);if(list instanceof List<?> l)result.addAll(l);}catch(Throwable ignored){}return new ArrayList<>(result);}
    private void notifyAdapters(){if(controller==null)return;for(String name:ADAPTERS)try{Object adapter=XposedHelpers.getObjectField(controller,name);if(adapter!=null)XposedHelpers.callMethod(adapter,"notifyDataSetChanged");}catch(Throwable ignored){}}
    private void background(View v){if(!active()||v==null)return;if(v.getBackground() instanceof SidebarSurfaceDrawable current&&current.matches(style,visible&&v.isAttachedToWindow()))return;remember(v);
        if(attachmentListeners.add(v))v.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View view){if(active())background(view);}public void onViewDetachedFromWindow(View view){Snapshot original=views.get(view);if(original!=null)original.background=null;if(active())view.setBackground(new SidebarSurfaceDrawable(style,app.getResources().getDisplayMetrics().density,null,SidebarRuntime.this::active));}});
        Drawable blur=null;
        if(style.blur()>0&&visible&&v.isAttachedToWindow())try{Object root=XposedHelpers.callMethod(v,"getViewRootImpl");blur=(Drawable)XposedHelpers.callMethod(root,"createBackgroundBlurDrawable");XposedHelpers.callMethod(blur,"setBlurRadius",style.blur());float r=dp(style.corner());XposedHelpers.callMethod(blur,"setCornerRadius",r,r,r,r);}catch(Throwable unavailable){}
        v.setBackground(new SidebarSurfaceDrawable(style,app.getResources().getDisplayMetrics().density,blur,this::active));
    }
    private void surfaces(){
        if(!active()||controller==null)return;View panel=fieldView("mCtlListView");if(panel==null)return;background(panel);View editor=fieldView("mEditLay");background(editor);if(editor!=null)decorate(editor);View footer=fieldView("mLLSmallBar");if(footer!=null){remember(footer);if(footer.getBackground()!=null)footer.setBackground(null);}
        ViewGroup.LayoutParams params=panel.getLayoutParams();Snapshot original=views.get(panel);
        if(params!=null){int w=style.width()==0?original.width:Math.min(dp(style.width()),app.getResources().getDisplayMetrics().widthPixels-dp(32)),h=style.height()==0?original.height:Math.min(dp(style.height()),app.getResources().getDisplayMetrics().heightPixels-dp(120));if(params.width!=w||params.height!=h){params.width=w;params.height=h;panel.setLayoutParams(params);}}
        if(style.width()!=0){View listView=fieldView("mListView");if(listView!=null&&listView.getLayoutParams()!=null&&listView.getLayoutParams().width!=ViewGroup.LayoutParams.MATCH_PARENT){remember(listView);ViewGroup.LayoutParams p=listView.getLayoutParams();p.width=ViewGroup.LayoutParams.MATCH_PARENT;listView.setLayoutParams(p);}}decorate(panel);
        Object grid=XposedHelpers.getObjectField(controller,"mGridLayout");if(grid!=null){if(XposedHelpers.getAdditionalInstanceField(grid,"nubialy_columns")==null)XposedHelpers.setAdditionalInstanceField(grid,"nubialy_columns",XposedHelpers.callMethod(grid,"getSpanCount"));if(((Number)XposedHelpers.callMethod(grid,"getSpanCount")).intValue()!=style.columns())XposedHelpers.callMethod(grid,"setSpanCount",style.columns());}
    }
    @SuppressWarnings("unchecked") private void apply(){if(!active()||controller==null||fieldView("mCtlListView")==null)return;Object ctl=controller;surfaces();
        List<Object> list=(List<Object>)XposedHelpers.getObjectField(ctl,"mList");list.removeAll(added);added.clear();for(var old:images.entrySet())try{XposedHelpers.callMethod(old.getKey(),"setImage",old.getValue());}catch(Throwable ignored){}images.clear();Set<String> existing=new HashSet<>();for(Object data:list)existing.add((String)XposedHelpers.callMethod(data,"getPkgName"));for(Object data:appData()){String pkg;try{pkg=(String)XposedHelpers.callMethod(data,"getPkgName");if(((Number)XposedHelpers.callMethod(data,"getUserId")).intValue()!=userId())continue;}catch(Throwable unknown){continue;}Entry e=entries.get(pkg);if(e!=null&&e.icon()!=null){if(!images.containsKey(data))images.put(data,(Drawable)XposedHelpers.callMethod(data,"getImage"));XposedHelpers.callMethod(data,"setImage",e.icon());}}
        Class<?> dataClass=XposedHelpers.findClass("com.zte.floatassist.ControllerData",app.getClassLoader());for(String pkg:style.apps()){Entry e=entries.get(pkg);if(e==null||existing.contains(pkg))continue;Object data=XposedHelpers.newInstance(dataClass);XposedHelpers.callMethod(data,"setUserId",userId());XposedHelpers.callMethod(data,"setPkgName",pkg);XposedHelpers.callMethod(data,"setTitle",e.label());XposedHelpers.callMethod(data,"setImage",e.icon());list.add(data);added.add(data);}
        notifyAdapters();
    }
    private void revoke(){main.removeCallbacks(poll);main.removeCallbacks(expire);style=null;deadline=0;for(var e:views.entrySet())try{e.getValue().restore(e.getKey());}catch(Throwable ignored){}views.clear();for(var e:images.entrySet())try{XposedHelpers.callMethod(e.getKey(),"setImage",e.getValue());}catch(Throwable ignored){}images.clear();try{if(controller!=null){Object grid=XposedHelpers.getObjectField(controller,"mGridLayout");if(grid!=null){Object columns=XposedHelpers.getAdditionalInstanceField(grid,"nubialy_columns");if(columns!=null){XposedHelpers.callMethod(grid,"setSpanCount",columns);XposedHelpers.removeAdditionalInstanceField(grid,"nubialy_columns");}}Object list=XposedHelpers.getObjectField(controller,"mList");if(list instanceof List<?> l)l.removeAll(added);notifyAdapters();}}catch(Throwable ignored){}added.clear();entries=Map.of();lastPackages=Set.of();dirty=true;}
}
