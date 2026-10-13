package com.elitedarkkaiser.nubialy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.UUID;
import java.util.function.Consumer;

/** One explicit asynchronous check; no polling, cached success or apply authority. */
public final class SystemUiGuardClient {
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Consumer<String> callback;
    private Runnable timeout;
    private long generation;
    public SystemUiGuardClient(Context context) { this.context = context.getApplicationContext(); }
    public void check(Consumer<String> result) { request(false, null, null, result); }
    public void preview(String operation, Consumer<String> result) { request(true, operation, null, result); }
    public void previewColor(int color, Consumer<String> result) {
        if (!ClockColorPolicy.valid(color)) { result.accept("Preview blocked: choose an opaque RGB color."); return; }
        request(true, "clock_start", color, result);
    }
    private void request(boolean preview, String operation, Integer color, Consumer<String> result) {
        cancel();
        callback = result;
        long request = generation;
        long started = SystemClock.elapsedRealtime();
        String nonce = UUID.randomUUID().toString();
        timeout = () -> finish(request, "System UI did not reply within 8 seconds. Guard access unverified.");
        handler.postDelayed(timeout, SystemUiGuardProtocol.DEADLINE_MS);
        Intent intent = new Intent(preview ? SystemUiGuardProtocol.PREVIEW_ACTION : SystemUiGuardProtocol.ACTION).setPackage("com.android.systemui")
            .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        intent.putExtra("nonce", nonce);
        intent.putExtra("request_deadline_ms", started + SystemUiGuardProtocol.DEADLINE_MS);
        if (preview) intent.putExtra("operation", operation);
        if (color != null) intent.putExtra("clock_color", color.intValue());
        try {
            context.sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
                @Override public void onReceive(Context ignored, Intent response) {
                    if (request != generation || callback == null) return;
                    Bundle extras = getResultExtras(false);
                    if (getResultCode() != SystemUiGuardProtocol.RESPONSE || extras == null) {
                        finish(request, "No diagnostic reply from System UI. Verify Vector scope and reboot.");
                        return;
                    }
                    if (!SystemUiGuardProtocol.accepts(nonce, extras.getString("nonce"),
                            BuildConfig.VERSION_CODE, extras.getInt("version", -1),
                            SystemClock.elapsedRealtime() - started)) {
                        finish(request, "System UI reply is stale or from another app version. Access unverified.");
                        return;
                    }
                    if (preview) {
                        String phase = extras.getString("phase", "UNKNOWN");
                        long deadline = extras.getLong("deadline_ms", 0);
                        if ("ACTIVE".equals(phase) && deadline <= SystemClock.elapsedRealtime()) phase = "EXPIRED";
                        if (color != null && "ACTIVE".equals(phase) && extras.getBoolean("visual", false)
                                && extras.getInt("clock_color", 0) != color.intValue()) {
                            finish(request, "System UI color reply did not match the requested selection. Preview unverified; restore clock color."); return;
                        }
                        finish(request, "System UI lifecycle: " + phase + (extras.getBoolean("visual", false) ? ". Temporary clock color preview.\n" : ". No appearance changes.\n")
                            + ("ACTIVE".equals(phase) ? "Expires within " + Math.max(0, (deadline - SystemClock.elapsedRealtime()) / 1000)
                                + " seconds; check status for a fresh result." : extras.getString("reason", "No valid session."))
                            + "\n" + extras.getString("clock_capture", "Clock capture unverified."));
                        return;
                    }
                    int status = extras.getInt("broker_status", GuardBrokerPolicy.UNAVAILABLE);
                    finish(request, switch (status) {
                        case GuardBrokerPolicy.READABLE -> "System UI received a fresh valid probe through the protected broker. Cosmetics remain blocked.";
                        case GuardBrokerPolicy.BLOCKED -> "System UI reached the broker; probe is blocked, expired or unreadable.";
                        case GuardBrokerPolicy.BUSY -> "System UI reached the broker; a read is already pending. Access unverified.";
                        default -> "System UI replied, but the protected broker is unavailable. Guard access unverified.";
                    });
                }
            }, handler, 0, null, null);
        } catch (RuntimeException unavailable) {
            finish(request, "System UI diagnostic could not be sent. Access unverified.");
        }
    }
    private void finish(long request, String message) {
        if (request != generation || callback == null) return;
        Consumer<String> current = callback;
        callback = null;
        if (timeout != null) handler.removeCallbacks(timeout);
        timeout = null;
        current.accept(message);
    }
    public void cancel() {
        generation++;
        callback = null;
        if (timeout != null) handler.removeCallbacks(timeout);
        timeout = null;
    }
}
