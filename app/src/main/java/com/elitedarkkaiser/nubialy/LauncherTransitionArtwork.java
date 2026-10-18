package com.elitedarkkaiser.nubialy;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import java.lang.reflect.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Presentation-only floating artwork. Leaves the launcher icon model intact; fallback drawing restores native state. */
public final class LauncherTransitionArtwork {
    private LauncherTransitionArtwork(){}
    public record Targets(Field source,Method setIcon,Method setBackground){}
    public static Targets resolve(Class<?> floating)throws ReflectiveOperationException {
        // Exact inspected APK: r is the original source view, distinct from the other View fields.
        Field source=floating.getDeclaredField("r");
        if(source.getType()!=View.class||Modifier.isStatic(source.getModifiers()))throw new NoSuchFieldException("Floating source mismatch");
        Method icon=null,background=null;
        for(Method m:floating.getDeclaredMethods()){
            if(Modifier.isStatic(m.getModifiers())||m.getReturnType()!=void.class)continue;
            Class<?>[] p=m.getParameterTypes();
            if(java.util.Arrays.equals(p,new Class<?>[]{Drawable.class,Drawable.class,Supplier.class,int.class})){
                if(icon!=null)throw new NoSuchMethodException("Ambiguous floating artwork");icon=m;
            }
            if(m.getName().equals("setOriginalDrawableBackground")&&java.util.Arrays.equals(p,new Class<?>[]{Supplier.class}))background=m;
        }
        if(icon==null||background==null)throw new NoSuchMethodException("Floating artwork unavailable");
        source.setAccessible(true);return new Targets(source,icon,background);
    }
    public static Drawable wrap(Drawable nativeIcon,Bitmap bitmap,int shape,BooleanSupplier allowed){
        if(nativeIcon==null||bitmap==null||shape<0||shape>4||allowed==null)return nativeIcon;
        return new Artwork(nativeIcon,bitmap,shape,allowed);
    }
    private static final class Artwork extends Drawable {
        private final Drawable original;private final Bitmap image;private final int shape;private final BooleanSupplier allowed;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        Artwork(Drawable original,Bitmap image,int shape,BooleanSupplier allowed){this.original=original;this.image=image;this.shape=shape;this.allowed=allowed;}
        @Override public void draw(Canvas canvas){
            boolean custom=false;try{custom=allowed.getAsBoolean()&&!image.isRecycled();}catch(RuntimeException ignored){}
            if(custom){LauncherIconShapes.draw(canvas,image,getBounds(),paint,shape);return;}
            Rect bounds=new Rect(original.getBounds());int alpha=original.getAlpha();ColorFilter filter=original.getColorFilter();
            try{original.setBounds(getBounds());original.setAlpha(alpha*paint.getAlpha()/255);if(paint.getColorFilter()!=null)original.setColorFilter(paint.getColorFilter());original.draw(canvas);}
            finally{original.setBounds(bounds);original.setAlpha(alpha);original.setColorFilter(filter);}
        }
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
        @Override public int getAlpha(){return paint.getAlpha();}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public ColorFilter getColorFilter(){return paint.getColorFilter();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
        @Override public int getIntrinsicWidth(){return original.getIntrinsicWidth();}
        @Override public int getIntrinsicHeight(){return original.getIntrinsicHeight();}
    }
}
