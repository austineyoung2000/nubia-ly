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
    private MaterialButton preview, restore, check;
    private ChipGroup presets;
    private int selected;
    private long saveGeneration;
    private boolean closed;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!CompatibilityPolicy.isNx809j(Build.MODEL, Build.DEVICE) || Build.VERSION.SDK_INT != 36) { finish(); return; }
        selected = ClockColorPreferences.read(this);
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
        content.addView(text("Choose your clock color. Saving a selection does not change your status bar.", 15, false));
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
        LinearLayout controls = card("Preview & restore");
        controls.addView(text("Preview the selected color for up to 30 seconds. Changing the selection does not change an active preview.", 15, false));
        controls.addView(text("Requires your clock guard permit. Preview also ends when the screen turns off. Nothing applies automatically when you reopen or reboot.", 14, false));
        previewStatus = text("No preview checked. Saved colors never apply automatically.", 14, false); controls.addView(previewStatus);
        preview = button("Preview selected color"); controls.addView(preview);
        check = button("Check preview"); controls.addView(check);
        restore = button("Restore stock color"); controls.addView(restore);
        preview.setOnClickListener(v -> start());
        check.setOnClickListener(v -> request("status"));
        restore.setOnClickListener(v -> {
            enabled(false);
            worker.execute(() -> {
                boolean saved = RecoveryStore.stop(getApplicationContext());
                runOnUiThread(() -> {
                    if (!alive()) return;
                    request("stop");
                    if (!saved) previewStatus.setText("Persistent disarm could not be saved. Restoration requested; check preview.");
                });
            });
        });
        redraw();
    }
    private void choose(int color) {
        if (!ClockColorPolicy.valid(color)) return;
        selected = color; redraw(); saveStatus.setText("Saving selection…");
        long ticket = ++saveGeneration;
        worker.execute(() -> {
            boolean saved = ClockColorPreferences.save(getApplicationContext(), color);
            runOnUiThread(() -> {
                if (alive() && ticket == saveGeneration) saveStatus.setText(saved
                    ? "Selection saved. Status bar unchanged until you preview."
                    : "Selected color could not be saved. You can still preview it.");
            });
        });
    }
    private void redraw() {
        swatch.setTextColor(selected); selection.setText(ClockColorPolicy.hex(selected));
        for (int i=0; i<presets.getChildCount(); i++) {
            Chip chip = (Chip)presets.getChildAt(i); chip.setChecked(((Integer)chip.getTag()) == selected);
        }
    }
    private void start() {
        enabled(false); final int requested = selected;
        previewStatus.setText("Checking clock permit…");
        worker.execute(() -> {
            boolean allowed = RecoveryStore.allowClock(getApplicationContext());
            runOnUiThread(() -> {
                if (!alive()) return;
                if (!allowed) { enabled(true); previewStatus.setText("Preview blocked. Publish a fresh clock permit and verify Vector activation."); return; }
                client.previewColor(requested, result -> { if (alive()) { enabled(true); previewStatus.setText(result); } });
            });
        });
    }
    private void request(String operation) {
        enabled(false);
        client.preview(operation, result -> { if (alive()) { enabled(true); previewStatus.setText(result); } });
    }
    private void enabled(boolean value) { preview.setEnabled(value); check.setEnabled(value); restore.setEnabled(value); }
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
