package com.elitedarkkaiser.nubialy;
import java.util.*;
/** Fixed, bounded view styling. No commands or paths are configuration. */
public record StatusBarStyle(int mask, int systemScale, int notificationScale, int batteryScale,
        int systemOpacity, int notificationOpacity, int batteryOpacity, Set<String> hiddenSlots) {
    public StatusBarStyle { hiddenSlots=hiddenSlots==null?Set.of("invalid\n"):Set.copyOf(hiddenSlots); }
    public StatusBarStyle(int mask,int systemScale,int notificationScale,int batteryScale){this(mask,systemScale,notificationScale,batteryScale,100,100,100,Set.of());}
    public static final int TINT=1, HIDE_SYSTEM=2, HIDE_NOTIFICATIONS=4, HIDE_BATTERY=8;
    public static final StatusBarStyle STOCK = new StatusBarStyle(0,100,100,100);
    public boolean valid() { return mask >= 0 && mask <= 15 && scale(systemScale) && scale(notificationScale) && scale(batteryScale)
        && opacity(systemOpacity)&&opacity(notificationOpacity)&&opacity(batteryOpacity)&&hiddenSlots.size()<=32&&hiddenSlots.stream().allMatch(StatusBarStyle::slot); }
    public boolean selected(int flag) { return (mask & flag) != 0; }
    public boolean any() { return mask != 0 || systemScale != 100 || notificationScale != 100 || batteryScale != 100
        ||systemOpacity!=100||notificationOpacity!=100||batteryOpacity!=100||!hiddenSlots.isEmpty(); }
    public static boolean slot(String value){return value!=null&&value.matches("[A-Za-z0-9_.:-]{1,64}");}
    public static Set<String> decodeSlots(String value){if(value==null||value.length()>2112)return null;if(value.isEmpty())return Set.of();String[] fields=value.split("\\n",-1);if(fields.length>32)return null;Set<String> result=new TreeSet<>();for(String v:fields){if(!slot(v))return null;result.add(v);}return Set.copyOf(result);}
    public String slotsText(){return String.join("\n",new TreeSet<>(hiddenSlots));}
    private static boolean scale(int n) { return n >= 80 && n <= 120 && n % 5 == 0; }
    private static boolean opacity(int n){return n>=25&&n<=100&&n%5==0;}
}
