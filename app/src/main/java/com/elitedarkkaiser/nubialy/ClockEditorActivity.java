package com.elitedarkkaiser.nubialy;

import android.os.Build;
import android.os.Bundle;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Gravity;
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
    private FingerprintServiceClient fingerprintClient;
    private MaterialButton fingerprintPicker;
    private android.widget.ImageView fingerprintPreview;
    private static final int PICK_FINGERPRINT=7402,PICK_ANIMATION=7403,PICK_CLOCK_FONT=7404;
    private View batteryControlPreview;
    private int qsStyle,brightnessStyle,volumeStyle,notificationStyle,wifiStyle,signalStyle,powerStyle,notificationAppearance,notificationBehavior,chargingInfo,batteryStyle,batteryPercent,batteryCharge,notificationCount,notificationLimit,qsColumns,qsRows;
    private int fingerprintAnimation;private MaterialButton animationPicker;
    private LinearLayout content;
    private EditorPages pages;
    private final java.util.List<EditorPreview> editorPreviews=new java.util.ArrayList<>();
    private TextView swatch, selection, saveStatus, previewStatus;
    private MaterialButton enable, restore, check;
    private ChipGroup presets;
    private Slider sizeControl;
    private com.google.android.material.materialswitch.MaterialSwitch weightControl;
    private MaterialButton stockSizeControl;
    private com.google.android.material.materialswitch.MaterialSwitch secondsControl;
    private int selected, sizePercent;
    private boolean bold, seconds, italic;
    private int spacing, fontFamily;
    private int statusMask, systemScale, notificationScale, batteryScale,systemOpacity,notificationOpacity,batteryOpacity,iconColor,dateMode,systemTransform,notificationTransform,batteryTransform,fanAppearance,pumpAppearance,refreshAppearance;
    private final int[] surfaceAppearance=new int[4],surfacePosition=new int[4];
    private TextView lockFontPreview;
    private MaterialButton lockFontPicker;
    private int surfaceHidden,tileShape,largeTileShape,widgetsHidden,fingerprintStyle,fingerprintHidden,lockClockFont;
    private final java.util.Set<String> hiddenSlots=new java.util.TreeSet<>();
    private LinearLayout iconSlots;
    private ChipGroup iconPresets;
    private TextView iconSelection;
    private MaterialButton inventoryButton,resetIcons;
    private final java.util.List<View> statusControls = new java.util.ArrayList<>();
    private boolean hidden;
    private ChipGroup fontControl;
    private com.google.android.material.materialswitch.MaterialSwitch hideControl;
    private com.google.android.material.materialswitch.MaterialSwitch italicControl;
    private Slider spacingControl;
    private long saveGeneration;
    private boolean closed;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!CompatibilityPolicy.isNx809j(Build.MODEL, Build.DEVICE) || Build.VERSION.SDK_INT != 36) { finish(); return; }
        selected = ClockColorPreferences.read(this);
        sizePercent = ClockColorPreferences.size(this); bold = ClockColorPreferences.bold(this); seconds = ClockColorPreferences.seconds(this); italic = ClockColorPreferences.italic(this); spacing = ClockColorPreferences.spacing(this); fontFamily = ClockColorPreferences.family(this); hidden = ClockColorPreferences.hidden(this);
        StatusBarStyle bar = StatusBarPreferences.read(this); statusMask = bar.mask(); systemScale = bar.systemScale(); notificationScale = bar.notificationScale(); batteryScale = bar.batteryScale();systemOpacity=bar.systemOpacity();notificationOpacity=bar.notificationOpacity();batteryOpacity=bar.batteryOpacity();systemTransform=bar.systemTransform();notificationTransform=bar.notificationTransform();batteryTransform=bar.batteryTransform();fanAppearance=bar.fanAppearance();pumpAppearance=bar.pumpAppearance();refreshAppearance=bar.refreshAppearance();hiddenSlots.addAll(bar.hiddenSlots());iconColor=StatusBarPreferences.color(this);dateMode=StatusBarPreferences.date(this);
        qsColumns=bar.surfaces().qsColumns();qsRows=bar.surfaces().qsRows();batteryPercent=bar.surfaces().batteryPercent();batteryCharge=bar.surfaces().batteryCharge();notificationCount=bar.surfaces().notificationCount();notificationLimit=bar.surfaces().notificationLimit();batteryStyle=bar.surfaces().batteryStyle();wifiStyle=bar.surfaces().wifiStyle();signalStyle=bar.surfaces().signalStyle();powerStyle=bar.surfaces().powerStyle();notificationAppearance=bar.surfaces().notificationAppearance();notificationBehavior=bar.surfaces().notificationBehavior();chargingInfo=bar.surfaces().chargingInfo();notificationStyle=bar.surfaces().notificationStyle();volumeStyle=bar.surfaces().volumeStyle();brightnessStyle=bar.surfaces().brightnessStyle();qsStyle=bar.surfaces().qsStyle();widgetsHidden=bar.surfaces().widgetsHidden();fingerprintAnimation=bar.surfaces().fingerprintAnimation();fingerprintStyle=bar.surfaces().fingerprintStyle();fingerprintHidden=bar.surfaces().fingerprintHidden();lockClockFont=bar.surfaces().clockFont();tileShape=bar.surfaces().tileShape();largeTileShape=bar.surfaces().largeTileShape();surfaceHidden=bar.surfaces().hidden();for(int i=0;i<4;i++){surfaceAppearance[i]=bar.surfaces().appearance(i);surfacePosition[i]=bar.surfaces().position(i);}
        if (state != null && ClockColorPolicy.valid(state.getInt("selection", 0))) selected = state.getInt("selection");
        client = new SystemUiGuardClient(this);fingerprintClient=new FingerprintServiceClient(this);
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(12), dp(20), dp(24)); scroll.addView(content); setContentView(scroll);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(scroll, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        var editorToolbar=PersonalizationUi.toolbar(this,"Personalizations");content.addView(editorToolbar);
        pages=new EditorPages(this,editorToolbar,scroll);
        editorOverview("lock_screen","Lock screen",new String[][]{{"clock","Clock","Size, opacity & position"},{"date","Date","Visibility & placement"},{"fonts","Clock font styles","Preview your typography"},{"fingerprint","Fingerprint","Icons & animations"},{"widgets","Widgets","Weather, alarm & battery"},{"charging","Charging information","Battery voltage, current & temperature"}});
        editorOverview("quick_settings","Quick Settings",new String[][]{{"grid","Rows & columns","Small tiles per page"},{"style","Tile style","Outline, depth & layered effects"},{"brightness","Brightness slider","Outline, depth & a slim track"},{"tiles","Tile shapes","Large & small tiles"},{"labels","Label styles","Main & secondary text"}});
        editorOverview("notifications","Notifications",new String[][]{{"styles","Notification styles","Card backgrounds & layers"},{"appearance","Appearance controls","Background opacity & rounded corners"},{"behavior","Notification controls","Heads-up, history & app settings"},{"icons","Counts & icon limits","Status-bar overflow & icon slots"}});
        editorOverview("power_menu","Power menu",new String[][]{{"styles","Power-menu styles","Duoline, CyberPunk, Fluid & layers"}});
        editorOverview("volume","Volume",new String[][]{{"styles","Volume styles","Outline, gradients & layered effects"}});
        editorOverview("status_bar","Status bar",new String[][]{{"typography","Clock style","Font, size & seconds"},{"date","Date format","Format, position & letter case"},{"color","Clock color","Presets & custom colors"},{"icons","Icons & battery","Colors, size & placement"},{"battery","Battery styles & controls","Design, percentage, charging & size"},{"connectivity","Wi-Fi & mobile signal","Choose independent icon styles"},{"inventory","Individual icons","Visibility & vendor indicators"}});
        LinearLayout colors = editorPanel("status_bar","color","Clock color");
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
        LinearLayout icons = editorPanel("status_bar","icons","Icons & battery");
        icons.addView(text("Mobile service icon controls style the compatible calling-service indicator, such as VoLTE. Other compatible system indicators can share these settings. App notification icons are separate. Some Wi-Fi and signal icons keep stock appearance. Battery keeps stock charge and warning colors.",14,false));
        LinearLayout iconTints=detailGroup(icons,"Icon color","Mobile service indicator");
        iconSelection=text("",16,true);iconTints.addView(iconSelection);iconPresets=new ChipGroup(this);iconPresets.setSingleSelection(true);iconTints.addView(iconPresets);
        for(ClockColorPolicy.Preset preset:ClockColorPolicy.PRESETS){
            Chip chip=new Chip(this);chip.setId(View.generateViewId());chip.setText(preset.name());chip.setTag(preset.color());chip.setCheckable(true);
            GradientDrawable dot=new GradientDrawable();dot.setShape(GradientDrawable.OVAL);dot.setColor(preset.color());dot.setSize(dp(20),dp(20));chip.setChipIcon(dot);chip.setChipIconVisible(true);
            chip.setOnClickListener(v->chooseIcon(preset.color()));iconPresets.addView(chip);statusControls.add(chip);
        }
        MaterialButton iconCustom=button("Custom mobile service icon color");iconTints.addView(iconCustom);statusControls.add(iconCustom);iconCustom.setOnClickListener(v->customColor(true));redrawIcon();
        statusSwitch(iconTints,"Tint mobile service icon",StatusBarStyle.TINT);
        LinearLayout visibility=detailGroup(icons,"Visibility","Mobile service, notifications & battery");
        statusSwitch(visibility,"Hide mobile service icon",StatusBarStyle.HIDE_SYSTEM);
        statusSwitch(visibility,"Hide notification icons",StatusBarStyle.HIDE_NOTIFICATIONS);
        statusSwitch(visibility,"Hide battery",StatusBarStyle.HIDE_BATTERY);
        LinearLayout sizes=detailGroup(icons,"Size & opacity","Scale compatible icons independently");
        statusScale(sizes,"Mobile service icon size",systemScale,n->systemScale=n);
        statusScale(sizes,"Notification icon size",notificationScale,n->notificationScale=n);
        MaterialButton batterySizing=button("Battery size & controls");sizes.addView(batterySizing);batterySizing.setOnClickListener(v->pages.show("status_bar","battery"));
        statusOpacity(sizes,"Mobile service icon opacity",systemOpacity,n->systemOpacity=n);
        statusOpacity(sizes,"Notification icon opacity",notificationOpacity,n->notificationOpacity=n);
        statusOpacity(sizes,"Battery opacity",batteryOpacity,n->batteryOpacity=n);
        LinearLayout geometry=detailGroup(icons,"Position & rotation","Offsets relative to stock");
        geometry.addView(text("Icon position & rotation",16,true));
        geometry.addView(text("Offsets move compatible icons relative to stock by up to 8 dp. Rotation adds up to 20° around the stock pivot. Zero keeps stock. Large combined values may clip or overlap; these controls do not reserve more layout space.",14,false));
        transformControls(geometry,"Mobile service icon",0);transformControls(geometry,"Notification icons",1);transformControls(geometry,"Battery",2);
        LinearLayout inventory=editorPanel("status_bar","inventory","Individual icons");
        inventory.addView(text("Choose which compatible system indicators appear in your status bar.",14,false));
        inventoryButton=button("Load individual system status icons");inventory.addView(inventoryButton);inventoryButton.setOnClickListener(v->{enabled(false);client.inventory(slots->{if(alive())showSlots(slots);},result->{if(alive()){enabled(true);saveStatus.setText(result);}});});
        iconSlots=new LinearLayout(this);iconSlots.setOrientation(LinearLayout.VERTICAL);inventory.addView(iconSlots);showSlots(java.util.List.of());
        resetIcons=button("Reset icons & battery selections");icons.addView(resetIcons);resetIcons.setOnClickListener(v->{
            enabled(false);worker.execute(()->{boolean saved=StatusBarPreferences.save(getApplicationContext(),StatusBarStyle.STOCK.withSurfaces(currentSurfaces().withBatteryStyle(0).withBatteryControls(0,0)));runOnUiThread(()->{if(alive()){if(saved){finish();startActivity(new android.content.Intent(this,ClockEditorActivity.class));}else {enabled(true);saveStatus.setText("Reset could not be saved.");}}});});
        });
        icons.addView(text("Reset clears draft icon/battery controls; press Enable afterward to apply stock icon/battery appearance while keeping your clock choices.",14,false));
        icons.addView(text("100% uses stock size. These controls apply to recognized top-status-bar views; other icons may keep their stock appearance. Press Enable to apply the entire saved style.",14,false));
        LinearLayout batteries=editorPanel("status_bar","battery","Battery styles & controls");batteryControls(batteries);batteryStyleControls(batteries);
        connectivityControls(editorPanel("status_bar","connectivity","Wi-Fi & mobile signal"));
        visualStyleChoices(editorPanel("power_menu","styles","Power-menu styles"),"power");
        notificationAppearanceControls(editorPanel("notifications","appearance","Appearance controls"));
        notificationIconControls(editorPanel("notifications","icons","Counts & icon limits"));
        notificationBehaviorControls(editorPanel("notifications","behavior","Notification controls"));
        chargingControls(editorPanel("lock_screen","charging","Charging information"));
        LinearLayout notificationPanel=editorPanel("notifications","styles","Notification styles");notificationStyleControls(notificationPanel);
        LinearLayout volumePanel=editorPanel("volume","styles","Volume styles");volumeStyleControls(volumePanel);
        gridControls(editorPanel("quick_settings","grid","Rows & columns"));
        LinearLayout stylePanel=editorPanel("quick_settings","style","Tile style");qsStyleControls(stylePanel);
        LinearLayout brightnessPanel=editorPanel("quick_settings","brightness","Brightness slider");brightnessStyleControls(brightnessPanel);
        LinearLayout surfaces=editorPanel("quick_settings","tiles","Tile shapes");
        surfaces.addView(text("Choose independent shapes for large and small tiles. Label appearance keeps tile actions unchanged. Press Enable to apply saved choices.",14,false));
        shapeControls(surfaces,"Large QS tiles",true);
        shapeControls(surfaces,"Small QS tiles",false);
        surfaces.addView(text("Large QS tiles are the four wide tiles at the top. Small QS tiles are the tiles below brightness. Each group has its own shape. Press Enable to apply.",14,false));
        LinearLayout labels=editorPanel("quick_settings","labels","Label styles");
        surfaceControls(labels,"Main labels",0);surfaceControls(labels,"Secondary labels",1);
        LinearLayout clock=editorPanel("lock_screen","clock","Clock appearance");surfaceControls(clock,"Clock",2);
        LinearLayout date=editorPanel("lock_screen","date","Date appearance");surfaceControls(date,"Date",3);
        LinearLayout lockscreen=editorPanel("lock_screen","widgets","Widgets");lockScreenExtras(lockscreen);
        if(getIntent().getBooleanExtra("surfaces",false))surfaces.post(()->scroll.smoothScrollTo(0,((View)surfaces.getParent()).getTop()));
        LinearLayout typography = editorPanel("status_bar","typography","Clock style");
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
        typography.addView(text("Clock font", 15, true));
        fontControl = new ChipGroup(this); fontControl.setSingleSelection(true); typography.addView(fontControl);
        String[] fonts = {"Stock", "Sans serif", "Serif", "Monospace"};
        for (int i = 0; i < fonts.length; i++) {
            final int font = i; Chip chip = new Chip(this); chip.setId(View.generateViewId()); chip.setText(fonts[i]); chip.setTag(i); chip.setCheckable(true); chip.setChecked(fontFamily == i);
            chip.setOnClickListener(v -> { fontFamily = font; for(int n=0;n<fontControl.getChildCount();n++){Chip c=(Chip)fontControl.getChildAt(n);c.setChecked((Integer)c.getTag()==font);} saveExtras(); }); fontControl.addView(chip);
        }
        hideControl = new com.google.android.material.materialswitch.MaterialSwitch(this); hideControl.setText("Hide status-bar clock"); hideControl.setChecked(hidden); typography.addView(hideControl);
        hideControl.setOnCheckedChangeListener((v, checked) -> { hidden = checked;refreshEditorPreviews(); final boolean requested = checked;
            worker.execute(() -> { boolean saved = ClockColorPreferences.saveHidden(getApplicationContext(), requested); runOnUiThread(() -> { if(alive()) saveStatus.setText(saved ? "Visibility saved. Press Enable to apply it." : "Visibility could not be saved."); }); }); });
        italicControl = new com.google.android.material.materialswitch.MaterialSwitch(this);
        italicControl.setText("Italic clock"); italicControl.setChecked(italic); typography.addView(italicControl);
        italicControl.setOnCheckedChangeListener((v, checked) -> { italic = checked; saveExtras(); });
        TextView spacingLabel = text("", 15, true); typography.addView(spacingLabel);
        spacingControl = new Slider(this); spacingControl.setValueFrom(-5); spacingControl.setValueTo(15); spacingControl.setStepSize(1);
        spacingControl.setValue(spacing); typography.addView(spacingControl);
        Runnable spacingText = () -> spacingLabel.setText(spacing == 0 ? "Letter spacing: Stock" : "Letter spacing: " + String.format(java.util.Locale.ROOT, "%+.2f em", spacing / 100f)); spacingText.run();
        spacingControl.addOnChangeListener((slider, value, fromUser) -> { if (fromUser) { spacing = (int)value; spacingText.run(); saveExtras(); } });
        typography.addView(text("Letter spacing adjusts the gaps between clock characters relative to stock. Zero uses stock spacing.", 14, false));
        secondsControl = new com.google.android.material.materialswitch.MaterialSwitch(this);
        secondsControl.setText("Show seconds"); secondsControl.setChecked(seconds); typography.addView(secondsControl);
        secondsControl.setOnCheckedChangeListener((v, checked) -> {
            seconds = checked; final boolean requested = checked;
            worker.execute(() -> { boolean saved = ClockColorPreferences.saveSeconds(getApplicationContext(), requested);
                runOnUiThread(() -> { if (alive()) saveStatus.setText(saved ? "Seconds selection saved. Press Enable to apply it." : "Seconds selection could not be saved."); }); });
        });
        typography.addView(text("Seconds update using the stock clock while awake. They can use more power and status-bar space.", 14, false));
        typography.addView(text("Size follows your stock font scaling. Press Enable to apply your saved size and weight with the selected color.", 14, false));
        LinearLayout dateFormats=editorPanel("status_bar","date","Date format");
        dateChoices(dateFormats,"Date format",ClockDatePolicy.LABELS,0);
        dateChoices(dateFormats,"Date position",ClockDatePolicy.POSITIONS,1);
        dateChoices(dateFormats,"Date separator",ClockDatePolicy.SEPARATOR_LABELS,2);
        dateChoices(dateFormats,"Date letter case",ClockDatePolicy.CASE_LABELS,3);
        dateFormats.addView(text("Date uses your stock locale and timezone. Position, separator and letter case affect only the added date; time keeps its stock format. Long dates add width. Press Enable to apply your saved choices.",14,false));
        String initial=state==null?getIntent().getStringExtra("section"):state.getString("editor_section");if(initial==null)initial=getIntent().getBooleanExtra("surfaces",false)?"quick_settings":"status_bar";
        pages.show(initial,state==null?"":state.getString("editor_page",""));
        LinearLayout controls = card("Apply your style");
        ((android.view.ViewGroup)saveStatus.getParent()).removeView(saveStatus);controls.addView(saveStatus);
        controls.addView(text("Enable applies saved choices across all personalization areas. Restore stock removes the applied style.", 15, false));
        controls.addView(text("Requires root and the updated recovery guard. Your enabled color resumes after wake or reboot. The guard Action independently turns it off.", 14, false));
        previewStatus = text(getSharedPreferences("recovery", MODE_PRIVATE).getBoolean("restart_recovery_stopped", false)
            ? "Clock color was stopped after repeated System UI restarts. Enable again only after checking recovery."
            : "Check customization to read the current state. Restore stock turns off enabled customization.", 14, false);
        MaterialButton details=button("Show technical details");controls.addView(details);previewStatus.setVisibility(View.GONE);controls.addView(previewStatus);details.setOnClickListener(v->{boolean show=previewStatus.getVisibility()!=View.VISIBLE;previewStatus.setVisibility(show?View.VISIBLE:View.GONE);details.setText(show?"Hide technical details":"Show technical details");});
        enable = button("Enable customization"); controls.addView(enable);
        check = button("Check customization"); controls.addView(check);
        restore = button("Restore stock appearance"); controls.addView(restore);
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
        swatch.setLetterSpacing(spacing / 100f);
        swatch.setTextSize(44 * (sizePercent == 0 ? 1f : sizePercent / 100f));
        swatch.setTypeface(android.graphics.Typeface.create(ClockStylePolicy.familyName(fontFamily), (bold ? android.graphics.Typeface.BOLD : 0) | (italic ? android.graphics.Typeface.ITALIC : 0)));
    }
    private StatusBarStyle currentStatusBar() { return new StatusBarStyle(statusMask,systemScale,notificationScale,batteryScale,systemOpacity,notificationOpacity,batteryOpacity,hiddenSlots,systemTransform,notificationTransform,batteryTransform,fanAppearance,pumpAppearance,refreshAppearance,currentSurfaces()); }
    private void redrawIcon(){iconSelection.setText("Mobile service icon color: "+ClockColorPolicy.hex(iconColor));iconSelection.setTextColor(iconColor);for(int n=0;n<iconPresets.getChildCount();n++){Chip c=(Chip)iconPresets.getChildAt(n);c.setChecked((Integer)c.getTag()==iconColor);}}
    private void chooseIcon(int color){if(!ClockColorPolicy.valid(color))return;iconColor=color;redrawIcon();refreshEditorPreviews();worker.execute(()->{boolean saved=StatusBarPreferences.saveColor(getApplicationContext(),color);runOnUiThread(()->{if(alive())saveStatus.setText(saved?"Icon color saved. Press Enable to apply it.":"Icon color could not be saved.");});});}
    private void showSlots(java.util.List<String> live){
        for(int n=0;n<iconSlots.getChildCount();n++)removeSlotControls(iconSlots.getChildAt(n));iconSlots.removeAllViews();java.util.Set<String> values=new java.util.TreeSet<>(live);values.addAll(hiddenSlots);
        indicatorHide("Hide cooling fan icon",StatusBarStyle.HIDE_FAN);
        indicatorHide("Hide cooling pump icon",StatusBarStyle.HIDE_PUMP);
        indicatorHide("Hide refresh-rate icon",StatusBarStyle.HIDE_REFRESH);
        if(values.isEmpty())iconSlots.addView(text("Load the live stock icon list to choose additional individual icons to hide.",14,false));
        for(String slot:values){var checkbox=new com.google.android.material.checkbox.MaterialCheckBox(this);checkbox.setText("Hide "+(slot.equals("alarm_clock")?"alarm indicator":slot.replace('_',' ')));checkbox.setChecked(hiddenSlots.contains(slot));iconSlots.addView(checkbox);statusControls.add(checkbox);
            checkbox.setOnCheckedChangeListener((v,checked)->{if(checked){if(hiddenSlots.size()>=32&&!hiddenSlots.contains(slot)){checkbox.setChecked(false);saveStatus.setText("Choose at most 32 icon types.");return;}hiddenSlots.add(slot);}else hiddenSlots.remove(slot);saveStatusBar();});
        }
        MaterialButton appearance=button("Fan, pump & refresh-rate appearance");iconSlots.addView(appearance);statusControls.add(appearance);
        LinearLayout details=new LinearLayout(this);details.setOrientation(LinearLayout.VERTICAL);details.setVisibility(View.GONE);iconSlots.addView(details);
        appearance.setOnClickListener(v->{boolean open=details.getVisibility()!=View.VISIBLE;details.setVisibility(open?View.VISIBLE:View.GONE);appearance.setText(open?"Close indicator appearance":"Fan, pump & refresh-rate appearance");});
        details.addView(text("Indicator size and opacity follow stock scaling and animation. Hidden indicators stay hidden. These controls only change appearance; cooling and refresh-rate operation stay unchanged.",14,false));
        indicatorAppearance(details,"Cooling fan",0);indicatorAppearance(details,"Cooling pump",1);indicatorAppearance(details,"Refresh rate",2);
    }
    private void statusOpacity(LinearLayout box,String title,int initial,java.util.function.IntConsumer selection){
        TextView label=text(title+": "+initial+"%",15,true);box.addView(label);Slider slider=new Slider(this);slider.setValueFrom(25);slider.setValueTo(100);slider.setStepSize(5);slider.setValue(initial);box.addView(slider);statusControls.add(slider);
        slider.addOnChangeListener((v,value,user)->{if(user){selection.accept((int)value);label.setText(title+": "+(int)value+"%");saveStatusBar();}});
    }
    private void saveStatusBar() {
        refreshEditorPreviews();
        final StatusBarStyle value=currentStatusBar();
        worker.execute(()->{boolean saved=StatusBarPreferences.save(getApplicationContext(),value);runOnUiThread(()->{if(alive())saveStatus.setText(saved?"Style choices saved. Press Enable to apply.":"Status-bar selections could not be saved.");});});
    }
    private void statusSwitch(LinearLayout box,String title,int flag) {
        var toggle=new com.google.android.material.materialswitch.MaterialSwitch(this);toggle.setText(title);toggle.setChecked((statusMask&flag)!=0);box.addView(toggle);statusControls.add(toggle);
        toggle.setOnCheckedChangeListener((v,checked)->{statusMask=checked?statusMask|flag:statusMask&~flag;saveStatusBar();});
    }
    private void statusScale(LinearLayout box,String title,int initial,java.util.function.IntConsumer selection) {
        TextView label=text(title+": "+initial+"%",15,true);box.addView(label);Slider slider=new Slider(this);slider.setValueFrom(80);slider.setValueTo(120);slider.setStepSize(5);slider.setValue(initial);box.addView(slider);statusControls.add(slider);
        slider.addOnChangeListener((v,value,user)->{if(user){selection.accept((int)value);label.setText(title+": "+(int)value+"%");saveStatusBar();}});
    }
    private void saveExtras() {
        refreshEditorPreviews();
        final boolean requestedItalic = italic; final int requestedSpacing = spacing, requestedFamily = fontFamily;
        worker.execute(() -> { boolean saved = ClockColorPreferences.saveExtras(getApplicationContext(), requestedItalic, requestedSpacing, requestedFamily);
            runOnUiThread(() -> { if (alive()) saveStatus.setText(saved ? "Style saved. Press Enable to apply it." : "Style could not be saved."); }); });
        swatch.setTypeface(android.graphics.Typeface.create(ClockStylePolicy.familyName(fontFamily), (bold ? android.graphics.Typeface.BOLD : 0) | (italic ? android.graphics.Typeface.ITALIC : 0)));
        swatch.setLetterSpacing(spacing / 100f);
    }
    private void saveStyle() {
        refreshEditorPreviews();
        final int requestedSize = sizePercent; final boolean requestedBold = bold, requestedSeconds = seconds;
        worker.execute(() -> { boolean saved = ClockColorPreferences.saveStyle(getApplicationContext(), requestedSize, requestedBold);
            runOnUiThread(() -> { if (alive()) saveStatus.setText(saved ? "Style saved. Press Enable to apply it." : "Style could not be saved."); }); });
        swatch.setTextSize(44 * (sizePercent == 0 ? 1f : sizePercent / 100f));
        swatch.setTypeface(android.graphics.Typeface.create(ClockStylePolicy.familyName(fontFamily), (bold ? android.graphics.Typeface.BOLD : 0) | (italic ? android.graphics.Typeface.ITALIC : 0)));
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
        refreshEditorPreviews();
        swatch.setTextColor(selected); selection.setText(ClockColorPolicy.hex(selected));
        for (int i=0; i<presets.getChildCount(); i++) {
            Chip chip = (Chip)presets.getChildAt(i); chip.setChecked(((Integer)chip.getTag()) == selected);
        }
    }
    private void enableColor() {
        enabled(false); final int requested = selected, requestedSize = sizePercent, requestedSpacing = spacing, requestedFamily = fontFamily; final boolean requestedBold = bold, requestedSeconds = seconds, requestedItalic = italic, requestedHidden = hidden;
        final StatusBarStyle requestedBar = currentStatusBar(); final int requestedIconColor=iconColor,requestedDate=dateMode;
        long deadline = android.os.SystemClock.elapsedRealtime() + 8000;
        previewStatus.setText("Enabling clock color… Grant root access if asked.");
        worker.execute(() -> {
            boolean allowed = StatusBarPreferences.saveColor(getApplicationContext(),requestedIconColor) && StatusBarPreferences.saveDate(getApplicationContext(),requestedDate) && StatusBarPreferences.save(getApplicationContext(), requestedBar) && ClockColorPreferences.saveHidden(getApplicationContext(), requestedHidden) && ClockColorPreferences.saveExtras(getApplicationContext(), requestedItalic, requestedSpacing, requestedFamily) && ClockColorPreferences.saveSeconds(getApplicationContext(), requestedSeconds) && ClockColorPreferences.saveStyle(getApplicationContext(), requestedSize, requestedBold) && RootGuardCommand.enableClock(deadline) && StatusBarPreferences.activate(getApplicationContext(), GuardReader.inspectEnabled(), requestedBar, requestedIconColor,requestedDate,new ClockSnapshot(requested,requestedSize,requestedBold,requestedSeconds,requestedItalic,requestedSpacing,requestedFamily,requestedHidden)) && RecoveryStore.allowEnabled(getApplicationContext());
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
    private void enabled(boolean value) { check.setEnabled(value); restore.setEnabled(value); enable.setEnabled(value); sizeControl.setEnabled(value); weightControl.setEnabled(value); stockSizeControl.setEnabled(value); secondsControl.setEnabled(value); italicControl.setEnabled(value); spacingControl.setEnabled(value); hideControl.setEnabled(value); inventoryButton.setEnabled(value);resetIcons.setEnabled(value); for(View control:statusControls) control.setEnabled(value); for (int n=0;n<fontControl.getChildCount();n++) fontControl.getChildAt(n).setEnabled(value); }
    private boolean alive() { return !closed && !isFinishing() && !isDestroyed(); }
    private void customColor(){customColor(false);}
    private void customColor(boolean forIcons) {
        LinearLayout picker = new LinearLayout(this); picker.setOrientation(LinearLayout.VERTICAL); picker.setPadding(dp(24), dp(8), dp(24), dp(8));
        TextView sample = text("12:34", 36, true); picker.addView(sample);
        TextInputLayout field = new TextInputLayout(this); field.setHint("Hex color · RRGGBB");
        TextInputEditText hex = new TextInputEditText(this); hex.setSingleLine(true); hex.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.addView(hex); picker.addView(field);
        Slider[] sliders = new Slider[3]; String[] names = {"Red", "Green", "Blue"};
        int[] draft = {forIcons?iconColor:selected}; boolean[] updating = {false};
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
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this).setTitle(forIcons?"Custom mobile service icon color":"Custom clock color")
            .setView(pickerScroll).setNegativeButton("Cancel", null).setPositiveButton("Use color", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(-1).setOnClickListener(v -> {
            Integer parsed=ClockColorPolicy.parse(hex.getText()==null ? null : hex.getText().toString());
            if(parsed==null) {field.setError("Enter six hex digits.");return;}
            if(forIcons)chooseIcon(parsed);else choose(parsed); dialog.dismiss();
        })); dialog.show();
    }
    private LinearLayout detailGroup(LinearLayout parent,String title,String summary){
        LinearLayout group=new LinearLayout(this);group.setOrientation(LinearLayout.VERTICAL);group.setPadding(dp(12),dp(8),dp(12),dp(8));
        GradientDrawable background=new GradientDrawable();background.setColor(getColor(R.color.surface_container_high));background.setCornerRadius(dp(22));group.setBackground(background);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(10);parent.addView(group,params);
        MaterialButton open=new MaterialButton(this);open.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));open.setTextColor(getColor(R.color.on_surface));open.setAllCaps(false);open.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);open.setText(title+"  ›");open.setContentDescription(title+". "+summary+". Expand settings");group.addView(open,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setVisibility(View.GONE);group.addView(body);open.setOnClickListener(v->{boolean show=body.getVisibility()!=View.VISIBLE;body.setVisibility(show?View.VISIBLE:View.GONE);open.setText(title+(show?"  ⌃":"  ›"));open.setContentDescription(title+". "+(show?"Collapse":"Expand")+" settings");});return body;
    }
    private LinearLayout editorPanel(String section,String page,String title){LinearLayout box=card(title);pages.register(section,page,title,(View)box.getParent());return box;}
    private void editorOverview(String section,String title,String[][] items){
        LinearLayout preview=editorPanel(section,"preview",title+" preview");
        EditorPreview artwork=new EditorPreview(this,section,()->new EditorPreview.Draft(currentStatusBar(),selected,iconColor,sizePercent,bold,italic,hidden,fontFamily,spacing/100f));
        editorPreviews.add(artwork);preview.addView(artwork,new LinearLayout.LayoutParams(-1,dp(section.equals("lock_screen")?280:section.equals("quick_settings")?180:section.equals("volume")?220:section.equals("notifications")?180:100)));
        preview.addView(text("Draft preview · sample content",12,false));
        LinearLayout overview=editorPanel(section,"", "Make it yours");
        for(String[] item:items){
            com.google.android.material.card.MaterialCardView row=new com.google.android.material.card.MaterialCardView(this);row.setRadius(dp(22));row.setCardElevation(0);row.setStrokeWidth(0);row.setCardBackgroundColor(getColor(R.color.surface_container_high));row.setClickable(true);row.setFocusable(true);row.setContentDescription(item[1]+". "+item[2]);row.setTag("editor-route:"+section+":"+item[0]);
            LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);line.setPadding(dp(18),dp(12),dp(18),dp(12));line.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);labels.addView(text(item[1],18,true));labels.addView(text(item[2],13,false));line.addView(labels,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=text("›",26,false);line.addView(arrow);row.addView(line);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(8);overview.addView(row,params);row.setOnClickListener(v->pages.show(section,item[0]));
        }
    }
    private void refreshEditorPreviews(){for(EditorPreview preview:editorPreviews)preview.invalidate();if(batteryControlPreview!=null)batteryControlPreview.invalidate();}
    private LinearLayout card(String title) {
        MaterialCardView panel = new MaterialCardView(this); PersonalizationUi.panel(panel);
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),dp(20),dp(20),dp(20)); panel.addView(box);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(18);content.addView(panel,params);box.addView(text(title,22,true));return box;
    }
    private TextView text(String value,int size,boolean bold) {
        TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setPadding(0,dp(6),0,dp(8));if(bold)view.setTypeface(null,android.graphics.Typeface.BOLD);return view;
    }
    private MaterialButton button(String title) { MaterialButton button=new MaterialButton(this);button.setText(title);button.setAllCaps(false);button.setCornerRadius(dp(28));button.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));return button; }
    private int dp(int n) {return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override protected void onSaveInstanceState(Bundle state) { if(pages!=null){state.putString("editor_section",pages.section());state.putString("editor_page",pages.page());}state.putInt("selection", selected);super.onSaveInstanceState(state); }
    @Override protected void onDestroy() {closed=true;if(client!=null)client.cancel();if(fingerprintClient!=null)fingerprintClient.cancel();worker.shutdown();super.onDestroy();}
    private void dateChoices(LinearLayout parent,String title,String[] labels,int component){
        parent.addView(text(title,15,true));ChipGroup group=new ChipGroup(this);group.setSingleSelection(true);parent.addView(group);
        int selected=switch(component){case 0->ClockDatePolicy.base(dateMode);case 1->ClockDatePolicy.position(dateMode);case 2->ClockDatePolicy.separator(dateMode);default->ClockDatePolicy.letterCase(dateMode);};
        for(int i=0;i<labels.length;i++){final int value=i;Chip chip=new Chip(this);chip.setId(View.generateViewId());chip.setText(labels[i]);chip.setTag(i);chip.setCheckable(true);chip.setChecked(selected==i);group.addView(chip);statusControls.add(chip);
            chip.setOnClickListener(v->{
                dateMode=ClockDatePolicy.selection(component==0?value:ClockDatePolicy.base(dateMode),component==1?value:ClockDatePolicy.position(dateMode),component==2?value:ClockDatePolicy.separator(dateMode),component==3?value:ClockDatePolicy.letterCase(dateMode));
                for(int n=0;n<group.getChildCount();n++){Chip c=(Chip)group.getChildAt(n);c.setChecked((Integer)c.getTag()==value);}
                final int requested=dateMode;worker.execute(()->{boolean saved=StatusBarPreferences.saveDate(getApplicationContext(),requested);runOnUiThread(()->{if(alive())saveStatus.setText(saved?"Date choices saved. Press Enable to apply them.":"Date choices could not be saved.");});});
            });
        }
    }

    private int currentTransform(int category){return category==0?systemTransform:category==1?notificationTransform:batteryTransform;}
    private void transformControls(LinearLayout box,String title,int category){
        for(int component=0;component<3;component++){
            final int part=component;int transform=currentTransform(category);
            int initial=part==0?StatusBarStyle.horizontal(transform):part==1?StatusBarStyle.vertical(transform):StatusBarStyle.rotation(transform);
            String labelText=title+(part==0?" horizontal offset":part==1?" vertical offset":" rotation"),unit=part==2?"°":" dp";
            TextView label=text(labelText+": "+initial+unit,15,true);box.addView(label);Slider slider=new Slider(this);slider.setValueFrom(part==2?-20:-8);slider.setValueTo(part==2?20:8);slider.setStepSize(part==2?5:2);slider.setValue(initial);box.addView(slider);statusControls.add(slider);
            slider.addOnChangeListener((v,value,user)->{if(user){int old=currentTransform(category),selected=(int)value;
                int next=StatusBarStyle.transform(part==0?selected:StatusBarStyle.horizontal(old),part==1?selected:StatusBarStyle.vertical(old),part==2?selected:StatusBarStyle.rotation(old));
                if(category==0)systemTransform=next;else if(category==1)notificationTransform=next;else batteryTransform=next;
                label.setText(labelText+": "+selected+unit);saveStatusBar();
            }});
        }
    }

    private void removeSlotControls(View view){statusControls.remove(view);if(view instanceof android.view.ViewGroup group)for(int i=0;i<group.getChildCount();i++)removeSlotControls(group.getChildAt(i));}
    private void indicatorHide(String title,int flag){
        var check=new com.google.android.material.checkbox.MaterialCheckBox(this);check.setText(title);check.setChecked((statusMask&flag)!=0);iconSlots.addView(check);statusControls.add(check);
        check.setOnCheckedChangeListener((view,selected)->{statusMask=selected?statusMask|flag:statusMask&~flag;saveStatusBar();});
    }
    private int indicatorSelection(int category){return category==0?fanAppearance:category==1?pumpAppearance:refreshAppearance;}
    private void saveIndicator(int category,int scale,int opacity){int value=StatusBarStyle.appearance(scale,opacity);if(category==0)fanAppearance=value;else if(category==1)pumpAppearance=value;else refreshAppearance=value;}
    private void indicatorAppearance(LinearLayout box,String title,int category){
        int value=indicatorSelection(category);
        statusScale(box,title+" icon size",StatusBarStyle.indicatorScale(value),n->saveIndicator(category,n,StatusBarStyle.indicatorOpacity(indicatorSelection(category))));
        statusOpacity(box,title+" icon opacity",StatusBarStyle.indicatorOpacity(value),n->saveIndicator(category,StatusBarStyle.indicatorScale(indicatorSelection(category)),n));
    }

    private void lockScreenExtras(LinearLayout parent){
        parent.addView(text("Widgets below the clock",18,true));
        String[] widgets={"Hide weather widget","Hide alarm widget","Hide battery widget"};for(int i=0;i<3;i++){final int bit=1<<i;var toggle=new com.google.android.material.materialswitch.MaterialSwitch(this);toggle.setText(widgets[i]);toggle.setChecked((widgetsHidden&bit)!=0);parent.addView(toggle);statusControls.add(toggle);toggle.setOnCheckedChangeListener((v,on)->{widgetsHidden=on?widgetsHidden|bit:widgetsHidden&~bit;saveStatusBar();});}
        parent=editorPanel("lock_screen","fingerprint","Fingerprint");
        parent.addView(text("Fingerprint icon",18,true));var hide=new com.google.android.material.materialswitch.MaterialSwitch(this);hide.setText("Hide fingerprint icon");hide.setChecked(fingerprintHidden!=0);parent.addView(hide);statusControls.add(hide);hide.setOnCheckedChangeListener((v,on)->{fingerprintHidden=on?1:0;saveStatusBar();});
        LinearLayout fingerprintHelp=detailGroup(parent,"Setup & service","Vector scope and fingerprint diagnostics");
        fingerprintHelp.addView(text("Enable nubia-ly for FingerprintSensorUi (com.fingerprint.sensorservice) in Vector as well as System UI, then reboot. Only the graphic changes. Keep touching the same sensor position to unlock. Enrollment, app authentication and always-on display use stock.",14,false));
        com.elitedarkkaiser.nubialy.xposed.FingerprintGlyphDrawable.artworkResources(getResources());
        fingerprintPreview=new android.widget.ImageView(this);fingerprintPreview.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);LinearLayout.LayoutParams iconSize=new LinearLayout.LayoutParams(dp(88),dp(88));iconSize.gravity=Gravity.CENTER_HORIZONTAL;parent.addView(fingerprintPreview,iconSize);
        fingerprintPicker=button("UDFPS icon");parent.addView(fingerprintPicker);statusControls.add(fingerprintPicker);redrawFingerprint();
        fingerprintPicker.setOnClickListener(v->{android.content.Intent intent=new android.content.Intent(this,FingerprintPickerActivity.class);intent.putExtra("selection",fingerprintStyle);startActivityForResult(intent,PICK_FINGERPRINT);});
        parent.addView(text("Choose an icon on its own page, then press Enable customization to apply. The icon follows stock visibility, including face unlock.",14,false));
        animationPicker=button("Fingerprint animation · "+FingerprintAnimations.NAMES[fingerprintAnimation]);parent.addView(animationPicker);statusControls.add(animationPicker);animationPicker.setOnClickListener(v->{android.content.Intent intent=new android.content.Intent(this,FingerprintAnimationPickerActivity.class);intent.putExtra("selection",fingerprintAnimation);startActivityForResult(intent,PICK_ANIMATION);});
        TextView serviceStatus=text("Fingerprint service has not been checked.",14,false);fingerprintHelp.addView(serviceStatus);MaterialButton serviceCheck=button("Check fingerprint service");fingerprintHelp.addView(serviceCheck);serviceCheck.setOnClickListener(v->{serviceStatus.setText("Checking fingerprint service…");fingerprintClient.check(serviceStatus::setText);});
        parent=editorPanel("lock_screen","fonts","Clock font styles");
        parent.addView(text("Clock font styles",18,true));
        lockFontPreview=text("12:34",48,false);lockFontPreview.setGravity(Gravity.CENTER);parent.addView(lockFontPreview);
        lockFontPicker=button("Clock font styles");parent.addView(lockFontPicker);statusControls.add(lockFontPicker);redrawClockFont();
        lockFontPicker.setOnClickListener(v->startActivityForResult(new android.content.Intent(this,ClockFontPickerActivity.class).putExtra("selection",lockClockFont),PICK_CLOCK_FONT));
        parent.addView(text("Preview a font on its own page. Text clocks use the selected face; image-based clock digits keep their artwork.",14,false));
    }
    private void redrawFingerprint(){fingerprintPreview.setImageDrawable(fingerprintStyle==0?getDrawable(R.drawable.rising_fp_udfps_icon_default):new com.elitedarkkaiser.nubialy.xposed.FingerprintGlyphDrawable(fingerprintStyle,0xffc0a8ff));fingerprintPreview.setContentDescription(FingerprintStyles.NAMES[fingerprintStyle]);fingerprintPicker.setText("UDFPS icon · "+FingerprintStyles.NAMES[fingerprintStyle]);}
    private void redrawClockFont(){if(lockFontPreview==null)return;var face=LockClockFonts.FACES[lockClockFont];lockFontPreview.setTypeface(android.graphics.Typeface.create(face.family(),face.style()));lockFontPicker.setText("Clock font styles · "+face.name());}
    @Override protected void onActivityResult(int request,int result,android.content.Intent data){super.onActivityResult(request,result,data);if(request==PICK_CLOCK_FONT&&result==RESULT_OK&&data!=null){int value=data.getIntExtra("selection",-1);if(LockClockFonts.valid(value)){lockClockFont=value;redrawClockFont();saveStatusBar();}}if(request==PICK_ANIMATION&&result==RESULT_OK&&data!=null){int value=data.getIntExtra("selection",-1);if(FingerprintAnimations.valid(value)){fingerprintAnimation=value;animationPicker.setText("Fingerprint animation · "+FingerprintAnimations.NAMES[value]);saveStatusBar();}}if(request==PICK_FINGERPRINT&&result==RESULT_OK&&data!=null){int value=data.getIntExtra("selection",-1);if(FingerprintStyles.valid(value)){fingerprintStyle=value;redrawFingerprint();saveStatusBar();}}}
    private void gridControls(LinearLayout parent){
        parent.addView(text("Arrange the small tiles below brightness. These choices apply to the portrait paged panel; landscape and large text keep stock layout. Enable applies the saved grid.",14,false));
        gridChoices(parent,true,new int[]{0,3,4},new String[]{"Stock","3 columns","4 columns"});
        gridChoices(parent,false,new int[]{0,2,3},new String[]{"Stock","2 rows","3 rows"});
        parent.addView(text("Rows are a maximum: the phone may fit fewer when space is limited. Page count follows your tiles. Large top tiles, tile order and tap actions remain native. Restore stock appearance or the guard Action restores the native grid.",14,false));
    }
    private void gridChoices(LinearLayout parent,boolean column,int[] values,String[] names){
        parent.addView(text(column?"Columns":"Rows per page",18,true));
        java.util.List<MaterialCardView> cards=new java.util.ArrayList<>();
        for(int i=0;i<values.length;i++){final int value=values[i];MaterialCardView card=new MaterialCardView(this);PersonalizationUi.panel(card);card.setCardBackgroundColor(getColor(R.color.surface_container_high));card.setRadius(dp(22));card.setClickable(true);card.setFocusable(true);card.setTag((column?"qs-columns:":"qs-rows:")+value);cards.add(card);statusControls.add(card);
            LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(12),dp(16),dp(12));content.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);TextView title=text(names[i],17,true);content.addView(title);
            int count=column?(value==0?4:value):4,lines=column?2:(value==0?3:value);for(int r=0;r<lines;r++){LinearLayout row=new LinearLayout(this);for(int c=0;c<count;c++){View tile=new View(this);android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(getColor(R.color.primary));bg.setCornerRadius(dp(8));tile.setBackground(bg);LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(0,dp(13),1);cell.setMargins(dp(3),dp(3),dp(3),dp(3));row.addView(tile,cell);}content.addView(row);}card.addView(content);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(0,dp(5),0,dp(5));parent.addView(card,params);
            card.setOnClickListener(v->{if(column)qsColumns=value;else qsRows=value;for(int k=0;k<cards.size();k++){boolean selected=values[k]==value;cards.get(k).setStrokeWidth(dp(selected?3:0));cards.get(k).setSelected(selected);cards.get(k).setContentDescription(names[k]+(selected?", selected":""));}saveStatusBar();});
        }
        for(int i=0;i<cards.size();i++){boolean selected=values[i]==(column?qsColumns:qsRows);cards.get(i).setStrokeColor(getColor(R.color.primary));cards.get(i).setStrokeWidth(dp(selected?3:0));cards.get(i).setSelected(selected);cards.get(i).setContentDescription(names[i]+(selected?", selected":""));}
    }
    private void qsStyleControls(LinearLayout parent){
        parent.addView(text("Choose a look for both large and small tiles. Styles follow native state colors and your separate shape choices.",14,false));
        java.util.List<MaterialCardView> choices=new java.util.ArrayList<>();String[] names=QsTileEffects.NAMES;
        LinearLayout row=null;boolean stacked=getResources().getConfiguration().fontScale>1.3f||getResources().getConfiguration().screenWidthDp<360;
        for(int i=0;i<names.length;i++){final int value=i;if(stacked||i%2==0){row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);parent.addView(row);}MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(24));card.setCardElevation(0);card.setCardBackgroundColor(getColor(R.color.surface_container_high));card.setClickable(true);card.setFocusable(true);card.setTag("qs-style:"+i);choices.add(card);statusControls.add(card);
            LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setGravity(Gravity.CENTER);body.setPadding(dp(12),dp(16),dp(12),dp(10));body.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);body.addView(new QsStylePreview(this,i),new LinearLayout.LayoutParams(-1,dp(112)));TextView title=text(names[i],16,true);title.setGravity(Gravity.CENTER);body.addView(title);card.addView(body);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(stacked?-1:0,-2,stacked?0:1);params.setMargins(dp(4),dp(4),dp(4),dp(4));row.addView(card,params);
            card.setOnClickListener(v->{qsStyle=value;for(int n=0;n<choices.size();n++){MaterialCardView option=choices.get(n);option.setStrokeWidth(dp(n==value?3:0));option.setStrokeColor(getColor(R.color.primary));option.setSelected(n==value);option.setContentDescription(names[n]+(n==value?", selected":""));}saveStatusBar();});
        }
        for(int i=0;i<choices.size();i++){MaterialCardView card=choices.get(i);card.setStrokeWidth(dp(i==qsStyle?3:0));card.setStrokeColor(getColor(R.color.primary));card.setSelected(i==qsStyle);card.setContentDescription(names[i]+(i==qsStyle?", selected":""));}
    }
    private void brightnessStyleControls(LinearLayout parent){
        parent.addView(text("Style the brightness track and its drag mirror. Thin keeps the full touch area. Press Enable to apply.",14,false));
        java.util.List<MaterialCardView> choices=new java.util.ArrayList<>();String[] names=BrightnessEffects.NAMES;
        LinearLayout row=null;boolean stacked=getResources().getConfiguration().fontScale>1.3f||getResources().getConfiguration().screenWidthDp<360;
        for(int i=0;i<names.length;i++){final int value=i;if(stacked||i%2==0){row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);parent.addView(row);}MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(24));card.setCardElevation(0);card.setCardBackgroundColor(getColor(R.color.surface_container_high));card.setClickable(true);card.setFocusable(true);card.setTag("brightness-style:"+i);choices.add(card);statusControls.add(card);
            LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setGravity(Gravity.CENTER);body.setPadding(dp(12),dp(16),dp(12),dp(10));body.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);body.addView(new BrightnessStylePreview(this,i),new LinearLayout.LayoutParams(-1,dp(72)));TextView title=text(names[i],16,true);title.setGravity(Gravity.CENTER);body.addView(title);card.addView(body);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(stacked?-1:0,-2,stacked?0:1);params.setMargins(dp(4),dp(4),dp(4),dp(4));row.addView(card,params);
            card.setOnClickListener(v->{brightnessStyle=value;for(int n=0;n<choices.size();n++){MaterialCardView option=choices.get(n);option.setStrokeWidth(dp(n==value?3:0));option.setStrokeColor(getColor(R.color.primary));option.setSelected(n==value);option.setContentDescription(names[n]+(n==value?", selected":""));}saveStatusBar();});
        }
        for(int i=0;i<choices.size();i++){MaterialCardView card=choices.get(i);card.setStrokeWidth(dp(i==brightnessStyle?3:0));card.setStrokeColor(getColor(R.color.primary));card.setSelected(i==brightnessStyle);card.setContentDescription(names[i]+(i==brightnessStyle?", selected":""));}
    }
    private void volumeStyleControls(LinearLayout parent){
        parent.addView(text("Choose a look for compatible sliders in the volume-button panel. Audio levels and controls stay stock. Press Enable to apply.",14,false));
        java.util.List<MaterialCardView> choices=new java.util.ArrayList<>();String[] names=VolumeEffects.NAMES;
        LinearLayout row=null;boolean stacked=getResources().getConfiguration().fontScale>1.3f||getResources().getConfiguration().screenWidthDp<360;
        for(int i=0;i<names.length;i++){final int value=i;if(stacked||i%2==0){row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);parent.addView(row);}MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(24));card.setCardElevation(0);card.setCardBackgroundColor(getColor(R.color.surface_container_high));card.setClickable(true);card.setFocusable(true);card.setTag("volume-style:"+i);choices.add(card);statusControls.add(card);
            LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setGravity(Gravity.CENTER);body.setPadding(dp(12),dp(16),dp(12),dp(10));body.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);body.addView(new VolumeStylePreview(this,i),new LinearLayout.LayoutParams(-1,dp(120)));TextView title=text(names[i],16,true);title.setGravity(Gravity.CENTER);body.addView(title);card.addView(body);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(stacked?-1:0,-2,stacked?0:1);params.setMargins(dp(4),dp(4),dp(4),dp(4));row.addView(card,params);
            card.setOnClickListener(v->{volumeStyle=value;for(int n=0;n<choices.size();n++){MaterialCardView option=choices.get(n);option.setStrokeWidth(dp(n==value?3:0));option.setStrokeColor(getColor(R.color.primary));option.setSelected(n==value);option.setContentDescription(names[n]+(n==value?", selected":""));}saveStatusBar();});
        }
        for(int i=0;i<choices.size();i++){MaterialCardView card=choices.get(i);card.setStrokeWidth(dp(i==volumeStyle?3:0));card.setStrokeColor(getColor(R.color.primary));card.setSelected(i==volumeStyle);card.setContentDescription(names[i]+(i==volumeStyle?", selected":""));}
    }
    private void batteryControls(LinearLayout parent){
        LinearLayout controls=detailGroup(parent,"Battery controls","Percentage, charging & size");
        batteryControlPreview=new BatteryControlsPreview(this,()->currentStatusBar());controls.addView(batteryControlPreview,new LinearLayout.LayoutParams(-1,dp(100)));controls.addView(text("Percentage placement",16,true));
        String[] names={"Stock","Inside","Outside","Hidden"};ChipGroup choices=new ChipGroup(this);choices.setSingleSelection(true);controls.addView(choices);
        for(int i=0;i<names.length;i++){final int value=i;Chip chip=new Chip(this);chip.setId(View.generateViewId());chip.setTag("battery-percent:"+i);chip.setText(names[i]);chip.setCheckable(true);chip.setChecked(batteryPercent==i);choices.addView(chip);statusControls.add(chip);chip.setOnClickListener(v->{batteryPercent=value;saveStatusBar();});}
        var charge=new com.google.android.material.materialswitch.MaterialSwitch(this);charge.setTag("battery-hide-charge");charge.setText("Hide charging symbol");charge.setChecked(batteryCharge!=0);controls.addView(charge);statusControls.add(charge);charge.setOnCheckedChangeListener((v,on)->{batteryCharge=on?1:0;saveStatusBar();});
        statusScale(controls,"Battery size",batteryScale,n->batteryScale=n);
        controls.addView(text("Press Enable to apply. Size shares the Icons & battery setting. Text only always displays its percentage; Hidden hides the whole battery. Charging can temporarily replace an inside percentage. Warning and unsupported charging states retain REDMAGIC indicators.",14,false));
    }
    private void notificationIconControls(LinearLayout parent){
        parent.addView(text("Control notification icons in the top status bar. The count replaces the overflow dot with the number of additional notification icons, including the icon represented by the dot. Grouped notifications follow Android’s icon grouping. Notifications in the shade are unchanged.",14,false));
        var count=new com.google.android.material.materialswitch.MaterialSwitch(this);count.setTag("notification-overflow-count");count.setText("Show overflow count");count.setChecked(notificationCount!=0);parent.addView(count);statusControls.add(count);count.setOnCheckedChangeListener((v,on)->{notificationCount=on?1:0;saveStatusBar();});
        parent.addView(text("Maximum icon slots",16,true));ChipGroup limits=new ChipGroup(this);limits.setSingleSelection(true);parent.addView(limits);for(int i=0;i<=8;i++){final int value=i;Chip chip=new Chip(this);chip.setId(View.generateViewId());chip.setTag("notification-limit:"+i);chip.setText(i==0?"Stock":Integer.toString(i));chip.setCheckable(true);chip.setChecked(notificationLimit==i);limits.addView(chip);statusControls.add(chip);chip.setOnClickListener(v->{notificationLimit=value;saveStatusBar();});}
        parent.addView(text("Overflow uses one slot. Available space and REDMAGIC may reduce the number further. Existing Hide notification icons takes priority. Lock screen, AOD and notification shelf retain stock behavior. Press Enable to apply.",14,false));
    }
    private void batteryStyleControls(LinearLayout parent){
        parent.addView(text("Choose a battery design inspired by RisingOS. Previews show sample low, normal and charging states. Styles follow your real battery level and native colors. Press Enable to apply.",14,false));
        parent.addView(text("Designs fit inside your current status-bar space. External percentage and charge indicators keep their stock positions. Text only replaces the battery group with its percentage; Hidden hides its graphics while keeping the space.",14,false));
        java.util.List<MaterialCardView> choices=new java.util.ArrayList<>();LinearLayout row=null;boolean stacked=getResources().getConfiguration().fontScale>1.3f||getResources().getConfiguration().screenWidthDp<360;
        for(int i=0;i<BatteryStyles.NAMES.length;i++){final int value=i;if(stacked||i%2==0){row=new LinearLayout(this);parent.addView(row);}MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(24));card.setCardElevation(0);card.setCardBackgroundColor(getColor(R.color.surface_container_high));card.setClickable(true);card.setFocusable(true);card.setTag("battery-style:"+i);choices.add(card);statusControls.add(card);
            LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setGravity(Gravity.CENTER);body.setPadding(dp(10),dp(12),dp(10),dp(12));body.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);body.addView(new BatteryStylePreview(this,i),new LinearLayout.LayoutParams(-1,dp(78)));TextView title=text(BatteryStyles.NAMES[i],16,true);title.setGravity(Gravity.CENTER);body.addView(title);card.addView(body);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(stacked?-1:0,-2,stacked?0:1);params.setMargins(dp(4),dp(4),dp(4),dp(4));row.addView(card,params);
            card.setOnClickListener(v->{batteryStyle=value;for(int n=0;n<choices.size();n++){MaterialCardView option=choices.get(n);option.setStrokeWidth(dp(n==value?3:0));option.setStrokeColor(getColor(R.color.primary));option.setSelected(n==value);option.setContentDescription(BatteryStyles.NAMES[n]+(n==value?", selected":""));}saveStatusBar();});
        }
        for(int n=0;n<choices.size();n++){MaterialCardView option=choices.get(n);option.setStrokeWidth(dp(n==batteryStyle?3:0));option.setStrokeColor(getColor(R.color.primary));option.setSelected(n==batteryStyle);option.setContentDescription(BatteryStyles.NAMES[n]+(n==batteryStyle?", selected":""));}
    }
    private void connectivityControls(LinearLayout parent){
        ConnectivityStyles.resources(getResources());
        parent.addView(text("Choose separate Wi-Fi and mobile-signal artwork. Signal strength, SIMs and connectivity remain controlled by Android. Warning and unsupported icons keep stock appearance. Press Enable to apply.",14,false));
        parent.addView(text("Wi-Fi",18,true));visualStyleChoices(parent,"wifi");parent.addView(text("Mobile signal",18,true));visualStyleChoices(parent,"signal");
    }
    private int visualSelection(String kind){return kind.equals("wifi")?wifiStyle:kind.equals("signal")?signalStyle:powerStyle;}
    private void visualStyleChoices(LinearLayout parent,String kind){
        boolean power=kind.equals("power"),wifi=kind.equals("wifi");String[] names=power?NotificationEffects.NAMES:ConnectivityStyles.NAMES;
        if(power)parent.addView(text("Style the stock power and restart button backgrounds. Power actions, confirmations and lock-screen security stay controlled by REDMAGIC. Press Enable to apply.",14,false));
        java.util.List<MaterialCardView> choices=new java.util.ArrayList<>();LinearLayout row=null;boolean stacked=getResources().getConfiguration().fontScale>1.3f||getResources().getConfiguration().screenWidthDp<360;
        for(int i=0;i<names.length;i++){final int value=i;if(stacked||i%2==0){row=new LinearLayout(this);parent.addView(row);}MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(24));card.setCardElevation(0);card.setCardBackgroundColor(getColor(R.color.surface_container_high));card.setClickable(true);card.setFocusable(true);card.setTag(kind+"-style:"+i);choices.add(card);statusControls.add(card);
            LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setGravity(Gravity.CENTER);body.setPadding(dp(12),dp(12),dp(12),dp(12));body.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            if(power)body.addView(new PowerStylePreview(this,i),new LinearLayout.LayoutParams(-1,dp(100)));else body.addView(new ConnectivityStylePreview(this,wifi,i),new LinearLayout.LayoutParams(-1,dp(90)));
            TextView title=text(names[i],16,true);title.setGravity(Gravity.CENTER);body.addView(title);card.addView(body);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(stacked?-1:0,-2,stacked?0:1);cp.setMargins(dp(4),dp(4),dp(4),dp(4));row.addView(card,cp);
            card.setOnClickListener(v->{if(power)powerStyle=value;else if(wifi)wifiStyle=value;else signalStyle=value;for(int n=0;n<choices.size();n++){MaterialCardView option=choices.get(n);option.setStrokeWidth(dp(n==value?3:0));option.setStrokeColor(getColor(R.color.primary));option.setSelected(n==value);option.setContentDescription(names[n]+(n==value?", selected":""));}saveStatusBar();});
        }
        for(int n=0;n<choices.size();n++){MaterialCardView card=choices.get(n);card.setStrokeWidth(dp(n==visualSelection(kind)?3:0));card.setStrokeColor(getColor(R.color.primary));card.setSelected(n==visualSelection(kind));card.setContentDescription(names[n]+(n==visualSelection(kind)?", selected":""));}
    }
    private void notificationAppearanceControls(LinearLayout parent){
        parent.addView(text("Adjust the compatible card background without fading text or actions. Corner clipping adds rounding inside the stock shape; grouped and newer vendor surfaces may keep stock. Press Enable to apply.",14,false));
        TextView label=text("",16,true);parent.addView(label);Slider opacity=new Slider(this);opacity.setValueFrom(50);opacity.setValueTo(100);opacity.setStepSize(5);int selectedOpacity=NotificationControls.opacity(notificationAppearance);opacity.setValue(selectedOpacity==0?100:selectedOpacity);parent.addView(opacity);statusControls.add(opacity);
        Runnable redraw=()->label.setText("Background opacity: "+(NotificationControls.opacity(notificationAppearance)==0?"Stock":NotificationControls.opacity(notificationAppearance)+"%"));redraw.run();opacity.addOnChangeListener((v,value,user)->{if(user){notificationAppearance=NotificationControls.encode((int)value,NotificationControls.corners(notificationAppearance));redraw.run();saveStatusBar();}});
        MaterialButton stock=button("Use stock opacity");parent.addView(stock);statusControls.add(stock);stock.setOnClickListener(v->{notificationAppearance=NotificationControls.encode(0,NotificationControls.corners(notificationAppearance));opacity.setValue(100);redraw.run();saveStatusBar();});
        parent.addView(text("Corner clipping",16,true));String[] names={"Stock","12 dp","24 dp","32 dp"};ChipGroup corners=new ChipGroup(this);corners.setSingleSelection(true);parent.addView(corners);for(int i=0;i<names.length;i++){final int n=i;Chip choice=new Chip(this);choice.setId(View.generateViewId());choice.setText(names[i]);choice.setCheckable(true);choice.setChecked(NotificationControls.corners(notificationAppearance)==i);corners.addView(choice);statusControls.add(choice);choice.setOnClickListener(v->{notificationAppearance=NotificationControls.encode(NotificationControls.opacity(notificationAppearance),n);saveStatusBar();});}
    }
    private void notificationBehaviorControls(LinearLayout parent){
        var toggle=new com.google.android.material.materialswitch.MaterialSwitch(this);toggle.setText("Suppress automatic heads-up popups");toggle.setChecked(notificationBehavior!=0);parent.addView(toggle);statusControls.add(toggle);toggle.setOnCheckedChangeListener((v,on)->{notificationBehavior=on?1:0;saveStatusBar();});
        parent.addView(text("Applies to new automatic heads-up presentations after Enable. Existing or user-pinned popups, notification delivery, sound, full-screen alerts and DND retain their native behavior.",14,false));
        MaterialButton history=button("Notification history");parent.addView(history);history.setOnClickListener(v->openNotificationSettings("android.settings.NOTIFICATION_HISTORY"));MaterialButton apps=button("App notification controls");parent.addView(apps);apps.setOnClickListener(v->openNotificationSettings("android.settings.ALL_APPS_NOTIFICATION_SETTINGS"));
        MaterialButton settings=button("System notification settings & snoozing");parent.addView(settings);settings.setOnClickListener(v->openNotificationSettings("android.settings.NOTIFICATION_SETTINGS"));parent.addView(text("History, snoozing and per-app behavior open REDMAGIC settings. Changes made there are managed by Android and are separate from Restore stock appearance.",14,false));
    }
    private void openNotificationSettings(String action){try{startActivity(new android.content.Intent(action));}catch(android.content.ActivityNotFoundException unavailable){new MaterialAlertDialogBuilder(this).setTitle("Not available").setMessage("REDMAGIC does not expose this settings page on your firmware.").setPositiveButton("OK",null).show();}}
    private void chargingControls(LinearLayout parent){
        var toggle=new com.google.android.material.materialswitch.MaterialSwitch(this);toggle.setText("Show live charging diagnostics");toggle.setChecked(chargingInfo!=0);parent.addView(toggle);statusControls.add(toggle);toggle.setOnCheckedChangeListener((v,on)->{chargingInfo=on?1:0;saveStatusBar();});
        parent.addView(text("Adds a second line beneath the stock charging message while plugged in and awake: measured battery voltage, battery current and temperature when available. The existing charging type and percentage remain above it. Press Enable to apply.",14,false));
        parent.addView(text("Example · Battery 4.420 V · +2,350 mA · 36.5 °C",16,true));parent.addView(text("Battery voltage is measured at the battery, not the USB charger. Current sign follows the device sensor. Readings follow native charging updates; there is no background polling.",14,false));
    }
    private void notificationStyleControls(LinearLayout parent){
        parent.addView(text("Choose a background treatment for notification cards. Text, actions and gestures follow Android. Press Enable to apply.",14,false));
        java.util.List<MaterialCardView> choices=new java.util.ArrayList<>();String[] names=NotificationEffects.NAMES;
        LinearLayout row=null;boolean stacked=getResources().getConfiguration().fontScale>1.3f||getResources().getConfiguration().screenWidthDp<360;
        for(int i=0;i<names.length;i++){final int value=i;if(stacked||i%2==0){row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);parent.addView(row);}MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(24));card.setCardElevation(0);card.setCardBackgroundColor(getColor(R.color.surface_container_high));card.setClickable(true);card.setFocusable(true);card.setTag("notification-style:"+i);choices.add(card);statusControls.add(card);
            LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setGravity(Gravity.CENTER);body.setPadding(dp(12),dp(16),dp(12),dp(10));body.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);body.addView(new NotificationStylePreview(this,i),new LinearLayout.LayoutParams(-1,dp(120)));TextView title=text(names[i],16,true);title.setGravity(Gravity.CENTER);body.addView(title);card.addView(body);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(stacked?-1:0,-2,stacked?0:1);params.setMargins(dp(4),dp(4),dp(4),dp(4));row.addView(card,params);
            card.setOnClickListener(v->{notificationStyle=value;for(int n=0;n<choices.size();n++){MaterialCardView option=choices.get(n);option.setStrokeWidth(dp(n==value?3:0));option.setStrokeColor(getColor(R.color.primary));option.setSelected(n==value);option.setContentDescription(names[n]+(n==value?", selected":""));}saveStatusBar();});
        }
        for(int i=0;i<choices.size();i++){MaterialCardView card=choices.get(i);card.setStrokeWidth(dp(i==notificationStyle?3:0));card.setStrokeColor(getColor(R.color.primary));card.setSelected(i==notificationStyle);card.setContentDescription(names[i]+(i==notificationStyle?", selected":""));}
    }
    private void shapeControls(LinearLayout parent,String title,boolean large){
        parent.addView(text(title,18,true));
        String[] names={"Stock","Square","Rounded square","Leaf"};
        java.util.List<MaterialCardView> cards=new java.util.ArrayList<>();
        boolean stacked=getResources().getConfiguration().fontScale>1.3f||getResources().getConfiguration().screenWidthDp<360;
        LinearLayout row=null;
        for(int i=0;i<names.length;i++){
            final int value=i;
            if(stacked||i%2==0){row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);parent.addView(row,new LinearLayout.LayoutParams(-1,-2));}
            MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(24));card.setCardElevation(0);card.setCardBackgroundColor(getColor(R.color.surface_container_high));card.setClickable(true);card.setFocusable(true);card.setTag("tile-shape:"+(large?"large:":"small:")+value);
            LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setGravity(Gravity.CENTER);body.setPadding(dp(14),dp(16),dp(14),dp(10));body.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            View sample=new View(this);GradientDrawable shape=new GradientDrawable();shape.setColor(getColor(R.color.primary));
            if(i==3)shape.setCornerRadii(new float[]{dp(22),dp(22),dp(2),dp(2),dp(22),dp(22),dp(2),dp(2)});else shape.setCornerRadius(dp(i==1?2:i==2?12:28));sample.setBackground(shape);body.addView(sample,new LinearLayout.LayoutParams(dp(large?90:48),dp(48)));
            TextView label=text(names[i],14,false);label.setGravity(Gravity.CENTER);body.addView(label);card.addView(body);cards.add(card);statusControls.add(card);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(stacked?-1:0,-2,stacked?0:1);params.setMargins(dp(4),dp(4),dp(4),dp(4));row.addView(card,params);
            card.setOnClickListener(v->{if(large)largeTileShape=value;else tileShape=value;for(int n=0;n<cards.size();n++){MaterialCardView option=cards.get(n);boolean checked=n==value;option.setStrokeWidth(dp(checked?3:0));option.setStrokeColor(getColor(R.color.primary));option.setSelected(checked);option.setContentDescription(names[n]+(checked?", selected":""));}saveStatusBar();});
        }
        int selection=large?largeTileShape:tileShape;for(int i=0;i<cards.size();i++){MaterialCardView card=cards.get(i);card.setStrokeWidth(dp(i==selection?3:0));card.setStrokeColor(getColor(R.color.primary));card.setSelected(i==selection);card.setContentDescription(title+", "+names[i]+(i==selection?", selected":""));}
    }
    private SurfaceStyle currentSurfaces(){return new SurfaceStyle(surfaceHidden,surfaceAppearance[0],surfaceAppearance[1],surfaceAppearance[2],surfaceAppearance[3],surfacePosition[0],surfacePosition[1],surfacePosition[2],surfacePosition[3],tileShape,largeTileShape,widgetsHidden,fingerprintStyle,fingerprintHidden,lockClockFont,fingerprintAnimation,qsStyle,brightnessStyle,volumeStyle,notificationStyle,wifiStyle,signalStyle,powerStyle,notificationAppearance,notificationBehavior,chargingInfo,batteryStyle,batteryPercent,batteryCharge,notificationCount,notificationLimit,qsColumns,qsRows);}
    private void surfaceControls(LinearLayout parent,String title,int group){
        parent.addView(text(title,18,true));
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);parent.addView(box);
        var hide=new com.google.android.material.checkbox.MaterialCheckBox(this);hide.setText("Hide "+title.toLowerCase(java.util.Locale.ROOT));hide.setChecked((surfaceHidden&(1<<group))!=0);box.addView(hide);statusControls.add(hide);
        hide.setOnCheckedChangeListener((v,value)->{surfaceHidden=value?surfaceHidden|(1<<group):surfaceHidden&~(1<<group);saveStatusBar();});
        statusScale(box,"Visual size",StatusBarStyle.indicatorScale(surfaceAppearance[group]),n->{surfaceAppearance[group]=StatusBarStyle.appearance(n,StatusBarStyle.indicatorOpacity(surfaceAppearance[group]));});
        statusOpacity(box,"Opacity",StatusBarStyle.indicatorOpacity(surfaceAppearance[group]),n->{surfaceAppearance[group]=StatusBarStyle.appearance(StatusBarStyle.indicatorScale(surfaceAppearance[group]),n);});
        for(int part=0;part<3;part++){final int component=part;int old=surfacePosition[group],initial=part==0?StatusBarStyle.horizontal(old):part==1?StatusBarStyle.vertical(old):StatusBarStyle.rotation(old);
            String titleText=part==0?"Horizontal offset":part==1?"Vertical offset":"Rotation",unit=part==2?"°":" dp";TextView label=text(titleText+": "+initial+unit,15,true);box.addView(label);Slider slider=new Slider(this);slider.setValueFrom(part==2?-20:-8);slider.setValueTo(part==2?20:8);slider.setStepSize(part==2?5:2);slider.setValue(initial);box.addView(slider);statusControls.add(slider);
            slider.addOnChangeListener((v,value,user)->{if(user){int current=surfacePosition[group],selected=(int)value;surfacePosition[group]=StatusBarStyle.transform(component==0?selected:StatusBarStyle.horizontal(current),component==1?selected:StatusBarStyle.vertical(current),component==2?selected:StatusBarStyle.rotation(current));label.setText(titleText+": "+selected+unit);saveStatusBar();}});
        }
    }

}
