package com.elitedarkkaiser.nubialy;

import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class QsTileEffectsTest {
    private GradientDrawable tile(){var tile=new GradientDrawable();tile.setBounds(0,0,120,64);tile.setCornerRadii(new float[]{18,18,0,0,18,18,0,0});tile.setColor(Color.BLUE);tile.setTint(Color.GREEN);tile.setAlpha(173);return tile;}
    @Test public void everyLayeredEffectPreservesNativeAlphaStateAndGeometry()throws Throwable{
        Set<Integer> signatures=new HashSet<>();
        for(int style=2;style<8;style++){
            var tile=tile();float[] corners=tile.getCornerRadii().clone();Bitmap nativeImage=Bitmap.createBitmap(120,64,Bitmap.Config.ARGB_8888);tile.draw(new Canvas(nativeImage));Bitmap image=Bitmap.createBitmap(120,64,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);int count=canvas.getSaveCount();AtomicInteger nativeCalls=new AtomicInteger();
            assertTrue(QsTileEffects.draw(style,canvas,tile,1,()->true,()->{nativeCalls.incrementAndGet();tile.draw(canvas);}));assertEquals(1,nativeCalls.get());assertEquals(count,canvas.getSaveCount());
            int[] pixels=new int[120*64];image.getPixels(pixels,0,120,0,0,120,64);signatures.add(Arrays.hashCode(pixels));
            for(int y=0;y<64;y++)for(int x=0;x<120;x++)assertEquals("style="+style+" at "+x+","+y,Color.alpha(nativeImage.getPixel(x,y)),Color.alpha(image.getPixel(x,y)),1);
            int center=image.getPixel(60,32);assertTrue(Color.green(center)>=Color.red(center));assertTrue(Color.green(center)>=Color.blue(center));assertEquals(173,tile.getAlpha());assertEquals(new Rect(0,0,120,64),tile.getBounds());assertArrayEquals(corners,tile.getCornerRadii(),0f);nativeImage.recycle();image.recycle();
        }assertEquals(6,signatures.size());
    }
    @Test public void neumorphDecoratesTwoToneWithoutReplacingItsGradient()throws Throwable{
        GradientDrawable tile=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.RED,Color.BLUE});tile.setCornerRadius(20);tile.setBounds(0,0,120,64);Bitmap image=Bitmap.createBitmap(120,64,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);int saves=canvas.getSaveCount();
        assertTrue(QsTileEffects.draw(3,canvas,tile,1,()->true,()->tile.draw(canvas)));assertEquals(saves,canvas.getSaveCount());int left=image.getPixel(15,32),right=image.getPixel(105,32);assertTrue(Color.red(left)>Color.blue(left));assertTrue(Color.blue(right)>Color.red(right));assertEquals(0,image.getPixel(0,0));assertEquals(255,Color.alpha(left));image.recycle();
    }
    @Test public void canceledAndFailedCompositionLeavesNoPartialLayer()throws Throwable{
        for(int style=2;style<8;style++){
            var tile=tile();Bitmap image=Bitmap.createBitmap(120,64,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);int count=canvas.getSaveCount();AtomicBoolean permit=new AtomicBoolean(true);
            assertFalse(QsTileEffects.draw(style,canvas,tile,1,permit::get,()->{tile.draw(canvas);permit.set(false);}));assertEquals(0,image.getPixel(60,32));assertEquals(count,canvas.getSaveCount());
            try{QsTileEffects.draw(style,canvas,tile,1,()->true,()->{tile.draw(canvas);throw new IllegalStateException();});fail();}catch(IllegalStateException expected){}assertEquals(0,image.getPixel(60,32));assertEquals(count,canvas.getSaveCount());image.recycle();
        }
    }
    @Test public void unsupportedOrUnpermittedDrawDoesNotInvokeNative()throws Throwable{
        var tile=tile();tile.setShape(GradientDrawable.LINE);AtomicInteger calls=new AtomicInteger();Canvas canvas=new Canvas(Bitmap.createBitmap(120,64,Bitmap.Config.ARGB_8888));assertFalse(QsTileEffects.draw(2,canvas,tile,1,()->true,()->calls.incrementAndGet()));assertFalse(QsTileEffects.draw(8,canvas,tile,1,()->true,()->calls.incrementAndGet()));assertEquals(0,calls.get());
    }
}
