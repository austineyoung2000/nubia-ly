package com.elitedarkkaiser.nubialy;

import android.os.SystemClock;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

/** Rootless read. SELinux denial, missing guard or malformed receipt all block the probe. */
public final class GuardReader {
    private static final String DIRECTORY = "/data/local/tmp/nubia-ly-guard";
    private GuardReader() {}
    public record Snapshot(boolean readable, String message) {}
    public static String inspect() { return inspectSnapshot().message(); }
    public static Snapshot inspectSnapshot() {
        try {
            if (!safeRootPath(DIRECTORY, true) || !safeRootPath(DIRECTORY + "/probe", false))
                return new Snapshot(false, "Recovery probe blocked: no valid root-owned receipt.");
            String bootId = boundedRead("/proc/sys/kernel/random/boot_id", 64).trim();
            String receipt = boundedRead(DIRECTORY + "/probe", 160);
            return GuardPolicy.probeValid(receipt, bootId, SystemClock.elapsedRealtime() / 1000)
                ? new Snapshot(true, "Recovery probe readable for this boot. Cosmetics remain blocked.")
                : new Snapshot(false, "Recovery probe blocked: expired, malformed or from another boot.");
        } catch (Exception unavailable) {
            return new Snapshot(false, "Recovery probe blocked: state missing or unreadable. No changes permitted.");
        }
    }
    public static PreviewPolicy.Permit inspectPreview() {
        try {
            if (!safeRootPath(DIRECTORY, true) || !safeRootPath(DIRECTORY + "/preview", false)) return null;
            return PreviewPolicy.parse(boundedRead(DIRECTORY + "/preview", 240),
                boundedRead("/proc/sys/kernel/random/boot_id", 64).trim(), SystemClock.elapsedRealtime());
        } catch (Exception unavailable) { return null; }
    }
    private static boolean safeRootPath(String path, boolean directory) throws Exception {
        StructStat stat = Os.lstat(path);
        return stat.st_uid == 0 && (stat.st_mode & 0022) == 0
            && (directory ? OsConstants.S_ISDIR(stat.st_mode) : OsConstants.S_ISREG(stat.st_mode))
            && new File(path).getCanonicalPath().equals(path);
    }
    private static String boundedRead(String path, int maximum) throws Exception {
        try (FileInputStream input = new FileInputStream(path)) {
            byte[] bytes = input.readNBytes(maximum + 1);
            if (bytes.length > maximum) throw new IllegalArgumentException("Oversized guard data");
            return new String(bytes, StandardCharsets.US_ASCII);
        }
    }
}
