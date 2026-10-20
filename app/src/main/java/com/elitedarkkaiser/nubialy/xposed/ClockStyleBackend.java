package com.elitedarkkaiser.nubialy.xposed;

import android.graphics.Typeface;
import android.os.Looper;
import android.util.TypedValue;
import android.widget.TextView;
import de.robv.android.xposed.*;
import java.util.*;

/** Typography authority belongs to the existing guarded clock session. Main thread only. */
final class ClockStyleBackend {
    // Only verified cleanup may clear a restoration-only failure; apply/hook failures stay latched.
    private static boolean hardFailed;
    private record Stock(float pixels, Typeface face, boolean fakeBold, float skew, float spacing) {}
    private static final Map<TextView, Stock> states = new WeakHashMap<>();
    private static final Map<TextView, Long> revisions = new WeakHashMap<>();
    private static long generation;
    private static final List<XC_MethodHook.Unhook> hooks = new ArrayList<>();
    private static int percent, depth, spacing, family;
    private static boolean bold, italic, active, writing;
    private static volatile boolean failed;
    static void begin(int size, boolean weight, boolean slanted, int tracking, int font) {
        if (!end() || failed) throw new IllegalStateException("Typography restoration incomplete");
        percent = size; bold = weight; italic = slanted; spacing = tracking; family = font; active = size != 0 || weight || slanted || tracking != 0 || font != 0;
        if (!active) return;
        XC_MethodHook callback = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!eligible(p.thisObject) || writing) return;
                if (Looper.myLooper() != Looper.getMainLooper()) { failed=true;hardFailed=true; return; }
                p.setObjectExtra("nubia-ly:style", Boolean.TRUE); depth++;
            }
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (p.getObjectExtra("nubia-ly:style") == null) return;
                depth--;
                if (depth != 0 || !active || writing) return;
                if (p.hasThrowable()) { failed=true;hardFailed=true; return; }
                TextView view = (TextView)p.thisObject;
                Stock old = states.get(view); if (old == null) return;
                Stock current = capture(view);
                revisions.put(view, revisions.getOrDefault(view, 0L) + 1);
                String method = ((java.lang.reflect.Method)p.method).getName();
                states.put(view, method.equals("setTextSize") ? new Stock(current.pixels, old.face, old.fakeBold, old.skew, old.spacing)
                    : method.equals("setLetterSpacing") ? new Stock(old.pixels, old.face, old.fakeBold, old.skew, current.spacing)
                    : new Stock(old.pixels, current.face, current.fakeBold, current.skew, old.spacing));
                try { paint(view); } catch (Throwable unavailable) { failed=true;hardFailed=true; }
            }
        };
        try {
            hooks.add(XposedHelpers.findAndHookMethod(TextView.class, "setTextSize", float.class, callback));
            hooks.add(XposedHelpers.findAndHookMethod(TextView.class, "setTextSize", int.class, float.class, callback));
            hooks.add(XposedHelpers.findAndHookMethod(TextView.class, "setTypeface", Typeface.class, callback));
            hooks.add(XposedHelpers.findAndHookMethod(TextView.class, "setTypeface", Typeface.class, int.class, callback));
            if (spacing != 0) hooks.add(XposedHelpers.findAndHookMethod(TextView.class, "setLetterSpacing", float.class, callback));
        } catch (Throwable unavailable) { failed=true;hardFailed=true; end(); throw new IllegalStateException("Typography hook unavailable", unavailable); }
    }
    record DimensionFrame(long generation, Stock stock, long revision) {}
    static DimensionFrame beforeDimensions(TextView view) {
        Stock stock = states.get(view);
        return !active || failed || stock == null || Looper.myLooper() != Looper.getMainLooper() ? null
            : new DimensionFrame(generation, stock, revisions.getOrDefault(view, 0L));
    }
    static void afterDimensions(TextView view, DimensionFrame frame) {
        if (frame == null || !active || failed || generation != frame.generation || !states.containsKey(view)
            || Looper.myLooper() != Looper.getMainLooper()) return;
        if (percent != 0 && revisions.getOrDefault(view, 0L) == frame.revision) {
            Stock old = states.get(view);
            states.put(view, new Stock(view.getTextSize(), old.face, old.fakeBold, old.skew, old.spacing));
        }
        try { paint(view); } catch (Throwable unavailable) { failed=true;hardFailed=true; }
    }
    private static boolean eligible(Object object) {
        return active && object instanceof TextView view && states.containsKey(view)
            && object.getClass().getName().equals("com.android.systemui.statusbar.policy.Clock");
    }
    private static Stock capture(TextView view) {
        return new Stock(view.getTextSize(), view.getTypeface(), view.getPaint().isFakeBoldText(), view.getPaint().getTextSkewX(), view.getLetterSpacing());
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
                states.put(view, new Stock(current.pixels, old.face, old.fakeBold, old.skew, old.spacing));
        }
        Stock baseline = states.get(view);
        if (spacing != 0 && Math.abs(view.getLetterSpacing() - (baseline.spacing + spacing / 100f)) > 0.00001f)
            states.put(view, new Stock(baseline.pixels, baseline.face, baseline.fakeBold, baseline.skew, view.getLetterSpacing()));
        paint(view);
    }
    private static void paint(TextView view) {
        Stock stock = states.get(view); if (stock == null) return;
        writing = true;
        try {
            if (percent != 0 && Math.abs(view.getTextSize() - stock.pixels * percent / 100f) > 0.01f)
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX, stock.pixels * percent / 100f);
            if (bold || italic || family != 0) {
                int style = (stock.face == null ? 0 : stock.face.getStyle()) | (bold ? Typeface.BOLD : 0) | (italic ? Typeface.ITALIC : 0);
                Typeface desired = family == 0 ? Typeface.create(stock.face, style)
                    : Typeface.create(com.elitedarkkaiser.nubialy.ClockStylePolicy.familyName(family), style);
                if (!Objects.equals(view.getTypeface(), desired)) view.setTypeface(desired);
                view.getPaint().setFakeBoldText(bold ? false : stock.fakeBold); view.getPaint().setTextSkewX(0);
            }
            if (spacing != 0 && Math.abs(view.getLetterSpacing() - (stock.spacing + spacing / 100f)) > 0.00001f)
                view.setLetterSpacing(stock.spacing + spacing / 100f);
        } finally { writing = false; }
    }
    static boolean restore(TextView view) {
        Stock stock = states.get(view); if (stock == null) return true;
        writing = true;
        try {
            if (percent != 0) view.setTextSize(TypedValue.COMPLEX_UNIT_PX, stock.pixels);
            if (bold || italic || family != 0) { view.setTypeface(stock.face); view.getPaint().setFakeBoldText(stock.fakeBold); view.getPaint().setTextSkewX(stock.skew); }
            if (spacing != 0) view.setLetterSpacing(stock.spacing);
            Stock actual = capture(view);
            if ((percent != 0 && Math.abs(actual.pixels - stock.pixels) > 0.01f)
                || (spacing != 0 && Math.abs(actual.spacing - stock.spacing) > 0.00001f)
                || ((bold || italic || family != 0) && (!Objects.equals(actual.face, stock.face) || actual.fakeBold != stock.fakeBold || actual.skew != stock.skew))) {
                failed=true; return false;
            }
            states.remove(view); revisions.remove(view); return true;
        } catch (Throwable unavailable) { failed=true; return false; }
        finally { writing = false; }
    }
    static boolean end() {
        generation++; active = false;
        for (TextView view : new ArrayList<>(states.keySet())) restore(view);
        for(XC_MethodHook.Unhook hook :new ArrayList<>(hooks))try{hook.unhook();hooks.remove(hook);}catch(Throwable unavailable){ failed=true;hardFailed=true; } depth = 0;
        if(!hardFailed&&states.isEmpty()&&hooks.isEmpty())failed=false;return !failed && states.isEmpty();
    }
    static boolean healthy() { return !failed; }
    static String status() { return "Size: " + (percent == 0 ? "stock" : percent + "%") + "; bold: " + bold + "; italic: " + italic + "; spacing offset: " + spacing / 100f + " em; typography remaining: " + states.size() + "."; }
}
