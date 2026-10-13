package com.elitedarkkaiser.nubialy;

import android.content.Context;

/** Private, persistent latch. Missing state defaults to stopped; this milestone has no arm API. */
public final class RecoveryStore {
    private RecoveryStore() {}
    public static boolean isStopped(Context context) {
        return context.getSharedPreferences("recovery", Context.MODE_PRIVATE)
            .getBoolean("emergency_stopped", true);
    }
    public static boolean stop(Context context) {
        return context.getSharedPreferences("recovery", Context.MODE_PRIVATE).edit()
            .putBoolean("emergency_stopped", true).commit();
    }
}
