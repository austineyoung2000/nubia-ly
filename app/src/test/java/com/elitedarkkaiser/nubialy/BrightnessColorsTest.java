package com.elitedarkkaiser.nubialy;
import android.graphics.*;import android.graphics.drawable.*;import android.view.Gravity;
import org.junit.Test;import org.junit.runner.RunWith;import org.robolectric.RobolectricTestRunner;import org.robolectric.annotation.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class BrightnessColorsTest {
 @Test public void twoToneRetainsNativeProgressClippingAndNeverMutatesNativeLeaf()throws Throwable{
  GradientDrawable original=new GradientDrawable();original.setColor(Color.GREEN);original.setCornerRadius(10);original.setBounds(0,0,100,30);var palette=SurfaceStyle.Palette.STOCK.with(5,Color.RED).with(6,Color.BLUE).with(7,1);var fill=BrightnessColors.copy(original,true,palette);assertNotNull(fill);assertEquals(Color.GREEN,original.getColor().getDefaultColor());assertEquals(10,fill.getCornerRadius(),0f);
  GradientDrawable background=new GradientDrawable();background.setColor(Color.GRAY);ClipDrawable clip=new ClipDrawable(fill,Gravity.LEFT,ClipDrawable.HORIZONTAL);LayerDrawable root=new LayerDrawable(new Drawable[]{background,clip});root.setBounds(0,0,100,30);clip.setLevel(5000);Bitmap bitmap=Bitmap.createBitmap(100,30,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));assertTrue(Color.red(bitmap.getPixel(10,15))>Color.blue(bitmap.getPixel(10,15)));assertEquals(Color.GRAY,bitmap.getPixel(75,15));assertEquals(5000,clip.getLevel());assertEquals(Color.GREEN,original.getColor().getDefaultColor());
  for(int style=1;style<=3;style++){Canvas c=new Canvas(Bitmap.createBitmap(100,30,Bitmap.Config.ARGB_8888));int saved=c.getSaveCount();assertTrue(BrightnessEffects.draw(style,c,fill,1,()->true,()->fill.draw(c)));assertEquals(saved,c.getSaveCount());}
 }
 @Test public void nativeResetReturnsNoReplacement(){GradientDrawable d=new GradientDrawable();d.setColor(Color.WHITE);assertNull(BrightnessColors.copy(d,true,SurfaceStyle.Palette.STOCK));assertNull(BrightnessColors.copy(d,false,SurfaceStyle.Palette.STOCK.with(5,Color.RED)));var track=BrightnessColors.copy(d,false,SurfaceStyle.Palette.STOCK.with(4,Color.BLACK));assertEquals(Color.BLACK,track.getColor().getDefaultColor());assertEquals(Color.WHITE,d.getColor().getDefaultColor());}
}
