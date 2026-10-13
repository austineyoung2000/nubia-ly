package com.elitedarkkaiser.nubialy.xposed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import com.elitedarkkaiser.nubialy.BuildConfig;
import com.elitedarkkaiser.nubialy.GuardReader;
import com.elitedarkkaiser.nubialy.SystemUiGuardProtocol;
import de.robv.android.xposed.XposedBridge;
import java.util.concurrent.atomic.AtomicBoolean;

/** Signature-protected, read-only diagnostic living only in System UI's main process. */
public final class SystemUiGuardReceiver extends BroadcastReceiver {
    private static boolean installed;
    private final AtomicBoolean busy = new AtomicBoolean();
    public static synchronized void install(Context context) {
        if (installed || !"com.android.systemui".equals(context.getPackageName())) return;
        try {
            context.registerReceiver(new SystemUiGuardReceiver(),
                new IntentFilter(SystemUiGuardProtocol.ACTION), SystemUiGuardProtocol.PERMISSION,
                null, Context.RECEIVER_EXPORTED);
            installed = true;
            XposedBridge.log("nubia-ly: System UI guard diagnostic registered; no cosmetic hooks.");
        } catch (Throwable unavailable) {
            XposedBridge.log("nubia-ly: System UI diagnostic registration unavailable; cosmetics blocked.");
        }
    }
    private static void finishQuietly(PendingResult pending) {
        try { pending.finish(); } catch (Throwable unavailable) { /* Never crash the host. */ }
    }
    @Override public void onReceive(Context context, Intent intent) {
        if (!isOrderedBroadcast() || !SystemUiGuardProtocol.ACTION.equals(intent.getAction())) return;
        String nonce = intent.getStringExtra("nonce");
        if (nonce == null || !nonce.matches("[0-9a-f-]{36}") || !busy.compareAndSet(false, true)) return;
        PendingResult pending = goAsync();
        try {
            new Thread(() -> {
                try {
                    GuardReader.Snapshot snapshot = GuardReader.inspectSnapshot();
                    Bundle reply = new Bundle();
                    reply.putString("nonce", nonce);
                    reply.putInt("version", BuildConfig.VERSION_CODE);
                    reply.putBoolean("readable", snapshot.readable());
                    pending.setResultExtras(reply);
                    pending.setResultCode(SystemUiGuardProtocol.RESPONSE);
                } catch (Throwable unavailable) {
                    // Initial non-success result is retained; never let diagnostics crash System UI.
                } finally {
                    finishQuietly(pending);
                    busy.set(false);
                }
            }, "nubia-ly-guard-check").start();
        } catch (Throwable unavailable) {
            finishQuietly(pending);
            busy.set(false);
        }
    }
}
