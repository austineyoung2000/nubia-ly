package com.elitedarkkaiser.nubialy;
import org.junit.Test;
import static org.junit.Assert.*;
public final class SystemUiGuardProtocolTest {
    private static final String NONCE = "12345678-1234-1234-1234-123456789abc";
    @Test public void matchingFreshVersionRequired() {
        assertTrue(SystemUiGuardProtocol.accepts(NONCE, NONCE, 4, 4, 8000));
        assertFalse(SystemUiGuardProtocol.accepts(NONCE, NONCE, 4, 4, 8001));
        assertFalse(SystemUiGuardProtocol.accepts(NONCE, NONCE, 4, 3, 1));
        assertFalse(SystemUiGuardProtocol.accepts(NONCE, null, 4, 4, 1));
        assertFalse(SystemUiGuardProtocol.accepts(null, null, 4, 4, 1));
        assertFalse(SystemUiGuardProtocol.accepts(NONCE, NONCE, 4, 4, -1));
        assertFalse(SystemUiGuardProtocol.accepts("invalid", "invalid", 4, 4, 1));
    }
}
