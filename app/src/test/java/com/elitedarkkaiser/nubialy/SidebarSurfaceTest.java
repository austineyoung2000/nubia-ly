package com.elitedarkkaiser.nubialy;
import android.graphics.*;
import android.graphics.drawable.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SidebarSurfaceTest {
    private int signature(int style){Bitmap b=Bitmap.createBitmap(140,240,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);int saves=c.getSaveCount();var d=new SidebarSurfaceDrawable(SidebarStyle.DEFAULT.withSurface(style),1,null,()->true);d.setBounds(2,2,138,238);d.draw(c);assertEquals(saves,c.getSaveCount());assertEquals(0,b.getPixel(0,0));int[] pixels=new int[140*240];b.getPixels(pixels,0,140,0,0,140,240);b.recycle();return Arrays.hashCode(pixels);}
    @Test public void sharedFamiliesProduceDistinctSidebarArtwork(){int stock=signature(0);for(int[] family:new int[][]{{1,2,3,4,5,6,7},{101,102,103,104,105},{201,202,203,204,205,206}}){Set<Integer> signatures=new HashSet<>();for(int id:family){assertTrue(SidebarStyle.DEFAULT.withSurface(id).valid());int sig=signature(id);assertNotEquals(stock,sig);signatures.add(sig);}assertEquals(family.length,signatures.size());}}
    @Test public void revokedSurfaceDoesNotDraw(){AtomicBoolean allowed=new AtomicBoolean(false);var d=new SidebarSurfaceDrawable(SidebarStyle.DEFAULT.withSurface(2),1,new ColorDrawable(Color.RED),allowed::get);d.setBounds(0,0,80,80);Bitmap b=Bitmap.createBitmap(80,80,Bitmap.Config.ARGB_8888);d.draw(new Canvas(b));assertEquals(0,b.getPixel(40,40));}
}
