package com.elitedarkkaiser.nubialy;
import android.graphics.Bitmap;
import android.graphics.drawable.*;
import android.view.View;
import java.util.*;
/** Restore only the exact background installed during the current folder session. UI thread only. */
public final class LauncherFolderBackgrounds {
    private record State(Drawable expected,Drawable original){}
    private final Map<View,State> views=new WeakHashMap<>();
    public void remember(View view,Bitmap nativeBitmap){
        if(view==null||!(view.getBackground() instanceof BitmapDrawable custom)||nativeBitmap==null||nativeBitmap.isRecycled())return;
        BitmapDrawable original=new BitmapDrawable(view.getResources(),nativeBitmap);original.setGravity(custom.getGravity());original.setTileModeXY(custom.getTileModeX(),custom.getTileModeY());original.setAlpha(custom.getAlpha());original.setBounds(custom.getBounds());
        views.put(view,new State(custom,original));
    }
    public void restore(){for(var entry:new ArrayList<>(views.entrySet())){View view=entry.getKey();if(view!=null&&view.getBackground()==entry.getValue().expected())view.setBackground(entry.getValue().original());}views.clear();}
}
