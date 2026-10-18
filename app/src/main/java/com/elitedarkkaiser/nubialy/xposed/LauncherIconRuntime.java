package com.elitedarkkaiser.nubialy.xposed;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.util.LruCache;
import android.widget.TextView;
import com.elitedarkkaiser.nubialy.*;
import java.util.*;
import java.util.concurrent.*;
/** Small asynchronous raster cache; misses draw native artwork and never block the UI thread. */
final class LauncherIconRuntime {
    private static final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"nubia-launcher-icons");t.setDaemon(true);return t;});
    private static final Handler main=new Handler(Looper.getMainLooper());
    private static final LruCache<String,Bitmap> images=new LruCache<>(48);
    private static final Set<String> pending=new HashSet<>();
    private static final LinkedHashSet<String> missing=new LinkedHashSet<>();
    private static final Map<TextView,ComponentName> seen=new WeakHashMap<>();
    private static volatile LauncherStyle style=LauncherStyle.STOCK;private static volatile long generation,deadline;
    private static LauncherIconPacks.Loaded pack; // worker confined
    private static String packId="",packProblem="";private static String report="native icons";
    static void apply(LauncherStyle selected,long until){boolean changed=!selected.iconPack().equals(style.iconPack())||selected.iconShape()!=style.iconShape();style=selected;deadline=until;if(changed){generation++;images.evictAll();pending.clear();missing.clear();report=selected.iconsCustom()?"waiting for visible app icons":"native icons";refresh(null);}}
    static void end(){worker.execute(()->{pack=null;packId="";packProblem="";});generation++;style=LauncherStyle.STOCK;deadline=0;images.evictAll();pending.clear();missing.clear();report="native icons";refresh(null);}
    static Bitmap image(TextView view,ComponentName component){
        if(component==null||!style.iconsCustom()||SystemClock.elapsedRealtime()>=deadline)return null;seen.put(view,component);String key=component.flattenToString();Bitmap ready=images.get(key);if(ready!=null)return ready;
        if(missing.contains(key)||pending.contains(key)||pending.size()>=32)return null;
        pending.add(key);Context context=view.getContext().getApplicationContext();long ticket=generation;LauncherStyle selection=style;
        worker.execute(()->{if(ticket!=generation||SystemClock.elapsedRealtime()>=deadline)return;Bitmap result=null;String message="";try{
            if(!selection.iconPack().equals(packId)){pack=null;packProblem="";packId=selection.iconPack();if(!packId.isEmpty())try{pack=LauncherIconPacks.load(context,packId);}catch(Exception unavailable){packProblem="icon pack unavailable; native fallback";}}
            android.graphics.drawable.Drawable source=pack==null?null:pack.icon(component);
            if(source==null&&selection.iconShape()!=0)source=context.getPackageManager().getActivityIcon(component);
            if(source!=null)result=LauncherIconShapes.raster(source,selection.iconShape()!=0);
            message=!packProblem.isEmpty()?packProblem:selection.iconPack().isEmpty()?"custom shapes":"icon pack: "+selection.iconPack()+" (unmatched apps stay native)";
        }catch(Throwable unavailable){message="icon pack/source unavailable; native fallback";}
            Bitmap image=result;String status=message;main.post(()->{if(ticket!=generation||SystemClock.elapsedRealtime()>=deadline)return;pending.remove(key);if(image==null){missing.add(key);while(missing.size()>128)missing.remove(missing.iterator().next());}else images.put(key,image);report=status;refresh(component);});
        });return null;
    }
    private static void refresh(ComponentName component){for(var e:new ArrayList<>(seen.entrySet()))if(e.getKey()!=null&&(component==null||component.equals(e.getValue())))e.getKey().invalidate();}
    static String status(){return "Icons: "+report+"; cached "+images.size()+".";}
}
