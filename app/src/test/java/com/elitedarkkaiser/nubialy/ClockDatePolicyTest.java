package com.elitedarkkaiser.nubialy;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class ClockDatePolicyTest {
    private Calendar date(){Calendar c=Calendar.getInstance(TimeZone.getTimeZone("UTC"),Locale.US);c.setTimeInMillis(java.time.Instant.parse("2026-10-13T00:30:00Z").toEpochMilli());return c;}
    @Test public void originalFormatsAndNewDateOrdersRemainDistinct(){
        String[] expected={"Tue","Oct 13","Tue, Oct 13","10/13","Tuesday","October 13","13 Oct","13/10","10/13/2026","13/10/2026","2026-10-13","Tuesday, October 13"};
        for(int i=0;i<expected.length;i++)assertEquals(expected[i],ClockDatePolicy.format(i+1,date(),Locale.US));
    }
    @Test public void stockTimezoneAndLocaleControlDateAndCase(){
        Calendar c=date();c.setTimeZone(TimeZone.getTimeZone("America/New_York"));assertEquals("2026-10-12",ClockDatePolicy.format(11,c,Locale.US));
        c=date();Locale turkish=Locale.forLanguageTag("tr-TR");
        assertEquals("SALI",ClockDatePolicy.format(ClockDatePolicy.selection(5,1,3,1),c,turkish));
        assertEquals("october 13",ClockDatePolicy.format(ClockDatePolicy.selection(6,0,1,2),c,Locale.US));
    }
    @Test public void reservedBitsAndMalformedComponentsAreRejected(){
        assertFalse(ClockDatePolicy.valid(-1));assertFalse(ClockDatePolicy.valid(13));assertFalse(ClockDatePolicy.valid(384));assertFalse(ClockDatePolicy.valid(512));
        for(int[] bad:new int[][]{{13,0,0,0},{1,2,0,0},{1,0,4,0},{1,0,0,3},{-1,0,0,0}}){try{ClockDatePolicy.selection(bad[0],bad[1],bad[2],bad[3]);fail();}catch(IllegalArgumentException expected){}}
        for(int i=0;i<5;i++)assertEquals(i,ClockDatePolicy.selection(i,0,0,0));
    }
}
