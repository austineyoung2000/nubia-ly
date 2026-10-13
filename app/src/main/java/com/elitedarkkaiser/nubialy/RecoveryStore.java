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
    public static boolean allowClock(Context context) {
        PreviewPolicy.Permit permit = GuardReader.inspectClock();
        if (permit == null || !ModuleStatus.isActive()) return false;
        return context.getSharedPreferences("recovery", Context.MODE_PRIVATE).edit()
            .putString("clock_permit_id", permit.id()).commit();
    }
    public static boolean clockAllowed(Context context, PreviewPolicy.Permit permit) {
        return permit != null && permit.id().equals(context.getSharedPreferences("recovery", Context.MODE_PRIVATE)
            .getString("clock_permit_id", ""));
    }
    public static boolean stop(Context context) {
        return context.getSharedPreferences("recovery", Context.MODE_PRIVATE).edit()
            .remove("clock_permit_id").putBoolean("emergency_stopped", true).putBoolean("lifecycle_test_allowed", false).commit();
    }
}
