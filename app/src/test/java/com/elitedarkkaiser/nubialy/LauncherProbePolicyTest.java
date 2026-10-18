package com.elitedarkkaiser.nubialy;
import org.junit.Test;
import static org.junit.Assert.*;
public final class LauncherProbePolicyTest {
 @Test public void probeDropsAllSavedVersionSpecificEffects(){
  var saved=new LauncherStyle(80,true,0xff123456,10,0xffabcdef,0xfffedcba,"com.example.icons",3,new LauncherStyle.Layout(6,6,true,0xff112233,150,70,true,60));
  var probe=LauncherProbePolicy.restrict(saved);
  assertEquals(new LauncherStyle(80,true,0xff123456),probe);
  assertEquals(LauncherStyle.Layout.STOCK,probe.layout());assertEquals("com.example.icons",saved.iconPack());
 }
 @Test public void onlyIconPageCanActivateTheProbe(){
  assertTrue(LauncherProbePolicy.pageAllowed("icons"));
  for(String page:new String[]{"","folders","grid","indicator","dock","artwork"})assertFalse(LauncherProbePolicy.pageAllowed(page));
 }
}
