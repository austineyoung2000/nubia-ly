package com.elitedarkkaiser.nubialy;

/** Lifecycle-only permits never authorize cosmetic hooks. */
public final class PreviewPolicy {
    private PreviewPolicy() {}
    public record Permit(String id, long deadlineMs) {}
    public static Permit parse(String receipt, String bootId, long nowMs) {
        if (receipt == null || receipt.length() > 240 || bootId == null || nowMs < 0) return null;
        String[] fields = receipt.split("\n", -1);
        if (fields.length != 6 || !"nubia-ly-preview-v1".equals(fields[0])
                || !bootId.equals(fields[1]) || !"lifecycle-only".equals(fields[3])
                || !uuid(bootId) || !uuid(fields[4]) || !fields[5].isEmpty()
                || !fields[2].matches("[0-9]{1,12}")) return null;
        try {
            long deadline = Math.multiplyExact(Long.parseLong(fields[2]), 1000);
            return deadline > nowMs && deadline - nowMs <= 60000
                ? new Permit(fields[4], deadline) : null;
        } catch (ArithmeticException | NumberFormatException invalid) { return null; }
    }
    public static boolean uuid(String value) {
        return value != null && value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
