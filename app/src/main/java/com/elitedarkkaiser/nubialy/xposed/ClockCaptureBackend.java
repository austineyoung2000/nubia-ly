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
    private static boolean ready, failed, capturing, attempted;
    private static long captures, updates, releases;
    private static String failure = "Not installed";
    private static String result = "Clock capture has not run.";
    static synchronized void install(ClassLoader loader) {
        if (attempted) return;
        attempted = true;
        ArrayList<XC_MethodHook.Unhook> hooks = new ArrayList<>();
        String stage = "Clock class lookup";
        try {
            Class<?> clock = XposedHelpers.findClass("com.android.systemui.statusbar.policy.Clock", loader);
            stage = "Clock superclass check";
            if (clock.getSuperclass() != TextView.class) throw new IllegalStateException();
            stage = "Clock adapter signature";
            Method adapt = clock.getDeclaredMethod("getAdapt");
            if (!adapt.getReturnType().getName().equals("com.zte.adapt.mifavor.statusbar.ClockAdapt")) throw new IllegalStateException();
            // Match the vendor classifier in the inspected NX809J code, not a guessed view ID.
            stage = "Vendor classifier field";
            Class<?> adapter = adapt.getReturnType();
            Class<?> viewType = adapter.getField("mViewType").getType();
            stage = "Vendor classifier signature";
            if (!viewType.getName().equals("com.zte.mifavor.views.MFVViewType")
                    || viewType.getMethod("isTopStatusBar").getReturnType() != boolean.class) throw new IllegalStateException();
            stage = "Dark callback signature";
            clock.getDeclaredMethod("onDarkChanged", java.util.ArrayList.class, float.class, int.class);
            for (String name : new String[]{"onAttachedToWindow", "onDetachedFromWindow", "updateClock", "reloadDimens", "onDarkChanged"}) {
                stage = "Hook registration: " + name;
                Method method = name.equals("onDarkChanged")
                    ? clock.getDeclaredMethod(name, java.util.ArrayList.class, float.class, int.class)
                    : clock.getDeclaredMethod(name);
                hooks.add(XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam hook) {
                        if (!ready || !name.equals("onDarkChanged")) return;
                        try { hook.setObjectExtra("nubia-ly:dark-frame", ClockColorBackend.beforeDark((TextView)hook.thisObject)); }
                        catch (Throwable unavailable) { fail("Clock dark entry", unavailable); }
                    }
                    @Override protected void afterHookedMethod(MethodHookParam hook) {
                        if (!ready || hook.hasThrowable()) return;
                        try {
                            if (Looper.myLooper() != Looper.getMainLooper()) return;
                            TextView view = (TextView) hook.thisObject;
                            if (name.equals("onDetachedFromWindow")) { ClockColorBackend.detach(view); release(view); clocks.remove(view); return; }
                            Object a = XposedHelpers.callMethod(view, "getAdapt");
                            if (a == null) return;
                            Object type = XposedHelpers.getObjectField(a, "mViewType");
                            if (type == null) return;
                            if (!Boolean.TRUE.equals(XposedHelpers.callMethod(type, "isTopStatusBar"))) return;
                            clocks.computeIfAbsent(view, ignored -> new ClockCapture<>());
                            if (name.equals("onDarkChanged")) ClockColorBackend.afterDark(view,
                                (ClockColorBackend.DarkFrame)hook.getObjectExtra("nubia-ly:dark-frame"));
                            ClockColorBackend.track(view);
                            observe(view);
                        } catch (Throwable unavailable) { fail("Clock callback: " + name, unavailable); }
                    }
                }));
            }
            ready = true;
        } catch (Throwable unavailable) {
            for (XC_MethodHook.Unhook hook : hooks) try { hook.unhook(); } catch (Throwable ignored) {}
            fail(stage, unavailable);
        }
    }
    static boolean available() { return ready && !failed; }
    static java.util.List<TextView> attachedClocks() {
        if (!ready || failed) return java.util.List.of();
        return new ArrayList<>(clocks.keySet());
    }
    static boolean begin() {
        end(); captures = updates = releases = 0;
        if (!ready || failed) { result = "Clock backend unavailable: " + failure + ". Rehearsal blocked."; return false; }
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
    private static void fail(String stage, Throwable error) {
        ready = false; failed = true; capturing = false;
        failure = stage + " (" + error.getClass().getSimpleName() + ")";
        result = "Clock observer unavailable: " + failure + ". Visual changes blocked.";
        XposedBridge.log("nubia-ly: " + failure);
    }
    static String status() {
        refresh();
        return result + "\nClock captures: " + captures + "; stock color changes observed: " + updates
            + "; verified releases: " + releases + ".";
    }
}
