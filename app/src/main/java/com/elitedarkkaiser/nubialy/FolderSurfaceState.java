package com.elitedarkkaiser.nubialy;
import java.lang.ref.WeakReference;
import java.util.*;
/** Remembers only the last still-owned surface override. Native writes retire ownership. */
public final class FolderSurfaceState {
    public record Restore(Object surface,Object controller,Object folder,int nativeRadius){}
    private record Entry(WeakReference<Object> controller,WeakReference<Object> folder,int radius){}
    private final Map<Object,Entry> overrides=new WeakHashMap<>();
    public static int radius(int nativeRadius,int selected,boolean folderOpen,boolean permitted){
        if(!folderOpen||!permitted||selected<0||selected>25||nativeRadius<0||nativeRadius>4096)return nativeRadius;
        return Math.round(nativeRadius*(selected/25f));
    }
    public synchronized void remember(Object surface,Object controller,Object folder,int original,int adjusted){
        if(surface==null)return;if(original==adjusted||controller==null||folder==null){overrides.remove(surface);return;}
        overrides.put(surface,new Entry(new WeakReference<>(controller),new WeakReference<>(folder),original));
    }
    public synchronized void nativeWrite(Object surface){overrides.remove(surface);}
    public synchronized List<Restore> drain(){List<Restore> result=new ArrayList<>();for(var e:overrides.entrySet()){Object controller=e.getValue().controller.get(),folder=e.getValue().folder.get();if(e.getKey()!=null&&controller!=null&&folder!=null)result.add(new Restore(e.getKey(),controller,folder,e.getValue().radius));}overrides.clear();return result;}
}
