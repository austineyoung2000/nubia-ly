package com.elitedarkkaiser.nubialy;

public final class SystemUiGuardProtocol {
    public static final String PREVIEW_ACTION = "com.elitedarkkaiser.nubialy.PREVIEW_LIFECYCLE";
    public static final String ACTION = "com.elitedarkkaiser.nubialy.CHECK_SYSTEM_UI_GUARD";
    public static final String PERMISSION = "com.elitedarkkaiser.nubialy.permission.GUARD_DIAGNOSTIC";
    public static final int RESPONSE = 1201;
    public static final long DEADLINE_MS = 8000;
    private SystemUiGuardProtocol() {}
    public static boolean accepts(String requestNonce, String replyNonce, int version,
            int replyVersion, long elapsedMs) {
        return requestNonce != null && requestNonce.matches("[0-9a-f-]{36}")
            && requestNonce.equals(replyNonce) && version == replyVersion
            && elapsedMs >= 0 && elapsedMs <= DEADLINE_MS;
    }
}
