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
    public static boolean allowEnabled(Context context) {
        String id = GuardReader.inspectEnabled();
        if (id == null || !ModuleStatus.isActive()) return false;
        return context.getSharedPreferences("recovery", Context.MODE_PRIVATE).edit()
            .putBoolean("emergency_stopped", false).putBoolean("restart_recovery_stopped", false).putString("enabled_clock_id", id).remove("startup_boot").remove("startup_pid").remove("startup_attempts").remove("sensor_startup_boot").remove("sensor_startup_pid").remove("sensor_startup_attempts").remove("launcher_startup_boot").remove("launcher_startup_pid").remove("launcher_startup_attempts").remove("sidebar_startup_boot").remove("sidebar_startup_pid").remove("sidebar_startup_attempts").commit();
    }
    public static boolean enabledAllowed(Context context, String id) {
        return !isStopped(context) && id != null && id.equals(context.getSharedPreferences("recovery", Context.MODE_PRIVATE).getString("enabled_clock_id", ""));
    }
    public static synchronized boolean claimStartup(Context context, int pid) {return claimStartup(context,pid,"startup_");}
    public static synchronized boolean claimSensorStartup(Context context,int pid){return claimStartup(context,pid,"sensor_startup_");}
    public static synchronized boolean claimLauncherStartup(Context context,int pid){return claimStartup(context,pid,"launcher_startup_");}
    public static synchronized boolean claimSidebarStartup(Context context,int pid){return claimStartup(context,pid,"sidebar_startup_");}
    private static boolean claimStartup(Context context,int pid,String prefix) {
        if (pid <= 0) return false;
        var prefs = context.getSharedPreferences("recovery", Context.MODE_PRIVATE);
        String boot = GuardReader.bootId(); if (!PreviewPolicy.uuid(boot)) return false;
        boolean sameBoot = boot.equals(prefs.getString(prefix+"boot", ""));
        int attempts = sameBoot ? prefs.getInt(prefix+"attempts", 0) : 0;
        if (sameBoot && prefs.getInt(prefix+"pid", -1) == pid) return true;
        if (!ClockEnabledPolicy.allowStartup(attempts)) {
            prefs.edit().remove("enabled_clock_id").putBoolean("emergency_stopped", true).putBoolean("restart_recovery_stopped", true).commit(); return false;
        }
        return prefs.edit().putString(prefix+"boot", boot).putInt(prefix+"pid", pid)
            .putInt(prefix+"attempts", attempts + 1).putBoolean("restart_recovery_stopped", false).commit();
    }
    public static boolean stop(Context context) {
        boolean launcherStopped=LauncherPreferences.stop(context);
        return context.getSharedPreferences("recovery", Context.MODE_PRIVATE).edit()
            .remove("enabled_clock_id").remove("clock_permit_id").putBoolean("emergency_stopped", true).putBoolean("lifecycle_test_allowed", false).commit() && launcherStopped;
    }
}
