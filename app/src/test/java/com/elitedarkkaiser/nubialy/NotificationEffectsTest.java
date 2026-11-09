package com.elitedarkkaiser.nubialy;
import android.graphics.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class NotificationEffectsTest {
    @Test public void distinctTreatmentsKeepContentOutsideNativeBackgroundPass()throws Throwable{
        Set<Integer> signatures=new HashSet<>();float[] radii={12,12,12,12,12,12,12,12};
        for(int style=1;style<=5;style++){Bitmap image=Bitmap.createBitmap(160,80,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(image);Paint p=new Paint();p.setColor(0xff45404f);int save=c.getSaveCount();AtomicInteger original=new AtomicInteger();float[] before=radii.clone();RectF b=new RectF(5,5,155,75);
            assertTrue(NotificationEffects.draw(style,c,b,radii,1,()->true,()->{original.incrementAndGet();c.drawRoundRect(b,12,12,p);}));assertEquals(1,original.get());assertEquals(save,c.getSaveCount());assertArrayEquals(before,radii,0f);assertEquals(0,image.getPixel(0,0));
            int[] pixels=new int[160*80];image.getPixels(pixels,0,160,0,0,160,80);signatures.add(Arrays.hashCode(pixels));image.recycle();
        }assertEquals(5,signatures.size());
    }
    @Test public void cancellationLeavesNativePassAndClearsOnlyOverlay()throws Throwable{
        for(int style=1;style<=5;style++){Bitmap image=Bitmap.createBitmap(120,60,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(image);int save=c.getSaveCount();float[] corners=new float[8];AtomicInteger checks=new AtomicInteger();
            assertTrue(NotificationEffects.draw(style,c,new RectF(0,0,120,60),corners,1,()->checks.incrementAndGet()<3,()->c.drawColor(Color.BLUE)));assertEquals(Color.BLUE,image.getPixel(50,30));assertEquals(save,c.getSaveCount());image.recycle();}
    }
    @Test public void colorsRenderInsideCornersAndLeaveCanvasBalanced(){
        for(int mode=1;mode<=3;mode++){Bitmap image=Bitmap.createBitmap(120,60,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(image);int save=c.getSaveCount();float[] corners={16,16,16,16,16,16,16,16};
            NotificationEffects.color(c,new RectF(0,0,120,60),corners,0xffff0000,0xff0000ff,mode);
            assertEquals(save,c.getSaveCount());assertEquals(0,image.getPixel(0,0));assertNotEquals(0,image.getPixel(60,15));
            assertEquals(255,Color.alpha(image.getPixel(60,30)));if(mode>=2){assertNotEquals(image.getPixel(20,30),image.getPixel(100,30));assertEquals(image.getPixel(60,15),image.getPixel(60,45));}image.recycle();}
    }
}
