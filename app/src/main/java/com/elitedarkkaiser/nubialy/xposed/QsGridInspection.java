package com.elitedarkkaiser.nubialy.xposed;

import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import de.robv.android.xposed.*;
import java.util.*;

/** Read-only layout evidence. No preferences, tile content, setters or replay. */
final class QsGridInspection {
    private static final String BASE="com.zte.mifavor.qs.MfvTileLayout";
    private static final String PAGER="com.zte.mifavor.qs.MfvPagedTileLayout";
    private static final String PAGE=PAGER+"$TilePage";
    private static final String CC="com.zte.controlcenter.view.ControlCenterTileLayout";
    private static final String TOP="com.zte.controlcenter.view.ControlCenterTopPanel";
    private static final Map<View,String> samples=new WeakHashMap<>();
    private static boolean attempted;
    private static String source="not inspected";
    static void install(ClassLoader loader){
        if(attempted)return;attempted=true;
        List<XC_MethodHook.Unhook> pending=new ArrayList<>();
        try{
            Class<?> base=XposedHelpers.findClass(BASE,loader);
            if(base.getSuperclass()!=ViewGroup.class)throw new IllegalStateException();
            for(String f:new String[]{"mRows","mColumns","mCellWidth","mCellHeight","mCellMarginHorizontal","mCellMarginVertical"})
                if(base.getDeclaredField(f).getType()!=int.class)throw new IllegalStateException();
            if(base.getDeclaredField("mRecords").getType()!=ArrayList.class)throw new IllegalStateException();
            XC_MethodHook sample=new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){
                if(!p.hasThrowable()&&Looper.myLooper()==Looper.getMainLooper()&&p.thisObject instanceof View v)observe(v);
            }};
            pending.add(XposedBridge.hookMethod(base.getDeclaredMethod("onLayout",boolean.class,int.class,int.class,int.class,int.class),sample));
            Class<?> pager=XposedHelpers.findClass(PAGER,loader);
            if(pager.getDeclaredField("mPages").getType()!=ArrayList.class||pager.getDeclaredField("mTiles").getType()!=ArrayList.class)throw new IllegalStateException();
            pending.add(XposedBridge.hookMethod(pager.getDeclaredMethod("onMeasure",int.class,int.class),sample));
            source="ready (read-only)";
        }catch(Throwable unavailable){for(var h:pending)try{h.unhook();}catch(Throwable ignored){}source="unavailable";}
    }
    private static int number(Object o,String field){return ((Number)XposedHelpers.getObjectField(o,field)).intValue();}
    static void observe(View v){
        if(Looper.myLooper()!=Looper.getMainLooper()||!v.isAttachedToWindow())return;
        String name=v.getClass().getName();
        if(!name.equals(PAGE)&&!name.equals(CC)&&!name.equals(TOP)&&!name.equals(PAGER))return;
        try{
            String label=name.equals(PAGE)?"paged tiles":name.equals(CC)?"control-center tiles":name.equals(TOP)?"control-center top":"pager";
            String value;
            if(name.equals(PAGER)){
                List<?> pages=(List<?>)XposedHelpers.getObjectField(v,"mPages"),tiles=(List<?>)XposedHelpers.getObjectField(v,"mTiles");
                value=label+": pages="+pages.size()+", tiles="+tiles.size();
            }else{
                int columns=number(v,"mColumns"),rows=number(v,"mRows"),cellW=number(v,"mCellWidth"),cellH=number(v,"mCellHeight");
                if(columns<1||columns>12||rows<0||rows>32||cellW<0||cellH<0)return;
                List<?> records=(List<?>)XposedHelpers.getObjectField(v,"mRecords");
                value=label+": "+columns+" columns × "+rows+" rows, tiles="+records.size()+", cell="+cellW+"×"+cellH+" px, gap="+number(v,"mCellMarginHorizontal")+"×"+number(v,"mCellMarginVertical")+" px";
            }
            value+=", bounds="+v.getWidth()+"×"+v.getHeight()+" px, "+(v.getResources().getConfiguration().orientation==2?"landscape":"portrait");
            if(samples.size()>=8&&!samples.containsKey(v))samples.clear();
            samples.put(v,value);
        }catch(Throwable unavailable){samples.remove(v);}
    }
    static String status(){
        StringBuilder out=new StringBuilder("QS grid inspection: ").append(source).append('.');
        TreeSet<String> unique=new TreeSet<>();
        for(var entry:samples.entrySet())if(entry.getKey().isAttachedToWindow())unique.add(entry.getValue());
        if(unique.isEmpty())out.append(" Open the expanded QS panel, swipe its pages, then check customization.");
        for(String value:unique)out.append(' ').append(value).append('.');
        return out.toString();
    }
}
