package com.elitedarkkaiser.nubialy;
import org.junit.Test;
import static org.junit.Assert.*;
public final class StatusBarTransformTest {
    @Test public void signedComponentsAndStockDefaultsRoundTrip(){
        for(int x=-8;x<=8;x+=2)for(int y=-8;y<=8;y+=2)for(int r=-20;r<=20;r+=5){int v=StatusBarStyle.transform(x,y,r);assertTrue(StatusBarStyle.transformValid(v));assertEquals(x,StatusBarStyle.horizontal(v));assertEquals(y,StatusBarStyle.vertical(v));assertEquals(r,StatusBarStyle.rotation(v));}
        assertEquals(0,StatusBarStyle.transform(0,0,0));assertFalse(StatusBarStyle.STOCK.any());assertTrue(StatusBarStyle.STOCK.valid());
    }
    @Test public void invalidStepsBoundsAndReservedBitsAreRejected(){
        for(int[] components:new int[][]{{9,0,0},{0,-10,0},{1,0,0},{0,0,21},{0,0,1}}){try{StatusBarStyle.transform(components[0],components[1],components[2]);fail();}catch(IllegalArgumentException expected){}}
        assertFalse(StatusBarStyle.transformValid(0x01000000));assertFalse(StatusBarStyle.transformValid(-1));assertFalse(StatusBarStyle.transformValid(1));
        assertFalse(new StatusBarStyle(0,100,100,100,100,100,100,java.util.Set.of(),1,0,0).valid());
    }
}
