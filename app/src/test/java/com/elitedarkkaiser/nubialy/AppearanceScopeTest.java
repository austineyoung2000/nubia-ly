package com.elitedarkkaiser.nubialy;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Set;
public final class AppearanceScopeTest {
 private static final AppearanceScope.Snapshot STOCK=AppearanceScope.Snapshot.STOCK;
 private static final String[][] ROUTES={
  {"status_bar","typography","color","date","icons","battery","connectivity","inventory","iconpacks"},
  {"quick_settings","colors","two_tone","background","labels","tiles","style","brightness","grid","iconpacks","landscape","top_layout","label_layout","brightness_layout"},
  {"lock_screen","clock","date","fonts","fingerprint","widgets","charging"},
  {"notifications","styles","appearance","behavior","icons"},{"volume","styles"},{"power_menu","styles"}};
 private AppearanceScope.Snapshot draft(){
  var s=new SurfaceStyle(15,5,10,15,20,2,4,6,8,2,3,7,1,1,1,1,1,1,1,1,1,1,1,50,1,1,1,1,1,1,5,5,4,3|(1<<2)|(1<<4)|(1<<6)|256|(5<<9)|(4<<12),2,3);
  s=s.withQsTheme(SurfaceStyle.QsTheme.STOCK.with(0,0xff112233).with(6,1).with(7,0xff445566).with(9,1).withImage("a".repeat(64)));
  var bar=new StatusBarStyle(127,110,115,120,90,85,80,Set.of("alarm_clock"),2,4,6,5,10,15,s);
  assertTrue(bar.valid());
  return new AppearanceScope.Snapshot(bar,0xff123456,1,new ClockSnapshot(0xffabcdef,110,true,true,true,5,1,true));
 }
 @Test public void sectionsComposeWithoutActivatingOtherDrafts(){
  var draft=draft();var combined=STOCK;
  for(String[] route:ROUTES){var section=AppearanceScope.merge(route[0],"",STOCK,draft);assertNotEquals(STOCK,section);
   // Applying another section in either order produces the same result.
   for(String[] other:ROUTES)if(!other[0].equals(route[0]))assertEquals(
    AppearanceScope.merge(other[0],"",section,draft),
    AppearanceScope.merge(route[0],"",AppearanceScope.merge(other[0],"",STOCK,draft),draft));
   combined=AppearanceScope.merge(route[0],"",combined,draft);
  }
  assertEquals(draft,combined);
 }
 @Test public void detailPagesPartitionTheirSectionAndRestoreOnlyThemselves(){
  var draft=draft();
  for(String[] route:ROUTES){var accumulated=STOCK;
   for(int i=1;i<route.length;i++){String page=route[i];var one=AppearanceScope.merge(route[0],page,STOCK,draft);assertNotEquals(route[0]+":"+page,STOCK,one);
    accumulated=AppearanceScope.merge(route[0],page,accumulated,draft);
    var restored=AppearanceScope.merge(route[0],page,draft,STOCK);
    assertEquals(draft,AppearanceScope.merge(route[0],page,restored,draft));
    assertEquals(STOCK,AppearanceScope.merge(route[0],page,one,STOCK));
   }
   assertEquals(AppearanceScope.merge(route[0],"",STOCK,draft),accumulated);
  }
  assertEquals(draft,AppearanceScope.merge("unknown","",draft,STOCK));
 }
 @Test public void qsOnlyKeepsNativeClockAndIndependentPacks(){
  var qs=AppearanceScope.merge("quick_settings","",STOCK,draft());
  assertEquals(ClockSnapshot.STOCK,qs.clock());assertEquals(0,qs.clock().color());assertEquals(0,qs.bar().surfaces().iconPack());assertEquals(3,qs.bar().surfaces().qsIconPack());
  var status=AppearanceScope.merge("status_bar","iconpacks",qs,draft());assertEquals(2,status.bar().surfaces().iconPack());assertEquals(3,status.bar().surfaces().qsIconPack());
  assertEquals(0,AppearanceScope.merge("quick_settings","iconpacks",status,STOCK).bar().surfaces().qsIconPack());
  assertEquals(2,AppearanceScope.merge("quick_settings","iconpacks",status,STOCK).bar().surfaces().iconPack());
 }
 @Test public void legacyPacksMigrateAndLauncherPagesStayIndependent(){
  String legacy=SurfaceStyle.STOCK.encode();legacy=legacy.substring(0,legacy.lastIndexOf(','));legacy=legacy.substring(0,legacy.lastIndexOf(',')+1)+"4";
  var decoded=SurfaceStyle.decode(legacy);assertNotNull(decoded);assertEquals(4,decoded.iconPack());assertEquals(4,decoded.qsIconPack());assertEquals(decoded,SurfaceStyle.decode(decoded.encode()));
  var draft=new LauncherStyle(70,true,0xffabcdef,5,0xff123456,0xff654321,"com.test.icons",3);var combined=LauncherStyle.STOCK;
  for(String page:new String[]{"icons","folders","artwork"}){combined=LauncherStyle.merge(page,combined,draft);assertEquals(LauncherStyle.STOCK,LauncherStyle.merge(page,LauncherStyle.merge(page,LauncherStyle.STOCK,draft),LauncherStyle.STOCK));}
  assertEquals(draft,combined);assertEquals(draft,LauncherStyle.merge("",LauncherStyle.STOCK,draft));
 }
}
