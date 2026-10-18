package com.elitedarkkaiser.nubialy;
import android.content.ComponentName;
import android.graphics.*;
import android.graphics.drawable.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class LauncherIconTest {
    @Test public void staticAppfilterNormalizesComponentsAndRejectsInvalidEntries()throws Exception{
        String xml="<resources><item component='ComponentInfo{com.example.app/.Main}' drawable='main_icon'/><item component='com.example.second/com.example.second.Entry' drawable='second'/><item component='bad' drawable='ignored'/><item component='com.example.app/.Bad' drawable='../bad'/><calendar component='com.example.calendar/.Main' prefix='day_'/></resources>";
        var pack=LauncherIconPacks.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));assertEquals(2,pack.size());assertEquals("main_icon",pack.get(new ComponentName("com.example.app","com.example.app.Main").flattenToString()));assertEquals("second",pack.get("com.example.second/com.example.second.Entry"));
    }
    @Test public void documentDeclarationsAreRejected()throws Exception{try{LauncherIconPacks.parse(new ByteArrayInputStream("<!DOCTYPE resources [<!ENTITY external SYSTEM 'file:///nonexistent'>]><resources/>".getBytes(StandardCharsets.UTF_8)));fail();}catch(org.xmlpull.v1.XmlPullParserException|IOException expected){}}
    @Test public void opaqueSourceIsClippedOnlyInsideArtworkWithoutChangingCanvasOrPaint(){
        Bitmap source=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);source.eraseColor(Color.RED);Bitmap result=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(result);Paint paint=new Paint(3);paint.setAlpha(100);int saved=canvas.getSaveCount();Rect bounds=new Rect(10,10,90,90),before=new Rect(bounds);
        LauncherIconShapes.draw(canvas,source,bounds,paint,1);assertEquals(saved,canvas.getSaveCount());assertEquals(before,bounds);assertEquals(100,paint.getAlpha());assertEquals(0,result.getPixel(10,10));assertTrue(Color.alpha(result.getPixel(50,50))>0);assertFalse(source.isRecycled());
        for(int shape=1;shape<5;shape++){RectF out=new RectF();LauncherIconShapes.path(shape,new RectF(bounds)).computeBounds(out,true);assertEquals(80f,out.width(),.1f);assertEquals(80f,out.height(),.1f);}
    }
    @Test public void adaptiveLayersAreUnmaskedOnlyForCustomShapeAndBoundsRestore(){
        ColorDrawable background=new ColorDrawable(Color.RED),foreground=new ColorDrawable(Color.TRANSPARENT);AdaptiveIconDrawable icon=new AdaptiveIconDrawable(background,foreground);icon.setBounds(3,4,53,54);Rect bounds=new Rect(icon.getBounds()),layer=new Rect(background.getBounds());Bitmap bitmap=LauncherIconShapes.raster(icon,true);assertEquals(Color.RED,bitmap.getPixel(0,0));assertEquals(bounds,icon.getBounds());assertEquals(layer,background.getBounds());
    }
    @Test public void selectedPackOverridesRememberedShapeIncludingOldProfiles(){
        LauncherStyle withPack=new LauncherStyle(70,false,0,-1,0,0,"com.example.icons",4);assertTrue(withPack.valid());assertEquals(4,withPack.iconShape());assertEquals(0,withPack.effectiveIconShape());LauncherStyle nativeIcons=new LauncherStyle(70,false,0,-1,0,0,"",withPack.iconShape());assertEquals(4,nativeIcons.effectiveIconShape());
        var context=org.robolectric.RuntimeEnvironment.getApplication();assertTrue(LauncherPreferences.save(context,withPack));assertTrue(LauncherPreferences.activate(context,withPack));assertEquals(0,LauncherPreferences.active(context).effectiveIconShape());assertEquals(4,LauncherPreferences.draft(context).iconShape());
    }
    @Test public void iconPackAndShapeUseDraftApplyAndNativeRestore(){
        var context=org.robolectric.RuntimeEnvironment.getApplication();var selected=new LauncherStyle(70,true,0xff123456,10,0xffabcdef,0xfffedcba,"com.example.icons",4);assertTrue(selected.valid());assertTrue(selected.iconsCustom());assertTrue(LauncherPreferences.save(context,selected));assertEquals(selected,LauncherPreferences.draft(context));assertEquals(LauncherStyle.STOCK,LauncherPreferences.active(context));assertTrue(LauncherPreferences.activate(context,selected));assertEquals(selected,LauncherPreferences.active(context));assertTrue(LauncherPreferences.stop(context));assertEquals(LauncherStyle.STOCK,LauncherPreferences.active(context));assertEquals(selected,LauncherPreferences.draft(context));assertFalse(new LauncherStyle(100,false,0,-1,0,0,"../invalid",1).valid());assertFalse(new LauncherStyle(100,false,0,-1,0,0,"",5).valid());
        context.getSharedPreferences("launcher_editor",0).edit().clear().putInt("draft_scale",80).commit();assertEquals("",LauncherPreferences.draft(context).iconPack());assertEquals(0,LauncherPreferences.draft(context).iconShape());
    }
}
