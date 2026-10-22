package com.elitedarkkaiser.nubialy;
import org.junit.Test;
import static org.junit.Assert.*;
public class QsThemeTest {
 @Test public void boundedConfigurationRoundTripsAndLegacyStaysStock(){var t=SurfaceStyle.QsTheme.STOCK.with(0,0xff102030).with(6,1).with(9,1).withImage("a".repeat(64));var s=SurfaceStyle.STOCK.withQsTheme(t);assertTrue(s.valid());assertEquals(s,SurfaceStyle.decode(s.encode()));assertEquals(t,s.withBatteryStyle(2).qsTheme());assertEquals(t,s.withBatteryControls(1,1).qsTheme());assertEquals(SurfaceStyle.QsTheme.STOCK,SurfaceStyle.decode(SurfaceStyle.STOCK.encode()).qsTheme());}
 @Test public void arbitraryPathsAlphaAndOutOfBoundsAreRejected(){assertFalse(SurfaceStyle.QsTheme.STOCK.withImage("../../secret").valid());assertFalse(SurfaceStyle.QsTheme.STOCK.with(0,0x10112233).valid());assertFalse(SurfaceStyle.QsTheme.STOCK.with(12,101).valid());assertFalse(SurfaceStyle.QsTheme.STOCK.with(14,9).valid());assertNull(SurfaceStyle.decode(SurfaceStyle.STOCK.encode()+"~bad"));assertNull(SurfaceStyle.decode(SurfaceStyle.STOCK.encode()+"~"+"x".repeat(2000)));}
}
