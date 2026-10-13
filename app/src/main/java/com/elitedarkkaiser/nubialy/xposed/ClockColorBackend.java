package com.elitedarkkaiser.nubialy.xposed;

import android.content.res.ColorStateList;
import android.os.*;
import android.widget.TextView;
import com.elitedarkkaiser.nubialy.ColorRestore;
import com.elitedarkkaiser.nubialy.ClockColorPolicy;
import de.robv.android.xposed.*;
import java.util.*;

/** Main-thread, temporary override. Setter hooks exist only during the explicit session. */
final class ClockColorBackend {
    private static final Map<TextView, ColorRestore<ColorStateList>> states = new WeakHashMap<>();
    private static final List<XC_MethodHook.Unhook> hooks = new ArrayList<>();
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static boolean active, writing, failed;
    private static long deadline;
    private static volatile long generation;
    private static int applied, restored;
    private static String message = "No clock color preview started.";
    private static int selectedColor = ClockColorPolicy.DEFAULT;
    static int color() { return selectedColor; }
    static boolean begin(long until, int color) {
        if (!ClockColorPolicy.valid(color)) return false;
        end();
        if (failed || !states.isEmpty() || until <= SystemClock.elapsedRealtime()
                || until - SystemClock.elapsedRealtime() > 30000) return false;
        applied = restored = 0; deadline = until; selectedColor = color;
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
            active = true;
            for (TextView view : ClockCaptureBackend.attachedClocks()) if (view.isAttachedToWindow()) track(view);
            if (!active || applied == 0) { end(); message = "No eligible attached clock; preview blocked."; return false; }
            message = "Clock preview " + ClockColorPolicy.hex(selectedColor) + " active."; return true;
        } catch (Throwable unavailable) { abort("Clock preview installation failed."); return false; }
    }
    static void track(TextView view) {
        if (!active || failed || Looper.myLooper() != Looper.getMainLooper()) return;
        if (SystemClock.elapsedRealtime() >= deadline) { end(); return; }
        if (states.containsKey(view)) return;
        ColorRestore<ColorStateList> state = new ColorRestore<>(); state.capture(view.getTextColors());
        states.put(view, state); applied++;
        try { paint(view); } catch (Throwable unavailable) { abort("Clock preview apply failed."); }
    }
    private static void paint(TextView view) {
        writing = true;
        try { view.setTextColor(selectedColor); } finally { writing = false; }
    }
    static void detach(TextView view) { if (states.containsKey(view)) restore(view); }
    private static void restore(TextView view) {
        ColorRestore<ColorStateList> state = states.get(view);
        if (state == null) return;
        try {
            writing = true;
            view.setTextColor(state.target());
            if (!view.getTextColors().equals(state.target())) throw new IllegalStateException();
            state.restored(); states.remove(view); restored++;
        } catch (Throwable unavailable) { failed = true; message = "Clock restoration incomplete. Disable nubia-ly in Vector and reboot."; }
        finally { writing = false; }
    }
    static void end() {
        generation++; active = false;
        for (TextView view : new ArrayList<>(states.keySet())) restore(view);
        for (XC_MethodHook.Unhook hook : hooks) try { hook.unhook(); } catch (Throwable unavailable) { failed = true; }
        hooks.clear(); deadline = 0;
        if (!failed && applied > 0) message = "Latest stock clock colors restored and read back.";
    }
    private static void abort(String reason) {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post(() -> abort(reason)); return; }
        end(); failed = true; message = reason + " Preview blocked; check restored count. Disable nubia-ly in Vector and reboot if incomplete.";
    }
    static boolean healthy() { return active && !failed && ClockCaptureBackend.available(); }
    static String status() { return message + " Applied: " + applied + "; restored: " + restored + "; remaining: " + states.size() + "."; }
}
