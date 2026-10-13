package com.elitedarkkaiser.nubialy;

/** A probe receipt proves only guard transport, never permission to apply cosmetics. */
public final class GuardPolicy {
    private GuardPolicy() {}
    public static boolean probeValid(String receipt, String bootId, long uptimeSeconds) {
        if (receipt == null || receipt.length() > 160 || bootId == null
                || !bootId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                || uptimeSeconds < 0) return false;
        String[] fields = receipt.split("\n", -1);
        if (fields.length != 5 || !"nubia-ly-probe-v1".equals(fields[0])
                || !bootId.equals(fields[1]) || !"probe-only".equals(fields[3])
                || !fields[4].isEmpty() || !fields[2].matches("[0-9]{1,12}")) return false;
        try {
            long expires = Long.parseLong(fields[2]);
            return expires > uptimeSeconds && expires - uptimeSeconds <= 60;
        } catch (NumberFormatException invalid) { return false; }
    }
}
