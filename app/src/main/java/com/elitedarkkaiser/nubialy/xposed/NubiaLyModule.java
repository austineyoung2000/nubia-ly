package com.elitedarkkaiser.nubialy.xposed;

import android.os.Build;
import android.app.Application;
import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import com.elitedarkkaiser.nubialy.ModuleStatus;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodReplacement;
import com.elitedarkkaiser.nubialy.CompatibilityPolicy;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Vector-compatible legacy entry point. App activation and a read-only System UI lifecycle diagnostic; no cosmetics. */
public final class NubiaLyModule implements IXposedHookLoadPackage {
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) {
        if ("com.elitedarkkaiser.nubialy".equals(param.packageName)
                && "com.elitedarkkaiser.nubialy".equals(param.processName)) {
            if (!CompatibilityPolicy.isNx809j(Build.MODEL, Build.DEVICE) || Build.VERSION.SDK_INT != 36) return;
            try {
                XposedHelpers.findAndHookMethod(ModuleStatus.class.getName(), param.classLoader,
                    "isActive", XC_MethodReplacement.returnConstant(true));
            } catch (Throwable failure) {
                XposedBridge.log("nubia-ly: activation probe unavailable; status remains inactive.");
            }
            return;
        }
        if (!"com.android.systemui".equals(param.packageName) || !"com.android.systemui".equals(param.processName)) return;
        if (!CompatibilityPolicy.isNx809j(Build.MODEL, Build.DEVICE)) return;
        if (Build.VERSION.SDK_INT != 36) return;
        try {
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam hook) {
                    if (hook.hasThrowable()) return;
                    try { SystemUiGuardReceiver.install((Application) hook.thisObject); }
                    catch (Throwable unavailable) { XposedBridge.log("nubia-ly: guard diagnostic unavailable."); }
                }
            });
        } catch (Throwable unavailable) {
            XposedBridge.log("nubia-ly: System UI lifecycle diagnostic unavailable; cosmetics blocked.");
        }
        XposedBridge.log("nubia-ly: Vector module entry loaded on NX809J; no cosmetic hooks registered.");
    }
}
