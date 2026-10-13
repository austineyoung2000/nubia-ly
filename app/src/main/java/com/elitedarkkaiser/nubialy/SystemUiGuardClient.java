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
    public void check(Consumer<String> result) {
        cancel();
        callback = result;
        long request = generation;
        long started = SystemClock.elapsedRealtime();
        String nonce = UUID.randomUUID().toString();
        timeout = () -> finish(request, "System UI did not reply within 8 seconds. Guard access unverified.");
        handler.postDelayed(timeout, SystemUiGuardProtocol.DEADLINE_MS);
        Intent intent = new Intent(SystemUiGuardProtocol.ACTION).setPackage("com.android.systemui");
        intent.putExtra("nonce", nonce);
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
                    finish(request, extras.getBoolean("readable", false)
                        ? "System UI read a valid current-boot probe. Cosmetics remain blocked."
                        : "System UI replied, but its guard probe is blocked or unreadable. Publish a fresh probe and retry.");
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
