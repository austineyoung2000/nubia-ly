package com.elitedarkkaiser.nubialy;

/** Binder UID plus framework-verified package attribution; no caller-supplied identity. */
public final class GuardBrokerPolicy {
    public static final String AUTHORITY = "com.elitedarkkaiser.nubialy.guard";
    public static final String PREVIEW_METHOD = "inspect_preview";
    public static final String METHOD = "inspect_probe";
    public static final int UNAVAILABLE = 0, BLOCKED = 1, READABLE = 2, BUSY = 3;
    private GuardBrokerPolicy() {}
    public static boolean permits(int callingUid, int systemUiUid, String verifiedPackage) {
        return systemUiUid >= 0 && callingUid == systemUiUid
            && "com.android.systemui".equals(verifiedPackage);
    }
    public static boolean validStatus(int status) {
        return status == BLOCKED || status == READABLE || status == BUSY;
    }
}
