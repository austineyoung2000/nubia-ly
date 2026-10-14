package com.elitedarkkaiser.nubialy;
/** Relative size keeps the stock font-scale baseline. Zero denotes stock size. */
public final class ClockStylePolicy {
    private ClockStylePolicy() {}
    public static boolean validFamily(int family) { return family >= 0 && family <= 3; }
    public static String familyName(int family) { return switch (family) { case 1 -> "sans-serif"; case 2 -> "serif"; case 3 -> "monospace"; default -> null; }; }
    public static boolean validSpacing(int hundredths) { return hundredths >= -5 && hundredths <= 15; }
    public static boolean valid(int percent) { return percent == 0 || (percent >= 80 && percent <= 120 && percent % 5 == 0); }
}
