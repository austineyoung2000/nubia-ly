package com.elitedarkkaiser.nubialy;

import java.util.concurrent.TimeUnit;

/** The only root payload in this build is an explicit id -u request. */
final class RootInspector {
    static String inspect() {
        Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", "id -u").redirectErrorStream(true).start();
            if (!process.waitFor(8, TimeUnit.SECONDS)) return "Root check timed out; grant access and try again.";
            byte[] output = process.getInputStream().readNBytes(1024);
            if (process.exitValue() == 0 && new String(output, java.nio.charset.StandardCharsets.UTF_8).trim().equals("0"))
                return "Root access available. No customization was applied.";
            return "Root access was not granted.";
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return "Root check cancelled.";
        } catch (Exception error) {
            return "Root manager unavailable.";
        } finally {
            if (process != null) {
                process.destroy();
                if (process.isAlive()) process.destroyForcibly();
                try { process.getInputStream().close(); process.getOutputStream().close(); process.getErrorStream().close(); }
                catch (Exception ignored) {}
            }
        }
    }
}
