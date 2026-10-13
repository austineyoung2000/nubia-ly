package com.elitedarkkaiser.nubialy.xposed;

import android.content.res.ColorStateList;
import android.os.Looper;
import android.widget.TextView;
import com.elitedarkkaiser.nubialy.ClockCapture;
import de.robv.android.xposed.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/** NX809J inspected Clock backend: observation only, absolutely no color/view writes. */
final class ClockCaptureBackend {
    private static final Map<TextView, ClockCapture<ColorStateList>> clocks = new WeakHashMap<>();
    private static boolean ready, failed, capturing;
    private static long captures, updates, releases;
    private static String result = "Clock capture has not run.";
    static void install(ClassLoader loader) {
        ArrayList<XC_MethodHook.Unhook> hooks = new ArrayList<>();
        try {
            Class<?> clock = XposedHelpers.findClass("com.android.systemui.statusbar.policy.Clock", loader);
            if (clock.getSuperclass() != TextView.class) throw new IllegalStateException();
            Method adapt = clock.getDeclaredMethod("getAdapt");
            if (!adapt.getReturnType().getName().equals("com.zte.adapt.mifavor.statusbar.ClockAdapt")) throw new IllegalStateException();
            // Match the vendor classifier in the inspected NX809J code, not a guessed view ID.
            Class<?> adapter = adapt.getReturnType();
            Class<?> viewType = adapter.getField("mViewType").getType();
            if (!viewType.getName().equals("com.zte.mifavor.views.MFVViewType")
                    || viewType.getMethod("isTopStatusBar").getReturnType() != boolean.class) throw new IllegalStateException();
            clock.getDeclaredMethod("onDarkChanged", java.util.ArrayList.class, float.class, int.class);
            for (String name : new String[]{"onAttachedToWindow", "onDetachedFromWindow", "updateClock", "onDarkChanged"}) {
                Method method = name.equals("onDarkChanged")
                    ? clock.getDeclaredMethod(name, java.util.ArrayList.class, float.class, int.class)
                    : clock.getDeclaredMethod(name);
                hooks.add(XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam hook) {
                        if (!ready || hook.hasThrowable()) return;
                        try {
                            if (Looper.myLooper() != Looper.getMainLooper()) return;
                            TextView view = (TextView) hook.thisObject;
                            if (name.equals("onDetachedFromWindow")) { release(view); clocks.remove(view); return; }
                            Object a = XposedHelpers.callMethod(view, "getAdapt");
                            Object type = XposedHelpers.getObjectField(a, "mViewType");
                            if (!Boolean.TRUE.equals(XposedHelpers.callMethod(type, "isTopStatusBar"))) return;
                            clocks.computeIfAbsent(view, ignored -> new ClockCapture<>());
                            observe(view);
                        } catch (Throwable unavailable) { fail(); }
                    }
                }));
            }
            ready = true;
        } catch (Throwable unavailable) {
            for (XC_MethodHook.Unhook hook : hooks) try { hook.unhook(); } catch (Throwable ignored) {}
            fail();
        }
    }
    static boolean begin() {
        end(); captures = updates = releases = 0;
        if (!ready || failed) { result = "Clock backend unavailable; rehearsal blocked."; return false; }
        capturing = true;
        for (TextView view : new ArrayList<>(clocks.keySet())) if (view.isAttachedToWindow()) observe(view);
        if (captures == 0 || failed) { end(); result = "No attached top-status-bar clock captured; rehearsal blocked."; return false; }
        result = "Stock clock color captured; no color override applied."; return true;
    }
    static void refresh() {
        if (!capturing) return;
        for (TextView view : new ArrayList<>(clocks.keySet())) observe(view);
    }
    private static void observe(TextView view) {
        ClockCapture<ColorStateList> ledger = clocks.get(view);
        if (ledger == null || !capturing || failed) return;
        ColorStateList colors = view.getTextColors();
        if (!ledger.captured()) { ledger.begin(colors); captures++; }
        else { long before = ledger.updates(); ledger.observe(colors); updates += ledger.updates() - before; }
    }
    private static void release(TextView view) {
        ClockCapture<ColorStateList> ledger = clocks.get(view);
        if (ledger == null || !ledger.captured()) return;
        // Refresh first so a stock theme update wins over the initially captured value.
        observe(view);
        ColorStateList expected = ledger.release();
        if (!view.getTextColors().equals(expected)) { failed = true; result = "Clock capture verification failed; visual changes blocked."; }
        else releases++;
    }
    static void end() {
        for (TextView view : new ArrayList<>(clocks.keySet())) release(view);
        capturing = false;
        if (!failed && captures > 0) result = "Captured clock state released and verified unchanged. This does not prove visual rollback.";
    }
    private static void fail() { ready = false; failed = true; capturing = false; result = "Clock observer unavailable; visual changes blocked."; }
    static String status() {
        refresh();
        return result + "\nClock captures: " + captures + "; stock color changes observed: " + updates
            + "; verified releases: " + releases + ".";
    }
}
