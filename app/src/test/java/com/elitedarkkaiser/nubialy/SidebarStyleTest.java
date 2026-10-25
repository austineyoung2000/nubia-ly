package com.elitedarkkaiser.nubialy;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;
public class SidebarStyleTest {
    @Test public void roundTripAndRejectMalformed(){var s=SidebarStyle.DEFAULT.withApps(List.of("com.example.app")).withPack("com.example.icons");assertEquals(s,SidebarStyle.decode(s.encode()));assertNull(SidebarStyle.decode(s.encode()+"|extra"));assertNull(SidebarStyle.decode("broken"));assertFalse(s.with(6,0).valid());assertFalse(s.with(6,5).valid());assertFalse(s.with(3,151).valid());assertFalse(s.with(4,1).valid());assertFalse(s.withPack("arbitrary/path").valid());assertFalse(s.withApps(List.of("com.example.app","com.example.app")).valid());}
    @Test public void legacyDraftAndEffectsSurviveEdits(){String legacy="-14343381,-724993,85,60,0,0,2,100,28,0|@launcher|";assertNotNull(SidebarStyle.decode(legacy));assertEquals(0,SidebarStyle.decode(legacy).surface());for(int id:new int[]{0,1,7,101,105,201,206,300,301,302}){var d=SidebarStyle.DEFAULT.withSurface(id);assertTrue(d.valid());assertEquals(d,SidebarStyle.decode(d.encode()));assertEquals(id,d.with(0,0xff123456).withPack("").withApps(List.of()).surface());}assertFalse(SidebarStyle.DEFAULT.withSurface(99).valid());}
    @Test public void selectionIsImmutable(){var apps=new java.util.ArrayList<>(List.of("com.example.app"));var s=SidebarStyle.DEFAULT.withApps(apps);apps.clear();assertEquals(1,s.apps().size());assertEquals("@launcher",SidebarStyle.DEFAULT.iconPack());assertEquals(0,SidebarStyle.DEFAULT.width());}
}
