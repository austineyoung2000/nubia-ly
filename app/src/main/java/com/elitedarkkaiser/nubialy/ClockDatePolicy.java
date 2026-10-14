package com.elitedarkkaiser.nubialy;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
public final class ClockDatePolicy {
    private ClockDatePolicy() {}
    public static final String[] LABELS={"Off","Day of week","Month & day","Day & date","Numeric date"};
    private static final String[] PATTERNS={"","EEE","MMM d","EEE, MMM d","MM/dd"};
    public static boolean valid(int mode){return mode>=0&&mode<PATTERNS.length;}
    public static String format(int mode,Calendar calendar,Locale locale){
        if(!valid(mode)||mode==0||calendar==null)throw new IllegalArgumentException();
        SimpleDateFormat format=new SimpleDateFormat(PATTERNS[mode],locale==null?Locale.getDefault():locale);
        format.setTimeZone(calendar.getTimeZone());return format.format(calendar.getTime());
    }
}
