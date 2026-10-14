package com.elitedarkkaiser.nubialy.xposed;

import android.content.res.ColorStateList;
import android.os.*;
import android.widget.TextView;
import com.elitedarkkaiser.nubialy.ColorRestore;
import com.elitedarkkaiser.nubialy.ClockColorPolicy;
import com.elitedarkkaiser.nubialy.ClockEnabledPolicy;
import de.robv.android.xposed.*;
import java.util.*;

/** Main-thread, temporary override. Setter hooks exist only during the explicit session. */
final class ClockColorBackend {
    private static final Map<TextView, ColorRestore<ColorStateList>> states = new WeakHashMap<>();
    private static final List<XC_MethodHook.Unhook> hooks = new ArrayList<>();
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static boolean active, writing, failed;
    private static long deadline;
    static boolean refreshHealth(long until) {
        long now = SystemClock.elapsedRealtime();
        if (!active || failed || now >= deadline || until <= now || until - now > ClockEnabledPolicy.HEALTH_MS) return false;
        deadline = until; return true;
    }
    private static volatile long generation;
    private static int applied, restored, darkReapplications;
    private static String message = "No clock color preview started.";
    private static int selectedColor = ClockColorPolicy.DEFAULT;
    static int color() { return selectedColor; }
    static boolean begin(long until, int color) { return begin(until, color, 0, false); }
    static boolean begin(long until, int color, int size, boolean bold) { return begin(until, color, size, bold, false); }
    static boolean begin(long until, int color, int size, boolean bold, boolean seconds) { return begin(until, color, size, bold, seconds, false, 0); }
    static boolean begin(long until, int color, int size, boolean bold, boolean seconds, boolean italic, int spacing) { return begin(until, color, size, bold, seconds, italic, spacing, 0, false); }
    static boolean begin(long until, int color, int size, boolean bold, boolean seconds, boolean italic, int spacing, int family, boolean hidden) {
        if (!ClockColorPolicy.valid(color) || !com.elitedarkkaiser.nubialy.ClockStylePolicy.valid(size) || !com.elitedarkkaiser.nubialy.ClockStylePolicy.validSpacing(spacing) || !com.elitedarkkaiser.nubialy.ClockStylePolicy.validFamily(family)) return false;
        end();
        if (failed || !states.isEmpty() || until <= SystemClock.elapsedRealtime()
                || until - SystemClock.elapsedRealtime() > 30000) return false;
        applied = restored = darkReapplications = 0; deadline = until; selectedColor = color;
        try {
            XC_MethodHook callback = new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    try {
                        if (!p.thisObject.getClass().getName().equals("com.android.systemui.statusbar.policy.Clock")) return;
                        if (Looper.myLooper() != Looper.getMainLooper()) return;
                        if (writing || !active || !(p.thisObject instanceof TextView view) || !states.containsKey(view)) return;
                        ColorStateList next = p.args[0] instanceof ColorStateList c ? c
                            : ColorStateList.valueOf((Integer)p.args[0]);
                        states.get(view).stockWrite(next);
                    } catch (Throwable unavailable) { abort("Clock stock capture failed."); }
                }
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    try {
                        if (!p.thisObject.getClass().getName().equals("com.android.systemui.statusbar.policy.Clock")) return;
                        if (Looper.myLooper() != Looper.getMainLooper()) {
                            long ticket = generation;
                            Object target = p.thisObject;
                            main.post(() -> {
                                if (ticket == generation && active && target instanceof TextView view && states.containsKey(view)) {
                                    states.get(view).stockWrite(view.getTextColors()); abort("Unexpected off-main clock write.");
                                }
                            }); return;
                        }
                        if (writing || !active || !(p.thisObject instanceof TextView view) || !states.containsKey(view)) return;
                        if (p.hasThrowable()) { abort("Stock clock color write failed."); return; }
                        if (SystemClock.elapsedRealtime() >= deadline) { end(); return; }
                        paint(view);
                    } catch (Throwable unavailable) { abort("Clock override callback failed."); }
                }
            };
            hooks.add(XposedHelpers.findAndHookMethod(TextView.class, "setTextColor", int.class, callback));
            hooks.add(XposedHelpers.findAndHookMethod(TextView.class, "setTextColor", ColorStateList.class, callback));
            ClockStyleBackend.begin(size, bold, italic, spacing, family); ClockSecondsBackend.begin(seconds); ClockVisibilityBackend.begin(hidden);
            active = true;
            for (TextView view : ClockCaptureBackend.attachedClocks()) if (view.isAttachedToWindow()) track(view);
            if (!active || applied == 0) { end(); message = "No eligible attached clock; preview blocked."; return false; }
            message = "Clock preview " + ClockColorPolicy.hex(selectedColor) + " active."; return true;
        } catch (Throwable unavailable) { abort("Clock preview installation failed."); return false; }
    }
    static void track(TextView view) {
        if (!active || failed || writing || Looper.myLooper() != Looper.getMainLooper()) return;
        if (SystemClock.elapsedRealtime() >= deadline) { end(); return; }
        if (states.containsKey(view)) {
            try { ClockStyleBackend.track(view); ClockSecondsBackend.track(view); ClockVisibilityBackend.track(view); } catch (Throwable unavailable) { abort("Clock typography reapplication failed."); return; }
            // Reassert after the inspected clock callbacks if an inherited setter was bypassed.
            if (view.getCurrentTextColor() != selectedColor) {
                states.get(view).stockWrite(view.getTextColors());
                try { paint(view); } catch (Throwable unavailable) { abort("Clock reapplication failed."); }
            }
            return;
        }
        ColorRestore<ColorStateList> state = new ColorRestore<>(); state.capture(view.getTextColors());
        states.put(view, state); applied++;
        try { paint(view); } catch (Throwable unavailable) { abort("Clock preview apply failed."); }
    }
    record DarkFrame(long generation, ColorRestore<ColorStateList> state, long revision) {}
    static DarkFrame beforeDark(TextView view) {
        if (!active || failed || Looper.myLooper() != Looper.getMainLooper()) return null;
        ColorRestore<ColorStateList> state = states.get(view);
        return state == null ? null : new DarkFrame(generation, state, state.revision());
    }
    static void afterDark(TextView view, DarkFrame frame) {
        if (frame == null || !active || failed || Looper.myLooper() != Looper.getMainLooper()
                || frame.generation() != generation || states.get(view) != frame.state()) return;
        if (SystemClock.elapsedRealtime() >= deadline) { end(); return; }
        try {
            // The inspected onDarkChanged always writes stock tint. If the inherited setter hook
            // was bypassed, capture its actual output; otherwise keep the setter's stock target.
            if (frame.state().revision() == frame.revision()) frame.state().stockWrite(view.getTextColors());
            paint(view); darkReapplications++;
        } catch (Throwable unavailable) { abort("Vendor clock dark update failed."); }
    }
    private static void paint(TextView view) {
        writing = true;
        try { view.setTextColor(selectedColor); ClockStyleBackend.track(view); ClockSecondsBackend.track(view); ClockVisibilityBackend.track(view); } finally { writing = false; }
    }
    static void detach(TextView view) { if (states.containsKey(view)) restore(view); }
    private static void restore(TextView view) {
        ColorRestore<ColorStateList> state = states.get(view);
        if (state == null) return;
        try {
            writing = true;
            view.setTextColor(state.target());
            if (!ClockStyleBackend.restore(view) || !ClockSecondsBackend.restore(view) || !ClockVisibilityBackend.restore(view)) throw new IllegalStateException();
            if (!view.getTextColors().equals(state.target())) throw new IllegalStateException();
            state.restored(); states.remove(view); restored++;
        } catch (Throwable unavailable) { failed = true; message = "Clock restoration incomplete. Disable nubia-ly in Vector and reboot."; }
        finally { writing = false; }
    }
    static void end() {
        generation++; active = false;
        for (TextView view : new ArrayList<>(states.keySet())) restore(view);
        for (XC_MethodHook.Unhook hook : hooks) try { hook.unhook(); } catch (Throwable unavailable) { failed = true; }
        hooks.clear(); if (!ClockStyleBackend.end()) failed = true; if (!ClockSecondsBackend.end()) failed = true; if (!ClockVisibilityBackend.end()) failed = true; deadline = 0;
        if (!failed && applied > 0) message = "Latest stock clock colors restored and read back.";
    }
    private static void abort(String reason) {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post(() -> abort(reason)); return; }
        end(); failed = true; message = reason + " Preview blocked; check restored count. Disable nubia-ly in Vector and reboot if incomplete.";
    }
    static boolean healthy() { return active && !failed && ClockStyleBackend.healthy() && ClockSecondsBackend.healthy() && ClockVisibilityBackend.healthy() && ClockCaptureBackend.available(); }
    static String status() { return message + " Applied: " + applied + "; restored: " + restored + "; remaining: " + states.size() + "; dark updates kept: " + darkReapplications + ". " + ClockStyleBackend.status() + " " + ClockSecondsBackend.status() + " " + ClockVisibilityBackend.status(); }
}
