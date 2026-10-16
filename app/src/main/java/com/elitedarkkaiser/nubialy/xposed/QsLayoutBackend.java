package com.elitedarkkaiser.nubialy.xposed;

import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import de.robv.android.xposed.*;
import java.lang.reflect.Method;
import java.util.*;

/** Native shade-only layout ownership. No host settings, tile dimensions or actions are changed. */
final class QsLayoutBackend {
    private static final String QS="com.zte.mifavor.qs.",CONTENT=QS+"MfvQSContent",PANEL=QS+"MfvQSPanel",QUICK=QS+"MfvQuickQSPanel",HEADER=QUICK+"$HeaderTileLayout",CIRCLE="com.zte.qs.tileimpl.QSTileViewCircle";
    private static final Map<ViewGroup,Boolean> roots=new WeakHashMap<>();
    private static final Map<ViewGroup,Integer> appliedGeneration=new WeakHashMap<>();
    private static String lastApply="not applied";
    private static final Set<ViewGroup> changedRoots=Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<ViewGroup> pending=Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<View,LabelState> labels=new WeakHashMap<>();
    private static final Map<View,Integer> visibility=new WeakHashMap<>();
    private static final Map<View,Integer> heights=new WeakHashMap<>();
    private static final Map<View,Integer> margins=new WeakHashMap<>();
    private static final Map<View,Integer> marginDeltas=new WeakHashMap<>();
    private static Class<?> dimensions;
    private static final ThreadLocal<ViewGroup> scope=new ThreadLocal<>();
    private static boolean attempted,ready,active,failed,writing,rebuilding;
    private static int top,labelPosition,labelLines,brightness,generation;
    private static boolean hideIcons;
    private static final class LabelState {
        final ViewGroup container,parent;final TextView text;final int index;int maxLines;boolean moved;
        LabelState(ViewGroup c,ViewGroup p,TextView t){container=c;parent=p;text=t;index=p.indexOfChild(c);maxLines=t.getMaxLines();}
    }
    private static boolean main(){return Looper.myLooper()==Looper.getMainLooper();}
    private static ViewGroup root(Object object){
        if(!(object instanceof View v))return null;
        for(int i=0;v!=null&&i<24;i++){if(v.getClass().getName().equals(CONTENT)&&v instanceof ViewGroup g)return g;v=v.getParent() instanceof View next?next:null;}return null;
    }
    private static ViewGroup descendantRoot(View v,int depth){if(depth>12)return null;if(v.getClass().getName().equals(CONTENT)&&v instanceof ViewGroup r)return r;if(v instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++){ViewGroup r=descendantRoot(g.getChildAt(i),depth+1);if(r!=null)return r;}return null;}
    private static ViewGroup contextRoot(Object object){ViewGroup r=root(object);if(r!=null)return r;if(object instanceof View v&&v.getClass().getName().equals(QS+"MfvQSContainerImpl"))return descendantRoot(v,0);try{Object v=field(object,"mRootView");if(v instanceof View view)return descendantRoot(view,0);}catch(Throwable ignored){}try{return root(field(object,"mPagedLayout"));}catch(Throwable ignored){}return null;}
    private static boolean usable(ViewGroup r){return r!=null&&r.isAttachedToWindow()&&r.getResources().getConfiguration().orientation==1&&r.getResources().getConfiguration().fontScale<=1.3f;}
    private static boolean custom(ViewGroup r){return active&&main()&&usable(r);}
    private static View find(ViewGroup r,String name){int id=r.getResources().getIdentifier(name,"id","com.android.systemui");return id==0?null:r.findViewById(id);}
    private static ViewGroup panel(ViewGroup r,boolean quick){View v=find(r,quick?"quick_qs_panel":"quick_settings_panel");return v instanceof ViewGroup g&&v.getClass().getName().equals(quick?QUICK:PANEL)&&root(v)==r?g:null;}
    private static View direct(ViewGroup r,View v){for(int i=0;v!=null&&i<24;i++){if(v.getParent()==r)return v;v=v.getParent() instanceof View next?next:null;}return null;}
    private static int topCount(){return top==1?2:top==2?6:4;}
    private static int topRows(){return top==1?1:top==2?3:4;}
    private static Object field(Object o,String name){return XposedHelpers.getObjectField(o,name);}
    static void install(ClassLoader loader){if(attempted)return;attempted=true;List<XC_MethodHook.Unhook> hooks=new ArrayList<>();try{
        dimensions=XposedHelpers.findClass("com.zte.utils.QsDimenUtils",loader);
        Class<?> content=XposedHelpers.findClass(CONTENT,loader),quick=XposedHelpers.findClass(QUICK,loader),normal=XposedHelpers.findClass(PANEL,loader),header=XposedHelpers.findClass(HEADER,loader),circle=XposedHelpers.findClass(CIRCLE,loader);
        if(content.getSuperclass()!=FrameLayout.class||header.getSuperclass()!=XposedHelpers.findClass(QS+"MfvTileLayout",loader))throw new IllegalStateException("Shade source mismatch");
        XC_MethodHook context=new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){if(!main())return;p.setObjectExtra("nubia-ly:layout-scope",scope.get());ViewGroup r=contextRoot(p.thisObject);scope.set(r);}
            @Override protected void afterHookedMethod(MethodHookParam p){if(main())scope.set((ViewGroup)p.getObjectExtra("nubia-ly:layout-scope"));}};
        // Only calls originating in the inspected normal shade can affect shared dimension getters.
        for(Class<?> c:new Class<?>[]{normal,quick,header,XposedHelpers.findClass(QS+"MfvQSContainerImpl",loader),XposedHelpers.findClass(QS+"MfvQSAnimator",loader),XposedHelpers.findClass(QS+"MfvQSFragment",loader)})
            for(Method m:c.getDeclaredMethods())if(Set.of("onMeasure","onLayout","updateResources","setTiles","setMaxTiles","onConfigurationChanged","updateAnimators","updateViews","setPosition","setQsExpansion").contains(m.getName()))hooks.add(XposedBridge.hookMethod(m,context));
        for(String name:new String[]{"com.zte.utils.QsDimenUtils","com.zte.utils.QsDimenUtils$Companion"})for(String getter:new String[]{"getQuickTileNums","getQuickTileRows"}){
            final boolean count=getter.equals("getQuickTileNums");hooks.add(XposedBridge.hookMethod(XposedHelpers.findClass(name,loader).getDeclaredMethod(getter),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!p.hasThrowable()&&top!=0&&custom(scope.get()))p.setResult(count?topCount():topRows());}}));
        }
        hooks.add(XposedBridge.hookMethod(XposedHelpers.findClass("com.zte.feature.qs.QSPanelLayoutFeature",loader).getDeclaredMethod("getZteQuickTilesNum"),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!p.hasThrowable()&&top!=0&&custom(scope.get()))p.setResult(topCount());}}));
        Class<?> adapter=XposedHelpers.findClass("com.zte.adapt.mifavor.qs.MfvQSPanelAdapt",loader);
        hooks.add(XposedBridge.hookMethod(adapter.getDeclaredMethod("getQQSMaxTilesCount",int.class),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{Object owner=field(p.thisObject,"mOwner");if(!p.hasThrowable()&&top!=0&&owner instanceof View v&&v.getClass().getName().equals(QUICK)&&custom(root(v)))p.setResult(topCount());}catch(Throwable error){failed=true;}}}));
        hooks.add(XposedBridge.hookMethod(adapter.getDeclaredMethod("getQSPanelTiles",Collection.class),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{Object owner=field(p.thisObject,"mOwner");if(!p.hasThrowable()&&top!=0&&owner instanceof View v&&v.getClass().getName().equals(PANEL)&&custom(root(v)))p.setResult(remaining((Collection<?>)p.args[0],topCount()));}catch(Throwable error){failed=true;}}}));
        Class<?> layoutAdapter=XposedHelpers.findClass("com.zte.adapt.mifavor.qs.MfvTileLayoutAdapt",loader);
        for(String method:new String[]{"getQuickTileCount","getQuickTileRows","getQuickTilesColumns"}){final String key=method;hooks.add(XposedBridge.hookMethod(layoutAdapter.getDeclaredMethod(method,int.class),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{Object owner=field(p.thisObject,"mOwner");if(!p.hasThrowable()&&top!=0&&owner instanceof View v&&v.getClass().getName().equals(HEADER)&&custom(root(v)))p.setResult(key.equals("getQuickTileCount")?topCount():key.equals("getQuickTileRows")?topRows():topCount()/topRows());}catch(Throwable error){failed=true;}}}));}
        hooks.add(XposedBridge.hookMethod(header.getDeclaredMethod("getColumnStart",int.class),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{View v=(View)p.thisObject;if(!p.hasThrowable()&&top==3&&custom(root(v))){int width=XposedHelpers.getIntField(v,"mCellWidth");if(width>0&&v.getMeasuredWidth()>=width)p.setResult((v.getMeasuredWidth()-width)/2);}}catch(Throwable error){failed=true;}}}));
        hooks.add(XposedBridge.hookMethod(FrameLayout.class.getDeclaredMethod("onLayout",boolean.class,int.class,int.class,int.class,int.class),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!main()||p.hasThrowable()||!p.thisObject.getClass().getName().equals(CONTENT))return;try{ViewGroup r=(ViewGroup)p.thisObject;Boolean previous=roots.put(r,usable(r));if(active&&(previous==null||previous!=usable(r)||usable(r)&&!Integer.valueOf(generation).equals(appliedGeneration.get(r))))schedule(r);if(custom(r)){moveBrightness(r);applyIcons(r);}}catch(Throwable error){failed=true;}}}));
        hooks.add(XposedBridge.hookMethod(circle.getDeclaredMethod("onMeasure",int.class,int.class),new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){try{if(main()&&custom(root(p.thisObject)))applyLabel((View)p.thisObject);}catch(Throwable error){failed=true;}}}));
        hooks.add(XposedHelpers.findAndHookMethod(View.class,"setLayoutParams",ViewGroup.LayoutParams.class,new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){if(!main()||writing||!active)return;View v=(View)p.thisObject;ViewGroup.LayoutParams lp=(ViewGroup.LayoutParams)p.args[0];if(heights.containsKey(v)){if(lp.height!=ViewGroup.LayoutParams.WRAP_CONTENT)heights.put(v,lp.height);if(custom(root(v))&&top!=0)lp.height=ViewGroup.LayoutParams.WRAP_CONTENT;}if(margins.containsKey(v)&&lp instanceof ViewGroup.MarginLayoutParams m){int delta=marginDeltas.getOrDefault(v,0);if(m.topMargin!=margins.get(v)+delta)margins.put(v,m.topMargin);if(custom(root(v)))m.topMargin=margins.get(v)+delta;}}}));
        hooks.add(XposedHelpers.findAndHookMethod(View.class,"setVisibility",int.class,new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){if(main()&&!writing&&visibility.containsKey((View)p.thisObject)){visibility.put((View)p.thisObject,(Integer)p.args[0]);if(active)p.args[0]=View.INVISIBLE;}}}));
        hooks.add(XposedHelpers.findAndHookMethod(TextView.class,"setMaxLines",int.class,new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){if(!main()||writing)return;for(LabelState s:labels.values())if(s.text==p.thisObject){s.maxLines=(Integer)p.args[0];if(active&&labelLines!=0)p.args[0]=labelLines;}}}));
        ready=true;
    }catch(Throwable error){for(var h:hooks)try{h.unhook();}catch(Throwable ignored){}ready=false;}}
    private static void restoreRootViews(ViewGroup r){writing=true;try{
        for(var e:new ArrayList<>(visibility.entrySet()))if(root(e.getKey())==r){e.getKey().setVisibility(e.getValue());visibility.remove(e.getKey());}
        for(var e:new ArrayList<>(labels.entrySet()))if(root(e.getKey())==r){restoreLabel(e.getValue());labels.remove(e.getKey());}
    }finally{writing=false;}}
    private static void restoreLabel(LabelState s){s.text.setMaxLines(s.maxLines);if(s.moved&&s.container.getParent()==s.parent){s.parent.removeView(s.container);s.parent.addView(s.container,Math.min(s.index,s.parent.getChildCount()));}}
    static List<?> remaining(Collection<?> all,int count){List<?> copy=new ArrayList<>(all);return new ArrayList<>(copy.subList(Math.min(count,copy.size()),copy.size()));}
    private static void schedule(ViewGroup r){if(!pending.add(r))return;int token=generation;r.post(()->{pending.remove(r);if(token!=generation||!active||!r.isAttachedToWindow())return;try{rebuild(r);}catch(Throwable error){failed=true;}});}
    private static void rebuild(ViewGroup r){if(rebuilding)return;ViewGroup q=panel(r,true),p=panel(r,false);if(q==null||p==null){lastApply="waiting for native panels (quick="+(q!=null)+", small="+(p!=null)+")";if(changedRoots.contains(r))throw new IllegalStateException("QS panels unavailable during restoration");return;}rebuilding=true;ViewGroup old=scope.get();scope.set(r);try{
        if(!custom(r))restoreRootViews(r);
        Object host=field(p,"mHost");if(host==null){lastApply="waiting for tile host";if(changedRoots.contains(r))throw new IllegalStateException("QS host unavailable during restoration");return;}if(custom(r)&&top!=0)changedRoots.add(r);Object tiles=XposedHelpers.callMethod(host,"getTiles");
        XposedHelpers.callMethod(q,"setMaxTiles",XposedHelpers.getIntField(q,"mMaxTiles"));XposedHelpers.callMethod(p,"setTiles",tiles);
        // Native fixed margins depend on the original quick-panel height. Shift whole blocks by the native row-height delta.
        ViewGroup previousScope=scope.get();scope.set(null);int stockHeight=((Number)XposedHelpers.callStaticMethod(dimensions,"getQuickQsPanelHeight")).intValue();scope.set(previousScope);int customHeight=((Number)XposedHelpers.callStaticMethod(dimensions,"getQuickQsPanelHeight")).intValue();
        int delta=custom(r)&&top!=0?customHeight-stockHeight:0;
        adjustMargin(find(r,"qs_brightness_dialog"),delta);adjustMargin(direct(r,p),delta);
        Object header=field(q,"mTileLayout");XposedHelpers.callMethod(header,"updateResources");
        ViewGroup.LayoutParams lp=q.getLayoutParams();if(custom(r)&&top!=0){heights.putIfAbsent(q,lp.height);lp.height=ViewGroup.LayoutParams.WRAP_CONTENT;writing=true;try{q.setLayoutParams(lp);}finally{writing=false;}}else restoreHeight(q);
        XposedHelpers.callMethod(p,"updateResources");q.requestLayout();p.requestLayout();r.requestLayout();if(!custom(r))changedRoots.remove(r);appliedGeneration.put(r,generation);lastApply="native quick count="+XposedHelpers.getIntField(q,"mMaxTiles");
    }finally{scope.set(old);rebuilding=false;}}
    private static void adjustMargin(View v,int delta){if(v==null||!(v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams lp))return;
        if(delta==0){Integer original=margins.get(v);if(original!=null){lp.topMargin=original;writing=true;try{v.setLayoutParams(lp);margins.remove(v);marginDeltas.remove(v);}finally{writing=false;}}return;}
        margins.putIfAbsent(v,lp.topMargin);marginDeltas.put(v,delta);lp.topMargin=margins.get(v)+delta;writing=true;try{v.setLayoutParams(lp);}finally{writing=false;}
    }
    private static void restoreHeight(View q){Integer height=heights.get(q);if(height!=null){ViewGroup.LayoutParams lp=q.getLayoutParams();lp.height=height;writing=true;try{q.setLayoutParams(lp);heights.remove(q);}finally{writing=false;}}}
    private static void hide(View view){if(view==null)return;visibility.putIfAbsent(view,view.getVisibility());writing=true;try{view.setVisibility(View.INVISIBLE);}finally{writing=false;}}
    private static void applyIcons(ViewGroup r){if(!hideIcons)return;View b=find(r,"qs_brightness_dialog");if(!(b instanceof ViewGroup group)||direct(r,b)!=b)return;for(String name:new String[]{"qs_brightness_low","qs_brightness_high"}){View v=find(r,name);if(v!=null&&direct(group,v)!=null)hide(v);}}
    private static void applyLabel(View tile){
        // Exclude the tile customizer and control center, even when the view class is shared.
        boolean page=false;for(View v=tile;v!=null&&root(v)!=null;v=v.getParent() instanceof View next?next:null)if(v.getClass().getName().equals(QS+"MfvPagedTileLayout$TilePage")){page=true;break;}if(!page)return;
        Object c=XposedHelpers.callMethod(tile,"getLabelContainer"),t=XposedHelpers.callMethod(tile,"getLabel");if(!(c instanceof ViewGroup container)||!(tile instanceof ViewGroup parent)||container.getParent()!=parent||!(t instanceof TextView text))return;
        if(labelPosition==0&&labelLines==0)return;LabelState state=labels.computeIfAbsent(tile,k->new LabelState(container,parent,text));
        if(labelPosition==1&&parent.indexOfChild(container)!=0){parent.removeView(container);parent.addView(container,0);state.moved=true;}else if(labelPosition==2)hide(container);
        if(labelLines!=0){writing=true;try{text.setMaxLines(labelLines);}finally{writing=false;}}
    }
    private static void moveBrightness(ViewGroup r){if(brightness==0)return;Object qs=field(r,"mQs");if(qs!=null&&Boolean.TRUE.equals(XposedHelpers.callMethod(qs,"isShowingDetail")))return;
        View q=panel(r,true),p=panel(r,false),b=find(r,"qs_brightness_dialog"),s=direct(r,p);
        if(q==null||b==null||s==null||q.getParent()!=r||b.getParent()!=r||q.getVisibility()!=View.VISIBLE||b.getVisibility()!=View.VISIBLE||s.getVisibility()!=View.VISIBLE)return;
        if(q.getHeight()<=0||b.getHeight()<=0||s.getHeight()<=0||q.getBottom()>b.getTop()||b.getBottom()>s.getTop())return;
        if(brightness==1){int gap=b.getTop()-q.getBottom(),start=q.getTop(),height=b.getHeight();place(b,start);place(q,start+height+gap);}else{int gap=s.getTop()-b.getBottom(),start=b.getTop(),height=s.getHeight();place(s,start);place(b,start+height+gap);}
    }
    private static void place(View v,int y){v.layout(v.getLeft(),y,v.getRight(),y+v.getHeight());}
    static void begin(int topLayout,int position,int lines,int brightnessPosition,boolean icons){if(!main()||topLayout<0||topLayout>3||position<0||position>2||lines<0||lines>3||brightnessPosition<0||brightnessPosition>2)throw new IllegalArgumentException();if(!end())throw new IllegalStateException("QS layout restore incomplete");if(topLayout==0&&position==0&&lines==0&&brightnessPosition==0&&!icons)return;if(!ready)throw new IllegalStateException("QS layout source unavailable");top=topLayout;labelPosition=position;labelLines=lines;brightness=brightnessPosition;hideIcons=icons;active=true;lastApply="waiting for attached shade";try{for(ViewGroup r:new ArrayList<>(roots.keySet())){if(usable(r))rebuild(r);else if(r.isAttachedToWindow())lastApply="suspended: orientation="+r.getResources().getConfiguration().orientation+", fontScale="+r.getResources().getConfiguration().fontScale;}}catch(Throwable error){failed=true;end();throw new IllegalStateException("QS layout apply failed",error);}}
    static boolean end(){if(!main())return false;active=false;generation++;pending.clear();appliedGeneration.clear();boolean ok=true;writing=true;try{
        for(var entry:new ArrayList<>(visibility.entrySet()))try{entry.getKey().setVisibility(entry.getValue());visibility.remove(entry.getKey());}catch(Throwable error){ok=false;}
        for(var entry:new ArrayList<>(labels.entrySet()))try{restoreLabel(entry.getValue());labels.remove(entry.getKey());}catch(Throwable error){ok=false;}
        for(View v:new ArrayList<>(margins.keySet()))try{adjustMargin(v,0);}catch(Throwable error){ok=false;}
        for(View v:new ArrayList<>(heights.keySet()))try{restoreHeight(v);}catch(Throwable error){ok=false;}
        for(ViewGroup r:new ArrayList<>(changedRoots))try{rebuild(r);}catch(Throwable error){ok=false;}
        for(ViewGroup r:new ArrayList<>(roots.keySet()))r.requestLayout();
    }finally{writing=false;top=labelPosition=labelLines=brightness=0;hideIcons=false;}failed=!ok;return ok;}
    static boolean healthy(){return !failed;}
    static String status(){return "QS layout: "+(active?"enabled":"stock")+"; source="+ready+"; large layout="+top+"; label position="+labelPosition+", lines="+labelLines+"; brightness position="+brightness+"; shade hosts="+roots.size()+"; "+lastApply+".";}
}
