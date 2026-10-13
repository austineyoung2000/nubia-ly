package com.elitedarkkaiser.nubialy;
import org.junit.Test;
import static org.junit.Assert.*;
public final class GuardBrokerPolicyTest {
    @Test public void onlyResolvedUidAndVerifiedSystemUiAttributionAccepted() {
        assertTrue(GuardBrokerPolicy.permits(10042, 10042, "com.android.systemui"));
        assertFalse(GuardBrokerPolicy.permits(10043, 10042, "com.android.systemui"));
        assertFalse(GuardBrokerPolicy.permits(10042, 10042, "other.package"));
        assertFalse(GuardBrokerPolicy.permits(10042, 10042, null));
        assertFalse(GuardBrokerPolicy.permits(0, -1, "com.android.systemui"));
        assertFalse(GuardBrokerPolicy.permits(-1, -1, "com.android.systemui"));
    }
    @Test public void unknownOrUnavailableStatusCannotBecomeValidEvidence() {
        assertTrue(GuardBrokerPolicy.validStatus(GuardBrokerPolicy.READABLE));
        assertTrue(GuardBrokerPolicy.validStatus(GuardBrokerPolicy.BLOCKED));
        assertTrue(GuardBrokerPolicy.validStatus(GuardBrokerPolicy.BUSY));
        assertFalse(GuardBrokerPolicy.validStatus(GuardBrokerPolicy.UNAVAILABLE));
        assertFalse(GuardBrokerPolicy.validStatus(999));
    }
}
