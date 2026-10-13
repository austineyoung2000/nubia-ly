package com.elitedarkkaiser.nubialy;
/** Boot-scoped enabled authority. No user-facing duration or expiration. */
public final class ClockEnabledPolicy {
    public static final long HEALTH_MS = 15000;
    private ClockEnabledPolicy() {}
    public static String parse(String receipt, String boot) {
        if (receipt == null || receipt.length() > 240 || !PreviewPolicy.uuid(boot)) return null;
        String[] f = receipt.split("\n", -1);
        return f.length == 5 && f[0].equals("nubia-ly-clock-enabled-v1") && f[1].equals(boot)
            && f[2].equals("clock-enabled") && PreviewPolicy.uuid(f[3]) && f[4].isEmpty() ? f[3] : null;
    }
    public static boolean allowStartup(int attempts) { return attempts >= 0 && attempts < 3; }
}
