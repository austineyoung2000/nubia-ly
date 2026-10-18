package com.elitedarkkaiser.nubialy.xposed;
import android.graphics.*;
import android.os.*;
import android.view.*;
import com.elitedarkkaiser.nubialy.*;
import de.robv.android.xposed.*;
import java.lang.reflect.*;
import java.util.*;

/** Narrow layout adapters for the exact inspected APK; no database or resource-registry writes. */
final class LauncherLayoutBackend {
 private static LauncherStyle.Layout selected=LauncherStyle.Layout.STOCK;private static long deadline;
 private static final LauncherGridCatalogue grids=new LauncherGridCatalogue();
 private static Class<?> desktopGrid,organizer;private static Field gridMap,folderOrganizer;
 private static boolean folderReady,indicatorReady,gridReady;
 private static boolean stock;private static String rowArray="J0",columnArray="K0",capacity="U",organizerField="m",bindMethod="X",measureMethod="k0";
 private static final Map<Object,FolderProfile> profiles=new WeakHashMap<>();
 private static final Map<View,Object> folderOriginals=new WeakHashMap<>();
 private static final Set<View> indicators=Collections.newSetFromMap(new WeakHashMap<>());
 private static final Map<Class<?>,List<Field>> paints=new HashMap<>();
 private record FolderProfile(Object inv,int rows,int columns,int capacity,int[] rowArray,int[] columnArray,int appliedRows,int appliedColumns){}
 private static boolean active(){return Looper.myLooper()==Looper.getMainLooper()&&SystemClock.elapsedRealtime()<deadline;}
 /** Explicit one-shot native reflow, called only after a fresh broker permit. Never called by polling. */
 static String applyGrid(android.content.Context context,int rows,int columns){
  if(!active()||!gridReady||!selected.expandedGrid()||!LauncherGridCatalogue.choice(rows,columns))return "Enable Home-screen grid before choosing a layout";
  try{
   if(!(gridMap.get(null) instanceof Map<?,?> choices)||!choices.containsKey((rows<<4)|columns))return "Expanded grid catalogue unavailable";
   Object manager=XposedHelpers.callStaticMethod(XposedHelpers.findClass(stock?"com.android.launcher3.z5":"com.android.launcher3.H5",context.getClassLoader()),stock?"N":"T",context);
   Object controller=XposedHelpers.callMethod(manager,stock?"C":"H");
   Object result=XposedHelpers.callMethod(controller,stock?"d":"e",context,(rows<<4)|columns);
   Object saved=XposedHelpers.callMethod(controller,stock?"i":"j",context);
   return Boolean.TRUE.equals(result)&&Integer.valueOf((rows<<4)|columns).equals(saved)?"Native grid change requested: "+columns+" × "+rows+". Return home to see the result":"Launcher rejected the grid change";
  }catch(Throwable unavailable){return "Native grid controller unavailable; existing layout retained";}
 }
 static void install(ClassLoader loader){
  gridReady=false;
  for(boolean candidateStock:new boolean[]{false,true})try{
   Class<?> candidate=XposedHelpers.findClass(candidateStock?"com.android.launcher3.Z":"com.android.launcher3.b0",loader);Field field=candidate.getDeclaredField("s");field.setAccessible(true);
   if(!Modifier.isStatic(field.getModifiers())||!Map.class.isAssignableFrom(field.getType())||!(field.get(null) instanceof Map<?,?> choices)||!choices.containsKey(100)||!choices.containsKey(101))continue;
   desktopGrid=candidate;gridMap=field;stock=candidateStock;gridReady=true;break;
  }catch(Throwable ignored){}
  if(stock){rowArray="A0";columnArray="B0";capacity="T";organizerField="j";bindMethod="S";measureMethod="f0";}
  try{
   organizer=XposedHelpers.findClass(stock?"com.android.launcher3.folder.B":"com.android.launcher3.folder.A",loader);organizer.getConstructor(int.class,int.class);
   Class<?> paged=XposedHelpers.findClass("com.android.launcher3.folder.FolderPagedView",loader);folderOrganizer=paged.getDeclaredField(organizerField);folderOrganizer.setAccessible(true);if(folderOrganizer.getType()!=organizer)throw new IllegalStateException();
   XposedBridge.hookMethod(paged.getDeclaredMethod(bindMethod,List.class),new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){
    try{View view=(View)p.thisObject;applyProfile(view);Object original=folderOriginals.computeIfAbsent(view,v->{try{return folderOrganizer.get(v);}catch(Exception e){return null;}});if(original==null)return;
     if(active()&&(selected.folderRows()!=0||selected.folderColumns()!=0)){
      int columns=selected.folderColumns()==0?XposedHelpers.getIntField(original,"b"):selected.folderColumns();int rows=selected.folderRows()==0?XposedHelpers.getIntField(original,"c"):selected.folderRows();
      folderOrganizer.set(view,organizer.getConstructor(int.class,int.class).newInstance(columns,rows));XposedHelpers.callMethod(view,measureMethod);
     }else folderOrganizer.set(view,original);
    }catch(Throwable ignored){}
   }});folderReady=true;
  }catch(Throwable ignored){folderReady=false;}
  for(String name:new String[]{"com.android.launcher3.pageindicators.PageIndicatorDots","com.android.launcher3.pageindicators.MfvPageIndicatorImpl","com.android.launcher3.helper.DotsPageIndicatorZTE"})try{
   Class<?> cls=XposedHelpers.findClass(name,loader);var method=cls.getDeclaredMethod("onDraw",Canvas.class);
   XposedBridge.hookMethod(method,new XC_MethodHook(){
    @Override protected void beforeHookedMethod(MethodHookParam p){View view=(View)p.thisObject;if(!homeIndicator(view))return;indicators.add(view);if(!active())return;
     if(selected.hideIndicator()){p.setResult(null);return;}
     try{Canvas canvas=(Canvas)p.args[0];int save=canvas.save();p.setObjectExtra("layout_canvas",save);canvas.scale(selected.indicatorScale()/100f,selected.indicatorScale()/100f,view.getWidth()/2f,view.getHeight()/2f);
      List<Paint> targets=new ArrayList<>();List<Integer> colors=new ArrayList<>();p.setObjectExtra("layout_paints",targets);p.setObjectExtra("layout_colors",colors);for(Field f:paintFields(cls)){Object value=f.get(view);if(value instanceof Paint paint){targets.add(paint);colors.add(paint.getColor());if(selected.indicatorColor()!=0)paint.setColor(LauncherStyle.labelTint(selected.indicatorColor(),paint.getColor()));}}
      p.setObjectExtra("layout_paints",targets);p.setObjectExtra("layout_colors",colors);
     }catch(Throwable ignored){}
    }
    @Override protected void afterHookedMethod(MethodHookParam p){try{
     Object targets=p.getObjectExtra("layout_paints"),colors=p.getObjectExtra("layout_colors");if(targets instanceof List<?> list&&colors instanceof List<?> values)for(int i=0;i<list.size();i++)((Paint)list.get(i)).setColor((Integer)values.get(i));
    }finally{Object save=p.getObjectExtra("layout_canvas");if(save instanceof Integer count)((Canvas)p.args[0]).restoreToCount(count);}}
   });indicatorReady=true;
  }catch(Throwable ignored){}
 }
 private static List<Field> paintFields(Class<?> cls){return paints.computeIfAbsent(cls,c->{List<Field> list=new ArrayList<>();for(Class<?> t=c;t!=null&&t!=View.class;t=t.getSuperclass())for(Field f:t.getDeclaredFields())if(f.getType()==Paint.class&&!Modifier.isStatic(f.getModifiers())){f.setAccessible(true);list.add(f);}return list;});}
 private static boolean homeIndicator(View view){for(var p=view.getParent();p!=null;p=p.getParent())if(p.getClass().getName().contains("Folder"))return false;return true;}
 private static void applyProfile(View view){if(!folderReady||!active()||selected.folderRows()==0&&selected.folderColumns()==0)return;
  try{Object profile=XposedHelpers.callMethod(view.getContext(),"getDeviceProfile");Object inv=XposedHelpers.getObjectField(profile,"C0");
   FolderProfile old=profiles.get(profile);int rows=old==null?XposedHelpers.getIntField(profile,"L1"):old.rows(),cols=old==null?XposedHelpers.getIntField(profile,"M1"):old.columns();
   int r=selected.folderRows()==0?rows:selected.folderRows(),c=selected.folderColumns()==0?cols:selected.folderColumns();
   if(old==null){FolderProfile shared=null;for(FolderProfile candidate:profiles.values())if(candidate.inv()==inv){shared=candidate;break;}
    int[] nativeRows=shared==null?((int[])XposedHelpers.getObjectField(inv,rowArray)).clone():shared.rowArray();int[] nativeColumns=shared==null?((int[])XposedHelpers.getObjectField(inv,columnArray)).clone():shared.columnArray();
    if(shared!=null){rows=Math.min(rows,Arrays.stream(nativeRows).max().orElse(rows));cols=Math.min(cols,Arrays.stream(nativeColumns).max().orElse(cols));}
    old=new FolderProfile(inv,rows,cols,shared==null?XposedHelpers.getIntField(inv,capacity):shared.capacity(),nativeRows,nativeColumns,r,c);
   }
   profiles.put(profile,new FolderProfile(inv,old.rows(),old.columns(),old.capacity(),old.rowArray(),old.columnArray(),r,c));
   XposedHelpers.setIntField(profile,"L1",r);XposedHelpers.setIntField(profile,"M1",c);XposedHelpers.setIntField(inv,capacity,r*c);XposedHelpers.setObjectField(inv,rowArray,dimensions(old.rowArray().length,r));XposedHelpers.setObjectField(inv,columnArray,dimensions(old.columnArray().length,c));
  }catch(Throwable ignored){}
 }
 private static int[] dimensions(int length,int value){int[] result=new int[length];Arrays.fill(result,value);return result;}
 private static void restoreProfiles(){for(var entry:new ArrayList<>(profiles.entrySet()))try{Object profile=entry.getKey();FolderProfile p=entry.getValue();if(profile==null)continue;
  if(XposedHelpers.getIntField(profile,"L1")==p.appliedRows())XposedHelpers.setIntField(profile,"L1",p.rows());
  if(XposedHelpers.getIntField(profile,"M1")==p.appliedColumns())XposedHelpers.setIntField(profile,"M1",p.columns());
  if(XposedHelpers.getIntField(p.inv(),capacity)==p.appliedRows()*p.appliedColumns())XposedHelpers.setIntField(p.inv(),capacity,p.capacity());
  if(Arrays.equals((int[])XposedHelpers.getObjectField(p.inv(),rowArray),dimensions(p.rowArray().length,p.appliedRows())))XposedHelpers.setObjectField(p.inv(),rowArray,p.rowArray());
  if(Arrays.equals((int[])XposedHelpers.getObjectField(p.inv(),columnArray),dimensions(p.columnArray().length,p.appliedColumns())))XposedHelpers.setObjectField(p.inv(),columnArray,p.columnArray());
 }catch(Throwable ignored){}profiles.clear();}
 static void observe(View root){applyProfile(root);}
 @SuppressWarnings("unchecked") static void apply(LauncherStyle.Layout layout,long until){if(layout==null||!layout.valid()){end();return;}if(!layout.equals(selected))restoreProfiles();selected=layout;deadline=until;
  try{if(gridReady&&active()&&layout.expandedGrid())grids.enable((Map<Integer,Object>)gridMap.get(null));else grids.restore();}catch(Throwable ignored){grids.restore();}
  if(layout.folderRows()==0&&layout.folderColumns()==0)restoreProfiles();for(View v:new ArrayList<>(indicators))v.invalidate();
 }
 static void end(){deadline=0;selected=LauncherStyle.Layout.STOCK;grids.restore();restoreProfiles();for(View v:new ArrayList<>(indicators))v.invalidate();}
 static String status(){return "Layout adapters ("+(stock?"stock":"11.5")+"): folders "+folderReady+", dots "+indicatorReady+", native grids "+gridReady+" / added "+grids.added()+". Reopen folders after layout changes.";}
}
