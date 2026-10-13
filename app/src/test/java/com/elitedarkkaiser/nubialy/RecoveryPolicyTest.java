package com.elitedarkkaiser.nubialy;
import org.junit.Test;
import static org.junit.Assert.*;
public final class RecoveryPolicyTest {
    @Test public void everyMissingPrerequisiteBlocksApplication() {
        assertFalse(RecoveryPolicy.canApply(true, true, true, true, true));
        assertFalse(RecoveryPolicy.canApply(false, false, true, true, true));
        assertFalse(RecoveryPolicy.canApply(false, true, false, true, true));
        assertFalse(RecoveryPolicy.canApply(false, true, true, false, true));
        assertFalse(RecoveryPolicy.canApply(false, true, true, true, false));
        assertTrue(RecoveryPolicy.canApply(false, true, true, true, true));
    }
    @Test public void shippedFoundationCannotApplyEvenWithActivationAndBaseline() {
        assertFalse(RecoveryPolicy.canApply(false, true, true, false, false));
    }
    @Test public void ordinaryAppCannotClaimActivation() { assertFalse(ModuleStatus.isActive()); }
}
