package com.elitedarkkaiser.nubialy;
import java.util.*;
/** Fixed, bounded view styling. No commands or paths are configuration. */
public record StatusBarStyle(int mask, int systemScale, int notificationScale, int batteryScale,
        int systemOpacity, int notificationOpacity, int batteryOpacity, Set<String> hiddenSlots, int systemTransform, int notificationTransform, int batteryTransform, int fanAppearance, int pumpAppearance, int refreshAppearance) {
    public StatusBarStyle { hiddenSlots=hiddenSlots==null?Set.of("invalid\n"):Set.copyOf(hiddenSlots); }
    public StatusBarStyle(int mask,int systemScale,int notificationScale,int batteryScale,int systemOpacity,int notificationOpacity,int batteryOpacity,Set<String> hiddenSlots,int systemTransform,int notificationTransform,int batteryTransform){this(mask,systemScale,notificationScale,batteryScale,systemOpacity,notificationOpacity,batteryOpacity,hiddenSlots,systemTransform,notificationTransform,batteryTransform,0,0,0);}
    public StatusBarStyle(int mask,int systemScale,int notificationScale,int batteryScale,int systemOpacity,int notificationOpacity,int batteryOpacity,Set<String> hiddenSlots){this(mask,systemScale,notificationScale,batteryScale,systemOpacity,notificationOpacity,batteryOpacity,hiddenSlots,0,0,0);}
    public StatusBarStyle(int mask,int systemScale,int notificationScale,int batteryScale){this(mask,systemScale,notificationScale,batteryScale,100,100,100,Set.of());}
    public static final int TINT=1, HIDE_SYSTEM=2, HIDE_NOTIFICATIONS=4, HIDE_BATTERY=8, HIDE_PUMP=16, HIDE_FAN=32, HIDE_REFRESH=64;
    public static final StatusBarStyle STOCK = new StatusBarStyle(0,100,100,100);
    public boolean valid() { return mask >= 0 && mask <= 127 && scale(systemScale) && scale(notificationScale) && scale(batteryScale)
        && appearanceValid(fanAppearance)&&appearanceValid(pumpAppearance)&&appearanceValid(refreshAppearance)
        && transformValid(systemTransform)&&transformValid(notificationTransform)&&transformValid(batteryTransform)
        && opacity(systemOpacity)&&opacity(notificationOpacity)&&opacity(batteryOpacity)&&hiddenSlots.size()<=32&&hiddenSlots.stream().allMatch(StatusBarStyle::slot); }
    public boolean selected(int flag) { return (mask & flag) != 0; }
    public boolean any() { return mask != 0 || systemScale != 100 || notificationScale != 100 || batteryScale != 100
        ||fanAppearance!=0||pumpAppearance!=0||refreshAppearance!=0||systemTransform!=0||notificationTransform!=0||batteryTransform!=0||systemOpacity!=100||notificationOpacity!=100||batteryOpacity!=100||!hiddenSlots.isEmpty(); }
    public static int indicatorScale(int value){return 100+(byte)value;}
    public static int indicatorOpacity(int value){return 100+(byte)(value>>8);}
    public static boolean appearanceValid(int value){return (value&0xffff0000)==0&&scale(indicatorScale(value))&&opacity(indicatorOpacity(value));}
    public static int appearance(int scale,int opacity){if(!scale(scale)||!opacity(opacity))throw new IllegalArgumentException();return ((scale-100)&255)|(((opacity-100)&255)<<8);}
    public static int horizontal(int transform){return (byte)transform;}
    public static int vertical(int transform){return (byte)(transform>>8);}
    public static int rotation(int transform){return (byte)(transform>>16);}
    public static boolean transformValid(int value){return (value&0xff000000)==0&&offset(horizontal(value))&&offset(vertical(value))&&rotation(value)>=-20&&rotation(value)<=20&&rotation(value)%5==0;}
    private static boolean offset(int value){return value>=-8&&value<=8&&value%2==0;}
    public static int transform(int horizontal,int vertical,int rotation){
        if(!offset(horizontal)||!offset(vertical)||rotation<-20||rotation>20||rotation%5!=0)throw new IllegalArgumentException();
        return (horizontal&255)|((vertical&255)<<8)|((rotation&255)<<16);
    }
    public static boolean slot(String value){return value!=null&&value.matches("[A-Za-z0-9_.:-]{1,64}");}
    public static Set<String> decodeSlots(String value){if(value==null||value.length()>2112)return null;if(value.isEmpty())return Set.of();String[] fields=value.split("\\n",-1);if(fields.length>32)return null;Set<String> result=new TreeSet<>();for(String v:fields){if(!slot(v))return null;result.add(v);}return Set.copyOf(result);}
    public String slotsText(){return String.join("\n",new TreeSet<>(hiddenSlots));}
    private static boolean scale(int n) { return n >= 80 && n <= 120 && n % 5 == 0; }
    private static boolean opacity(int n){return n>=25&&n<=100&&n%5==0;}
}
