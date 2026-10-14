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
    private boolean visual, enabledMode, startup, resumeSuppressed;
    private int brokerColor, brokerSize, brokerSpacing, brokerFamily;
    private boolean brokerBold, brokerSeconds, brokerItalic, brokerHidden;
    private int brokerDate;
    private int brokerBarColor = ClockColorPolicy.DEFAULT;
    private StatusBarStyle brokerBar = StatusBarStyle.STOCK;
    private String reason = "No lifecycle test started.";
    private Runnable tick, expiry;
    PreviewRuntime(Context context) { this.context = context; }
    void request(String operation, long requestDeadline, Consumer<Bundle> reply) {
        request(operation, requestDeadline, ClockColorPolicy.DEFAULT, reply);
    }
    void request(String operation, long requestDeadline, int color, Consumer<Bundle> reply) {
        actor.post(() -> {
            try {
                switch (operation) {
                    case "start" -> start(false, false, false, requestDeadline, color, reply);
                    case "clock_start" -> start(true, false, false, requestDeadline, color, reply);
                    case "clock_enable" -> { resumeSuppressed = false; start(true, true, false, requestDeadline, color, reply); }
                    case "clock_resume" -> {
                        PreviewLifecycle.Phase phase = session.state(now());
                        if (resumeSuppressed) { reply.accept(snapshot()); break; }
                        if (enabledMode && (phase == PreviewLifecycle.Phase.ACTIVE || phase == PreviewLifecycle.Phase.CHECKING)) reply.accept(snapshot());
                        else start(true, true, true, requestDeadline, color, reply);
                    }
                    case "inventory" -> {
                        Bundle inventory=new Bundle();inventory.putString("phase",SystemUiGuardProtocol.admitted(requestDeadline,now())?"INVENTORY":"REJECTED");
                        if(SystemUiGuardProtocol.admitted(requestDeadline,now()))inventory.putStringArrayList("statusbar_slots",StatusBarBackend.inventory());
                        reply.accept(inventory);
                    }
                    case "status" -> {
                        if (session.state(now()) == PreviewLifecycle.Phase.ACTIVE) validate(session.generation(), reply);
                        else { cancelTimers(); reply.accept(snapshot()); }
                    }
                    case "screen_off" -> { stop("Screen off; stock restored until wake."); reply.accept(snapshot()); }
                    case "stop" -> { resumeSuppressed = true; stop("Stopped by request."); reply.accept(snapshot()); }
                    default -> { stop("Unknown operation rejected."); reply.accept(snapshot()); }
                }
            } catch (Throwable unavailable) { stop("Lifecycle diagnostic unavailable."); reply.accept(snapshot()); }
        });
    }
    private void start(boolean colorPreview, boolean enabled, boolean resume, long requestDeadline, int color, Consumer<Bundle> reply) {
        if ((colorPreview && !ClockColorPolicy.valid(color)) || !SystemUiGuardProtocol.admitted(requestDeadline, now())) {
            Bundle rejected = snapshot(); rejected.putString("phase", "REJECTED");
            rejected.putString("reason", "Start admission deadline passed; no new session started.");
            reply.accept(rejected); return;
        }
        cancelTimers();
        ClockCaptureBackend.end();
        visual = colorPreview; enabledMode = enabled; startup = resume;
        long ticket = session.begin(enabledMode);
        reason = "Checking fresh lifecycle-only permit.";
        read(ticket, permit -> {
            if (!session.current(ticket)) { reply.accept(snapshot()); return; }
            if (!SystemUiGuardProtocol.admitted(requestDeadline, now())) {
                session.stop(); reason = "Start expired before activation; no session started.";
            } else if (!session.activate(ticket, permit, now())) {
                reason = "Start blocked: lifecycle permit or broker unavailable.";
            } else {
                if (!(visual ? ClockColorBackend.begin(session.deadline(), startup ? brokerColor : color, enabledMode ? brokerSize : 0, enabledMode && brokerBold, enabledMode && brokerSeconds, enabledMode && brokerItalic, enabledMode ? brokerSpacing : 0, enabledMode ? brokerFamily : 0, enabledMode && brokerHidden, enabledMode ? brokerBar : StatusBarStyle.STOCK, enabledMode ? brokerBarColor : color, enabledMode ? brokerDate : 0) : ClockCaptureBackend.begin())) {
                    stop(visual ? "Clock preview unavailable; check restoration result." : "Clock capture rehearsal unavailable."); reply.accept(snapshot()); return;
                }
                reason = visual ? "Temporary selected-color clock preview started." : "Clock capture rehearsal started; no appearance changes.";
                startup = false;
                armExpiry();
                schedule(ticket);
            }
            reply.accept(snapshot());
        });
    }
    private void armExpiry() {
        if (expiry != null) actor.removeCallbacks(expiry);
        expiry = () -> { session.state(now()); cancelTimers(); };
        actor.postDelayed(expiry, Math.max(0, session.deadline() - now()));
    }
    private void schedule(long ticket) {
        tick = () -> validate(ticket, ignored -> {});
        actor.postDelayed(tick, enabledMode ? 5000 : 2000);
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
                if (enabledMode) {
                    if (!ClockColorBackend.refreshHealth(session.deadline())) { stop("Clock health unavailable; stock restoration requested."); reply.accept(snapshot()); return; }
                    armExpiry();
                }
                if (tick != null) actor.removeCallbacks(tick);
                schedule(ticket);
            }
            reply.accept(snapshot());
        });
    }
    private void read(long ticket, Consumer<PreviewPolicy.Permit> result) {
        if (!busy.compareAndSet(false, true)) { result.accept(null); return; }
        boolean readVisual = visual; boolean readEnabled = enabledMode; boolean readStartup = startup;
        long started = now();
        String nonce = UUID.randomUUID().toString();
        try {
            worker.execute(() -> {
                PreviewPolicy.Permit permit = null; int receivedColor = ClockColorPolicy.DEFAULT; int receivedSize = 0, receivedSpacing = 0, receivedFamily = 0; boolean receivedBold = false, receivedSeconds = false, receivedItalic = false, receivedHidden = false; StatusBarStyle receivedBar = StatusBarStyle.STOCK; int receivedBarColor = ClockColorPolicy.DEFAULT; int receivedDate=0;
                try {
                    Bundle request = new Bundle(); request.putString("nonce", nonce);
                    request.putInt("version", BuildConfig.VERSION_CODE);
                    Bundle response = context.getContentResolver().call(Uri.parse("content://" + GuardBrokerPolicy.AUTHORITY),
                        (readEnabled ? (readStartup ? GuardBrokerPolicy.STARTUP_METHOD : GuardBrokerPolicy.ENABLED_METHOD) : readVisual ? GuardBrokerPolicy.CLOCK_METHOD : GuardBrokerPolicy.PREVIEW_METHOD), null, request);
                    if (response != null && SystemUiGuardProtocol.accepts(nonce, response.getString("nonce"),
                            BuildConfig.VERSION_CODE, response.getInt("version", -1), now() - started)
                            && response.getInt("status", -1) == GuardBrokerPolicy.READABLE) {
                        permit = new PreviewPolicy.Permit(response.getString("permit_id"), response.getLong("deadline_ms", 0));
                        receivedColor = response.getInt("clock_color", ClockColorPolicy.DEFAULT);
                        receivedSize = response.getInt("clock_size", 0); receivedBold = response.getBoolean("clock_bold", false); receivedSeconds = response.getBoolean("clock_seconds", false); receivedItalic = response.getBoolean("clock_italic", false); receivedSpacing = response.getInt("clock_spacing", 0); receivedFamily = response.getInt("clock_family", 0); receivedHidden = response.getBoolean("clock_hidden", false);
                        receivedDate=response.getInt("clock_date",0);
                        receivedBarColor = response.getInt("statusbar_color",ClockColorPolicy.DEFAULT);
                        receivedBar = new StatusBarStyle(response.getInt("statusbar_mask",0),response.getInt("statusbar_system_scale",100),response.getInt("statusbar_notification_scale",100),response.getInt("statusbar_battery_scale",100),response.getInt("statusbar_system_opacity",100),response.getInt("statusbar_notification_opacity",100),response.getInt("statusbar_battery_opacity",100),StatusBarStyle.decodeSlots(response.getString("statusbar_hidden_slots","")));
                        if (!ClockColorPolicy.valid(receivedColor) || !ClockStylePolicy.valid(receivedSize) || !ClockStylePolicy.validSpacing(receivedSpacing) || !ClockStylePolicy.validFamily(receivedFamily) || !receivedBar.valid() || !ClockColorPolicy.valid(receivedBarColor) || !ClockDatePolicy.valid(receivedDate)) permit = null;
                    }
                } catch (Throwable unavailable) { /* No permit on any failure. */ }
                finally { busy.set(false); }
                PreviewPolicy.Permit captured = permit; int capturedColor = receivedColor; int capturedSize = receivedSize, capturedSpacing = receivedSpacing, capturedFamily = receivedFamily; boolean capturedBold = receivedBold, capturedSeconds = receivedSeconds, capturedItalic = receivedItalic, capturedHidden = receivedHidden; StatusBarStyle capturedBar = receivedBar; int capturedBarColor = receivedBarColor; int capturedDate=receivedDate;
                actor.post(() -> {
                    try { if (session.current(ticket)) { brokerColor = capturedColor; brokerSize = capturedSize; brokerBold = capturedBold; brokerSeconds = capturedSeconds; brokerItalic = capturedItalic; brokerSpacing = capturedSpacing; brokerFamily = capturedFamily; brokerHidden = capturedHidden; brokerBar = capturedBar; brokerBarColor = capturedBarColor; brokerDate=capturedDate; } result.accept(session.current(ticket) ? captured : null); }
                    catch (Throwable unavailable) { stop("Lifecycle callback failed."); }
                });
            });
        } catch (Throwable unavailable) { busy.set(false); result.accept(null); }
    }
    private Bundle snapshot() {
        PreviewLifecycle.Phase phase = session.state(now());
        if (phase != PreviewLifecycle.Phase.ACTIVE) { ClockCaptureBackend.end(); ClockColorBackend.end(); }
        if (phase == PreviewLifecycle.Phase.EXPIRED) reason = enabledMode ? "Clock guard became unavailable; stock restoration requested." : visual ? "Clock permit expired; restoration requested." : "Lifecycle permit expired; no appearance changes were made.";
        Bundle result = new Bundle(); result.putString("phase", phase.name());
        result.putString("reason", reason); result.putString("clock_capture", visual ? ClockColorBackend.status() : ClockCaptureBackend.status()); result.putBoolean("visual", visual); result.putBoolean("enabled_mode", enabledMode); result.putInt("clock_color", ClockColorBackend.color()); result.putLong("deadline_ms", session.deadline());
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
