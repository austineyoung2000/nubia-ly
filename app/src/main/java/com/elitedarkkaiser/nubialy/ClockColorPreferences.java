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
    public static int size(Context context) {
        try { int v = context.getSharedPreferences("clock_editor", Context.MODE_PRIVATE).getInt("size_percent", 0); return ClockStylePolicy.valid(v) ? v : 0; }
        catch (ClassCastException invalid) { return 0; }
    }
    public static boolean bold(Context context) {
        try { return context.getSharedPreferences("clock_editor", Context.MODE_PRIVATE).getBoolean("bold", false); }
        catch (ClassCastException invalid) { return false; }
    }
    public static boolean saveStyle(Context context, int percent, boolean bold) {
        return ClockStylePolicy.valid(percent) && context.getSharedPreferences("clock_editor", Context.MODE_PRIVATE)
            .edit().putInt("size_percent", percent).putBoolean("bold", bold).commit();
    }
    public static boolean save(Context context, int color) {
        return ClockColorPolicy.valid(color) && context.getSharedPreferences("clock_editor", Context.MODE_PRIVATE)
            .edit().putInt("selected_color", color).commit();
    }
}
