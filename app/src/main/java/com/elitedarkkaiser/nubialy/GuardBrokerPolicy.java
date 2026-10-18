package com.elitedarkkaiser.nubialy;

/** Binder UID plus framework-verified package attribution; no caller-supplied identity. */
public final class GuardBrokerPolicy {
    public static final String LAUNCHER_PACKAGE="com.zte.mifavor.launcher", LAUNCHER_METHOD="inspect_launcher_enabled", LAUNCHER_ACTION="com.elitedarkkaiser.nubialy.LAUNCHER";
    public static boolean permitsLauncher(int uid,int launcherUid,String verifiedPackage){return launcherUid>=0&&uid==launcherUid&&LAUNCHER_PACKAGE.equals(verifiedPackage);}
    public static final String SENSOR_PACKAGE="com.fingerprint.sensorservice";
    public static final String FINGERPRINT_METHOD="inspect_fingerprint_enabled", FINGERPRINT_ACTION="com.elitedarkkaiser.nubialy.FINGERPRINT";
    public static boolean permitsFingerprint(int uid,int sensorUid,String verifiedPackage){return sensorUid>=0&&uid==sensorUid&&SENSOR_PACKAGE.equals(verifiedPackage);}
    public static final String AUTHORITY = "com.elitedarkkaiser.nubialy.guard";
    public static final String ENABLED_METHOD = "inspect_clock_enabled", STARTUP_METHOD = "claim_clock_enabled";
    public static final String CLOCK_METHOD = "inspect_clock";
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
