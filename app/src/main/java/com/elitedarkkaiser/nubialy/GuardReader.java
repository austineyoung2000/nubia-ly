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
            String receipt = readGuarded(DIRECTORY + "/probe", 160);
            if (receipt == null) return new Snapshot(false, "Recovery probe blocked: no valid root-owned receipt.");
            String bootId = boundedRead("/proc/sys/kernel/random/boot_id", 64).trim();
            return GuardPolicy.probeValid(receipt, bootId, SystemClock.elapsedRealtime() / 1000)
                ? new Snapshot(true, "Recovery probe readable for this boot. Cosmetics remain blocked.")
                : new Snapshot(false, "Recovery probe blocked: expired, malformed or from another boot.");
        } catch (Exception unavailable) {
            return new Snapshot(false, "Recovery probe blocked: state missing or unreadable. No changes permitted.");
        }
    }
    public static PreviewPolicy.Permit inspectPreview() {
        try {
            String receipt = readGuarded(DIRECTORY + "/preview", 240);
            if (receipt == null) return null;
            return PreviewPolicy.parse(receipt,
                boundedRead("/proc/sys/kernel/random/boot_id", 64).trim(), SystemClock.elapsedRealtime());
        } catch (Exception unavailable) { return null; }
    }
    public static PreviewPolicy.Permit inspectClock() {
        try {
            String receipt = readGuarded(DIRECTORY + "/clock", 240);
            if (receipt == null) return null;
            return ClockPermitPolicy.parse(receipt,
                boundedRead("/proc/sys/kernel/random/boot_id", 64).trim(), SystemClock.elapsedRealtime());
        } catch (Exception unavailable) { return null; }
    }
    public static String bootId() {
        try { return boundedRead("/proc/sys/kernel/random/boot_id", 64).trim(); }
        catch (Exception unavailable) { return ""; }
    }
    public static String inspectEnabled() {
        try {
            String receipt = readGuarded(DIRECTORY + "/enabled", 240);
            if (receipt == null) return null;
            return ClockEnabledPolicy.parse(receipt, bootId());
        } catch (Exception unavailable) { return null; }
    }
    /**
     * Rootless read first. Some firmware (e.g. REDMAGIC 10 Pro, Android 15) denies app reads of
     * shell_data_file under SELinux; then read through root while re-applying the identical
     * integrity checks (root-owned, no group/world write, regular file in a root directory,
     * canonical path). Returns null when a check fails; throws only on transport failure.
     */
    private static String readGuarded(String file, int maximum) throws Exception {
        try {
            if (!safeRootPath(DIRECTORY, true) || !safeRootPath(file, false)) return null;
            return boundedRead(file, maximum);
        } catch (android.system.ErrnoException denied) {
            if (denied.errno != OsConstants.EACCES && denied.errno != OsConstants.EPERM) throw denied;
        } catch (java.io.FileNotFoundException denied) {
            if (!String.valueOf(denied.getMessage()).contains("EACCES") && !String.valueOf(denied.getMessage()).contains("Permission denied")) throw denied;
        }
        return rootRead(file, maximum);
    }
    private static String rootRead(String file, int maximum) throws Exception {
        if (!file.startsWith(DIRECTORY + "/") || !file.substring(DIRECTORY.length() + 1).matches("[a-z]{1,16}")) return null;
        String script = "d='" + DIRECTORY + "'; f='" + file + "'; "
            + "stat -c '%u %a %F' \"$d\" || exit 3; stat -c '%u %a %F' \"$f\" || exit 3; "
            + "readlink -f \"$d\"; readlink -f \"$f\"; echo __NUBIALY_BODY__; head -c " + (maximum + 1) + " \"$f\"";
        Process process = new ProcessBuilder("su", "-c", script).redirectErrorStream(true).start();
        GuardRootProcess.Result result=GuardRootProcess.capture(process,5000,4096);
        if(result.exitCode()!=0)return null;
        byte[] out=result.output();
        String text = new String(out, StandardCharsets.US_ASCII);
        int marker = text.indexOf("__NUBIALY_BODY__\n");
        if (marker < 0) return null;
        String[] meta = text.substring(0, marker).split("\n", -1);
        if (meta.length < 4) return null;
        if (!rootMeta(meta[0], "directory") || !rootMeta(meta[1], "regular file")) return null;
        if (!DIRECTORY.equals(meta[2]) || !file.equals(meta[3])) return null;
        String body = text.substring(marker + "__NUBIALY_BODY__\n".length());
        if (body.length() > maximum) throw new IllegalArgumentException("Oversized guard data");
        return body;
    }
    /** "uid mode type" from stat: uid must be 0 and mode must not carry group/world write bits. */
    private static boolean rootMeta(String line, String type) {
        String[] f = line.trim().split(" ", 3);
        if (f.length != 3 || !"0".equals(f[0]) || !type.equals(f[2])) return false;
        try { return (Integer.parseInt(f[1], 8) & 0022) == 0; } catch (NumberFormatException e) { return false; }
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
