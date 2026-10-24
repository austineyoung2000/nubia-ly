package com.elitedarkkaiser.nubialy;
import android.graphics.*;
import android.view.*;
import java.io.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SidebarActivityTest {
    @Test public void visualStyleSelectionIsDraftOnlyAndSurvivesRecreation()throws Exception {
        var controller=Robolectric.buildActivity(SidebarActivity.class).setup();var a=controller.get();
        ReflectionHelpers.callInstanceMethod(a,"open",ReflectionHelpers.ClassParameter.from(String.class,"styles_qs"));
        a.findViewById(android.R.id.content).findViewWithTag("sidebar-surface-3").performClick();
        ExecutorService worker=ReflectionHelpers.getField(a,"worker");worker.submit(()->{}).get(3,TimeUnit.SECONDS);
        assertEquals(3,SidebarPreferences.draft(a).surface());assertNull(SidebarPreferences.active(a));
        controller.recreate();a=controller.get();assertNotNull(a.findViewById(android.R.id.content).findViewWithTag("sidebar-surface-3"));assertEquals("styles_qs",ReflectionHelpers.getField(a,"page"));a.onBackPressed();assertEquals("styles",ReflectionHelpers.getField(a,"page"));controller.pause().stop().destroy();
    }
    @Test public void renderStyleGallery()throws Exception {
        var controller=Robolectric.buildActivity(SidebarActivity.class).setup();var a=controller.get();
        ReflectionHelpers.callInstanceMethod(a,"open",ReflectionHelpers.ClassParameter.from(String.class,"styles_qs"));
        View root=a.findViewById(android.R.id.content);root.measure(View.MeasureSpec.makeMeasureSpec(440,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));root.layout(0,0,440,900);
        Bitmap b=Bitmap.createBitmap(440,900,Bitmap.Config.ARGB_8888);root.draw(new Canvas(b));File output=new File("build/editor-previews/sidebar-styles.png");output.getParentFile().mkdirs();try(var stream=new FileOutputStream(output)){b.compress(Bitmap.CompressFormat.PNG,100,stream);}b.recycle();controller.pause().stop().destroy();
    }
}
