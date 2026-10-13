package com.elitedarkkaiser.nubialy.xposed;

import android.os.Build;
import com.elitedarkkaiser.nubialy.CompatibilityPolicy;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Vector-compatible legacy entry point. Deliberately registers zero hooks in milestone 1. */
public final class NubiaLyModule implements IXposedHookLoadPackage {
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) {
        if (!"com.android.systemui".equals(param.packageName) || !"com.android.systemui".equals(param.processName)) return;
        if (!CompatibilityPolicy.isNx809j(Build.MODEL, Build.DEVICE)) return;
        if (Build.VERSION.SDK_INT != 36) return;
        XposedBridge.log("nubia-ly: Vector module entry loaded on NX809J; no hooks registered.");
    }
}
