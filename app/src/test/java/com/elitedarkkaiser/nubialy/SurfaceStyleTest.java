package com.elitedarkkaiser.nubialy;
import org.junit.Test;
import static org.junit.Assert.*;
public final class SurfaceStyleTest {
    @Test public void notificationMigrationAndBounds(){for(int id=0;id<=5;id++){var s=new SurfaceStyle(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,3,6,id);assertTrue(s.valid());assertEquals(s,SurfaceStyle.decode(s.encode()));}assertEquals(0,SurfaceStyle.decode("0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,3,6").notificationStyle());assertNull(SurfaceStyle.decode("0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,3,6,6"));}

    @Test public void volumeMigrationAndBounds(){for(int id=0;id<=6;id++){var s=new SurfaceStyle(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,3,id);assertTrue(s.valid());assertEquals(s,SurfaceStyle.decode(s.encode()));}assertEquals(0,SurfaceStyle.decode("0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,3").volumeStyle());assertNull(SurfaceStyle.decode("0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,3,7"));}

    @Test public void brightnessMigrationAndBounds(){
        for(int style=0;style<=3;style++){var value=new SurfaceStyle(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,style);assertTrue(value.valid());assertEquals(value,SurfaceStyle.decode(value.encode()));}
        assertEquals(0,SurfaceStyle.decode("0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7").brightnessStyle());
        assertNull(SurfaceStyle.decode("0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,4"));
    }

    @Test public void layeredIdsAreAppendOnly(){for(int id=0;id<8;id++){var value=new SurfaceStyle(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,id);assertTrue(value.valid());assertEquals(value,SurfaceStyle.decode(value.encode()));}assertEquals("Outline",QsTileEffects.NAMES[1]);}
    @Test public void outlineStyleRoundTripsAndLegacyUsesStock(){
        for(int shape=0;shape<4;shape++){var value=new SurfaceStyle(0,0,0,0,0,0,0,0,0,shape,3,0,0,0,0,0,1);assertTrue(value.valid());assertEquals(value,SurfaceStyle.decode(value.encode()));}
        assertEquals(0,SurfaceStyle.decode("0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0").qsStyle());
        assertNull(SurfaceStyle.decode("0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,8"));
    }
    @Test public void everyFingerprintStyleRoundTrips(){for(int id=0;id<FingerprintStyles.NAMES.length;id++){var style=new SurfaceStyle(0,0,0,0,0,0,0,0,0,0,0,0,id,0,0);assertTrue(style.valid());assertEquals(style,SurfaceStyle.decode(style.encode()));}}
    @Test public void animationMigrationAndBounds(){for(int id=0;id<FingerprintAnimations.NAMES.length;id++){var s=new SurfaceStyle(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,id);assertTrue(s.valid());assertEquals(s,SurfaceStyle.decode(s.encode()));}assertEquals(0,SurfaceStyle.decode("0,0,0,0,0,0,0,0,0,0,0,0,0,0,0").fingerprintAnimation());assertNull(SurfaceStyle.decode("0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,41"));}
    @Test public void boundedScopeAndLegacyDefaults(){
        assertEquals(SurfaceStyle.STOCK,SurfaceStyle.decode("0,0,0,0,0,0,0,0,0"));assertFalse(SurfaceStyle.STOCK.any());
        var chosen=new SurfaceStyle(15,StatusBarStyle.appearance(120,25),0,StatusBarStyle.appearance(80,100),0,StatusBarStyle.transform(-8,8,20),0,0,0,3);assertTrue(chosen.valid());assertEquals(chosen,SurfaceStyle.decode(chosen.encode()));
        for(String invalid:new String[]{"","0,0","-1,0,0,0,0,0,0,0,0","0,1,0,0,0,0,0,0,0","0,0,0,0,0,0,0,0,0,5","0,0,0,0,0,0,0,0,16777216","0,0,0,0,0,0,0,0,0,0,5","0,0,0,0,0,0,0,0,0,0,-1","0,0,0,0,0,0,0,0,0,0,0,0"})assertNull(SurfaceStyle.decode(invalid));
        assertEquals(SurfaceStyle.STOCK,SurfaceStyle.decode("0,0,0,0,0,0,0,0,0,0,0"));
        assertEquals(0,SurfaceStyle.decode("0,0,0,0,0,0,0,0,0,3").largeTileShape());
        for(int small=0;small<=4;small++)for(int large=0;large<=4;large++){
            var split=new SurfaceStyle(0,0,0,0,0,0,0,0,0,small,large);
            assertTrue(split.valid());assertEquals(split,SurfaceStyle.decode(split.encode()));
            assertEquals(small,split.tileShape());assertEquals(large,split.largeTileShape());
        }
        // Pill (4) and custom roundness (100-150) are valid; the gap (5-99) and >150 are not.
        assertTrue(new SurfaceStyle(0,0,0,0,0,0,0,0,0,4,4).valid());
        var custom=new SurfaceStyle(0,0,0,0,0,0,0,0,0,120,140);
        assertTrue(custom.valid());assertEquals(custom,SurfaceStyle.decode(custom.encode()));
        for(String badShape:new String[]{"0,0,0,0,0,0,0,0,0,5","0,0,0,0,0,0,0,0,0,99","0,0,0,0,0,0,0,0,0,151"})assertNull(SurfaceStyle.decode(badShape));
        var extra=new SurfaceStyle(12,0,0,0,0,0,0,0,0,2,3,7,4,1,25);
        assertTrue(extra.valid());assertEquals(extra,SurfaceStyle.decode(extra.encode()));
        for(String bad:new String[]{"0,0,0,0,0,0,0,0,0,0,0,8,0,0,0","0,0,0,0,0,0,0,0,0,0,0,0,189,0,0","0,0,0,0,0,0,0,0,0,0,0,0,0,2,0","0,0,0,0,0,0,0,0,0,0,0,0,0,0,26"})assertNull(SurfaceStyle.decode(bad));
        java.util.Set<String> names=new java.util.HashSet<>();for(var face:LockClockFonts.FACES){assertTrue(names.add(face.name()));assertFalse(face.category().isEmpty());}assertEquals(26,names.size());
        assertFalse(StatusBarStyle.STOCK.withSurfaces(null).valid());assertEquals(SurfaceStyle.STOCK,StatusBarStyle.STOCK.surfaces());
    }
}
