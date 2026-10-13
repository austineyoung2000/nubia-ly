package com.elitedarkkaiser.nubialy.xposed;

import android.content.*;
import android.os.Bundle;
import com.elitedarkkaiser.nubialy.*;
import de.robv.android.xposed.XposedBridge;

/** Signature-protected rehearsal and temporary clock controls; no settings writes. */
final class PreviewReceiver extends BroadcastReceiver {
    private final PreviewRuntime runtime;
    private PreviewReceiver(Context context) { runtime = new PreviewRuntime(context); }
    static void install(Context context) {
        try {
            PreviewReceiver receiver = new PreviewReceiver(context);
            context.registerReceiver(new BroadcastReceiver() {
                @Override public void onReceive(Context ignored, Intent intent) {
                    if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) receiver.runtime.request("screen_off", 0, result -> {});
                    else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) receiver.runtime.request("clock_resume",
                        android.os.SystemClock.elapsedRealtime() + SystemUiGuardProtocol.DEADLINE_MS, result -> {});
                }
            }, screenFilter(), Context.RECEIVER_NOT_EXPORTED);
            receiver.runtime.request("clock_resume", android.os.SystemClock.elapsedRealtime() + SystemUiGuardProtocol.DEADLINE_MS,
                result -> {});
            context.registerReceiver(receiver, new IntentFilter(SystemUiGuardProtocol.PREVIEW_ACTION),
                SystemUiGuardProtocol.PERMISSION, null, Context.RECEIVER_EXPORTED);
        } catch (Throwable unavailable) { XposedBridge.log("nubia-ly: lifecycle receiver unavailable; cosmetics blocked."); }
    }
    @Override public void onReceive(Context context, Intent intent) {
        try { receive(context, intent); }
        catch (Throwable unavailable) { XposedBridge.log("nubia-ly: malformed preview request rejected."); }
    }
    private void receive(Context context, Intent intent) {
        if (!isOrderedBroadcast()) return;
        String operation = intent.getStringExtra("operation");
        String nonce = intent.getStringExtra("nonce");
        if (!SystemUiGuardProtocol.PREVIEW_ACTION.equals(intent.getAction()) || !PreviewPolicy.uuid(nonce)
                || !("start".equals(operation) || "clock_start".equals(operation) || "clock_enable".equals(operation) || "clock_resume".equals(operation) || "status".equals(operation) || "stop".equals(operation))) return;
        int color = "clock_resume".equals(operation) ? ClockColorPolicy.DEFAULT : intent.getIntExtra("clock_color", 0);
        if (("clock_start".equals(operation) || "clock_enable".equals(operation)) && (!intent.hasExtra("clock_color") || !ClockColorPolicy.valid(color))) return;
        PendingResult pending = goAsync();
        try {
            runtime.request(operation, intent.getLongExtra("request_deadline_ms", 0), color, result -> {
                try {
                    result.putString("nonce", nonce); result.putInt("version", BuildConfig.VERSION_CODE);
                    pending.setResultExtras(result); pending.setResultCode(SystemUiGuardProtocol.RESPONSE);
                } catch (Throwable ignored) { /* Host must survive diagnostics. */ }
                finally { finish(pending); }
            });
        } catch (Throwable unavailable) { finish(pending); }
    }
    private static IntentFilter screenFilter() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF); filter.addAction(Intent.ACTION_SCREEN_ON); return filter;
    }
    private static void finish(PendingResult pending) { try { pending.finish(); } catch (Throwable ignored) {} }
}
