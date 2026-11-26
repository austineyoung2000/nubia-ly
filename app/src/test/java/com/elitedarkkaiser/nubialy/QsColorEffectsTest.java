package com.elitedarkkaiser.nubialy;
import org.junit.Test;
import static org.junit.Assert.*;

public class QsColorEffectsTest {
 @Test public void choicesPreserveOtherSettingsAndRoundTrip(){
  var stock=SurfaceStyle.QsTheme.STOCK.with(2,0xff123456).with(25,2);
  assertEquals(QsColorEffects.NATIVE,QsColorEffects.selected(stock,true));
  var solid=QsColorEffects.select(stock,true,QsColorEffects.SOLID);
  assertEquals(QsColorEffects.SOLID,QsColorEffects.selected(solid,true));
  assertEquals(0xff123456,solid.activeIcon());assertEquals(2,solid.inactiveNeon());
  var gradient=QsColorEffects.select(solid,true,QsColorEffects.GRADIENT);
  assertEquals(QsColorEffects.GRADIENT,QsColorEffects.selected(gradient,true));
  var glow=QsColorEffects.select(gradient,true,QsColorEffects.DUAL_NEON);
  assertEquals(QsColorEffects.DUAL_NEON,QsColorEffects.selected(glow,true));assertEquals(1,glow.twoTone());
  assertEquals(glow,SurfaceStyle.QsTheme.decode(glow.encode()));
  var nativeTile=QsColorEffects.select(glow,true,QsColorEffects.NATIVE);
  assertEquals(QsColorEffects.NATIVE,QsColorEffects.selected(nativeTile,true));assertEquals(2,nativeTile.inactiveNeon());
  var inactive=QsColorEffects.select(nativeTile,false,QsColorEffects.NEON);
  assertEquals(QsColorEffects.NEON,QsColorEffects.selected(inactive,false));
  inactive=QsColorEffects.select(inactive,false,QsColorEffects.SOLID);
  assertEquals(QsColorEffects.SOLID,QsColorEffects.selected(inactive,false));assertEquals(0,inactive.inactiveNeon());
 }
 @Test(expected=IllegalArgumentException.class) public void inactiveGradientIsNotOffered(){QsColorEffects.select(SurfaceStyle.QsTheme.STOCK,false,QsColorEffects.GRADIENT);}
}
