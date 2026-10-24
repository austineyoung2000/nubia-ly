package com.elitedarkkaiser.nubialy;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;
public class SidebarStyleTest {
    @Test public void roundTripAndRejectMalformed(){var s=SidebarStyle.DEFAULT.withApps(List.of("com.example.app")).withPack("com.example.icons");assertEquals(s,SidebarStyle.decode(s.encode()));assertNull(SidebarStyle.decode(s.encode()+"|extra"));assertNull(SidebarStyle.decode("broken"));assertFalse(s.with(6,0).valid());assertFalse(s.with(6,5).valid());assertFalse(s.with(3,151).valid());assertFalse(s.with(4,1).valid());assertFalse(s.withPack("arbitrary/path").valid());assertFalse(s.withApps(List.of("com.example.app","com.example.app")).valid());}
    @Test public void selectionIsImmutable(){var apps=new java.util.ArrayList<>(List.of("com.example.app"));var s=SidebarStyle.DEFAULT.withApps(apps);apps.clear();assertEquals(1,s.apps().size());assertEquals("@launcher",SidebarStyle.DEFAULT.iconPack());assertEquals(0,SidebarStyle.DEFAULT.width());}
}
