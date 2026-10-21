package com.elitedarkkaiser.nubialy.xposed;

import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.view.View;
import android.widget.TextView;
import de.robv.android.xposed.*;
import java.lang.reflect.Method;
import java.util.*;

/** Hide only tracked status-bar clocks; retain stock policy decisions and saved visibility. */
final class ClockVisibilityBackend {
    // Only verified cleanup may clear a restoration-only failure; apply/hook failures stay latched.
    private static boolean hardFailed;
    private static final Map<TextView, Integer> stock = new WeakHashMap<>();
    private static final List<XC_MethodHook.Unhook> hooks = new ArrayList<>();
    private static boolean active, writing;
    private static volatile boolean failed;
    private static int depth;
    private static Class<?> installed;
    static void begin(boolean hide) {
        if (!end() || failed) throw new IllegalStateException("Visibility restoration incomplete"); active = hide;
    }
    private static void install(Class<?> clock) throws Exception {
        if (installed != null) { if (installed != clock) throw new IllegalStateException(); return; }
        Method setter = clock.getDeclaredMethod("setVisibility", int.class);
        Method restore = clock.getDeclaredMethod("onRestoreInstanceState", Parcelable.class);
        Method save = clock.getDeclaredMethod("onSaveInstanceState");
        if (clock.getSuperclass() != TextView.class || setter.getReturnType() != void.class
            || restore.getReturnType() != void.class || save.getReturnType() != Parcelable.class) throw new IllegalStateException();
        XC_MethodHook callback = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!active || writing || !(p.thisObject instanceof TextView view) || !stock.containsKey(view)) return;
                if (Looper.myLooper() != Looper.getMainLooper()) { failed=true;hardFailed=true; return; }
                depth++; p.setObjectExtra("nubia-ly:visibility", Boolean.TRUE);
            }
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (p.getObjectExtra("nubia-ly:visibility") == null) return;
                depth--; if (depth != 0 || !active) return;
                try {
                    if (p.hasThrowable()) throw new IllegalStateException();
                    TextView view = (TextView)p.thisObject;
                    stock.put(view, read(view)); hide(view);
                } catch (Throwable unavailable) { failed=true;hardFailed=true; }
            }
        };
        hooks.add(XposedBridge.hookMethod(setter, callback));
        hooks.add(XposedBridge.hookMethod(restore, callback));
        hooks.add(XposedBridge.hookMethod(save, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (!active || !(p.thisObject instanceof TextView view) || !stock.containsKey(view)) return;
                if (Looper.myLooper() != Looper.getMainLooper() || p.hasThrowable() || !(p.getResult() instanceof Bundle bundle)) { failed=true;hardFailed=true; return; }
                Bundle clean = new Bundle(bundle); clean.putInt("visibility", stock.get(view)); p.setResult(clean);
            }
        })); installed = clock;
    }
    private static int read(TextView view) {
        int v = view.getVisibility();
        if (v != View.VISIBLE && v != View.INVISIBLE && v != View.GONE) throw new IllegalStateException(); return v;
    }
    static void track(TextView view) {
        if (!active || writing || depth != 0) return;
        if (failed || Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Visibility unavailable");
        try { install(view.getClass()); stock.putIfAbsent(view, read(view)); hide(view); }
        catch (Throwable unavailable) { failed=true;hardFailed=true; throw new IllegalStateException("Clock visibility unavailable", unavailable); }
    }
    private static void hide(TextView view) {
        if (read(view) == View.GONE) return;
        writing = true;
        try { view.setVisibility(View.GONE); if (read(view) != View.GONE) throw new IllegalStateException(); }
        finally { writing = false; }
    }
    static boolean restore(TextView view) {
        Integer previous = stock.get(view); if (previous == null) return true;
        writing = true;
        try {
            // Keep the inspected Clock setter's stock visibility policy and vendor adapter.
            view.setVisibility(previous); if (read(view) != previous) throw new IllegalStateException();
            stock.remove(view); return true;
        } catch (Throwable unavailable) { failed=true; return false; }
        finally { writing = false; }
    }
    static boolean end() {
        active = false;
        for (TextView view : new ArrayList<>(stock.keySet())) restore(view);
        for(XC_MethodHook.Unhook hook :new ArrayList<>(hooks))try{hook.unhook();hooks.remove(hook);}catch(Throwable unavailable){ failed=true;hardFailed=true; } installed = null; depth = 0; if(!hardFailed&&stock.isEmpty()&&hooks.isEmpty())failed=false;return !failed && stock.isEmpty();
    }
    static boolean healthy() { return !failed; }
    static String status() { return "Hide clock: " + active + "; visibility remaining: " + stock.size() + "."; }
}
