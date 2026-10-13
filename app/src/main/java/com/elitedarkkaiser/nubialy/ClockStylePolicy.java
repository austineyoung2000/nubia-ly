package com.elitedarkkaiser.nubialy;
/** Relative size keeps the stock font-scale baseline. Zero denotes stock size. */
public final class ClockStylePolicy {
    private ClockStylePolicy() {}
    public static boolean valid(int percent) { return percent == 0 || (percent >= 80 && percent <= 120 && percent % 5 == 0); }
}
