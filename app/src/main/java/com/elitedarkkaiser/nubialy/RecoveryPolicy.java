package com.elitedarkkaiser.nubialy;

/** Every prerequisite must pass. No current build supplies a validated recovery backend. */
public final class RecoveryPolicy {
    private RecoveryPolicy() {}
    public static boolean canApply(boolean emergencyStopped, boolean moduleActive,
            boolean baselineValidated, boolean recoveryVerified, boolean featureValidated) {
        return !emergencyStopped && moduleActive && baselineValidated
            && recoveryVerified && featureValidated;
    }
}
