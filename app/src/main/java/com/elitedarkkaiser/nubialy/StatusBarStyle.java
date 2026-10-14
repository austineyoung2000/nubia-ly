package com.elitedarkkaiser.nubialy;
/** Fixed, bounded view styling. No commands or paths are configuration. */
public record StatusBarStyle(int mask, int systemScale, int notificationScale, int batteryScale) {
    public static final int TINT=1, HIDE_SYSTEM=2, HIDE_NOTIFICATIONS=4, HIDE_BATTERY=8;
    public static final StatusBarStyle STOCK = new StatusBarStyle(0,100,100,100);
    public boolean valid() { return mask >= 0 && mask <= 15 && scale(systemScale) && scale(notificationScale) && scale(batteryScale); }
    public boolean selected(int flag) { return (mask & flag) != 0; }
    public boolean any() { return mask != 0 || systemScale != 100 || notificationScale != 100 || batteryScale != 100; }
    private static boolean scale(int n) { return n >= 80 && n <= 120 && n % 5 == 0; }
}
