package com.elitedarkkaiser.nubialy;

import android.graphics.*;
import android.graphics.drawable.*;
import java.util.Arrays;
import java.util.function.BooleanSupplier;
/** Uses the existing QS, power-menu and volume renderers, behind sidebar content only. */
public final class SidebarSurfaceDrawable extends Drawable implements Drawable.Callback {
    private final GradientDrawable tint=new GradientDrawable();
    private final Drawable blur;
    private final int surface;
    private final SidebarStyle style;
    private final float density,radius;
    private final BooleanSupplier allowed;
    public SidebarSurfaceDrawable(SidebarStyle style,float density,Drawable blur,BooleanSupplier allowed){
        this.style=style;this.blur=blur;this.surface=style.surface();this.density=density;this.radius=style.corner()*density;this.allowed=allowed;
        tint.setColor((style.opacity()*255/100<<24)|(style.color()&0xffffff));tint.setCornerRadius(radius);if(blur!=null)blur.setCallback(this);
    }
    public boolean matches(SidebarStyle candidate,boolean attached){return style.equals(candidate)&&(candidate.blur()==0||!attached||blur!=null);}
    @Override protected void onBoundsChange(Rect b){tint.setBounds(b);if(blur!=null)blur.setBounds(b);}
    @Override public void draw(Canvas canvas){
        if(!allowed.getAsBoolean())return;
        // Native background blur must stay outside offscreen effect layers.
        if(blur!=null)blur.draw(canvas);
        RectF b=new RectF(getBounds());float[] radii=new float[8];Arrays.fill(radii,radius);
        int save=canvas.save();Path clip=new Path();clip.addRoundRect(b,radii,Path.Direction.CW);canvas.clipPath(clip);
        try{boolean handled=false;
            if(surface>=1&&surface<=7)handled=QsTileEffects.draw(surface,canvas,tint,density,allowed,()->tint.draw(canvas));
            else if(surface>=101&&surface<=105)handled=NotificationEffects.draw(surface-100,canvas,b,radii,density,allowed,()->tint.draw(canvas));
            else if(surface>=201&&surface<=206)handled=VolumeEffects.draw(surface-200,canvas,b,radii,density,allowed,()->tint.draw(canvas));
            if(!handled)tint.draw(canvas);
        }catch(Throwable failure){tint.draw(canvas);}finally{canvas.restoreToCount(save);}
    }
    @Override public void setAlpha(int alpha){tint.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){tint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    @Override public void invalidateDrawable(Drawable d){invalidateSelf();}
    @Override public void scheduleDrawable(Drawable d,Runnable r,long when){scheduleSelf(r,when);}
    @Override public void unscheduleDrawable(Drawable d,Runnable r){unscheduleSelf(r);}
}
