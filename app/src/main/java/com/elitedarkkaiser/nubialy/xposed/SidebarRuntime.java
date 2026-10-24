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
    private boolean visible,busy,dirty=true;
    private Set<String> lastPackages=Set.of();
    private volatile long epoch;
    private long deadline;
    private final Map<View,Snapshot> views=new WeakHashMap<>();
    private final Map<Object,Drawable> images=new IdentityHashMap<>();
    private final Set<Object> added=Collections.newSetFromMap(new IdentityHashMap<>());
    private Map<String,Entry> entries=Map.of();
    private static final String[] APP_LISTS={"mList","mEditList","mCtlEditAdapterList","mOriginEditList","mRecentList"};
    private static final String[] ADAPTERS={"mListAdapter","mEditListAdapter"};
    private final Runnable poll=()->read();
    private final Runnable expire=()->{epoch++;busy=false;revoke();};
    private record Entry(String label,Drawable icon,Intent launch){}
    private record Read(SidebarStyle style,long deadline,Map<String,Entry> entries){}
    private static final class Snapshot {
        final Drawable background;final int width,height;final android.content.res.ColorStateList color;final float sx,sy;
        Snapshot(View v){background=v.getBackground();ViewGroup.LayoutParams p=v.getLayoutParams();width=p==null?0:p.width;height=p==null?0:p.height;color=v instanceof TextView t?t.getTextColors():null;sx=v.getScaleX();sy=v.getScaleY();}
        void restore(View v){v.setBackground(background);ViewGroup.LayoutParams p=v.getLayoutParams();if(p!=null){p.width=width;p.height=height;v.setLayoutParams(p);}if(v instanceof TextView t)t.setTextColor(color);v.setScaleX(sx);v.setScaleY(sy);}
    }
    private SidebarRuntime(Application a){app=a;}
    static void install(Application a){SidebarRuntime r=new SidebarRuntime(a);r.worker.execute(()->{boolean match=r.matches();r.main.post(()->{if(match)r.hooks();else XposedBridge.log("nubia-ly: sidebar APK mismatch; stock retained");});});}
    private boolean matches(){return SidebarCompatibility.supported(app);}
    private void hooks(){try{Class<?> ctl=XposedHelpers.findClass("com.zte.floatassist.Controller",app.getClassLoader());
        for(String field:new String[]{"mCtlListView","mEditLay","mLLSmallBar","mList","mListAdapter","mGridLayout"})XposedHelpers.findField(ctl,field);
        XposedBridge.hookAllMethods(ctl,"showInner",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(p.hasThrowable())return;epoch++;busy=false;revoke();controller=p.thisObject;visible=true;read();}});
        XposedBridge.hookAllMethods(ctl,"hideInner",new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){visible=false;epoch++;busy=false;revoke();}});
        for(String method:new String[]{"initListAdapter","initEditListAdapter","showEditInitListView","showAllApps","resRefresh","updateCtlLay"})XposedBridge.hookAllMethods(ctl,method,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!p.hasThrowable()&&active()){controller=p.thisObject;dirty=true;main.removeCallbacks(poll);main.post(poll);}}});
        XposedBridge.hookAllMethods(ctl,"setBottomBlurBg",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!p.hasThrowable()&&active()&&p.args.length==1&&p.args[0] instanceof View v){if(!(v.getBackground() instanceof SidebarSurfaceDrawable))views.put(v,new Snapshot(v));if(v==fieldView("mCtlListView")||v==fieldView("mEditLay"))background(v);else if(v==fieldView("mLLSmallBar"))v.setBackground(null);}}});
        XposedBridge.hookAllMethods(ctl,"getAppIcon",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(active()&&p.args.length==2&&p.args[1] instanceof Integer user&&user==userId()){Entry e=entries.get(p.args[0]);if(e!=null&&e.icon()!=null)p.setResult(e.icon().getConstantState()==null?e.icon():e.icon().getConstantState().newDrawable(app.getResources()));}}});
        Class<?> adapter=XposedHelpers.findClass("com.zte.floatassist.Controller$FloatingControllerAdapter",app.getClassLoader());
        XposedBridge.hookAllMethods(adapter,"onBindViewHolder",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(p.hasThrowable()||!active()||p.args.length!=2)return;try{View item=(View)XposedHelpers.getObjectField(p.args[0],"itemView");decorate(item);Object list=XposedHelpers.getObjectField(p.thisObject,"mAdapterList");int position=(Integer)p.args[1];if(list instanceof List<?> l&&position>=0&&position<l.size()&&added.contains(l.get(position))){String pkg=(String)XposedHelpers.callMethod(l.get(position),"getPkgName");Entry entry=entries.get(pkg);if(entry!=null)item.setOnClickListener(v->{try{app.startActivity(new Intent(entry.launch()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));XposedHelpers.callMethod(controller,"hide");}catch(Throwable failure){Toast.makeText(app,"App could not be opened",Toast.LENGTH_SHORT).show();}});}}catch(Throwable ignored){}}});
        Class<?> editAdapter=XposedHelpers.findClass("com.zte.floatassist.Controller$FloatingCtlEditAdapter",app.getClassLoader());
        XposedBridge.hookAllMethods(editAdapter,"onBindViewHolder",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!p.hasThrowable()&&active()&&p.args.length>=2)try{decorate((View)XposedHelpers.getObjectField(p.args[0],"itemView"));}catch(Throwable ignored){}}});
        app.registerReceiver(new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){visible=false;epoch++;busy=false;revoke();}},new IntentFilter(Intent.ACTION_SCREEN_OFF),Context.RECEIVER_NOT_EXPORTED);
        XposedBridge.log("nubia-ly: exact sidebar adapter installed; explicit Apply required");
    }catch(Throwable e){revoke();XposedBridge.log("nubia-ly: sidebar hooks unavailable; stock retained");}}
    private boolean active(){return visible&&style!=null&&SystemClock.elapsedRealtime()<deadline;}
    private void read(){if(!visible||busy||controller==null)return;busy=true;long request=++epoch;Set<String> packages=new LinkedHashSet<>();for(Object data:appData())try{packages.add((String)XposedHelpers.callMethod(data,"getPkgName"));}catch(Throwable ignored){}
        SidebarStyle cachedStyle=style;Map<String,Entry> cachedEntries=entries;Set<String> cachedPackages=lastPackages;boolean rebuild=dirty;
        Runnable timeout=()->{if(epoch==request){epoch++;busy=false;revoke();}};main.postDelayed(timeout,6500);
        worker.execute(()->{Read result=null;try{String nonce=UUID.randomUUID().toString();Bundle in=new Bundle();in.putString("nonce",nonce);in.putInt("version",BuildConfig.VERSION_CODE);Bundle out=app.getContentResolver().call(android.net.Uri.parse("content://"+GuardBrokerPolicy.AUTHORITY),SidebarStyle.METHOD,null,in);long until=out==null?0:out.getLong("deadline_ms");if(out!=null&&nonce.equals(out.getString("nonce"))&&out.getInt("version")==BuildConfig.VERSION_CODE&&out.getInt("status")==GuardBrokerPolicy.BUSY)result=new Read(null,-1,Map.of());if(out!=null&&nonce.equals(out.getString("nonce"))&&out.getInt("version")==BuildConfig.VERSION_CODE&&out.getInt("status")==GuardBrokerPolicy.READABLE&&PreviewPolicy.uuid(out.getString("permit_id"))&&until>SystemClock.elapsedRealtime()&&until-SystemClock.elapsedRealtime()<=ClockEnabledPolicy.HEALTH_MS){SidebarStyle next=SidebarStyle.decode(out.getString("sidebar_style"));if(next!=null){packages.addAll(next.apps());if(!rebuild&&next.equals(cachedStyle)&&packages.equals(cachedPackages)){result=new Read(next,until,cachedEntries);}else{Map<String,Entry> loaded=new HashMap<>();SidebarIconPack pack=null;try{if(!next.iconPack().isEmpty())pack=SidebarIconPack.load(app,next.iconPack());}catch(Exception ignored){}var pm=app.getPackageManager();for(String pkg:packages){if(request!=epoch)break;Intent launch=pm.getLaunchIntentForPackage(pkg);if(launch==null||launch.getComponent()==null)continue;try{var info=pm.getApplicationInfo(pkg,0);Drawable icon=pack==null?null:pack.icon(launch.getComponent(),pm.getApplicationIcon(info));if(icon==null&&next.apps().contains(pkg))icon=pm.getApplicationIcon(info);loaded.put(pkg,new Entry(pm.getApplicationLabel(info).toString(),icon,launch));}catch(Exception ignored){}}result=new Read(next,until,Map.copyOf(loaded));}}}}catch(Throwable ignored){}Read answer=result;main.post(()->{main.removeCallbacks(timeout);if(request!=epoch)return;busy=false;if(answer!=null&&answer.deadline()==-1&&visible){main.removeCallbacks(poll);main.postDelayed(poll,1000);return;}if(answer==null||!visible){revoke();return;}boolean changed=dirty||!answer.style().equals(style)||answer.entries()!=entries;style=answer.style();deadline=answer.deadline();entries=answer.entries();lastPackages=Set.copyOf(packages);dirty=false;try{if(changed)apply();main.removeCallbacks(expire);main.postDelayed(expire,Math.max(0,deadline-SystemClock.elapsedRealtime()));main.removeCallbacks(poll);main.postDelayed(poll,3000);}catch(Throwable failure){revoke();XposedBridge.log("nubia-ly: sidebar appearance failed; stock restored");}});});
    }
    private void remember(View v){views.computeIfAbsent(v,Snapshot::new);}
    private void decorate(View v){remember(v);if(v instanceof TextView t)t.setTextColor(style.text());if(v instanceof ImageView){v.setScaleX(style.iconSize()/100f);v.setScaleY(style.iconSize()/100f);}if(v instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)decorate(group.getChildAt(i));}
    private int userId(){return ((Number)XposedHelpers.callMethod(android.os.Process.myUserHandle(),"getIdentifier")).intValue();}
    private int dp(int n){return Math.round(n*app.getResources().getDisplayMetrics().density);}
    private View fieldView(String name){try{return (View)XposedHelpers.getObjectField(controller,name);}catch(Throwable absent){return null;}}
    private java.util.List<Object> appData(){Set<Object> result=Collections.newSetFromMap(new IdentityHashMap<>());if(controller!=null)for(String field:APP_LISTS)try{Object list=XposedHelpers.getObjectField(controller,field);if(list instanceof List<?> l)result.addAll(l);}catch(Throwable ignored){}return new ArrayList<>(result);}
    private void notifyAdapters(){if(controller==null)return;for(String name:ADAPTERS)try{Object adapter=XposedHelpers.getObjectField(controller,name);if(adapter!=null)XposedHelpers.callMethod(adapter,"notifyDataSetChanged");}catch(Throwable ignored){}}
    private void background(View v){if(!active()||v==null)return;remember(v);Drawable blur=null;
        if(style.blur()>0&&v.isAttachedToWindow())try{Object root=XposedHelpers.callMethod(v,"getViewRootImpl");blur=(Drawable)XposedHelpers.callMethod(root,"createBackgroundBlurDrawable");XposedHelpers.callMethod(blur,"setBlurRadius",style.blur());float r=dp(style.corner());XposedHelpers.callMethod(blur,"setCornerRadius",r,r,r,r);}catch(Throwable unavailable){}
        v.setBackground(new SidebarSurfaceDrawable(style,app.getResources().getDisplayMetrics().density,blur,this::active));
    }
    @SuppressWarnings("unchecked") private void apply(){if(!active())return;Object ctl=controller;View panel=(View)XposedHelpers.getObjectField(ctl,"mCtlListView");background(panel);View editor=fieldView("mEditLay");background(editor);if(editor!=null)decorate(editor);View footer=fieldView("mLLSmallBar");if(footer!=null){remember(footer);footer.setBackground(null);}ViewGroup.LayoutParams params=panel.getLayoutParams();Snapshot original=views.get(panel);if(params!=null){params.width=style.width()==0?original.width:Math.min(dp(style.width()),app.getResources().getDisplayMetrics().widthPixels-dp(32));params.height=style.height()==0?original.height:Math.min(dp(style.height()),app.getResources().getDisplayMetrics().heightPixels-dp(120));panel.setLayoutParams(params);}if(style.width()!=0){View listView=fieldView("mListView");if(listView!=null&&listView.getLayoutParams()!=null){remember(listView);ViewGroup.LayoutParams p=listView.getLayoutParams();p.width=ViewGroup.LayoutParams.MATCH_PARENT;listView.setLayoutParams(p);}}decorate(panel);
        Object grid=XposedHelpers.getObjectField(ctl,"mGridLayout");if(grid!=null){if(XposedHelpers.getAdditionalInstanceField(grid,"nubialy_columns")==null)XposedHelpers.setAdditionalInstanceField(grid,"nubialy_columns",XposedHelpers.callMethod(grid,"getSpanCount"));XposedHelpers.callMethod(grid,"setSpanCount",style.columns());}
        List<Object> list=(List<Object>)XposedHelpers.getObjectField(ctl,"mList");list.removeAll(added);added.clear();for(var old:images.entrySet())try{XposedHelpers.callMethod(old.getKey(),"setImage",old.getValue());}catch(Throwable ignored){}images.clear();Set<String> existing=new HashSet<>();for(Object data:list)existing.add((String)XposedHelpers.callMethod(data,"getPkgName"));for(Object data:appData()){String pkg;try{pkg=(String)XposedHelpers.callMethod(data,"getPkgName");if(((Number)XposedHelpers.callMethod(data,"getUserId")).intValue()!=userId())continue;}catch(Throwable unknown){continue;}Entry e=entries.get(pkg);if(e!=null&&e.icon()!=null){if(!images.containsKey(data))images.put(data,(Drawable)XposedHelpers.callMethod(data,"getImage"));XposedHelpers.callMethod(data,"setImage",e.icon());}}
        Class<?> dataClass=XposedHelpers.findClass("com.zte.floatassist.ControllerData",app.getClassLoader());for(String pkg:style.apps()){Entry e=entries.get(pkg);if(e==null||existing.contains(pkg))continue;Object data=XposedHelpers.newInstance(dataClass);XposedHelpers.callMethod(data,"setUserId",userId());XposedHelpers.callMethod(data,"setPkgName",pkg);XposedHelpers.callMethod(data,"setTitle",e.label());XposedHelpers.callMethod(data,"setImage",e.icon());list.add(data);added.add(data);}
        notifyAdapters();
    }
    private void revoke(){main.removeCallbacks(poll);main.removeCallbacks(expire);style=null;deadline=0;for(var e:views.entrySet())try{e.getValue().restore(e.getKey());}catch(Throwable ignored){}views.clear();for(var e:images.entrySet())try{XposedHelpers.callMethod(e.getKey(),"setImage",e.getValue());}catch(Throwable ignored){}images.clear();try{if(controller!=null){Object grid=XposedHelpers.getObjectField(controller,"mGridLayout");if(grid!=null){Object columns=XposedHelpers.getAdditionalInstanceField(grid,"nubialy_columns");if(columns!=null){XposedHelpers.callMethod(grid,"setSpanCount",columns);XposedHelpers.removeAdditionalInstanceField(grid,"nubialy_columns");}}Object list=XposedHelpers.getObjectField(controller,"mList");if(list instanceof List<?> l)l.removeAll(added);notifyAdapters();}}catch(Throwable ignored){}added.clear();entries=Map.of();lastPackages=Set.of();dirty=true;}
}
