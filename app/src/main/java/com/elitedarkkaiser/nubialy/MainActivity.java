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
    private SystemUiGuardClient previewCheck;
    private TextView previewStatus;
    private MaterialButton previewStart, previewRefresh, previewStop;
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
        content.setPadding(dp(20), dp(28), dp(20), dp(24));
        scroll.addView(content);
        setContentView(scroll);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(scroll, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        content.addView(label("Personalizations", 34, true));
        content.addView(label("Your REDMAGIC. Your style.", 16, false));
        spacer(24);
        if (!CompatibilityPolicy.isNx809j(Build.MODEL, Build.DEVICE)) {
            content.addView(label("Device not supported", 24, true));
            content.addView(label("This first release is restricted to REDMAGIC 11 Pro (NX809J).", 16, false));
            MaterialButton close = button("Close app");
            close.setOnClickListener(v -> finish());
            content.addView(close);
            return;
        }
        int setupStart=content.getChildCount();
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
        vector.addView(label("App activation does not confirm System UI loading. Verify its entry in Vector logs. Enable persistent status-bar styling in its editor.", 14, false));
        int setupEnd=content.getChildCount();
        spacer(20);
        content.addView(PersonalizationUi.dashboard(this, section -> startActivity(
            new android.content.Intent(this,section.equals("launcher")?LauncherActivity.class:section.equals("boot_animation")?BootAnimationActivity.class:ClockEditorActivity.class).putExtra("section",section))));
        spacer(16);
        int recoveryStart=content.getChildCount();
        LinearLayout recovery = card("Restore & recovery", "Recovery must be ready before the first customization is enabled.");
        recoveryStatus = label("", 14, false);
        recovery.addView(recoveryStatus);
        MaterialButton stop = button("Emergency stop customizations");
        recovery.addView(stop);
        stop.setOnClickListener(v -> {
            stop.setEnabled(false);
            worker.execute(() -> {
                boolean saved = RecoveryStore.stop(getApplicationContext());
                boolean guardStopped=RootGuardCommand.stopClock(android.os.SystemClock.elapsedRealtime()+8000);
                runOnUiThread(() -> {
                    if (closed || isFinishing() || isDestroyed()) return;
                    stop.setEnabled(true);
                    if (saved && previewCheck != null) {
                        previewCheck.cancel(); setPreviewButtons(true);
                        previewStatus.setText("Emergency stop saved. Lifecycle stop requested; check status to verify.");
                        previewCheck.preview("stop", result -> {
                            if (!closed && !isFinishing() && !isDestroyed()) previewStatus.setText(result);
                        });
                    }
                    recoveryStatus.setText(saved ? (guardStopped?"Emergency stop saved. Boot animations revert on reboot.":"System UI stop saved. Root guard stop unconfirmed; use its Action button.")
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
        recovery.addView(label("Clock styling uses explicit Enable and an independent root guard. Other customization areas remain unavailable. This latch does not disable Vector injection or restore stock resources. The app does not install the companion module. Full boot recovery remains unverified. If the app cannot open, disable nubia-ly in Vector and reboot.", 14, false));
        systemUiCheck = new SystemUiGuardClient(this);
        TextView systemUiStatus = label("System UI broker access has not been checked.", 14, false);
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
        LinearLayout preview = card("Clock capture rehearsal", "Capture the stock status-bar clock color and release it through stop/expiry. Your clock stays unchanged. Requires a fresh lifecycle-only permit.");
        previewCheck = new SystemUiGuardClient(this);
        previewStatus = label("No rehearsal checked. This card never changes clock color.", 14, false);
        preview.addView(previewStatus);
        previewStart = button("Start clock capture"); preview.addView(previewStart);
        previewRefresh = button("Check clock capture"); preview.addView(previewRefresh);
        previewStop = button("End clock capture"); preview.addView(previewStop);
        previewStart.setOnClickListener(v -> {
            setPreviewButtons(false); previewStatus.setText("Checking lifecycle-only permit…");
            worker.execute(() -> {
                boolean allowed = RecoveryStore.allowLifecycle(getApplicationContext());
                runOnUiThread(() -> {
                    if (closed || isFinishing() || isDestroyed()) return;
                    if (!allowed) { setPreviewButtons(true); previewStatus.setText("Start blocked: publish a fresh lifecycle-only permit first."); return; }
                    requestPreview("start");
                });
            });
        });
        previewRefresh.setOnClickListener(v -> requestPreview("status"));
        previewStop.setOnClickListener(v -> {
            setPreviewButtons(false);
            worker.execute(() -> {
                boolean saved = RecoveryStore.stop(getApplicationContext());
                runOnUiThread(() -> {
                    if (closed || isFinishing() || isDestroyed()) return;
                    requestPreview("stop");
                    if (!saved) previewStatus.setText("Persistent stop could not be saved; System UI stop requested. Check status.");
                });
            });
        });
        // Keep tools available while letting customization lead the home screen.
        java.util.List<View> setupViews=new java.util.ArrayList<>();for(int i=setupStart;i<setupEnd;i++)setupViews.add(content.getChildAt(i));
        java.util.List<View> recoveryViews=new java.util.ArrayList<>();for(int i=recoveryStart;i<content.getChildCount();i++)recoveryViews.add(content.getChildAt(i));
        for(View v:setupViews)content.removeView(v);for(View v:recoveryViews)content.removeView(v);
        LinearLayout setup=foldout("Setup & device", "Firmware, root access and Vector",false);for(View v:setupViews)setup.addView(v);
        LinearLayout tools=foldout("Recovery & diagnostics", "Restore tools, guard checks and technical details",false);for(View v:recoveryViews)tools.addView(v);
        refreshSafetyStatus();
        content.addView(label(BuildConfig.VERSION_NAME+" · nubia-ly", 12, false));
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
            ? "Emergency stop active. Use Enable clock color to explicitly turn clock styling on."
            : "Emergency stop inactive. Open Status bar → Clock for current clock controls.");
    }

    private void setPreviewButtons(boolean enabled) {
        if (previewStart == null) return;
        previewStart.setEnabled(enabled); previewRefresh.setEnabled(enabled); previewStop.setEnabled(enabled);
    }
    private void requestPreview(String operation) {
        setPreviewButtons(false);
        previewCheck.preview(operation, result -> {
            if (closed || isFinishing() || isDestroyed()) return;
            setPreviewButtons(true); previewStatus.setText(result);
        });
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
    private LinearLayout foldout(String title,String subtitle,boolean expanded){
        LinearLayout card=card(title,subtitle),body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setVisibility(expanded?View.VISIBLE:View.GONE);
        MaterialButton toggle=button(expanded?"Hide details":"Show details");card.addView(toggle);card.addView(body);toggle.setOnClickListener(v->{boolean show=body.getVisibility()!=View.VISIBLE;body.setVisibility(show?View.VISIBLE:View.GONE);toggle.setText(show?"Hide details":"Show details");});return body;
    }
    private LinearLayout card(String title, String description) {
        MaterialCardView card = new MaterialCardView(this);
        PersonalizationUi.panel(card);
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
        view.setTextColor(getColor(bold ? R.color.on_surface : R.color.on_surface_variant));
        view.setPadding(0, 0, 0, dp(8));
        if (bold) view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }
    private MaterialButton button(String text) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text); button.setAllCaps(false); button.setCornerRadius(dp(28));
        return button;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void spacer(int height) { View space = new View(this); content.addView(space, new LinearLayout.LayoutParams(1, dp(height))); }
    @Override protected void onDestroy() { closed = true; generation++; if (systemUiCheck != null) systemUiCheck.cancel(); if (previewCheck != null) previewCheck.cancel(); worker.shutdownNow(); super.onDestroy(); }
}
