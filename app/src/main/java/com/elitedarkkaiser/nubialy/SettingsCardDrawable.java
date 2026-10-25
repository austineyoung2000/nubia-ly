package com.elitedarkkaiser.nubialy;
import android.graphics.*;import android.graphics.drawable.*;import java.util.Arrays;import java.util.function.BooleanSupplier;
/** Shared effects rendered on Settings cards; no dependency on another editor. */
public final class SettingsCardDrawable extends Drawable {
 private final SettingsStyle style;private final float density;private final BooleanSupplier allowed;private final GradientDrawable base=new GradientDrawable();
 public SettingsCardDrawable(SettingsStyle s,float d,BooleanSupplier gate){style=s;density=d;allowed=gate;base.setColor(s.card());base.setCornerRadius(s.corner()*d);}
 @Override protected void onBoundsChange(Rect b){base.setBounds(b);}
 @Override public void draw(Canvas c){if(!allowed.getAsBoolean())return;RectF b=new RectF(getBounds());float[] radii=new float[8];Arrays.fill(radii,style.corner()*density);int save=c.save();Path clip=new Path();clip.addRoundRect(b,radii,Path.Direction.CW);c.clipPath(clip);try{boolean done=false;int n=style.surface();if(n>=1&&n<=7)done=QsTileEffects.draw(n,c,base,density,allowed,()->base.draw(c));else if(n>=101&&n<=105)done=NotificationEffects.draw(n-100,c,b,radii,density,allowed,()->base.draw(c));else if(n>=201&&n<=206)done=VolumeEffects.draw(n-200,c,b,radii,density,allowed,()->base.draw(c));if(!done&&allowed.getAsBoolean())base.draw(c);}finally{c.restoreToCount(save);}}
 @Override public void setAlpha(int a){base.setAlpha(a);invalidateSelf();}@Override public void setColorFilter(ColorFilter f){base.setColorFilter(f);invalidateSelf();}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
