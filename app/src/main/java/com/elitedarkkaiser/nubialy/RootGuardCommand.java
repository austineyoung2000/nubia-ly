package com.elitedarkkaiser.nubialy;
import android.os.SystemClock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
/** One fixed module command; dispatched work is never replayed after timeout. */
final class RootGuardCommand {
    private static final AtomicBoolean busy = new AtomicBoolean();
    static boolean enableClock(long deadline) { return execute("enable_clock", deadline); }
    static boolean stopClock(long deadline) { return execute("stop", deadline); }
    private static boolean execute(String command, long deadline) {
        if (SystemClock.elapsedRealtime() >= deadline || !busy.compareAndSet(false, true)) return false;
        Process process = null; boolean deferred = false;
        try {
            process = new ProcessBuilder("su", "-c", "sh /data/adb/modules/nubia_ly_guard/guardctl.sh " + command)
                .redirectErrorStream(true).start();
            long remaining = deadline - SystemClock.elapsedRealtime();
            if (remaining <= 0 || !process.waitFor(remaining, TimeUnit.MILLISECONDS)) {
                Process pending = process; process = null; deferred = true;
                new Thread(() -> { try { pending.waitFor(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    finally { close(pending); busy.set(false); } }, "nubia-ly-root-completion").start();
                return false;
            }
            return process.exitValue() == 0 && SystemClock.elapsedRealtime() < deadline;
        } catch (Exception unavailable) {
            if (process != null && process.isAlive()) {
                Process pending = process; deferred = true; process = null;
                new Thread(() -> { try { pending.waitFor(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    finally { close(pending); busy.set(false); } }, "nubia-ly-root-completion").start();
            }
            return false;
        }
        finally { if (process != null) { close(process); busy.set(false); } else if (!deferred) busy.set(false); }
    }
    private static void close(Process p) {
        try { p.getInputStream().close(); p.getOutputStream().close(); p.getErrorStream().close(); } catch (Exception ignored) {}
    }
}
