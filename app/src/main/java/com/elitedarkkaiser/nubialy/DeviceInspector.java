package com.elitedarkkaiser.nubialy;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import java.io.FileInputStream;
import java.security.MessageDigest;

/** Inspection only: reads the installed System UI APK without root or changing package state. */
final class DeviceInspector {
    record Snapshot(CompatibilityPolicy.Status status, String fingerprint, String systemUiHash) {}
    static Snapshot inspect(Context context) {
        String fingerprint = Build.FINGERPRINT;
        if (!CompatibilityPolicy.isSupportedDevice(Build.MODEL, Build.DEVICE))
            return new Snapshot(CompatibilityPolicy.Status.UNSUPPORTED_DEVICE, fingerprint, "");
        String hash = "";
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo("com.android.systemui", 0);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long deadline = System.nanoTime() + 15_000_000_000L;
            try (FileInputStream input = new FileInputStream(info.sourceDir)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted() || System.nanoTime() > deadline)
                        throw new java.io.IOException("Inspection deadline reached");
                    digest.update(buffer, 0, count);
                }
            }
            StringBuilder encoded = new StringBuilder(64);
            for (byte b : digest.digest()) encoded.append(String.format(java.util.Locale.ROOT, "%02x", b & 0xff));
            hash = encoded.toString();
        } catch (Exception ignored) { /* Unreadable APK remains unverified. */ }
        return new Snapshot(CompatibilityPolicy.classify(Build.MODEL, Build.DEVICE, Build.VERSION.SDK_INT, fingerprint, hash, BuildConfig.BASELINE_FINGERPRINT, BuildConfig.BASELINE_SYSTEM_UI_SHA256), fingerprint, hash);
    }
}
