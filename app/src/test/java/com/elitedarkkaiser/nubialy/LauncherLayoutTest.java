package com.elitedarkkaiser.nubialy;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public final class LauncherLayoutTest {
 @Test public void folderScaleMigratesAndStaysScoped(){
  assertEquals(100,LauncherStyle.Layout.decode("6,6,0,0,100,0,1").folderScale());
  var l=new LauncherStyle.Layout(6,6,false,0,100,75,true,65);assertEquals(l,LauncherStyle.Layout.decode(l.encode()));
  assertNull(LauncherStyle.Layout.decode("6,6,0,0,100,0,1,49"));assertNull(LauncherStyle.Layout.decode("6,6,0,0,100,0,1,101"));
  assertEquals(65,LauncherStyle.Layout.merge("folders",LauncherStyle.Layout.STOCK,l).folderScale());
  assertEquals(100,LauncherStyle.Layout.merge("dock",LauncherStyle.Layout.STOCK,l).folderScale());
 }
 @Test public void expandedCatalogueIsAdditiveBoundedAndRestoresOnlyOwnedEntries(){
  Object native4=new Object(),native5=new Object();Map<Integer,Object> map=new HashMap<>();map.put(100,native4);map.put(101,native5);var grids=new LauncherGridCatalogue();assertTrue(grids.enable(map));assertEquals(15,map.size());assertEquals(13,grids.added());assertSame(native4,map.get(100));assertSame(native5,map.get(101));
  for(int row=6;row<=8;row++)for(int column=4;column<=8;column++)assertTrue(map.containsKey(row*16+column));
  assertTrue(grids.enable(map));assertEquals(15,map.size());Object external=new Object();map.put(102,external);grids.restore();assertEquals(3,map.size());assertSame(external,map.get(102));assertSame(native4,map.get(100));assertSame(native5,map.get(101));
  assertFalse(grids.enable(new HashMap<>()));assertFalse(LauncherGridCatalogue.choice(5,5));assertFalse(LauncherGridCatalogue.choice(6,9));assertFalse(LauncherGridCatalogue.choice(9,8));assertTrue(LauncherGridCatalogue.choice(8,8));
 }
 @Test public void layoutValidationAndMigrationKeepLegacyDefaults(){
  var l=new LauncherStyle.Layout(6,5,true,0xff123456,150,70,true);assertTrue(l.valid());assertEquals(l,LauncherStyle.Layout.decode(l.encode()));assertEquals(LauncherStyle.Layout.STOCK,new LauncherStyle(70,false).layout());
  for(String bad:new String[]{"","1,3,0,0,100,0,0","3,7,0,0,100,0,0","3,3,2,0,100,0,0","3,3,0,1,100,0,0","3,3,0,0,151,0,0","3,3,0,0,100,69,0","3,3,0,0,100,0,2"})assertNull(bad,LauncherStyle.Layout.decode(bad));
 }
 @Test public void layoutPagesMergeWithoutApplyingOtherDrafts(){
  var l=new LauncherStyle.Layout(4,5,true,0xff123456,125,75,true);var draft=new LauncherStyle(70,true,0xffabcdef,5,0xffaabbcc,0xff112233,"com.example.icons",2,l);var active=LauncherStyle.STOCK;
  var folders=LauncherStyle.merge("folders",active,draft);assertEquals(4,folders.layout().folderRows());assertEquals(5,folders.layout().folderColumns());assertFalse(folders.layout().hideIndicator());assertFalse(folders.layout().expandedGrid());assertEquals(0,folders.layout().dockScale());
  var dock=LauncherStyle.merge("dock",folders,draft);assertEquals(75,dock.layout().dockScale());assertEquals(4,dock.layout().folderRows());assertFalse(dock.layout().expandedGrid());
  var restored=LauncherStyle.merge("dock",dock,LauncherStyle.STOCK);assertEquals(folders,restored);
  var complete=active;for(String page:new String[]{"icons","folders","artwork","grid","dock","indicator"})complete=LauncherStyle.merge(page,complete,draft);assertEquals(draft,complete);
 }
}
