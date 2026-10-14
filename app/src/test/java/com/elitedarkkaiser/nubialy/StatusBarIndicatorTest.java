package com.elitedarkkaiser.nubialy;
import org.junit.Test;
import static org.junit.Assert.*;
public final class StatusBarIndicatorTest {
    @Test public void stockAndBoundedSelectionsRoundTrip(){
        assertEquals(0,StatusBarStyle.appearance(100,100));
        for(int size=80;size<=120;size+=5)for(int opacity=25;opacity<=100;opacity+=5){int value=StatusBarStyle.appearance(size,opacity);assertTrue(StatusBarStyle.appearanceValid(value));assertEquals(size,StatusBarStyle.indicatorScale(value));assertEquals(opacity,StatusBarStyle.indicatorOpacity(value));}
        assertTrue(new StatusBarStyle(112,100,100,100).valid());assertFalse(new StatusBarStyle(128,100,100,100).valid());
    }
    @Test public void malformedAppearanceCannotReachBackend(){
        assertFalse(StatusBarStyle.appearanceValid(0x10000));assertFalse(StatusBarStyle.appearanceValid(-1));assertFalse(StatusBarStyle.appearanceValid(1));
        for(int[] bad:new int[][]{{79,100},{121,100},{81,100},{100,20},{100,101},{100,26}}){try{StatusBarStyle.appearance(bad[0],bad[1]);fail();}catch(IllegalArgumentException expected){}}
        assertFalse(new StatusBarStyle(0,100,100,100,100,100,100,java.util.Set.of(),0,0,0,1,0,0).valid());
    }
}
