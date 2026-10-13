package com.elitedarkkaiser.nubialy.xposed;

import android.graphics.Typeface;
import android.os.Looper;
import android.util.TypedValue;
import android.widget.TextView;
import de.robv.android.xposed.*;
import java.util.*;

/** Typography authority belongs to the existing guarded clock session. Main thread only. */
final class ClockStyleBackend {
    private record Stock(float pixels, Typeface face, boolean fakeBold, float skew) {}
    private static final Map<TextView, Stock> states = new WeakHashMap<>();
    private static final List<XC_MethodHook.Unhook> hooks = new ArrayList<>();
    private static int percent, depth;
    private static boolean bold, active, writing;
    private static volatile boolean failed;
    static void begin(int size, boolean weight) {
        if (!end() || failed) throw new IllegalStateException("Typography restoration incomplete");
        percent = size; bold = weight; active = size != 0 || weight;
        if (!active) return;
        XC_MethodHook callback = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!eligible(p.thisObject) || writing) return;
                if (Looper.myLooper() != Looper.getMainLooper()) { failed = true; return; }
                p.setObjectExtra("nubia-ly:style", Boolean.TRUE); depth++;
            }
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (p.getObjectExtra("nubia-ly:style") == null) return;
                depth--;
                if (depth != 0 || !active || writing) return;
                if (p.hasThrowable()) { failed = true; return; }
                TextView view = (TextView)p.thisObject;
                Stock old = states.get(view); if (old == null) return;
                Stock current = capture(view);
                boolean sizeWrite = ((java.lang.reflect.Method)p.method).getName().equals("setTextSize");
                states.put(view, sizeWrite ? new Stock(current.pixels, old.face, old.fakeBold, old.skew)
                    : new Stock(old.pixels, current.face, current.fakeBold, current.skew));
                try { paint(view); } catch (Throwable unavailable) { failed = true; }
            }
        };
        try {
            hooks.add(XposedHelpers.findAndHookMethod(TextView.class, "setTextSize", float.class, callback));
            hooks.add(XposedHelpers.findAndHookMethod(TextView.class, "setTextSize", int.class, float.class, callback));
            hooks.add(XposedHelpers.findAndHookMethod(TextView.class, "setTypeface", Typeface.class, callback));
            hooks.add(XposedHelpers.findAndHookMethod(TextView.class, "setTypeface", Typeface.class, int.class, callback));
        } catch (Throwable unavailable) { failed = true; end(); throw new IllegalStateException("Typography hook unavailable", unavailable); }
    }
    private static boolean eligible(Object object) {
        return active && object instanceof TextView view && states.containsKey(view)
            && object.getClass().getName().equals("com.android.systemui.statusbar.policy.Clock");
    }
    private static Stock capture(TextView view) {
        return new Stock(view.getTextSize(), view.getTypeface(), view.getPaint().isFakeBoldText(), view.getPaint().getTextSkewX());
    }
    static void track(TextView view) {
        if (!active) return;
        if (failed || Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Typography unavailable");
        Stock old = states.get(view);
        if (old == null) states.put(view, capture(view));
        else {
            Stock current = capture(view);
            float expected = percent == 0 ? old.pixels : old.pixels * percent / 100f;
            // A vendor dimension refresh may bypass inherited setters. Capture its real result.
            if (percent != 0 && Math.abs(current.pixels - expected) > 0.01f)
                states.put(view, new Stock(current.pixels, old.face, old.fakeBold, old.skew));
        }
        paint(view);
    }
    private static void paint(TextView view) {
        Stock stock = states.get(view); if (stock == null) return;
        writing = true;
        try {
            if (percent != 0 && Math.abs(view.getTextSize() - stock.pixels * percent / 100f) > 0.01f)
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX, stock.pixels * percent / 100f);
            if (bold) {
                Typeface desired = Typeface.create(stock.face, (stock.face == null ? 0 : stock.face.getStyle()) | Typeface.BOLD);
                if (!Objects.equals(view.getTypeface(), desired)) view.setTypeface(desired);
                view.getPaint().setFakeBoldText(false); view.getPaint().setTextSkewX(0);
            }
        } finally { writing = false; }
    }
    static boolean restore(TextView view) {
        Stock stock = states.get(view); if (stock == null) return true;
        writing = true;
        try {
            if (percent != 0) view.setTextSize(TypedValue.COMPLEX_UNIT_PX, stock.pixels);
            if (bold) { view.setTypeface(stock.face); view.getPaint().setFakeBoldText(stock.fakeBold); view.getPaint().setTextSkewX(stock.skew); }
            Stock actual = capture(view);
            if ((percent != 0 && Math.abs(actual.pixels - stock.pixels) > 0.01f)
                || (bold && (!Objects.equals(actual.face, stock.face) || actual.fakeBold != stock.fakeBold || actual.skew != stock.skew))) {
                failed = true; return false;
            }
            states.remove(view); return true;
        } catch (Throwable unavailable) { failed = true; return false; }
        finally { writing = false; }
    }
    static boolean end() {
        active = false;
        for (TextView view : new ArrayList<>(states.keySet())) restore(view);
        for (XC_MethodHook.Unhook hook : hooks) try { hook.unhook(); } catch (Throwable unavailable) { failed = true; }
        hooks.clear(); depth = 0;
        return !failed && states.isEmpty();
    }
    static boolean healthy() { return !failed; }
    static String status() { return "Size: " + (percent == 0 ? "stock" : percent + "%") + "; bold: " + bold + "; typography remaining: " + states.size() + "."; }
}
