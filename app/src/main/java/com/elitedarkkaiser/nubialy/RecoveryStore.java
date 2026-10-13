package com.elitedarkkaiser.nubialy;

import android.content.Context;

/** Private, persistent latch. Cosmetics default to stopped; lifecycle simulation is a separate explicit opt-in. */
public final class RecoveryStore {
    private RecoveryStore() {}
    public static boolean isStopped(Context context) {
        return context.getSharedPreferences("recovery", Context.MODE_PRIVATE)
            .getBoolean("emergency_stopped", true);
    }
    public static boolean lifecycleAllowed(Context context) {
        return context.getSharedPreferences("recovery", Context.MODE_PRIVATE)
            .getBoolean("lifecycle_test_allowed", false);
    }
    public static boolean allowLifecycle(Context context) {
        if (GuardReader.inspectPreview() == null) return false;
        return context.getSharedPreferences("recovery", Context.MODE_PRIVATE).edit()
            .putBoolean("lifecycle_test_allowed", true).commit();
    }
    public static boolean stop(Context context) {
        return context.getSharedPreferences("recovery", Context.MODE_PRIVATE).edit()
            .putBoolean("emergency_stopped", true).putBoolean("lifecycle_test_allowed", false).commit();
    }
}
