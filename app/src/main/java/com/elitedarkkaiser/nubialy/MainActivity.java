package com.elitedarkkaiser.nubialy;

import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends AppCompatActivity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView compatibility;
    private TextView rootStatus;
    private SystemUiGuardClient systemUiCheck;
    private TextView activationStatus;
    private TextView recoveryStatus;
    private MaterialButton inspectButton;
    private MaterialButton rootButton;
    private boolean closed;
    private long generation;
    private LinearLayout content;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(28), dp(24), dp(24));
        scroll.addView(content);
        setContentView(scroll);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(scroll, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        content.addView(label("nubia-ly", 36, true));
        content.addView(label("Make your REDMAGIC feel like yours.", 16, false));
        spacer(24);
        if (!CompatibilityPolicy.isNx809j(Build.MODEL, Build.DEVICE)) {
            content.addView(label("Device not supported", 24, true));
            content.addView(label("This first release is restricted to REDMAGIC 11 Pro (NX809J).", 16, false));
            MaterialButton close = button("Close app");
            close.setOnClickListener(v -> finish());
            content.addView(close);
            return;
        }
        LinearLayout device = card("Your device", "REDMAGIC 11 Pro · NX809J");
        compatibility = label("Firmware has not been checked.", 15, false);
        device.addView(compatibility);
        inspectButton = button("Check firmware");
        inspectButton.setOnClickListener(v -> inspect());
        device.addView(inspectButton);
        rootStatus = label("Root is optional for inspection.", 14, false);
        device.addView(rootStatus);
        rootButton = button("Check root access");
        rootButton.setOnClickListener(v -> checkRoot());
        device.addView(rootButton);
        spacer(12);
        LinearLayout vector = card("Vector framework", "Enable nubia-ly for System UI in Vector, then reboot.");
        activationStatus = label("", 14, false);
        vector.addView(activationStatus);
        vector.addView(label("App activation does not confirm System UI loading. Verify its entry in Vector logs. No System UI customization hooks are registered.", 14, false));
        spacer(20);
        content.addView(label("Your canvas", 24, true));
        content.addView(label("Customization features are being developed. This foundation build only inspects your device.", 14, false));
        spacer(10);
        for (FeatureCatalog.Feature feature : FeatureCatalog.FEATURES) {
            LinearLayout featureCard = card(feature.title(), feature.description());
            TextView badge = label(feature.state() == FeatureCatalog.State.BLOCKED ? "Blocked experiment" : "Coming later", 13, true);
            badge.setTextColor(0xffffbe98);
            featureCard.addView(badge);
            featureCard.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(feature.title()).setMessage(feature.description() + "\n\nNo change will be applied in this build.")
                .setPositiveButton("Got it", null).show());
        }
        spacer(12);
        LinearLayout recovery = card("Restore & recovery", "Recovery must be ready before the first customization is enabled.");
        recoveryStatus = label("", 14, false);
        recovery.addView(recoveryStatus);
        MaterialButton stop = button("Emergency stop customizations");
        recovery.addView(stop);
        stop.setOnClickListener(v -> {
            stop.setEnabled(false);
            worker.execute(() -> {
                boolean saved = RecoveryStore.stop(getApplicationContext());
                runOnUiThread(() -> {
                    if (closed || isFinishing() || isDestroyed()) return;
                    stop.setEnabled(true);
                    recoveryStatus.setText(saved ? "Emergency stop saved. Customizations remain blocked."
                        : "Could not save emergency stop. Customizations remain blocked in this build.");
                });
            });
        });
        TextView guardStatus = label("Independent guard has not been checked.", 14, false);
        recovery.addView(guardStatus);
        MaterialButton guardCheck = button("Check recovery guard");
        recovery.addView(guardCheck);
        guardCheck.setOnClickListener(v -> {
            guardCheck.setEnabled(false);
            worker.execute(() -> {
                String result = GuardReader.inspect();
                runOnUiThread(() -> {
                    if (closed || isFinishing() || isDestroyed()) return;
                    guardCheck.setEnabled(true);
                    guardStatus.setText(result);
                });
            });
        });
        recovery.addView(label("Optional companion guard: install its test ZIP using your root manager. Its Action button stops the guard probe independently of this app. Each reboot invalidates the probe. This is a transport test, not cosmetic permission or verified boot-loop recovery.", 14, false));
        recovery.addView(label("This build cannot arm customizations. This latch does not disable Vector injection or restore stock resources. The app does not install the companion module. Full boot recovery remains unverified. If the app cannot open, disable nubia-ly in Vector and reboot.", 14, false));
        systemUiCheck = new SystemUiGuardClient(this);
        TextView systemUiStatus = label("System UI guard access has not been checked.", 14, false);
        recovery.addView(systemUiStatus);
        MaterialButton checkSystemUi = button("Check System UI guard");
        recovery.addView(checkSystemUi);
        checkSystemUi.setOnClickListener(v -> {
            checkSystemUi.setEnabled(false);
            systemUiStatus.setText("Waiting for System UI's guard check…");
            systemUiCheck.check(result -> {
                if (closed || isFinishing() || isDestroyed()) return;
                checkSystemUi.setEnabled(true);
                systemUiStatus.setText(result);
            });
        });
        refreshSafetyStatus();
        content.addView(label("0.1.3 · Independent from Redmagic 11 Toolbox", 12, false));
    }

    @Override protected void onResume() {
        super.onResume();
        refreshSafetyStatus();
    }
    private void refreshSafetyStatus() {
        if (activationStatus == null || recoveryStatus == null) return;
        activationStatus.setText(ModuleStatus.isActive()
            ? "Vector activation confirmed in nubia-ly."
            : "Vector activation not detected in nubia-ly. Enable the module and reboot.");
        recoveryStatus.setText(RecoveryStore.isStopped(this)
            ? "Emergency stop active. Customizations blocked."
            : "Customizations blocked: recovery backend has not been verified.");
    }

    private void inspect() {
        inspectButton.setEnabled(false);
        compatibility.setText("Checking your installed System UI…");
        long current = ++generation;
        worker.execute(() -> {
            DeviceInspector.Snapshot snapshot = DeviceInspector.inspect(getApplicationContext());
            runOnUiThread(() -> {
                if (closed || isFinishing() || isDestroyed() || current != generation) return;
                inspectButton.setEnabled(true);
                compatibility.setText(switch (snapshot.status()) {
                    case RESEARCH_BASELINE -> "Matches the inspected stock build. Customization is not yet enabled.";
                    case UNRECOGNIZED_FIRMWARE -> "This firmware needs its own compatibility validation.";
                    case INSPECTION_UNAVAILABLE -> "System UI could not be verified. No changes are available.";
                    default -> "Unsupported device.";
                });
                compatibility.setOnLongClickListener(v -> {
                    new MaterialAlertDialogBuilder(this).setTitle("Firmware details")
                        .setMessage(snapshot.fingerprint() + "\n\nSystem UI SHA-256:\n" + (snapshot.systemUiHash().isEmpty() ? "Unavailable" : snapshot.systemUiHash()))
                        .setPositiveButton("Close", null).show();
                    return true;
                });
            });
        });
    }
    private void checkRoot() {
        rootButton.setEnabled(false);
        rootStatus.setText("Waiting for root permission…");
        worker.execute(() -> {
            String result = RootInspector.inspect();
            runOnUiThread(() -> {
                if (closed || isFinishing() || isDestroyed()) return;
                rootButton.setEnabled(true);
                rootStatus.setText(result);
            });
        });
    }
    private LinearLayout card(String title, String description) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(24));
        card.setCardElevation(0);
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(0xff302c42);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(params);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(20), dp(18), dp(20), dp(18));
        inner.addView(label(title, 20, true));
        inner.addView(label(description, 14, false));
        card.addView(inner);
        content.addView(card);
        return inner;
    }
    private TextView label(String text, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text); view.setTextSize(size);
        view.setTextColor(bold ? 0xfff0ecf8 : 0xffbcb6ca);
        view.setPadding(0, 0, 0, dp(8));
        if (bold) view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }
    private MaterialButton button(String text) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text); button.setAllCaps(false); button.setCornerRadius(dp(18));
        return button;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void spacer(int height) { View space = new View(this); content.addView(space, new LinearLayout.LayoutParams(1, dp(height))); }
    @Override protected void onDestroy() { closed = true; generation++; if (systemUiCheck != null) systemUiCheck.cancel(); worker.shutdownNow(); super.onDestroy(); }
}
