package com.elitedarkkaiser.nubialy;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
/** Compact, validated date selection; original format IDs remain unchanged. */
public final class ClockDatePolicy {
    private ClockDatePolicy() {}
    public static final String[] LABELS={"Off","Day of week","Month & day","Day & date","Numeric date","Full weekday","Full month & day","Day & month","Day/month","Month/day/year","Day/month/year","ISO date","Full day & date"};
    private static final String[] PATTERNS={"","EEE","MMM d","EEE, MMM d","MM/dd","EEEE","MMMM d","d MMM","dd/MM","MM/dd/yyyy","dd/MM/yyyy","yyyy-MM-dd","EEEE, MMMM d"};
    public static final String[] POSITIONS={"After time","Before time"};
    public static final String[] SEPARATOR_LABELS={"Dot","Space","Dash","Bar"};
    private static final String[] SEPARATORS={" · "," "," — "," | "};
    public static final String[] CASE_LABELS={"Stock case","UPPERCASE","lowercase"};
    public static int base(int mode){return mode&15;}
    public static int position(int mode){return (mode>>4)&1;}
    public static int separator(int mode){return (mode>>5)&3;}
    public static int letterCase(int mode){return (mode>>7)&3;}
    public static boolean valid(int mode){return mode>=0&&mode<384&&base(mode)<PATTERNS.length&&letterCase(mode)<3;}
    public static int selection(int format,int position,int separator,int letterCase){
        if(format<0||format>=PATTERNS.length||position<0||position>1||separator<0||separator>3||letterCase<0||letterCase>2)throw new IllegalArgumentException();
        return format|(position<<4)|(separator<<5)|(letterCase<<7);
    }
    public static String separatorText(int mode){if(!valid(mode))throw new IllegalArgumentException();return SEPARATORS[separator(mode)];}
    public static String format(int mode,Calendar calendar,Locale locale){
        if(!valid(mode)||base(mode)==0||calendar==null)throw new IllegalArgumentException();
        Locale effective=locale==null?Locale.getDefault():locale;
        SimpleDateFormat format=new SimpleDateFormat(PATTERNS[base(mode)],effective);
        format.setTimeZone(calendar.getTimeZone());String value=format.format(calendar.getTime());
        return switch(letterCase(mode)){case 1->value.toUpperCase(effective);case 2->value.toLowerCase(effective);default->value;};
    }
}
