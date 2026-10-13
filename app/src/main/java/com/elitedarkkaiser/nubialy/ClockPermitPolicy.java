package com.elitedarkkaiser.nubialy;

/** Separate 30-second visual permit. Lifecycle/probe receipts never grant color authority. */
public final class ClockPermitPolicy {
    private ClockPermitPolicy() {}
    public static PreviewPolicy.Permit parse(String receipt, String bootId, long now) {
        if (receipt == null || !receipt.startsWith("nubia-ly-clock-v1\n")) return null;
        String[] fields = receipt.split("\n", -1);
        if (fields.length != 6 || !"clock-color-only".equals(fields[3])) return null;
        PreviewPolicy.Permit permit = PreviewPolicy.parse(receipt.replace("nubia-ly-clock-v1\n", "nubia-ly-preview-v1\n")
            .replace("\nclock-color-only\n", "\nlifecycle-only\n"), bootId, now);
        return permit != null && permit.deadlineMs() - now <= 30000 ? permit : null;
    }
}
