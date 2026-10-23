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
    private int iconPack,qsStyle,brightnessStyle,volumeStyle,notificationStyle,wifiStyle,signalStyle,powerStyle,notificationAppearance,notificationBehavior,chargingInfo,batteryStyle,batteryPercent,batteryCharge,notificationCount,notificationLimit,qsColumns,qsRows,qsOptions;
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
    private int qsIconPack;
    private SurfaceStyle.QsTheme qsTheme=SurfaceStyle.QsTheme.STOCK;private QsThemeEditor qsThemeEditor;private static final int PICK_QS_IMAGE=946;
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
        if (!CompatibilityPolicy.isSupportedDevice(Build.MODEL, Build.DEVICE) || !CompatibilityPolicy.isSupportedSdk(Build.VERSION.SDK_INT)) { finish(); return; }
        selected = ClockColorPreferences.read(this);
        sizePercent = ClockColorPreferences.size(this); bold = ClockColorPreferences.bold(this); seconds = ClockColorPreferences.seconds(this); italic = ClockColorPreferences.italic(this); spacing = ClockColorPreferences.spacing(this); fontFamily = ClockColorPreferences.family(this); hidden = ClockColorPreferences.hidden(this);
        StatusBarStyle bar = StatusBarPreferences.read(this); statusMask = bar.mask(); systemScale = bar.systemScale(); notificationScale = bar.notificationScale(); batteryScale = bar.batteryScale();systemOpacity=bar.systemOpacity();notificationOpacity=bar.notificationOpacity();batteryOpacity=bar.batteryOpacity();systemTransform=bar.systemTransform();notificationTransform=bar.notificationTransform();batteryTransform=bar.batteryTransform();fanAppearance=bar.fanAppearance();pumpAppearance=bar.pumpAppearance();refreshAppearance=bar.refreshAppearance();hiddenSlots.addAll(bar.hiddenSlots());iconColor=StatusBarPreferences.color(this);dateMode=StatusBarPreferences.date(this);
        qsTheme=bar.surfaces().qsTheme();qsIconPack=bar.surfaces().qsIconPack();iconPack=bar.surfaces().iconPack();qsOptions=bar.surfaces().qsOptions();qsColumns=bar.surfaces().qsColumns()==4?0:bar.surfaces().qsColumns();qsRows=bar.surfaces().qsRows()==3?0:bar.surfaces().qsRows();batteryPercent=bar.surfaces().batteryPercent();batteryCharge=bar.surfaces().batteryCharge();notificationCount=bar.surfaces().notificationCount();notificationLimit=bar.surfaces().notificationLimit();batteryStyle=bar.surfaces().batteryStyle();wifiStyle=bar.surfaces().wifiStyle();signalStyle=bar.surfaces().signalStyle();powerStyle=bar.surfaces().powerStyle();notificationAppearance=bar.surfaces().notificationAppearance();notificationBehavior=bar.surfaces().notificationBehavior();chargingInfo=bar.surfaces().chargingInfo();notificationStyle=bar.surfaces().notificationStyle();volumeStyle=bar.surfaces().volumeStyle();brightnessStyle=bar.surfaces().brightnessStyle();qsStyle=bar.surfaces().qsStyle();widgetsHidden=bar.surfaces().widgetsHidden();fingerprintAnimation=bar.surfaces().fingerprintAnimation();fingerprintStyle=bar.surfaces().fingerprintStyle();fingerprintHidden=bar.surfaces().fingerprintHidden();lockClockFont=bar.surfaces().clockFont();tileShape=bar.surfaces().tileShape();largeTileShape=bar.surfaces().largeTileShape();surfaceHidden=bar.surfaces().hidden();for(int i=0;i<4;i++){surfaceAppearance[i]=bar.surfaces().appearance(i);surfacePosition[i]=bar.surfaces().position(i);}
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
        editorOverview("quick_settings","Quick Settings",new String[][]{{"colors","QS colors","Backgrounds, icons & labels"},{"background","Upper QS background","Image, color & opacity"},{"grid","Portrait grid","Small tiles per page"},{"landscape","Landscape grid","Independent rows & columns"},{"top_layout","Large tile layout","Count & arrangement"},{"label_layout","Label layout","Position, lines & icons only"},{"brightness_layout","Brightness placement","Position & sun icons"},{"style","Tile style","Outline, depth & layered effects"},{"brightness","Brightness slider","Outline, depth & a slim track"},{"tiles","Tile shapes","Large & small tiles"},{"labels","Label styles","Main & secondary text"},{"iconpacks","Icon packs","Quick Settings artwork"}});
        editorOverview("notifications","Notifications",new String[][]{{"styles","Notification styles","Card backgrounds & layers"},{"appearance","Appearance controls","Background opacity & rounded corners"},{"behavior","Notification controls","Heads-up, history & app settings"},{"icons","Counts & icon limits","Status-bar overflow & icon slots"}});
        editorOverview("power_menu","Power menu",new String[][]{{"styles","Power-menu styles","Duoline, CyberPunk, Fluid & layers"}});
        editorOverview("volume","Volume",new String[][]{{"styles","Volume styles","Outline, gradients & layered effects"}});
        editorOverview("status_bar","Status bar",new String[][]{{"typography","Clock style","Font, size & seconds"},{"date","Date format","Format, position & letter case"},{"color","Clock color","Hex color editor"},{"icons","Icons & battery","Colors, size & placement"},{"battery","Battery styles & controls","Design, percentage, charging & size"},{"iconpacks","Icon packs","Status-bar artwork"},{"connectivity","Wi-Fi & mobile signal","Choose independent icon styles"},{"inventory","Individual icons","Visibility & vendor indicators"}});
        LinearLayout colors = editorPanel("status_bar","color","Clock color");
        swatch = text("12:34", 44, true); colors.addView(swatch);
        selection = text("", 16, true); colors.addView(selection);
        presets = new ChipGroup(this); presets.setSingleSelection(true); colors.addView(presets);
        MaterialButton custom = button("Edit hex color"); colors.addView(custom); custom.setOnClickListener(v -> customColor());
        saveStatus = text("Saved selection loaded.", 14, false); colors.addView(saveStatus);
        LinearLayout icons = editorPanel("status_bar","icons","Icons & battery");
        icons.addView(text("Mobile service icon controls style the compatible calling-service indicator, such as VoLTE. Other compatible system indicators can share these settings. App notification icons are separate. Some Wi-Fi and signal icons keep stock appearance. Battery keeps stock charge and warning colors.",14,false));
        LinearLayout iconTints=detailGroup(icons,"Icon color","Mobile service indicator");
        iconSelection=text("",16,true);iconTints.addView(iconSelection);iconPresets=new ChipGroup(this);iconPresets.setSingleSelection(true);iconTints.addView(iconPresets);
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
        icons.addView(text("Reset clears draft icon/battery controls; press Apply afterward to apply stock icon/battery appearance while keeping your clock choices.",14,false));
        icons.addView(text("100% uses stock size. These controls apply to recognized top-status-bar views; other icons may keep their stock appearance. Press Apply to apply this page.",14,false));
        LinearLayout batteries=editorPanel("status_bar","battery","Battery styles & controls");batteryControls(batteries);batteryStyleControls(batteries);
        iconPackControls(editorPanel("status_bar","iconpacks","Icon packs"),false);
        iconPackControls(editorPanel("quick_settings","iconpacks","Icon packs"),true);
        connectivityControls(editorPanel("status_bar","connectivity","Wi-Fi & mobile signal"));
        visualStyleChoices(editorPanel("power_menu","styles","Power-menu styles"),"power");
        notificationAppearanceControls(editorPanel("notifications","appearance","Appearance controls"));
        notificationIconControls(editorPanel("notifications","icons","Counts & icon limits"));
        notificationBehaviorControls(editorPanel("notifications","behavior","Notification controls"));
        chargingControls(editorPanel("lock_screen","charging","Charging information"));
        LinearLayout notificationPanel=editorPanel("notifications","styles","Notification styles");notificationStyleControls(notificationPanel);
        LinearLayout volumePanel=editorPanel("volume","styles","Volume styles");volumeStyleControls(volumePanel);
        gridControls(editorPanel("quick_settings","grid","Portrait grid"));
        qsLayoutControls();
        qsThemeEditor=new QsThemeEditor(this,()->qsTheme,value->{qsTheme=value;saveStatusBar();},()->{android.content.Intent pick=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(android.content.Intent.CATEGORY_OPENABLE);startActivityForResult(pick,PICK_QS_IMAGE);});
        qsThemeEditor.colors(editorPanel("quick_settings","colors","QS colors"));
        qsThemeEditor.header(editorPanel("quick_settings","header","QS header"));
        qsThemeEditor.background(editorPanel("quick_settings","background","Upper QS background"));
        if(!qsTheme.image().isEmpty()){String imageHash=qsTheme.image();worker.execute(()->{android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeFile(QsImageStore.file(getApplicationContext(),imageHash).toString());runOnUiThread(()->{if(alive()){qsThemeEditor.image(bitmap,imageHash);for(EditorPreview preview:editorPreviews)preview.qsImage(bitmap,imageHash);}});});}
        LinearLayout stylePanel=editorPanel("quick_settings","style","Tile style");qsStyleControls(stylePanel);
        LinearLayout brightnessPanel=editorPanel("quick_settings","brightness","Brightness slider");brightnessStyleControls(brightnessPanel);
        LinearLayout surfaces=editorPanel("quick_settings","tiles","Tile shapes");
        surfaces.addView(text("Choose independent shapes for large and small tiles. Label appearance keeps tile actions unchanged. Press Apply to apply saved choices.",14,false));
        MaterialButton oneui=button("Apply One UI-style look");surfaces.addView(oneui);statusControls.add(oneui);
        surfaces.addView(text("One tap: pill large & small tiles, filled tile style and a denser 5-column grid. Tweak anything afterwards, then press Apply. Panel pull-down stays stock.",13,false));
        oneui.setOnClickListener(v->{tileShape=4;largeTileShape=4;qsStyle=0;qsColumns=5;final StatusBarStyle value=currentStatusBar();enabled(false);worker.execute(()->{boolean saved=StatusBarPreferences.save(getApplicationContext(),value);runOnUiThread(()->{if(alive()){if(saved){finish();startActivity(new android.content.Intent(this,ClockEditorActivity.class).putExtra("surfaces",true).putExtra("section","quick_settings"));}else{enabled(true);saveStatus.setText("Could not apply the One UI-style preset.");}}});});});
        shapeControls(surfaces,"Large QS tiles",true);
        shapeControls(surfaces,"Small QS tiles",false);
        surfaces.addView(text("Large QS tiles are the four wide tiles at the top. Small QS tiles are the tiles below brightness. Each group has its own shape. Press Apply to apply.",14,false));
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
            worker.execute(() -> { boolean saved = ClockColorPreferences.saveHidden(getApplicationContext(), requested); runOnUiThread(() -> { if(alive()) saveStatus.setText(saved ? "Visibility saved. Press Apply to apply it." : "Visibility could not be saved."); }); }); });
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
                runOnUiThread(() -> { if (alive()) saveStatus.setText(saved ? "Seconds selection saved. Press Apply to apply it." : "Seconds selection could not be saved."); }); });
        });
        typography.addView(text("Seconds update using the stock clock while awake. They can use more power and status-bar space.", 14, false));
        typography.addView(text("Size follows your stock font scaling. Press Apply to apply your saved size and weight with the selected color.", 14, false));
        LinearLayout dateFormats=editorPanel("status_bar","date","Date format");
        dateChoices(dateFormats,"Date format",ClockDatePolicy.LABELS,0);
        dateChoices(dateFormats,"Date position",ClockDatePolicy.POSITIONS,1);
        dateChoices(dateFormats,"Date separator",ClockDatePolicy.SEPARATOR_LABELS,2);
        dateChoices(dateFormats,"Date letter case",ClockDatePolicy.CASE_LABELS,3);
        dateFormats.addView(text("Date uses your stock locale and timezone. Position, separator and letter case affect only the added date; time keeps its stock format. Long dates add width. Press Apply to apply your saved choices.",14,false));
        String initial=state==null?getIntent().getStringExtra("section"):state.getString("editor_section");if(initial==null)initial=getIntent().getBooleanExtra("surfaces",false)?"quick_settings":"status_bar";
        pages.show(initial,state==null?"":state.getString("editor_page",""));
        LinearLayout controls = card("Apply your style");
        ((android.view.ViewGroup)saveStatus.getParent()).removeView(saveStatus);controls.addView(saveStatus);
        controls.addView(text("Apply affects only this page. Other applied areas stay as they are; their drafts wait until you enable them. Restore resets only this page.", 15, false));
        controls.addView(text("Requires root and the updated recovery guard. Your enabled color resumes after wake or reboot. The guard Action independently turns it off.", 14, false));
        previewStatus = text(getSharedPreferences("recovery", MODE_PRIVATE).getBoolean("restart_recovery_stopped", false)
            ? "Clock color was stopped after repeated System UI restarts. Enable again only after checking recovery."
            : "Check customization to read the current state. Restore stock turns off enabled customization.", 14, false);
        MaterialButton details=button("Show technical details");controls.addView(details);previewStatus.setVisibility(View.GONE);controls.addView(previewStatus);details.setOnClickListener(v->{boolean show=previewStatus.getVisibility()!=View.VISIBLE;previewStatus.setVisibility(show?View.VISIBLE:View.GONE);details.setText(show?"Hide technical details":"Show technical details");});
        enable = button("Enable customization");enable.setTag("editor-enable"); controls.addView(enable);
        check = button("Check customization"); controls.addView(check);
        restore = button("Restore stock appearance");restore.setTag("editor-restore"); controls.addView(restore);
        enable.setOnClickListener(v -> applyScope(false));
        check.setOnClickListener(v -> request("status"));
        restore.setOnClickListener(v -> applyScope(true));
        pages.onChange(()->{enable.setText("Apply "+pages.title());restore.setText("Restore");});
        pages.layout(enable,restore,saveStatus,(View)controls.getParent());
        previewStatus.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int before,int count){saveStatus.setText(s.toString().split("\\n",2)[0]);}public void afterTextChanged(android.text.Editable e){}});
        redraw();
        swatch.setLetterSpacing(spacing / 100f);
        swatch.setTextSize(44 * (sizePercent == 0 ? 1f : sizePercent / 100f));
        swatch.setTypeface(android.graphics.Typeface.create(ClockStylePolicy.familyName(fontFamily), (bold ? android.graphics.Typeface.BOLD : 0) | (italic ? android.graphics.Typeface.ITALIC : 0)));
    }
    private StatusBarStyle currentStatusBar() { return new StatusBarStyle(statusMask,systemScale,notificationScale,batteryScale,systemOpacity,notificationOpacity,batteryOpacity,hiddenSlots,systemTransform,notificationTransform,batteryTransform,fanAppearance,pumpAppearance,refreshAppearance,currentSurfaces()); }
    private void redrawIcon(){iconSelection.setText("Mobile service icon color: "+ClockColorPolicy.hex(iconColor));iconSelection.setTextColor(iconColor);for(int n=0;n<iconPresets.getChildCount();n++){Chip c=(Chip)iconPresets.getChildAt(n);c.setChecked((Integer)c.getTag()==iconColor);}}
    private void chooseIcon(int color){if(!ClockColorPolicy.valid(color))return;iconColor=color;redrawIcon();refreshEditorPreviews();worker.execute(()->{boolean saved=StatusBarPreferences.saveColor(getApplicationContext(),color);runOnUiThread(()->{if(alive())saveStatus.setText(saved?"Icon color saved. Press Apply to apply it.":"Icon color could not be saved.");});});}
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
        worker.execute(()->{boolean saved=StatusBarPreferences.save(getApplicationContext(),value);runOnUiThread(()->{if(alive())saveStatus.setText(saved?"Style choices saved. Press Apply to apply.":"Status-bar selections could not be saved.");});});
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
            runOnUiThread(() -> { if (alive()) saveStatus.setText(saved ? "Style saved. Press Apply to apply it." : "Style could not be saved."); }); });
        swatch.setTypeface(android.graphics.Typeface.create(ClockStylePolicy.familyName(fontFamily), (bold ? android.graphics.Typeface.BOLD : 0) | (italic ? android.graphics.Typeface.ITALIC : 0)));
        swatch.setLetterSpacing(spacing / 100f);
    }
    private void saveStyle() {
        refreshEditorPreviews();
        final int requestedSize = sizePercent; final boolean requestedBold = bold, requestedSeconds = seconds;
        worker.execute(() -> { boolean saved = ClockColorPreferences.saveStyle(getApplicationContext(), requestedSize, requestedBold);
            runOnUiThread(() -> { if (alive()) saveStatus.setText(saved ? "Style saved. Press Apply to apply it." : "Style could not be saved."); }); });
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
                    ? "Selection saved. Press Apply to apply it."
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
    private void applyScope(boolean restoring) {
        final String section=pages.section(),page=pages.page(),title=pages.title();
        final var draft=new AppearanceScope.Snapshot(currentStatusBar(),iconColor,dateMode,
            new ClockSnapshot(selected,sizePercent,bold,seconds,italic,spacing,fontFamily,hidden));
        enabled(false);previewStatus.setVisibility(View.VISIBLE);previewStatus.setText((restoring?"Restoring ":"Enabling ")+title+"…");
        worker.execute(()->{
            boolean allowed=false;try{
                String id=GuardReader.inspectEnabled();boolean authority=RecoveryStore.enabledAllowed(getApplicationContext(),id);
                var active=authority?new AppearanceScope.Snapshot(StatusBarPreferences.active(getApplicationContext(),id),
                    StatusBarPreferences.activeColor(getApplicationContext(),id),StatusBarPreferences.activeDate(getApplicationContext(),id),
                    StatusBarPreferences.activeClock(getApplicationContext(),id)):AppearanceScope.Snapshot.STOCK;
                var result=AppearanceScope.merge(section,page,active,restoring?AppearanceScope.Snapshot.STOCK:draft);
                if(restoring&&!authority)allowed=true;
                else allowed=(authority||RootGuardCommand.enableClock(android.os.SystemClock.elapsedRealtime()+8000))
                    &&StatusBarPreferences.activate(getApplicationContext(),GuardReader.inspectEnabled(),result.bar(),result.color(),result.date(),result.clock())
                    &&RecoveryStore.allowEnabled(getApplicationContext());
            }catch(RuntimeException unavailable){}
            final boolean done=allowed;runOnUiThread(()->{if(!alive())return;if(!done){enabled(true);previewStatus.setText("Could not apply this page. Check root, Recovery Guard and Vector.");return;}
                client.enableSavedAppearance(result->{if(alive()){enabled(true);previewStatus.setText(title+(restoring?" restored. ":" enabled. ")+"Other pages unchanged.\n"+result);}});
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
        ColorPickerDialog.show(this,forIcons?"Custom mobile service icon color":"Custom clock color","12:34",forIcons?iconColor:selected,color->{if(alive()){if(forIcons)chooseIcon(color);else choose(color);}});
    }
    private LinearLayout detailGroup(LinearLayout parent,String title,String summary){
        LinearLayout group=new LinearLayout(this);group.setOrientation(LinearLayout.VERTICAL);group.setPadding(dp(12),dp(8),dp(12),dp(8));
        GradientDrawable background=new GradientDrawable();background.setColor(getColor(R.color.surface_container_high));background.setCornerRadius(dp(22));group.setBackground(background);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(10);parent.addView(group,params);
        MaterialButton open=new MaterialButton(this);open.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));open.setTextColor(getColor(R.color.on_surface));open.setAllCaps(false);open.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);open.setText(title+"  ›");open.setContentDescription(title+". "+summary+". Expand settings");group.addView(open,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setVisibility(View.VISIBLE);group.addView(body);open.setOnClickListener(v->{boolean show=body.getVisibility()!=View.VISIBLE;body.setVisibility(show?View.VISIBLE:View.GONE);open.setText(title+(show?"  ⌃":"  ›"));open.setContentDescription(title+". "+(show?"Collapse":"Expand")+" settings");});return body;
    }
    private LinearLayout editorPanel(String section,String page,String title){LinearLayout box=card(title);pages.register(section,page,title,(View)box.getParent());return box;}
    private void editorOverview(String section,String title,String[][] items){
        LinearLayout preview=editorPanel(section,"preview",title+" preview");
        EditorPreview artwork=new EditorPreview(this,section,()->new EditorPreview.Draft(currentStatusBar(),selected,iconColor,sizePercent,bold,italic,hidden,fontFamily,spacing/100f));
        editorPreviews.add(artwork);preview.addView(artwork,new LinearLayout.LayoutParams(-1,dp(section.equals("lock_screen")?160:section.equals("quick_settings")?130:section.equals("volume")?140:section.equals("notifications")?130:80)));
        TextView previewHint=text("Draft preview · tap title to collapse",12,false);preview.addView(previewHint);
        preview.getChildAt(0).setOnClickListener(v->{boolean show=artwork.getVisibility()!=View.VISIBLE;artwork.setVisibility(show?View.VISIBLE:View.GONE);previewHint.setVisibility(show?View.VISIBLE:View.GONE);});
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
                final int requested=dateMode;worker.execute(()->{boolean saved=StatusBarPreferences.saveDate(getApplicationContext(),requested);runOnUiThread(()->{if(alive())saveStatus.setText(saved?"Date choices saved. Press Apply to apply them.":"Date choices could not be saved.");});});
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
        parent.addView(text("Choose an icon on its own page, then press Apply customization to apply. The icon follows stock visibility, including face unlock.",14,false));
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
    @Override protected void onActivityResult(int request,int result,android.content.Intent data){super.onActivityResult(request,result,data);if(request==PICK_QS_IMAGE&&result==RESULT_OK&&data!=null&&data.getData()!=null){android.net.Uri uri=data.getData();saveStatus.setText("Importing background…");worker.execute(()->{String hash=null,error=null;try{hash=QsImageStore.importImage(getApplicationContext(),uri);}catch(Exception failure){error=failure.getMessage();}String imported=hash,problem=error;android.graphics.Bitmap imagePreview=hash==null?null:android.graphics.BitmapFactory.decodeFile(QsImageStore.file(getApplicationContext(),hash).toString());runOnUiThread(()->{if(!alive())return;if(imported!=null){qsTheme=qsTheme.withImage(imported);qsThemeEditor.image(imagePreview,imported);for(EditorPreview preview:editorPreviews)preview.qsImage(imagePreview,imported);saveStatusBar();}else saveStatus.setText("Background import failed: "+problem);});});}if(request==PICK_CLOCK_FONT&&result==RESULT_OK&&data!=null){int value=data.getIntExtra("selection",-1);if(LockClockFonts.valid(value)){lockClockFont=value;redrawClockFont();saveStatusBar();}}if(request==PICK_ANIMATION&&result==RESULT_OK&&data!=null){int value=data.getIntExtra("selection",-1);if(FingerprintAnimations.valid(value)){fingerprintAnimation=value;animationPicker.setText("Fingerprint animation · "+FingerprintAnimations.NAMES[value]);saveStatusBar();}}if(request==PICK_FINGERPRINT&&result==RESULT_OK&&data!=null){int value=data.getIntExtra("selection",-1);if(FingerprintStyles.valid(value)){fingerprintStyle=value;redrawFingerprint();saveStatusBar();}}}
    private void gridControls(LinearLayout parent){
        parent.addView(text("Arrange the small tiles below brightness. These choices apply to the portrait paged panel; landscape has its own grid page and large text keeps stock layout. Apply affects the saved grid.",14,false));
        gridChoices(parent,true,new int[]{0,2,3,5},new String[]{"Stock","2 columns","3 columns","5 columns"});
        gridChoices(parent,false,new int[]{0,1,2,4,5,6},new String[]{"Stock","1 row","2 rows","4 rows","5 rows","6 rows"});
        parent.addView(text("Rows are a maximum: the phone may fit fewer when space is limited. Extra rows include matching expansion animations when the height allows. Five columns require enough width for stock-size tiles and spacing; narrower panels keep stock columns. Page count follows your tiles. The large-tile layout has its own page; tile order and tap actions remain native. Restore stock appearance or the guard Action restores the native grid.",14,false));
    }
    private void qsLayoutControls(){
        LinearLayout top=editorPanel("quick_settings","top_layout","Large tile layout");top.addView(text("Change the number and arrangement of native large tiles. Remaining tiles move into the small grid in their original order. Tile size and spacing stay native.",14,false));
        qsOptionCards(top,0,2,new int[]{0,1,2,3},new String[]{"Stock · 4 tiles, 2 × 2","2 tiles · 2 × 1","6 tiles · 2 × 3","4 tiles · 1 × 4"},"qs-top-layout");
        LinearLayout labels=editorPanel("quick_settings","label_layout","Label layout");labels.addView(text("Small tile labels keep the native text and accessibility actions. Large tiles retain their native side labels.",14,false));
        qsOptionCards(labels,2,2,new int[]{0,1,2},new String[]{"Stock · labels below","Labels above icons","Icons only"},"qs-label-position");
        qsOptionCards(labels,4,2,new int[]{0,1,2,3},new String[]{"Stock label lines","1 line","2 lines","3 lines"},"qs-label-lines");
        LinearLayout brightness=editorPanel("quick_settings","brightness_layout","Brightness placement");brightness.addView(text("Move the native brightness row within the expanded QS panel. Its slider, auto-brightness control and touch behavior remain native.",14,false));
        qsOptionCards(brightness,6,2,new int[]{0,1,2},new String[]{"Stock · between tile groups","Above all tiles","Below small tiles"},"qs-brightness-position");
        qsOptionCards(brightness,8,1,new int[]{0,1},new String[]{"Show native sun icons","Hide sun icons"},"qs-brightness-icons");
        LinearLayout land=editorPanel("quick_settings","landscape","Landscape grid");land.addView(text("Independent landscape choices. Stock follows the firmware. Native tile dimensions stay unchanged; wider grids or extra rows apply only when they fit. Large text retains stock geometry.",14,false));
        qsOptionCards(land,9,3,new int[]{0,2,3,4,5,6},new String[]{"Stock columns","2 columns","3 columns","4 columns","5 columns","6 columns"},"qs-landscape-columns");
        qsOptionCards(land,12,3,new int[]{0,1,2,3,4,5,6},new String[]{"Stock rows","1 row","2 rows","3 rows","4 rows","5 rows","6 rows"},"qs-landscape-rows");
    }
    private void qsOptionCards(LinearLayout parent,int shift,int bits,int[] values,String[] names,String tag){
        if(tag.equals("qs-landscape-columns")||tag.equals("qs-landscape-rows")||tag.equals("qs-top-layout")||tag.equals("qs-label-lines")){int mask=(1<<bits)-1,current=(qsOptions>>shift)&mask,index=0;for(int i=0;i<values.length;i++)if(values[i]==current)index=i;
            TextView label=text(names[index],16,true);parent.addView(label);Slider slider=new Slider(this);slider.setValueFrom(0);slider.setValueTo(values.length-1);slider.setStepSize(1);slider.setValue(index);slider.setLabelFormatter(v->names[(int)v]);slider.setTag(tag+"-slider");parent.addView(slider);statusControls.add(slider);slider.addOnChangeListener((v,n,user)->{{int i=(int)n;qsOptions=(qsOptions&~(mask<<shift))|(values[i]<<shift);label.setText(names[i]);saveStatusBar();}});return;}

        java.util.List<MaterialCardView> cards=new java.util.ArrayList<>();int mask=(1<<bits)-1;
        for(int i=0;i<values.length;i++){final int value=values[i];MaterialCardView card=new MaterialCardView(this);PersonalizationUi.panel(card);card.setCardBackgroundColor(getColor(R.color.surface_container_high));card.setRadius(dp(22));card.setClickable(true);card.setFocusable(true);card.setTag(tag+":"+value);cards.add(card);statusControls.add(card);TextView label=text(names[i],17,true);label.setPadding(dp(20),dp(18),dp(20),dp(18));label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);card.addView(label);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(0,dp(5),0,dp(5));parent.addView(card,params);card.setStrokeColor(getColor(R.color.primary));
            card.setOnClickListener(v->{qsOptions=(qsOptions&~(mask<<shift))|(value<<shift);for(int k=0;k<cards.size();k++){boolean chosen=values[k]==value;cards.get(k).setStrokeWidth(dp(chosen?3:0));cards.get(k).setSelected(chosen);cards.get(k).setContentDescription(names[k]+(chosen?", selected":""));}saveStatusBar();});
        }
        for(int i=0;i<cards.size();i++){boolean chosen=values[i]==((qsOptions>>shift)&mask);cards.get(i).setStrokeWidth(dp(chosen?3:0));cards.get(i).setSelected(chosen);cards.get(i).setContentDescription(names[i]+(chosen?", selected":""));}
    }
    private void gridChoices(LinearLayout parent,boolean column,int[] unused,String[] names){
        int nativeValue=column?4:3,stored=column?qsColumns:qsRows;TextView label=text((column?"Columns: ":"Rows: ")+(stored==0?"Native ("+nativeValue+")":stored),16,true);parent.addView(label);
        Slider slider=new Slider(this);slider.setValueFrom(column?2:1);slider.setValueTo(column?5:6);slider.setStepSize(1);slider.setValue(stored==0?nativeValue:stored);slider.setTag(column?"qs-columns-slider":"qs-rows-slider");parent.addView(slider);statusControls.add(slider);
        slider.addOnChangeListener((v,value,user)->{{int n=(int)value;if(column)qsColumns=n==nativeValue?0:n;else qsRows=n==nativeValue?0:n;label.setText((column?"Columns: ":"Rows: ")+n);saveStatusBar();}});
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
        parent.addView(text("Style the brightness track and its drag mirror. Thin keeps the full touch area. Press Apply to apply.",14,false));
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
        parent.addView(text("Choose a look for compatible sliders in the volume-button panel. Audio levels and controls stay stock. Press Apply to apply.",14,false));
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
        controls.addView(text("Press Apply to apply. Size shares the Icons & battery setting. Text only always displays its percentage; Hidden hides the whole battery. Charging can temporarily replace an inside percentage. Warning and unsupported charging states retain REDMAGIC indicators.",14,false));
    }
    private void notificationIconControls(LinearLayout parent){
        parent.addView(text("Control notification icons in the top status bar. The count replaces the overflow dot with the number of additional notification icons, including the icon represented by the dot. Grouped notifications follow Android’s icon grouping. Notifications in the shade are unchanged.",14,false));
        var count=new com.google.android.material.materialswitch.MaterialSwitch(this);count.setTag("notification-overflow-count");count.setText("Show overflow count");count.setChecked(notificationCount!=0);parent.addView(count);statusControls.add(count);count.setOnCheckedChangeListener((v,on)->{notificationCount=on?1:0;saveStatusBar();});
        parent.addView(text("Maximum icon slots",16,true));ChipGroup limits=new ChipGroup(this);limits.setSingleSelection(true);parent.addView(limits);for(int i=0;i<=8;i++){final int value=i;Chip chip=new Chip(this);chip.setId(View.generateViewId());chip.setTag("notification-limit:"+i);chip.setText(i==0?"Stock":Integer.toString(i));chip.setCheckable(true);chip.setChecked(notificationLimit==i);limits.addView(chip);statusControls.add(chip);chip.setOnClickListener(v->{notificationLimit=value;saveStatusBar();});}
        parent.addView(text("Overflow uses one slot. Available space and REDMAGIC may reduce the number further. Existing Hide notification icons takes priority. Lock screen, AOD and notification shelf retain stock behavior. Press Apply to apply.",14,false));
    }
    private void batteryStyleControls(LinearLayout parent){
        parent.addView(text("Choose a battery design inspired by RisingOS. Previews show sample low, normal and charging states. Styles follow your real battery level and native colors. Press Apply to apply.",14,false));
        parent.addView(text("Designs fit inside your current status-bar space. External percentage and charge indicators keep their stock positions. Text only replaces the battery group with its percentage; Hidden hides its graphics while keeping the space.",14,false));
        java.util.List<MaterialCardView> choices=new java.util.ArrayList<>();LinearLayout row=null;boolean stacked=getResources().getConfiguration().fontScale>1.3f||getResources().getConfiguration().screenWidthDp<360;
        for(int i=0;i<BatteryStyles.NAMES.length;i++){final int value=i;if(stacked||i%2==0){row=new LinearLayout(this);parent.addView(row);}MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(24));card.setCardElevation(0);card.setCardBackgroundColor(getColor(R.color.surface_container_high));card.setClickable(true);card.setFocusable(true);card.setTag("battery-style:"+i);choices.add(card);statusControls.add(card);
            LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setGravity(Gravity.CENTER);body.setPadding(dp(10),dp(12),dp(10),dp(12));body.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);body.addView(new BatteryStylePreview(this,i),new LinearLayout.LayoutParams(-1,dp(78)));TextView title=text(BatteryStyles.NAMES[i],16,true);title.setGravity(Gravity.CENTER);body.addView(title);card.addView(body);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(stacked?-1:0,-2,stacked?0:1);params.setMargins(dp(4),dp(4),dp(4),dp(4));row.addView(card,params);
            card.setOnClickListener(v->{batteryStyle=value;for(int n=0;n<choices.size();n++){MaterialCardView option=choices.get(n);option.setStrokeWidth(dp(n==value?3:0));option.setStrokeColor(getColor(R.color.primary));option.setSelected(n==value);option.setContentDescription(BatteryStyles.NAMES[n]+(n==value?", selected":""));}saveStatusBar();});
        }
        for(int n=0;n<choices.size();n++){MaterialCardView option=choices.get(n);option.setStrokeWidth(dp(n==batteryStyle?3:0));option.setStrokeColor(getColor(R.color.primary));option.setSelected(n==batteryStyle);option.setContentDescription(BatteryStyles.NAMES[n]+(n==batteryStyle?", selected":""));}
    }
    private void iconPackControls(LinearLayout parent,boolean quickSettings){
        SystemIconPacks.resources(getResources());
        parent.addView(text("Choose icons for this area only. Your separate Wi-Fi and mobile-signal styles take priority in the Status bar. Android keeps control of state and tint; unsupported icons stay stock. Press Apply to apply.",14,false));
        java.util.List<MaterialCardView> choices=new java.util.ArrayList<>();LinearLayout row=null;
        boolean stacked=getResources().getConfiguration().fontScale>1.3f||getResources().getConfiguration().screenWidthDp<360;
        for(int i=0;i<SystemIconPacks.NAMES.length;i++){final int value=i;
            if(stacked||i%2==0){row=new LinearLayout(this);parent.addView(row);}
            MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(24));card.setCardElevation(0);card.setCardBackgroundColor(getColor(R.color.surface_container_high));card.setClickable(true);card.setFocusable(true);card.setTag((quickSettings?"qs-iconpack-style:":"iconpack-style:")+i);statusControls.add(card);choices.add(card);
            LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setGravity(Gravity.CENTER);body.setPadding(dp(12),dp(12),dp(12),dp(12));body.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            body.addView(new SystemIconPackPreview(this,i),new LinearLayout.LayoutParams(-1,dp(100)));
            TextView title=text(SystemIconPacks.NAMES[i],16,true);title.setGravity(Gravity.CENTER);body.addView(title);card.addView(body);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(stacked?-1:0,-2,stacked?0:1);cp.setMargins(dp(4),dp(4),dp(4),dp(4));row.addView(card,cp);
            card.setOnClickListener(v->{if(quickSettings)qsIconPack=value;else iconPack=value;for(int n=0;n<choices.size();n++){MaterialCardView c=choices.get(n);c.setStrokeWidth(dp(n==value?3:0));c.setStrokeColor(getColor(R.color.primary));c.setSelected(n==value);c.setContentDescription(SystemIconPacks.NAMES[n]+(n==value?", selected":""));}saveStatusBar();});
        }
        for(int n=0;n<choices.size();n++){MaterialCardView c=choices.get(n);c.setStrokeWidth(dp(n==(quickSettings?qsIconPack:iconPack)?3:0));c.setStrokeColor(getColor(R.color.primary));c.setSelected(n==(quickSettings?qsIconPack:iconPack));c.setContentDescription(SystemIconPacks.NAMES[n]+(n==(quickSettings?qsIconPack:iconPack)?", selected":""));}
    }
    private void connectivityControls(LinearLayout parent){
        ConnectivityStyles.resources(getResources());
        parent.addView(text("Choose separate Wi-Fi and mobile-signal artwork. Signal strength, SIMs and connectivity remain controlled by Android. Warning and unsupported icons keep stock appearance. Press Apply to apply.",14,false));
        parent.addView(text("Wi-Fi",18,true));visualStyleChoices(parent,"wifi");parent.addView(text("Mobile signal",18,true));visualStyleChoices(parent,"signal");
    }
    private int visualSelection(String kind){return kind.equals("wifi")?wifiStyle:kind.equals("signal")?signalStyle:powerStyle;}
    private void visualStyleChoices(LinearLayout parent,String kind){
        boolean power=kind.equals("power"),wifi=kind.equals("wifi");String[] names=power?NotificationEffects.NAMES:ConnectivityStyles.NAMES;
        if(power)parent.addView(text("Style the stock power and restart button backgrounds. Power actions, confirmations and lock-screen security stay controlled by REDMAGIC. Press Apply to apply.",14,false));
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
        parent.addView(text("Adjust the compatible card background without fading text or actions. Corner clipping adds rounding inside the stock shape; grouped and newer vendor surfaces may keep stock. Press Apply to apply.",14,false));
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
        parent.addView(text("Adds a second line beneath the stock charging message while plugged in and awake: measured battery voltage, battery current and temperature when available. The existing charging type and percentage remain above it. Press Apply to apply.",14,false));
        parent.addView(text("Example · Battery 4.420 V · +2,350 mA · 36.5 °C",16,true));parent.addView(text("Battery voltage is measured at the battery, not the USB charger. Current sign follows the device sensor. Readings follow native charging updates; there is no background polling.",14,false));
    }
    private void notificationStyleControls(LinearLayout parent){
        parent.addView(text("Choose a background treatment for notification cards. Text, actions and gestures follow Android. Press Apply to apply.",14,false));
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
        String[] names={"Stock","Square","Rounded square","Leaf","Pill"};
        java.util.List<MaterialCardView> cards=new java.util.ArrayList<>();
        final Slider[] roundRef=new Slider[1];final TextView[] roundLabelRef=new TextView[1];
        boolean stacked=getResources().getConfiguration().fontScale>1.3f||getResources().getConfiguration().screenWidthDp<360;
        LinearLayout row=null;
        for(int i=0;i<names.length;i++){
            final int value=i;
            if(stacked||i%2==0){row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);parent.addView(row,new LinearLayout.LayoutParams(-1,-2));}
            MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(24));card.setCardElevation(0);card.setCardBackgroundColor(getColor(R.color.surface_container_high));card.setClickable(true);card.setFocusable(true);card.setTag("tile-shape:"+(large?"large:":"small:")+value);
            LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setGravity(Gravity.CENTER);body.setPadding(dp(14),dp(16),dp(14),dp(10));body.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            View sample=new View(this);GradientDrawable shape=new GradientDrawable();shape.setColor(getColor(R.color.primary));
            if(i==3)shape.setCornerRadii(new float[]{dp(22),dp(22),dp(2),dp(2),dp(22),dp(22),dp(2),dp(2)});else shape.setCornerRadius(dp(i==1?2:i==2?12:i==4?24:28));sample.setBackground(shape);body.addView(sample,new LinearLayout.LayoutParams(dp(large?90:48),dp(48)));
            TextView label=text(names[i],14,false);label.setGravity(Gravity.CENTER);body.addView(label);card.addView(body);cards.add(card);statusControls.add(card);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(stacked?-1:0,-2,stacked?0:1);params.setMargins(dp(4),dp(4),dp(4),dp(4));row.addView(card,params);
            card.setOnClickListener(v->{if(large)largeTileShape=value;else tileShape=value;for(int n=0;n<cards.size();n++){MaterialCardView option=cards.get(n);boolean checked=n==value;option.setStrokeWidth(dp(checked?3:0));option.setStrokeColor(getColor(R.color.primary));option.setSelected(checked);option.setContentDescription(names[n]+(checked?", selected":""));}if(roundRef[0]!=null){roundRef[0].setValue(0);roundLabelRef[0].setText("Custom roundness: off");}saveStatusBar();});
        }
        int selection=large?largeTileShape:tileShape;for(int i=0;i<cards.size();i++){MaterialCardView card=cards.get(i);card.setStrokeWidth(dp(i==selection?3:0));card.setStrokeColor(getColor(R.color.primary));card.setSelected(i==selection);card.setContentDescription(title+", "+names[i]+(i==selection?", selected":""));}
        int currentShape=large?largeTileShape:tileShape;int custom0=currentShape>=100?currentShape-100:0;
        TextView roundLabel=text(custom0==0?"Custom roundness: off":"Custom roundness: "+custom0+"%",15,true);parent.addView(roundLabel);roundLabelRef[0]=roundLabel;
        Slider round=new Slider(this);round.setValueFrom(0);round.setValueTo(50);round.setStepSize(2);round.setValue(custom0);parent.addView(round);statusControls.add(round);roundRef[0]=round;
        round.addOnChangeListener((sl,value,fromUser)->{if(!fromUser)return;int v=(int)value;
            if(v>0){if(large)largeTileShape=100+v;else tileShape=100+v;for(MaterialCardView c:cards){c.setStrokeWidth(dp(0));c.setSelected(false);}}
            else{if(large)largeTileShape=0;else tileShape=0;for(int n=0;n<cards.size();n++){MaterialCardView c=cards.get(n);boolean ck=n==0;c.setStrokeWidth(dp(ck?3:0));c.setStrokeColor(getColor(R.color.primary));c.setSelected(ck);}}
            roundLabel.setText(v==0?"Custom roundness: off":"Custom roundness: "+v+"%");saveStatusBar();});
    }
    private SurfaceStyle currentSurfaces(){return new SurfaceStyle(surfaceHidden,surfaceAppearance[0],surfaceAppearance[1],surfaceAppearance[2],surfaceAppearance[3],surfacePosition[0],surfacePosition[1],surfacePosition[2],surfacePosition[3],tileShape,largeTileShape,widgetsHidden,fingerprintStyle,fingerprintHidden,lockClockFont,fingerprintAnimation,qsStyle,brightnessStyle,volumeStyle,notificationStyle,wifiStyle,signalStyle,powerStyle,notificationAppearance,notificationBehavior,chargingInfo,batteryStyle,batteryPercent,batteryCharge,notificationCount,notificationLimit,qsColumns,qsRows,qsOptions,iconPack,qsIconPack,qsTheme);}
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
