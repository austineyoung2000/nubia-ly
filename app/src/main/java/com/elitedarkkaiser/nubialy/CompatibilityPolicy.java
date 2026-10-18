package com.elitedarkkaiser.nubialy;

/** Pure policy. Matching a build identifies the research baseline; it never grants apply access. */
public final class CompatibilityPolicy {
    public enum Status { UNSUPPORTED_DEVICE, UNRECOGNIZED_FIRMWARE, INSPECTION_UNAVAILABLE, RESEARCH_BASELINE }
    private CompatibilityPolicy() {}
    public static boolean isNx809j(String model, String device) {
        return "NX809J".equals(model) || "NX809J-UN".equals(model) || "NX809J".equals(device);
    }
    /** Fork gate: REDMAGIC 11 Pro (NX809J) and REDMAGIC 10 Pro / 10S Pro (NX789J), any region suffix. */
    public static boolean isSupportedDevice(String model, String device) {
        if (isNx809j(model, device)) return true;
        return (model != null && model.startsWith("NX789J")) || "NX789J".equals(device);
    }
    /** Fork gate: Android 15 (35) or Android 16 (36). */
    public static boolean isSupportedSdk(int sdk) { return sdk == 35 || sdk == 36; }
    public static Status classify(String model, String device, int sdk, String fingerprint, String hash, String baselineFingerprint, String baselineHash) {
        if (!isSupportedDevice(model, device)) return Status.UNSUPPORTED_DEVICE;
        if (baselineFingerprint == null || baselineFingerprint.isBlank() || baselineHash == null || baselineHash.isBlank()) return Status.UNRECOGNIZED_FIRMWARE;
        if (!isSupportedSdk(sdk) || !baselineFingerprint.equals(fingerprint)) return Status.UNRECOGNIZED_FIRMWARE;
        if (hash == null || hash.isBlank()) return Status.INSPECTION_UNAVAILABLE;
        if (!baselineHash.equals(hash)) return Status.UNRECOGNIZED_FIRMWARE;
        return Status.RESEARCH_BASELINE;
    }
}
