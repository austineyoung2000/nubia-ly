package com.elitedarkkaiser.nubialy;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SidebarIconPackTest {
    private static class Art extends Resources {
        Art(Resources r){super(r.getAssets(),r.getDisplayMetrics(),r.getConfiguration());}
        @Override public int getIdentifier(String n,String t,String p){return switch(n){case "mapped"->1;case "back"->2;case "front"->3;default->0;};}
        @Override public Drawable getDrawable(int id,Theme theme){return new ColorDrawable(id==1?Color.GREEN:id==2?Color.BLUE:Color.TRANSPARENT);}
    }
    @Test public void exactAndUniqueAliasMappingsWinWithoutGuessingAmbiguousPackages(){Map<String,String> icons=Map.of("com.example.app/com.example.app.Old","mapped");var r=new Art(RuntimeEnvironment.getApplication().getResources());var pack=new SidebarIconPack(new LauncherIconPacks.Loaded("com.example.icons",r,icons),SidebarIconPack.Decorations.EMPTY);Drawable d=pack.icon(new ComponentName("com.example.app","com.example.app.New"),new ColorDrawable(Color.RED));assertTrue(d instanceof ColorDrawable);assertEquals(Color.GREEN,((ColorDrawable)d).getColor());assertNull(SidebarIconPack.uniquePackageMapping(Map.of("com.example.app/.One","a","com.example.app/.Two","b"),"com.example.app"));assertNull(pack.icon(new ComponentName("com.else.app","com.else.app.Main"),new ColorDrawable(Color.RED)));}
    @Test public void generatedArtworkPreservesStockBoundsAndUsesPackBackground(){var r=new Art(RuntimeEnvironment.getApplication().getResources());var pack=new SidebarIconPack(new LauncherIconPacks.Loaded("com.example.icons",r,Map.of()),new SidebarIconPack.Decorations(List.of("back"),"","",.5f));var stock=new ColorDrawable(Color.RED);stock.setBounds(3,4,20,25);Drawable icon=pack.icon(new ComponentName("com.example.app","com.example.app.Main"),stock);Bitmap b=((BitmapDrawable)icon).getBitmap();assertEquals(Color.RED,b.getPixel(80,80));assertEquals(Color.BLUE,b.getPixel(5,5));assertEquals(new Rect(3,4,20,25),stock.getBounds());}
    @Test public void standardFallbackTagsAreBoundedAndMalformedFactorsIgnored()throws Exception {String xml="<resources><iconback img1='back'/><iconmask img1='cut'/><iconupon img1='front'/><scale factor='0.75'/></resources>";var decor=SidebarIconPack.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));assertEquals(List.of("back"),decor.backs());assertEquals("cut",decor.mask());assertEquals("front",decor.front());assertEquals(.75f,decor.scale(),0);assertTrue(decor.any());var invalid=SidebarIconPack.parse(new ByteArrayInputStream("<resources><iconback img1='../private'/><scale factor='NaN'/></resources>".getBytes(StandardCharsets.UTF_8)));assertFalse(invalid.any());assertEquals(1,invalid.scale(),0);}
}
