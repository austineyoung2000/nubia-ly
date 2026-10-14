package com.elitedarkkaiser.nubialy;

import android.os.Build;
import android.os.Bundle;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Saved color is a preference, never a saved active session. */
public final class ClockEditorActivity extends AppCompatActivity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private SystemUiGuardClient client;
    private LinearLayout content;
    private TextView swatch, selection, saveStatus, previewStatus;
    private MaterialButton enable, restore, check;
    private ChipGroup presets;
    private Slider sizeControl;
    private com.google.android.material.materialswitch.MaterialSwitch weightControl;
    private MaterialButton stockSizeControl;
    private com.google.android.material.materialswitch.MaterialSwitch secondsControl;
    private int selected, sizePercent;
    private boolean bold, seconds;
    private long saveGeneration;
    private boolean closed;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!CompatibilityPolicy.isNx809j(Build.MODEL, Build.DEVICE) || Build.VERSION.SDK_INT != 36) { finish(); return; }
        selected = ClockColorPreferences.read(this);
        sizePercent = ClockColorPreferences.size(this); bold = ClockColorPreferences.bold(this); seconds = ClockColorPreferences.seconds(this);
        if (state != null && ClockColorPolicy.valid(state.getInt("selection", 0))) selected = state.getInt("selection");
        client = new SystemUiGuardClient(this);
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(20), dp(24), dp(24)); scroll.addView(content); setContentView(scroll);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(scroll, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        MaterialButton back = button("Back"); back.setOnClickListener(v -> finish()); content.addView(back);
        content.addView(text("Status bar · Clock", 28, true));
        content.addView(text("Choose your clock color. Choose a color, then Enable to keep it on your status bar.", 15, false));
        LinearLayout colors = card("Clock color");
        swatch = text("12:34", 44, true); colors.addView(swatch);
        selection = text("", 16, true); colors.addView(selection);
        presets = new ChipGroup(this); presets.setSingleSelection(true); colors.addView(presets);
        for (ClockColorPolicy.Preset preset : ClockColorPolicy.PRESETS) {
            Chip chip = new Chip(this); chip.setId(View.generateViewId()); chip.setText(preset.name()); chip.setCheckable(true);
            chip.setTag(preset.color());
            GradientDrawable dot = new GradientDrawable(); dot.setShape(GradientDrawable.OVAL); dot.setColor(preset.color());
            dot.setSize(dp(20), dp(20)); chip.setChipIcon(dot); chip.setChipIconVisible(true);
            chip.setOnClickListener(v -> choose(preset.color())); presets.addView(chip);
        }
        MaterialButton custom = button("Custom color"); colors.addView(custom); custom.setOnClickListener(v -> customColor());
        saveStatus = text("Saved selection loaded.", 14, false); colors.addView(saveStatus);
        LinearLayout typography = card("Clock style");
        TextView sizeLabel = text("", 15, true); typography.addView(sizeLabel);
        Slider size = new Slider(this); sizeControl = size; size.setValueFrom(80); size.setValueTo(120); size.setStepSize(5);
        size.setValue(sizePercent == 0 ? 100 : sizePercent); typography.addView(size);
        Runnable label = () -> sizeLabel.setText(sizePercent == 0 ? "Size: Stock" : "Size: " + sizePercent + "% of stock"); label.run();
        size.addOnChangeListener((slider, value, fromUser) -> { if (fromUser) { sizePercent = (int)value; label.run(); saveStyle(); } });
        MaterialButton stockSize = button("Use stock size"); stockSizeControl = stockSize; typography.addView(stockSize);
        stockSize.setOnClickListener(v -> { sizePercent = 0; size.setValue(100); label.run(); saveStyle(); });
        com.google.android.material.materialswitch.MaterialSwitch weight = new com.google.android.material.materialswitch.MaterialSwitch(this);
        weightControl = weight; weight.setText("Bold clock"); weight.setChecked(bold); typography.addView(weight);
        weight.setOnCheckedChangeListener((v, checked) -> { bold = checked; saveStyle(); });
        secondsControl = new com.google.android.material.materialswitch.MaterialSwitch(this);
        secondsControl.setText("Show seconds"); secondsControl.setChecked(seconds); typography.addView(secondsControl);
        secondsControl.setOnCheckedChangeListener((v, checked) -> {
            seconds = checked; final boolean requested = checked;
            worker.execute(() -> { boolean saved = ClockColorPreferences.saveSeconds(getApplicationContext(), requested);
                runOnUiThread(() -> { if (alive()) saveStatus.setText(saved ? "Seconds selection saved. Press Enable to apply it." : "Seconds selection could not be saved."); }); });
        });
        typography.addView(text("Seconds update using the stock clock while awake. They can use more power and status-bar space.", 14, false));
        typography.addView(text("Size follows your stock font scaling. Press Enable to apply your saved size and weight with the selected color.", 14, false));
        LinearLayout controls = card("Clock customization");
        controls.addView(text("Enable your selected color until you restore stock. Press Enable again to apply a different selection.", 15, false));
        controls.addView(text("Requires root and the updated recovery guard. Your enabled color resumes after wake or reboot. The guard Action independently turns it off.", 14, false));
        previewStatus = text(getSharedPreferences("recovery", MODE_PRIVATE).getBoolean("restart_recovery_stopped", false)
            ? "Clock color was stopped after repeated System UI restarts. Enable again only after checking recovery."
            : "Check clock style to read the current state. Restore stock turns off enabled customization.", 14, false); controls.addView(previewStatus);
        enable = button("Enable clock style"); controls.addView(enable);
        check = button("Check clock style"); controls.addView(check);
        restore = button("Restore stock clock"); controls.addView(restore);
        enable.setOnClickListener(v -> enableColor());
        check.setOnClickListener(v -> request("status"));
        restore.setOnClickListener(v -> {
            long stopDeadline = android.os.SystemClock.elapsedRealtime() + 8000;
            enabled(false);
            worker.execute(() -> {
                boolean saved = RecoveryStore.stop(getApplicationContext());
                boolean rootStopped = RootGuardCommand.stopClock(stopDeadline);
                runOnUiThread(() -> {
                    if (!alive()) return;
                    request("stop");
                    if (!saved || !rootStopped) previewStatus.setText("Persistent disarm could not be saved. Restoration requested; check preview.");
                });
            });
        });
        redraw();
        swatch.setTextSize(44 * (sizePercent == 0 ? 1f : sizePercent / 100f));
        swatch.setTypeface(null, bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }
    private void saveStyle() {
        final int requestedSize = sizePercent; final boolean requestedBold = bold, requestedSeconds = seconds;
        worker.execute(() -> { boolean saved = ClockColorPreferences.saveStyle(getApplicationContext(), requestedSize, requestedBold);
            runOnUiThread(() -> { if (alive()) saveStatus.setText(saved ? "Style saved. Press Enable to apply it." : "Style could not be saved."); }); });
        swatch.setTextSize(44 * (sizePercent == 0 ? 1f : sizePercent / 100f));
        swatch.setTypeface(null, bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }
    private void choose(int color) {
        if (!ClockColorPolicy.valid(color)) return;
        selected = color; redraw(); saveStatus.setText("Saving selection…");
        long ticket = ++saveGeneration;
        worker.execute(() -> {
            boolean saved = ClockColorPreferences.save(getApplicationContext(), color);
            runOnUiThread(() -> {
                if (alive() && ticket == saveGeneration) saveStatus.setText(saved
                    ? "Selection saved. Press Enable to apply it."
                    : "Selected color could not be saved. You can still enable it.");
            });
        });
    }
    private void redraw() {
        swatch.setTextColor(selected); selection.setText(ClockColorPolicy.hex(selected));
        for (int i=0; i<presets.getChildCount(); i++) {
            Chip chip = (Chip)presets.getChildAt(i); chip.setChecked(((Integer)chip.getTag()) == selected);
        }
    }
    private void enableColor() {
        enabled(false); final int requested = selected, requestedSize = sizePercent; final boolean requestedBold = bold, requestedSeconds = seconds;
        long deadline = android.os.SystemClock.elapsedRealtime() + 8000;
        previewStatus.setText("Enabling clock color… Grant root access if asked.");
        worker.execute(() -> {
            boolean allowed = ClockColorPreferences.saveSeconds(getApplicationContext(), requestedSeconds) && ClockColorPreferences.saveStyle(getApplicationContext(), requestedSize, requestedBold) && RootGuardCommand.enableClock(deadline) && RecoveryStore.allowEnabled(getApplicationContext());
            runOnUiThread(() -> {
                if (!alive()) return;
                if (!allowed) { enabled(true); previewStatus.setText("Could not enable. Check root access, updated guard module and Vector activation."); return; }
                client.enableColor(requested, result -> { if (alive()) { enabled(true); previewStatus.setText(result); } });
            });
        });
    }
    private void request(String operation) {
        enabled(false);
        client.preview(operation, result -> { if (alive()) { enabled(true); previewStatus.setText(result); } });
    }
    private void enabled(boolean value) { check.setEnabled(value); restore.setEnabled(value); enable.setEnabled(value); sizeControl.setEnabled(value); weightControl.setEnabled(value); stockSizeControl.setEnabled(value); secondsControl.setEnabled(value); }
    private boolean alive() { return !closed && !isFinishing() && !isDestroyed(); }
    private void customColor() {
        LinearLayout picker = new LinearLayout(this); picker.setOrientation(LinearLayout.VERTICAL); picker.setPadding(dp(24), dp(8), dp(24), dp(8));
        TextView sample = text("12:34", 36, true); picker.addView(sample);
        TextInputLayout field = new TextInputLayout(this); field.setHint("Hex color · RRGGBB");
        TextInputEditText hex = new TextInputEditText(this); hex.setSingleLine(true); hex.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.addView(hex); picker.addView(field);
        Slider[] sliders = new Slider[3]; String[] names = {"Red", "Green", "Blue"};
        int[] draft = {selected}; boolean[] updating = {false};
        for (int i=0;i<3;i++) {
            picker.addView(text(names[i],14,true)); Slider slider = new Slider(this); sliders[i]=slider;
            slider.setValueFrom(0); slider.setValueTo(255); slider.setStepSize(1); picker.addView(slider);
        }
        Runnable update = () -> {
            updating[0]=true; sample.setTextColor(draft[0]); hex.setText(ClockColorPolicy.hex(draft[0]));
            for(int i=0;i<3;i++) sliders[i].setValue((draft[0] >> (16-i*8)) & 255);
            field.setError(null); updating[0]=false;
        };
        for(int i=0;i<3;i++) {
            sliders[i].addOnChangeListener((slider,value,fromUser) -> {
                if (!fromUser || updating[0]) return;
                draft[0]=0xff000000 | ((int)sliders[0].getValue()<<16) | ((int)sliders[1].getValue()<<8) | (int)sliders[2].getValue(); update.run();
            });
        }
        hex.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) {
                if(updating[0])return;
                Integer parsed=ClockColorPolicy.parse(s.toString());
                field.setError(parsed==null ? "Enter six hex digits, such as C0A8FF." : null);
                if(parsed!=null) {
                    draft[0]=parsed; updating[0]=true; sample.setTextColor(parsed);
                    for(int i=0;i<3;i++)sliders[i].setValue((parsed>>(16-i*8))&255); updating[0]=false;
                }
            }
            public void afterTextChanged(Editable s) {}
        });
        update.run();
        ScrollView pickerScroll = new ScrollView(this); pickerScroll.addView(picker);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this).setTitle("Custom clock color")
            .setView(pickerScroll).setNegativeButton("Cancel", null).setPositiveButton("Use color", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(-1).setOnClickListener(v -> {
            Integer parsed=ClockColorPolicy.parse(hex.getText()==null ? null : hex.getText().toString());
            if(parsed==null) {field.setError("Enter six hex digits.");return;}
            choose(parsed); dialog.dismiss();
        })); dialog.show();
    }
    private LinearLayout card(String title) {
        MaterialCardView panel = new MaterialCardView(this); panel.setRadius(dp(24)); panel.setStrokeWidth(dp(1)); panel.setStrokeColor(0xff373243);
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),dp(20),dp(20),dp(20)); panel.addView(box);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(18);content.addView(panel,params);box.addView(text(title,22,true));return box;
    }
    private TextView text(String value,int size,boolean bold) {
        TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setPadding(0,dp(6),0,dp(8));if(bold)view.setTypeface(null,android.graphics.Typeface.BOLD);return view;
    }
    private MaterialButton button(String title) { MaterialButton button=new MaterialButton(this);button.setText(title);button.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));return button; }
    private int dp(int n) {return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override protected void onSaveInstanceState(Bundle state) { state.putInt("selection", selected);super.onSaveInstanceState(state); }
    @Override protected void onDestroy() {closed=true;if(client!=null)client.cancel();worker.shutdown();super.onDestroy();}
}
