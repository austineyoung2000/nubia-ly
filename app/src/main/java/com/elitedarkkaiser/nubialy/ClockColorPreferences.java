package com.elitedarkkaiser.nubialy;
import android.content.Context;
/** Save selection only. Neither read nor save starts a preview or changes recovery state. */
public final class ClockColorPreferences {
    private ClockColorPreferences() {}
    public static int read(Context context) {
        int color;
        try { color = context.getSharedPreferences("clock_editor", Context.MODE_PRIVATE)
            .getInt("selected_color", ClockColorPolicy.DEFAULT); }
        catch (ClassCastException invalid) { return ClockColorPolicy.DEFAULT; }
        return ClockColorPolicy.valid(color) ? color : ClockColorPolicy.DEFAULT;
    }
    public static boolean save(Context context, int color) {
        return ClockColorPolicy.valid(color) && context.getSharedPreferences("clock_editor", Context.MODE_PRIVATE)
            .edit().putInt("selected_color", color).commit();
    }
}
