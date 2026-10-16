"""Run read-only grid sampling with firmware-shaped classes; never alter layout."""
from pathlib import Path
import tempfile,subprocess
root=Path(__file__).resolve().parents[1]
sources={
'android/os/Looper.java':'package android.os;public class Looper {static final Looper MAIN=new Looper();public static boolean main=true;public static Looper myLooper(){return main?MAIN:null;}public static Looper getMainLooper(){return MAIN;}}',
'android/content/res/Resources.java':'package android.content.res;public class Resources {public static class Configuration {public int orientation=1;}private final Configuration config=new Configuration();public Configuration getConfiguration(){return config;}}',
'android/view/View.java':'package android.view;public class View {public boolean attached=true;public boolean isAttachedToWindow(){return attached;}public int getWidth(){return 400;}public int getHeight(){return 300;}public android.content.res.Resources getResources(){return new android.content.res.Resources();}}',
'android/view/ViewGroup.java':'package android.view;public class ViewGroup extends View {}',
'de/robv/android/xposed/XC_MethodHook.java':'package de.robv.android.xposed;public class XC_MethodHook {public class Unhook {public void unhook(){}}public static class MethodHookParam {public Object thisObject;public boolean hasThrowable(){return false;}}protected void afterHookedMethod(MethodHookParam p){}}',
'de/robv/android/xposed/XposedBridge.java':'package de.robv.android.xposed;public class XposedBridge {public static XC_MethodHook.Unhook hookMethod(java.lang.reflect.Method m,XC_MethodHook h){return h.new Unhook();}}',
'de/robv/android/xposed/XposedHelpers.java':'package de.robv.android.xposed;public class XposedHelpers {public static Class<?> findClass(String n,ClassLoader l){try{return Class.forName(n);}catch(Exception e){throw new RuntimeException(e);}}public static Object getObjectField(Object o,String n){for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException absent){}catch(Exception e){throw new RuntimeException(e);}throw new RuntimeException();}}',
'com/zte/mifavor/qs/MfvTileLayout.java':'package com.zte.mifavor.qs;public class MfvTileLayout extends android.view.ViewGroup {public int mRows=3,mColumns=4,mCellWidth=80,mCellHeight=90,mCellMarginHorizontal=10,mCellMarginVertical=12;public java.util.ArrayList<Object> mRecords=new java.util.ArrayList<>();protected void onLayout(boolean b,int l,int t,int r,int bot){}}',
'com/zte/mifavor/qs/MfvPagedTileLayout.java':'package com.zte.mifavor.qs;public class MfvPagedTileLayout extends android.view.ViewGroup {public java.util.ArrayList<Object> mPages=new java.util.ArrayList<>(),mTiles=new java.util.ArrayList<>();protected void onMeasure(int w,int h){}public static class TilePage extends MfvTileLayout {}}',
'com/zte/controlcenter/view/ControlCenterTileLayout.java':'package com.zte.controlcenter.view;public class ControlCenterTileLayout extends com.zte.mifavor.qs.MfvTileLayout {}',
'com/zte/controlcenter/view/ControlCenterTopPanel.java':'package com.zte.controlcenter.view;public class ControlCenterTopPanel extends ControlCenterTileLayout {}',
'com/elitedarkkaiser/nubialy/xposed/GridInspectionTest.java':'''package com.elitedarkkaiser.nubialy.xposed;
public class GridInspectionTest {
static void check(boolean v){if(!v)throw new AssertionError(QsGridInspection.status());}
public static void main(String[] args){QsGridInspection.install(GridInspectionTest.class.getClassLoader());check(QsGridInspection.status().contains("ready (read-only)"));
var page=new com.zte.mifavor.qs.MfvPagedTileLayout.TilePage();page.mRecords.add(new Object());QsGridInspection.observe(page);check(QsGridInspection.status().contains("4 columns × 3 rows, tiles=1"));check(page.mColumns==4&&page.mRows==3&&page.mRecords.size()==1);
var pager=new com.zte.mifavor.qs.MfvPagedTileLayout();pager.mPages.add(page);pager.mTiles.add(new Object());QsGridInspection.observe(pager);check(QsGridInspection.status().contains("pager: pages=1, tiles=1"));
var cc=new com.zte.controlcenter.view.ControlCenterTileLayout();cc.mColumns=5;QsGridInspection.observe(cc);check(QsGridInspection.status().contains("control-center tiles: 5 columns"));
var top=new com.zte.controlcenter.view.ControlCenterTopPanel();top.mColumns=2;QsGridInspection.observe(top);check(QsGridInspection.status().contains("control-center top: 2 columns"));
cc.attached=false;check(!QsGridInspection.status().contains("control-center tiles:"));
android.os.Looper.main=false;page.mColumns=6;QsGridInspection.observe(page);check(!QsGridInspection.status().contains("6 columns"));android.os.Looper.main=true;
page.attached=false;pager.attached=false;top.attached=false;check(QsGridInspection.status().contains("Open the expanded QS panel"));
var unrelated=new com.zte.mifavor.qs.MfvTileLayout();QsGridInspection.observe(unrelated);check(QsGridInspection.status().contains("Open the expanded QS panel"));
}}
'''
}
with tempfile.TemporaryDirectory() as d:
 w=Path(d)
 for name,s in sources.items():p=w/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s)
 files=[str(p) for p in w.rglob('*.java')]+[str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/xposed/QsGridInspection.java')]
 subprocess.run(['java','com.sun.tools.javac.Main','-d',str(w),*files],check=True)
 subprocess.run(['java','-cp',str(w),'com.elitedarkkaiser.nubialy.xposed.GridInspectionTest'],check=True)
print('QS read-only sampling passed: exact hosts, inherited fields, pages/counts, detached/off-main exclusion and no layout mutation')
