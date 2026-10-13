package com.elitedarkkaiser.nubialy;
import org.junit.Test;
import static org.junit.Assert.*;
public final class PreviewLifecycleTest {
    private static final String ID = "12345678-1234-1234-1234-123456789abc";
    @Test public void startAdmissionIncludesQueueTime() {
        assertTrue(SystemUiGuardProtocol.admitted(9000, 1000));
        assertFalse(SystemUiGuardProtocol.admitted(9000, 9000));
        assertFalse(SystemUiGuardProtocol.admitted(9000, 999));
        assertFalse(SystemUiGuardProtocol.admitted(0, 1000));
    }
    @Test public void stoppedAttemptCannotBeActivatedByLateWorker() {
        PreviewLifecycle session = new PreviewLifecycle(); long ticket = session.begin(); session.stop();
        assertFalse(session.activate(ticket, new PreviewPolicy.Permit(ID, 61000), 1000));
        assertEquals(PreviewLifecycle.Phase.STOPPED, session.state(1000));
    }
    @Test public void replacingOrExtendingPermitEndsSession() {
        PreviewLifecycle session = new PreviewLifecycle(); long ticket = session.begin();
        assertTrue(session.activate(ticket, new PreviewPolicy.Permit(ID, 61000), 1000));
        assertFalse(session.validate(ticket, new PreviewPolicy.Permit(ID, 62000), 3000));
        assertEquals(PreviewLifecycle.Phase.STOPPED, session.state(3000));
    }
    @Test public void deadlineAndUnavailableBrokerFailClosed() {
        PreviewLifecycle session = new PreviewLifecycle(); long ticket = session.begin();
        assertTrue(session.activate(ticket, new PreviewPolicy.Permit(ID, 61000), 1000));
        assertEquals(PreviewLifecycle.Phase.EXPIRED, session.state(61000));
        assertFalse(session.validate(ticket, new PreviewPolicy.Permit(ID, 121000), 61000));
        ticket = session.begin(); assertFalse(session.activate(ticket, null, 1000));
    }
    @Test public void reconstructedSessionNeverRestartsAutomatically() {
        PreviewLifecycle recreated = new PreviewLifecycle();
        assertEquals(PreviewLifecycle.Phase.IDLE, recreated.state(1000));
        assertFalse(recreated.activate(0, new PreviewPolicy.Permit(ID, 61000), 1000));
    }
}
