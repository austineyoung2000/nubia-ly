package com.elitedarkkaiser.nubialy.xposed;
import android.os.Looper;
import android.view.*;
import de.robv.android.xposed.*;
import java.util.*;
import java.lang.reflect.Array;
/** Bounded portrait paged-grid policy. Native measurement, records, paging and RTL remain in charge. */
final class QsGridBackend {
 private static final String ROOT="com.zte.mifavor.qs.",PAGER=ROOT+"MfvPagedTileLayout",PAGE=PAGER+"$TilePage",BASE=ROOT+"MfvTileLayout",PANEL=ROOT+"MfvQSPanel",ANIM=ROOT+"MfvQSAnimator";
 private static final Map<ViewGroup,String> pagers=new WeakHashMap<>();
 private static final Set<ViewGroup> changed=Collections.newSetFromMap(new WeakHashMap<>());
 private static final Map<Object,Boolean> animators=new WeakHashMap<>();
 private record AnimationState(Object originalRows,Object originalDelays,Object ownedRows,Object ownedDelays){}
 private static final Map<Object,AnimationState> animationStates=new WeakHashMap<>();
 private static final ThreadLocal<ViewGroup> animationOwner=new ThreadLocal<>();
 private static boolean ready,attempted,active,failed,restoring;private static int columns,rows,landColumns,landRows;private static String message="not ready",lastFailure="none";
 private static void fail(Throwable error){if(!failed){Throwable cause=error;for(int i=0;i<4&&cause.getCause()!=null;i++)cause=cause.getCause();lastFailure=cause.getClass().getSimpleName();for(StackTraceElement frame:cause.getStackTrace())if(frame.getClassName().startsWith("com.elitedarkkaiser.nubialy")||frame.getClassName().startsWith("com.zte.")){lastFailure+="@"+frame.getMethodName()+":"+frame.getLineNumber();break;}}failed=true;}
 private static boolean main(){return Looper.myLooper()==Looper.getMainLooper();}
 private static int n(Object o,String f){return ((Number)XposedHelpers.getObjectField(o,f)).intValue();}
 private static void put(Object o,String f,int v){XposedHelpers.setIntField(o,f,v);}
 private static ViewGroup owner(View v){for(View current=v;current!=null;){if(current.getClass().getName().equals(PAGER)&&current instanceof ViewGroup g)return g;var p=current.getParent();current=p instanceof View next?next:null;}return null;}
 private static boolean owned(ViewGroup pager){
  if(pager==null||!pager.getClass().getName().equals(PAGER)||!pager.isAttachedToWindow())return false;
  for(View v=pager;v!=null;){if(v.getClass().getName().equals(PANEL))return XposedHelpers.getObjectField(v,"mTileLayout")==pager;var p=v.getParent();v=p instanceof View next?next:null;}return false;
 }
 private static boolean portrait(View v){return v.getResources().getConfiguration().orientation==1;}
 private static int selectedColumns(View v){return portrait(v)?columns:landColumns;}
 private static int selectedRows(View v){return portrait(v)?rows:landRows;}
 private static boolean applies(ViewGroup pager){return active&&!restoring&&main()&&owned(pager)&&pager.getResources().getConfiguration().fontScale<=1.3f&&(selectedColumns(pager)!=0||selectedRows(pager)!=0);}
 static void install(ClassLoader loader){if(attempted)return;attempted=true;List<XC_MethodHook.Unhook> installed=new ArrayList<>();try{
  Class<?> base=XposedHelpers.findClass(BASE,loader),pager=XposedHelpers.findClass(PAGER,loader),page=XposedHelpers.findClass(PAGE,loader),panel=XposedHelpers.findClass(PANEL,loader),anim=XposedHelpers.findClass(ANIM,loader);
  if(base.getSuperclass()!=ViewGroup.class||page.getSuperclass()!=base||panel.getDeclaredField("mTileLayout")==null||anim.getDeclaredField("mPagedLayout").getType()!=pager)throw new IllegalStateException();
  for(String f:new String[]{"mResourceColumns","mColumns","mMaxAllowedRows","mRows","mCellWidth","mCellHeight"})if(base.getDeclaredField(f).getType()!=int.class)throw new IllegalStateException();
  if(!anim.getDeclaredField("mRowAnimators").getType().getName().equals("[[Lcom.android.systemui.qs.TouchAnimator;")||anim.getDeclaredField("ROW_START_DELAY").getType()!=Float[].class||anim.getDeclaredField("mNeedsAnimatorUpdate").getType()!=boolean.class)throw new IllegalStateException();
  if(pager.getDeclaredField("mExcessHeight").getType()!=int.class)throw new IllegalStateException();
  if(pager.getDeclaredField("mPages").getType()!=ArrayList.class||pager.getDeclaredField("mTiles").getType()!=ArrayList.class||pager.getDeclaredField("mDistributeTiles").getType()!=boolean.class)throw new IllegalStateException();
  installed.add(XposedBridge.hookMethod(base.getDeclaredMethod("updateResources"),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(p.hasThrowable()||!main()||!active||restoring)return;try{View v=(View)p.thisObject;if(!v.getClass().getName().equals(PAGE)||!applies(owner(v)))return;
   configure(v,owner(v));
   p.setResult(true);
  }catch(Throwable error){fail(error);}}}));
  installed.add(XposedBridge.hookMethod(pager.getDeclaredMethod("createTilePage"),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(p.hasThrowable()||!main()||!applies((ViewGroup)p.thisObject))return;try{if(p.getResult() instanceof View v&&v.getClass().getName().equals(PAGE))configure(v,(ViewGroup)p.thisObject);}catch(Throwable error){fail(error);}}}));
  installed.add(XposedBridge.hookMethod(pager.getDeclaredMethod("onMeasure",int.class,int.class),new XC_MethodHook(){
   @Override protected void beforeHookedMethod(MethodHookParam p){if(!main())return;try{ViewGroup v=(ViewGroup)p.thisObject;if(!owned(v))return;if(!pagers.containsKey(v))pagers.put(v,"");
    int width=View.MeasureSpec.getSize((Integer)p.args[0]);XposedHelpers.setAdditionalInstanceField(v,"nubia-ly:grid-width",width);
    Object adapt=XposedHelpers.callMethod(v,"getAdapt");int decor=((Number)XposedHelpers.callMethod(adapt,"getDecorHeight")).intValue();
    int budget=Math.max(0,View.MeasureSpec.getSize((Integer)p.args[1])-n(v,"mExcessHeight")-Math.max(0,decor));XposedHelpers.setAdditionalInstanceField(v,"nubia-ly:grid-height",budget);
    String mode=(applies(v)?v.getResources().getConfiguration().orientation+":"+selectedColumns(v)+":"+selectedRows(v):"stock")+":"+width+":"+budget;
    // Native configuration may change while enabled: recompute resources on every policy transition.
    String key="policy:"+mode;if(!key.equals(XposedHelpers.getAdditionalInstanceField(v,"nubia-ly:grid-policy"))){XposedHelpers.setAdditionalInstanceField(v,"nubia-ly:grid-policy",key);if(applies(v)||changed.contains(v)){reflow(v);if(!applies(v))changed.remove(v);}}
   }catch(Throwable error){fail(error);}}
   @Override protected void afterHookedMethod(MethodHookParam p){if(!main())return;try{ViewGroup v=(ViewGroup)p.thisObject;if(!owned(v))return;if(p.hasThrowable()){failed=true;return;}List<?> pages=(List<?>)XposedHelpers.getObjectField(v,"mPages");if(pages.isEmpty())return;Object first=pages.get(0);String key=n(first,"mColumns")+":"+n(first,"mRows")+":"+pages.size()+":"+v.getResources().getConfiguration().orientation;
    if(!key.equals(pagers.put(v,key)))refreshAnimators(v);
   }catch(Throwable error){fail(error);}}
  }));
  XC_MethodHook animation=new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){if(!main())return;try{Object o=p.thisObject;animators.put(o,true);p.setObjectExtra("nubia-ly:grid-animation",animationOwner.get());Object v=XposedHelpers.getObjectField(o,"mPagedLayout");prepareAnimation(o,v instanceof ViewGroup g?g:null);animationOwner.set(v instanceof ViewGroup g&&applies(g)?g:null);}catch(Throwable error){fail(error);}}
   @Override protected void afterHookedMethod(MethodHookParam p){if(main())animationOwner.set((ViewGroup)p.getObjectExtra("nubia-ly:grid-animation"));}};
  for(String method:new String[]{"updateAnimators","updateViews","setPosition"})installed.add(XposedBridge.hookMethod(method.equals("setPosition")?anim.getDeclaredMethod(method,float.class):anim.getDeclaredMethod(method),animation));
  for(String dimensionClass:new String[]{"com.zte.utils.QsDimenUtils","com.zte.utils.QsDimenUtils$Companion"}){Class<?> dimensions=XposedHelpers.findClass(dimensionClass,loader);
  for(String method:new String[]{"getTileColumns","getMaxQsPanelRowCount"}){final boolean col=method.equals("getTileColumns");installed.add(XposedBridge.hookMethod(dimensions.getDeclaredMethod(method),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){ViewGroup v=animationOwner.get();if(p.hasThrowable()||!applies(v))return;try{List<?> pages=(List<?>)XposedHelpers.getObjectField(v,"mPages");if(pages.isEmpty())return;int value=n(pages.get(0),col?"mColumns":"mRows");if(value>=1&&value<=6)p.setResult(value);}catch(Throwable error){fail(error);}}}));}
  }
  ready=true;message="Stock";
 }catch(Throwable error){for(var hook:installed)try{hook.unhook();}catch(Throwable ignored){}message="source unavailable";}}
 private static void configure(View v,ViewGroup pager){
  if(!applies(pager)||!v.getClass().getName().equals(PAGE)||n(v,"mColumns")<2||n(v,"mColumns")>6)return;
  changed.add(pager);
  int requestedColumns=selectedColumns(pager),requestedRows=selectedRows(pager);int effective=requestedColumns;
  if(requestedColumns>=5){
   Object measured=XposedHelpers.getAdditionalInstanceField(pager,"nubia-ly:grid-width");int width=measured instanceof Integer w?w:pager.getWidth();
   Object adapt=XposedHelpers.callMethod(v,"getAdapt");int cell=((Number)XposedHelpers.callMethod(adapt,"getCellWidth",0)).intValue();
   int edge=((Number)XposedHelpers.callMethod(v,"getColumnStart",0)).intValue();
   int gap=(int)Math.ceil(4*v.getResources().getDisplayMetrics().density);
   if(cell<=0||edge<0||width<(long)requestedColumns*cell+2L*edge+(requestedColumns-1L)*gap)effective=0;
  }
  if(effective!=0){put(v,"mResourceColumns",effective);put(v,"mColumns",effective);}
  if(requestedRows!=0){int cap=Math.min(requestedRows,n(v,"mMaxAllowedRows"));
   if(requestedRows>=4){Object measured=XposedHelpers.getAdditionalInstanceField(pager,"nubia-ly:grid-height");int budget=measured instanceof Integer h?h:0;
    int cell=n(v,"mCellHeight"),gap=n(v,"mCellMarginVertical"),top=n(v,"mCellMarginTop");
    if(budget>0&&cell>0&&gap>=0)cap=Math.max(1,Math.min(requestedRows,(budget-top+gap)/(cell+gap)));
   }put(v,"mMaxAllowedRows",cap);
  }
 }
 private static void prepareAnimation(Object animator,ViewGroup pager){
  int count=0;if(applies(pager)){List<?> pages=(List<?>)XposedHelpers.getObjectField(pager,"mPages");if(!pages.isEmpty())count=n(pages.get(0),"mRows");}
  AnimationState state=animationStates.get(animator);
  if(count<4){if(state!=null)restoreAnimation(animator,state);return;}
  if(count>6)throw new IllegalStateException("Animation row bound");
  Object current=XposedHelpers.getObjectField(animator,"mRowAnimators"),delays=XposedHelpers.getObjectField(animator,"ROW_START_DELAY");
  if(state!=null){if(current!=state.ownedRows()||delays!=state.ownedDelays())throw new IllegalStateException("Animation ownership changed");if(Array.getLength(current)==count)return;restoreAnimation(animator,state);current=XposedHelpers.getObjectField(animator,"mRowAnimators");delays=XposedHelpers.getObjectField(animator,"ROW_START_DELAY");}
  if(!(delays instanceof Float[] nativeDelays)||nativeDelays.length<3||Array.getLength(current)<3)throw new IllegalStateException("Animation capacity");
  Float[] expandedDelays=Arrays.copyOf(nativeDelays,Math.max(count,nativeDelays.length));
  if(nativeDelays[2]==null||!Float.isFinite(nativeDelays[2])||nativeDelays[2]<0||nativeDelays[2]>.65f)throw new IllegalStateException("Animation delay");
  for(int i=3;i<count;i++)expandedDelays[i]=nativeDelays[2]+(.65f-nativeDelays[2])*(i-2)/(count-3);
  Object expanded=Array.newInstance(current.getClass().getComponentType(),count);
  for(int i=0;i<count;i++)Array.set(expanded,i,Array.newInstance(current.getClass().getComponentType().getComponentType(),2));
  state=new AnimationState(current,delays,expanded,expandedDelays);animationStates.put(animator,state);
  XposedHelpers.setObjectField(animator,"mRowAnimators",expanded);XposedHelpers.setObjectField(animator,"ROW_START_DELAY",expandedDelays);XposedHelpers.setBooleanField(animator,"mNeedsAnimatorUpdate",true);
 }
 private static void restoreAnimation(Object animator,AnimationState state){
  Object current=XposedHelpers.getObjectField(animator,"mRowAnimators"),delays=XposedHelpers.getObjectField(animator,"ROW_START_DELAY");
  // A native replacement belongs to the firmware; never overwrite it with an old capture.
  if(current==state.ownedRows())XposedHelpers.setObjectField(animator,"mRowAnimators",state.originalRows());
  if(delays==state.ownedDelays())XposedHelpers.setObjectField(animator,"ROW_START_DELAY",state.originalDelays());
  XposedHelpers.setBooleanField(animator,"mNeedsAnimatorUpdate",true);animationStates.remove(animator);
 }
 private static void reflow(ViewGroup v){XposedHelpers.callMethod(v,"updateResources");XposedHelpers.setBooleanField(v,"mDistributeTiles",true);v.requestLayout();}
 private static void refreshAnimators(ViewGroup v){for(Object a:new ArrayList<>(animators.keySet()))if(XposedHelpers.getObjectField(a,"mPagedLayout")==v)XposedHelpers.callMethod(a,"updateAnimators");}
 static void begin(int col,int row){begin(col,row,0,0);}
 static void begin(int col,int row,int landscapeColumns,int landscapeRows){if(!main()||!validColumns(col)||!validColumns(landscapeColumns)||row<0||row>6||landscapeRows<0||landscapeRows>6)throw new IllegalArgumentException();if(!end())throw new IllegalStateException("Grid restoration incomplete");if(col==0&&row==0&&landscapeColumns==0&&landscapeRows==0)return;if(!ready)throw new IllegalStateException("Grid source unavailable");columns=col;rows=row;landColumns=landscapeColumns;landRows=landscapeRows;active=true;try{for(ViewGroup v:new ArrayList<>(pagers.keySet()))if(owned(v)){XposedHelpers.setAdditionalInstanceField(v,"nubia-ly:grid-policy",null);reflow(v);}message="Enabled; small tiles, native cell size";}catch(Throwable error){failed=true;end();throw new IllegalStateException("Grid apply failed",error);}}
 private static boolean validColumns(int value){return value==0||value>=2&&value<=6;}
 static boolean end(){if(!main())return false;active=false;restoring=true;boolean ok=true;try{for(ViewGroup v:new ArrayList<>(changed))try{XposedHelpers.setAdditionalInstanceField(v,"nubia-ly:grid-policy",null);reflow(v);changed.remove(v);}catch(Throwable error){ok=false;}}finally{for(var entry:new ArrayList<>(animationStates.entrySet()))try{restoreAnimation(entry.getKey(),entry.getValue());}catch(Throwable error){ok=false;}restoring=false;columns=rows=landColumns=landRows=0;}if(ok){failed=false;message="Stock";}else{failed=true;message="Restoration incomplete";}return ok;}

 static boolean healthy(){return !failed;}
 static String status(){return "QS grid: "+message+"; requested columns="+columns+", rows="+rows+"; landscape columns="+landColumns+", rows="+landRows+"; native reflow hosts="+pagers.size()+"; extended row animators="+animationStates.size()+"; last failure="+lastFailure+".";}
}
