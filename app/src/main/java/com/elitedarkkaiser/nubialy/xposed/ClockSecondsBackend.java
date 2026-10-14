package com.elitedarkkaiser.nubialy.xposed;

import android.os.Looper;
import android.widget.TextView;
import de.robv.android.xposed.*;
import java.lang.reflect.Method;
import java.util.*;

/** Reuse the inspected NX809J clock's own seconds lifecycle; no module ticker or settings write. */
final class ClockSecondsBackend {
    private static final Map<TextView, Boolean> stock = new WeakHashMap<>();
    private static final List<XC_MethodHook.Unhook> hooks = new ArrayList<>();
    private static boolean active, writing;
    private static volatile boolean failed;
    private static int depth;
    private static Class<?> installed;
    static void begin(boolean seconds) {
        if (!end() || failed) throw new IllegalStateException("Seconds restoration incomplete");
        active = seconds;
    }
    private static void install(Class<?> clock) throws Exception {
        if (installed != null) { if (installed != clock) throw new IllegalStateException(); return; }
        if (clock.getSuperclass() != TextView.class || clock.getDeclaredField("mShowSeconds").getType() != boolean.class
            || clock.getDeclaredField("mSecondsHandler").getType() != android.os.Handler.class
            || clock.getDeclaredMethod("isShowSeconds").getReturnType() != boolean.class
            || clock.getDeclaredMethod("setShowSeconds", boolean.class).getReturnType() != void.class
            || clock.getDeclaredMethod("updateShowSeconds").getReturnType() != void.class
            || clock.getDeclaredMethod("updateClock").getReturnType() != void.class) throw new IllegalStateException();
        Method setter = clock.getDeclaredMethod("setShowSeconds", boolean.class);
        Method tuner = clock.getDeclaredMethod("onTuningChanged", String.class, String.class);
        XC_MethodHook callback = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!active || writing || !(p.thisObject instanceof TextView view) || !stock.containsKey(view)) return;
                if (((Method)p.method).getName().equals("onTuningChanged") && !"clock_seconds".equals(p.args[0])) return;
                if (Looper.myLooper() != Looper.getMainLooper()) { failed = true; return; }
                depth++; p.setObjectExtra("nubia-ly:seconds", Boolean.TRUE);
            }
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (p.getObjectExtra("nubia-ly:seconds") == null) return;
                depth--; if (depth != 0 || !active) return;
                try {
                    if (p.hasThrowable()) throw new IllegalStateException();
                    TextView view = (TextView)p.thisObject;
                    stock.put(view, read(view)); ensure(view);
                } catch (Throwable unavailable) { failed = true; }
            }
        };
        hooks.add(XposedBridge.hookMethod(setter, callback));
        hooks.add(XposedBridge.hookMethod(tuner, callback)); installed = clock;
    }
    private static boolean read(TextView view) { return Boolean.TRUE.equals(XposedHelpers.callMethod(view, "isShowSeconds")); }
    static void track(TextView view) {
        if (!active || writing || depth != 0) return;
        if (failed || Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Seconds unavailable");
        try {
            install(view.getClass()); stock.putIfAbsent(view, read(view)); ensure(view);
        } catch (Throwable unavailable) { failed = true; throw new IllegalStateException("Stock seconds unavailable", unavailable); }
    }
    private static void ensure(TextView view) {
        if (read(view) && XposedHelpers.getObjectField(view, "mSecondsHandler") != null) return;
        writing = true;
        try {
            XposedHelpers.callMethod(view, "setShowSeconds", true);
            XposedHelpers.callMethod(view, "updateShowSeconds");
            XposedHelpers.callMethod(view, "updateClock");
            if (!read(view) || XposedHelpers.getObjectField(view, "mSecondsHandler") == null) throw new IllegalStateException();
        } finally { writing = false; }
    }
    static boolean restore(TextView view) {
        Boolean previous = stock.get(view); if (previous == null) return true;
        writing = true;
        try {
            XposedHelpers.callMethod(view, "setShowSeconds", previous.booleanValue());
            XposedHelpers.callMethod(view, "updateShowSeconds");
            XposedHelpers.callMethod(view, "updateClock");
            if (read(view) != previous.booleanValue() || (!previous && XposedHelpers.getObjectField(view, "mSecondsHandler") != null)) throw new IllegalStateException();
            stock.remove(view); return true;
        } catch (Throwable unavailable) { failed = true; return false; }
        finally { writing = false; }
    }
    static boolean end() {
        active = false;
        for (TextView view : new ArrayList<>(stock.keySet())) restore(view);
        for (XC_MethodHook.Unhook hook : hooks) try { hook.unhook(); } catch (Throwable unavailable) { failed = true; }
        hooks.clear(); installed = null; depth = 0;
        return !failed && stock.isEmpty();
    }
    static boolean healthy() { return !failed; }
    static String status() { return "Seconds override: " + active + "; seconds remaining: " + stock.size() + "."; }
}
