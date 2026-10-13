package com.elitedarkkaiser.nubialy.xposed;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.elitedarkkaiser.nubialy.*;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Bounded rehearsal/visual sessions on the main actor; single-admission background IPC. */
final class PreviewRuntime {
    private final PreviewLifecycle session = new PreviewLifecycle();
    private final Handler actor = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final Context context;
    private boolean visual;
    private String reason = "No lifecycle test started.";
    private Runnable tick, expiry;
    PreviewRuntime(Context context) { this.context = context; }
    void request(String operation, long requestDeadline, Consumer<Bundle> reply) {
        actor.post(() -> {
            try {
                switch (operation) {
                    case "start" -> start(false, requestDeadline, reply);
                    case "clock_start" -> start(true, requestDeadline, reply);
                    case "status" -> {
                        if (session.state(now()) == PreviewLifecycle.Phase.ACTIVE) validate(session.generation(), reply);
                        else { cancelTimers(); reply.accept(snapshot()); }
                    }
                    case "stop" -> { stop("Stopped by request."); reply.accept(snapshot()); }
                    default -> { stop("Unknown operation rejected."); reply.accept(snapshot()); }
                }
            } catch (Throwable unavailable) { stop("Lifecycle diagnostic unavailable."); reply.accept(snapshot()); }
        });
    }
    private void start(boolean color, long requestDeadline, Consumer<Bundle> reply) {
        if (!SystemUiGuardProtocol.admitted(requestDeadline, now())) {
            Bundle rejected = snapshot(); rejected.putString("phase", "REJECTED");
            rejected.putString("reason", "Start admission deadline passed; no new session started.");
            reply.accept(rejected); return;
        }
        cancelTimers();
        ClockCaptureBackend.end();
        visual = color;
        long ticket = session.begin();
        reason = "Checking fresh lifecycle-only permit.";
        read(ticket, permit -> {
            if (!session.current(ticket)) { reply.accept(snapshot()); return; }
            if (!SystemUiGuardProtocol.admitted(requestDeadline, now())) {
                session.stop(); reason = "Start expired before activation; no session started.";
            } else if (!session.activate(ticket, permit, now())) {
                reason = "Start blocked: lifecycle permit or broker unavailable.";
            } else {
                if (!(visual ? ClockColorBackend.begin(session.deadline()) : ClockCaptureBackend.begin())) {
                    stop(visual ? "Clock preview unavailable; check restoration result." : "Clock capture rehearsal unavailable."); reply.accept(snapshot()); return;
                }
                reason = visual ? "Temporary lavender clock preview started." : "Clock capture rehearsal started; no appearance changes.";
                expiry = () -> { session.state(now()); cancelTimers(); };
                actor.postDelayed(expiry, session.deadline() - now());
                schedule(ticket);
            }
            reply.accept(snapshot());
        });
    }
    private void schedule(long ticket) {
        tick = () -> validate(ticket, ignored -> {});
        actor.postDelayed(tick, 2000);
    }
    private void validate(long ticket, Consumer<Bundle> reply) {
        if (!session.current(ticket)) { reply.accept(snapshot()); return; }
        if (session.state(now()) != PreviewLifecycle.Phase.ACTIVE) {
            cancelTimers(); reply.accept(snapshot()); return;
        }
        read(ticket, permit -> {
            if (!session.current(ticket)) { reply.accept(snapshot()); return; }
            if ((visual && !ClockColorBackend.healthy()) || !session.validate(ticket, permit, now())) {
                session.stop();
                reason = "Lifecycle ended: permit removed, replaced, expired or broker unavailable.";
                cancelTimers();
            } else {
                if (tick != null) actor.removeCallbacks(tick);
                schedule(ticket);
            }
            reply.accept(snapshot());
        });
    }
    private void read(long ticket, Consumer<PreviewPolicy.Permit> result) {
        if (!busy.compareAndSet(false, true)) { result.accept(null); return; }
        boolean readVisual = visual;
        long started = now();
        String nonce = UUID.randomUUID().toString();
        try {
            worker.execute(() -> {
                PreviewPolicy.Permit permit = null;
                try {
                    Bundle request = new Bundle(); request.putString("nonce", nonce);
                    request.putInt("version", BuildConfig.VERSION_CODE);
                    Bundle response = context.getContentResolver().call(Uri.parse("content://" + GuardBrokerPolicy.AUTHORITY),
                        (readVisual ? GuardBrokerPolicy.CLOCK_METHOD : GuardBrokerPolicy.PREVIEW_METHOD), null, request);
                    if (response != null && SystemUiGuardProtocol.accepts(nonce, response.getString("nonce"),
                            BuildConfig.VERSION_CODE, response.getInt("version", -1), now() - started)
                            && response.getInt("status", -1) == GuardBrokerPolicy.READABLE) {
                        permit = new PreviewPolicy.Permit(response.getString("permit_id"), response.getLong("deadline_ms", 0));
                    }
                } catch (Throwable unavailable) { /* No permit on any failure. */ }
                finally { busy.set(false); }
                PreviewPolicy.Permit captured = permit;
                actor.post(() -> {
                    try { result.accept(session.current(ticket) ? captured : null); }
                    catch (Throwable unavailable) { stop("Lifecycle callback failed."); }
                });
            });
        } catch (Throwable unavailable) { busy.set(false); result.accept(null); }
    }
    private Bundle snapshot() {
        PreviewLifecycle.Phase phase = session.state(now());
        if (phase != PreviewLifecycle.Phase.ACTIVE) { ClockCaptureBackend.end(); ClockColorBackend.end(); }
        if (phase == PreviewLifecycle.Phase.EXPIRED) reason = visual ? "Clock permit expired; restoration requested." : "Lifecycle permit expired; no appearance changes were made.";
        Bundle result = new Bundle(); result.putString("phase", phase.name());
        result.putString("reason", reason); result.putString("clock_capture", visual ? ClockColorBackend.status() : ClockCaptureBackend.status()); result.putBoolean("visual", visual); result.putLong("deadline_ms", session.deadline());
        return result;
    }
    private void stop(String why) { session.stop(); reason = why; cancelTimers(); }
    private void cancelTimers() {
        ClockColorBackend.end();
        ClockCaptureBackend.end();
        if (tick != null) actor.removeCallbacks(tick);
        if (expiry != null) actor.removeCallbacks(expiry);
        tick = null; expiry = null;
    }
    private static long now() { return SystemClock.elapsedRealtime(); }
}
