package com.elitedarkkaiser.nubialy;
import org.junit.Test;
import static org.junit.Assert.*;
public final class GuardPolicyTest {
    private static final String BOOT = "12345678-1234-1234-1234-123456789abc";
    private String receipt(String expires) { return "nubia-ly-probe-v1\n" + BOOT + "\n" + expires + "\nprobe-only\n"; }
    @Test public void currentBootAndBoundedLifetimeRequired() {
        assertTrue(GuardPolicy.probeValid(receipt("160"), BOOT, 100));
        assertFalse(GuardPolicy.probeValid(receipt("160"), BOOT, 160));
        assertFalse(GuardPolicy.probeValid(receipt("160"), BOOT, 99));
        assertFalse(GuardPolicy.probeValid(receipt("160"), "87654321-1234-1234-1234-123456789abc", 100));
    }
    @Test public void malformedAndApplyTokensRejected() {
        assertFalse(GuardPolicy.probeValid(null, BOOT, 100));
        assertFalse(GuardPolicy.probeValid(receipt("-1"), BOOT, 100));
        assertFalse(GuardPolicy.probeValid(receipt("999999999999999999999"), BOOT, 100));
        assertFalse(GuardPolicy.probeValid(receipt("160").replace("probe-only", "apply"), BOOT, 100));
        assertFalse(GuardPolicy.probeValid(receipt("160") + "extra", BOOT, 100));
    }
}
