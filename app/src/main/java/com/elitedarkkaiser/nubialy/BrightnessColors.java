package com.elitedarkkaiser.nubialy;
import android.graphics.drawable.GradientDrawable;
/** Clone the native leaf. Native LayerDrawable/ClipDrawable still own level and clipping. */
public final class BrightnessColors {
 private BrightnessColors(){}
 public static GradientDrawable copy(GradientDrawable nativeLeaf,boolean fill,SurfaceStyle.Palette palette){
  boolean gradient=fill&&palette.brightnessTwoTone()!=0;int color=fill?palette.brightnessFill():palette.brightnessTrack();if(!gradient&&color==0)return null;
  var state=nativeLeaf.getConstantState();if(state==null)return null;var candidate=state.newDrawable().mutate();if(!(candidate instanceof GradientDrawable copy))return null;copy.setBounds(nativeLeaf.getBounds());copy.setAlpha(nativeLeaf.getAlpha());copy.setState(nativeLeaf.getState());copy.setLevel(nativeLeaf.getLevel());copy.setLayoutDirection(nativeLeaf.getLayoutDirection());copy.setTintList(null);copy.setColorFilter(null);
  if(gradient){copy.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);copy.setGradientType(GradientDrawable.LINEAR_GRADIENT);copy.setColors(new int[]{palette.first(),palette.second()});}else copy.setColor(color);return copy;
 }
}
