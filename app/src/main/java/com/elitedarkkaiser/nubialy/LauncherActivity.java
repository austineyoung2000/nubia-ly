package com.elitedarkkaiser.nubialy;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.concurrent.*;

/** One launcher draft, explicit apply and reversible drawing; native grid changes use the launcher’s own controller. */
public final class LauncherActivity extends AppCompatActivity {
    static final String PACKAGE="com.zte.mifavor.launcher";
    static final String SETTINGS="com.android.launcher3.settings.SettingsActivityMFV";
    static final String RESEARCH_VERSION="16.0.010.000.2604151532";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private TextView status,details,scaleLabel;
    private MaterialButton open,enable,restore;
    private SeekBar scale;
    private MaterialSwitch hide;
    private EditorPages pages;
    private LauncherPreview preview;
    private LauncherServiceClient client;
    private boolean closed,busy,compatible,gridPending;
    private int draftScale=100;
    private boolean draftHide;
    private int draftColor;
    private TextView labelSelection;
    private MaterialButton customColor;
    private com.google.android.material.chip.ChipGroup labelPresets;
    private int customLabelColor=ClockColorPolicy.DEFAULT;
    private MaterialSwitch nativeColor;
    private int draftFolderBlur=-1,draftFolderLabels,draftFolderTitle;
    private SeekBar folderBlur;private MaterialSwitch nativeBlur;private TextView folderBlurLabel;
    private FolderColor folderLabels,folderTitle;private LauncherFolderPreview folderPreview;
    private String draftPack="";private int draftShape;private TextView packSelection,packSummary,shapeHint;
    private MaterialButton choosePack,refreshPacks;private final java.util.List<MaterialButton> shapeChoices=new java.util.ArrayList<>();
    private LinearLayout iconSamples;private long iconPreviewGeneration;
    private LauncherStyle.Layout draftLayout=LauncherStyle.Layout.STOCK;
    private final java.util.List<View> layoutControls=new java.util.ArrayList<>();
    private View indicatorPreview;
    private String diagnostic="";
    private LauncherStyle draftStyle(){return new LauncherStyle(draftScale,draftHide,draftColor,draftFolderBlur,draftFolderLabels,draftFolderTitle,draftPack,draftShape,draftLayout);}
    private final class FolderColor {
        final MaterialSwitch nativeSwitch;final TextView value;final MaterialButton custom;final boolean title;
        int last=ClockColorPolicy.DEFAULT;
        FolderColor(LinearLayout editor,boolean title){this.title=title;int selected=title?draftFolderTitle:draftFolderLabels;if(selected!=0)last=selected;
            String name=title?"Folder title":"App labels",tag=title?"folder-title":"folder-labels";
            editor.addView(text(name+" color",18));nativeSwitch=new MaterialSwitch(LauncherActivity.this);nativeSwitch.setText("Use native "+name.toLowerCase(java.util.Locale.ROOT)+" color");nativeSwitch.setTag(tag+"-native");nativeSwitch.setChecked(selected==0);editor.addView(nativeSwitch);
            value=text("",16);value.setTag(tag+"-selection");editor.addView(value);custom=button("Custom color");custom.setTag(tag+"-custom");editor.addView(custom);
            custom.setOnClickListener(v->ColorPickerDialog.show(LauncherActivity.this,"Custom "+name.toLowerCase(java.util.Locale.ROOT)+" color",title?"Folder":"App label",last,color->{if(closed||busy)return;last=color;if(nativeSwitch.isChecked())nativeSwitch.setChecked(false);else choose(color);}));
            nativeSwitch.setOnCheckedChangeListener((v,checked)->choose(checked?0:last));
        }
        void choose(int color){if(title)draftFolderTitle=color;else draftFolderLabels=color;mark();saveDraft();}
        void mark(){int color=title?draftFolderTitle:draftFolderLabels;value.setText(color==0?"Native color":ClockColorPolicy.hex(color));value.setTextColor(color==0?getColor(R.color.on_surface):color);}
        void controls(){nativeSwitch.setEnabled(!busy);custom.setEnabled(!busy);}
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LauncherStyle draft=LauncherPreferences.draft(this);draftScale=state==null?draft.iconScale():state.getInt("scale",draft.iconScale());draftHide=state==null?draft.hideLabels():state.getBoolean("hide",draft.hideLabels());
        draftColor=state==null?draft.labelColor():state.getInt("color",draft.labelColor());
        draftFolderBlur=state==null?draft.folderBlur():state.getInt("folder_blur",draft.folderBlur());draftFolderLabels=state==null?draft.folderLabelColor():state.getInt("folder_labels",draft.folderLabelColor());draftFolderTitle=state==null?draft.folderTitleColor():state.getInt("folder_title",draft.folderTitleColor());
        draftPack=state==null?draft.iconPack():state.getString("icon_pack",draft.iconPack());draftShape=state==null?draft.iconShape():state.getInt("icon_shape",draft.iconShape());
        draftLayout=state==null?draft.layout():LauncherStyle.Layout.decode(state.getString("layout",draft.layout().encode()));
        if(draftLayout==null)draftLayout=LauncherStyle.Layout.STOCK;
        if(!draftStyle().valid()){draftScale=100;draftHide=false;draftColor=0;draftFolderBlur=-1;draftFolderLabels=0;draftFolderTitle=0;draftPack="";draftShape=0;}
        customLabelColor=draftColor==0?ClockColorPolicy.DEFAULT:draftColor;
        if(state!=null&&ClockColorPolicy.valid(state.getInt("custom_label_color",customLabelColor)))customLabelColor=state.getInt("custom_label_color",customLabelColor);
        client=new LauncherServiceClient(this);
        ScrollView scroll=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20),dp(16),dp(20),dp(24));scroll.addView(content);setContentView(scroll);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(scroll,(v,insets)->{var bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
        var toolbar=PersonalizationUi.toolbar(this,"Launcher");content.addView(toolbar);pages=new EditorPages(this,toolbar,scroll);
        LinearLayout hero=panel(content,"Your home screen");preview=new LauncherPreview(this);preview.setStyle(draftStyle());hero.addView(preview,new LinearLayout.LayoutParams(-1,dp(150)));hero.addView(text("Draft preview · home-screen shortcuts only",14));pages.register("launcher","preview","Launcher",(View)hero.getParent());
        LinearLayout overview=panel(content,"Home screen");MaterialButton icons=button("Icons & labels");icons.setTag("launcher-icons");overview.addView(icons);icons.setOnClickListener(v->pages.show("launcher","icons"));MaterialButton folders=button("Folders");folders.setTag("launcher-folders");overview.addView(folders);folders.setOnClickListener(v->pages.show("launcher","folders"));MaterialButton artwork=button("Icon packs & shapes");artwork.setTag("launcher-artwork");overview.addView(artwork);artwork.setOnClickListener(v->pages.show("launcher","artwork"));pages.register("launcher","","Launcher",(View)overview.getParent());
        for(String[] route:new String[][]{{"grid","Home-screen grid"},{"dock","Dock icon size"},{"indicator","Page indicator"}}){MaterialButton row=button(route[1]);row.setTag("launcher-"+route[0]);overview.addView(row);row.setOnClickListener(v->pages.show("launcher",route[0]));}
        LinearLayout gridEditor=panel(content,"Home-screen grid");
        MaterialSwitch expanded=new MaterialSwitch(this);expanded.setText("Expanded grid choices");expanded.setTag("launcher-expanded-grid");expanded.setChecked(draftLayout.expandedGrid());gridEditor.addView(expanded);layoutControls.add(expanded);expanded.setOnCheckedChangeListener((v,value)->changeLayout(6,value?1:0));
        gridEditor.addView(text("25 layouts: 4–8 columns and 6–10 rows. Enable this page, then choose a grid below. The launcher moves icons and widgets using its own layout flow.",14));
        MaterialButton gridSettings=button("Choose home-screen grid");gridSettings.setTag("launcher-grid-settings");gridEditor.addView(gridSettings);layoutControls.add(gridSettings);gridSettings.setOnClickListener(v->chooseGrid());
        gridEditor.addView(text("Grid changes are saved by the launcher and may move icons and widgets. Its built-in Icon layout screen still shows two fixed choices. Restore here removes expanded choices; use that screen to return to 4 × 6 or 5 × 6.",14));pages.register("launcher","grid","Home-screen grid",(View)gridEditor.getParent());
        LinearLayout dockEditor=panel(content,"Dock icon size");layoutSlider(dockEditor,"Dock icon size",5,70,100,"launcher-dock-scale");MaterialButton inheritDock=button("Follow home-screen icon size");dockEditor.addView(inheritDock);layoutControls.add(inheritDock);inheritDock.setOnClickListener(v->changeLayout(5,0));dockEditor.addView(text("Native follows your Icons & labels size. Choose a percentage to size dock icons independently. Touch targets and the number of dock slots stay native.",14));pages.register("launcher","dock","Dock icon size",(View)dockEditor.getParent());
        LinearLayout indicatorEditor=panel(content,"Page indicator");indicatorPreview=new View(this){final android.graphics.Paint p=new android.graphics.Paint(3);@Override protected void onDraw(android.graphics.Canvas c){super.onDraw(c);if(draftLayout.hideIndicator())return;float factor=draftLayout.indicatorScale()/100f,spacing=dp(20)*factor;for(int i=0;i<5;i++){p.setColor(draftLayout.indicatorColor()==0?getColor(R.color.primary):draftLayout.indicatorColor());p.setAlpha(i==2?255:90);c.drawCircle(getWidth()/2f+(i-2)*spacing,getHeight()/2f,dp(4)*factor,p);}}};indicatorEditor.addView(indicatorPreview,new LinearLayout.LayoutParams(-1,dp(90)));
        MaterialSwitch indicatorHide=new MaterialSwitch(this);indicatorHide.setText("Hide home-screen page indicator");indicatorHide.setTag("launcher-indicator-hide");indicatorHide.setChecked(draftLayout.hideIndicator());indicatorEditor.addView(indicatorHide);layoutControls.add(indicatorHide);indicatorHide.setOnCheckedChangeListener((v,value)->changeLayout(2,value?1:0));
        layoutSlider(indicatorEditor,"Indicator size",4,50,150,"launcher-indicator-scale");
        MaterialButton indicatorColor=button("Custom indicator color");indicatorColor.setTag("launcher-indicator-color");indicatorEditor.addView(indicatorColor);layoutControls.add(indicatorColor);indicatorColor.setOnClickListener(v->ColorPickerDialog.show(this,"Page indicator color","Dots",draftLayout.indicatorColor()==0?ClockColorPolicy.DEFAULT:draftLayout.indicatorColor(),color->changeLayout(3,color)));
        MaterialButton nativeIndicator=button("Use native indicator color");indicatorEditor.addView(nativeIndicator);layoutControls.add(nativeIndicator);nativeIndicator.setOnClickListener(v->changeLayout(3,0));
        indicatorEditor.addView(text("Preview shows sample pages. Folder page indicators keep their native appearance.",14));pages.register("launcher","indicator","Page indicator",(View)indicatorEditor.getParent());
        LinearLayout editor=panel(content,"Icons & labels");editor.addView(text("Visual icon size",18));scaleLabel=text("",14);editor.addView(scaleLabel);
        scale=new SeekBar(this);scale.setTag("launcher-scale");scale.setMax(30);scale.setProgress(draftScale-70);editor.addView(scale);
        scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int value,boolean user){if(user){draftScale=70+value;mark();}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){saveDraft();}});
        editor.addView(text("70–100% of the launcher's current icon size. Layout, icon positions and touch targets stay unchanged.",14));
        hide=new MaterialSwitch(this);hide.setText("Hide home-screen app labels");hide.setTag("launcher-hide-labels");hide.setChecked(draftHide);editor.addView(hide);hide.setOnCheckedChangeListener((v,checked)->{draftHide=checked;mark();saveDraft();});
        editor.addView(text("Label color",18));
        nativeColor=new MaterialSwitch(this);nativeColor.setText("Use native label color");nativeColor.setTag("launcher-native-label-color");nativeColor.setChecked(draftColor==0);editor.addView(nativeColor);
        labelSelection=text("",16);labelSelection.setTag("launcher-label-selection");editor.addView(labelSelection);
        labelPresets=new com.google.android.material.chip.ChipGroup(this);labelPresets.setSingleSelection(true);editor.addView(labelPresets);
        for(ClockColorPolicy.Preset preset:ClockColorPolicy.PRESETS){
            com.google.android.material.chip.Chip chip=new com.google.android.material.chip.Chip(this);chip.setId(View.generateViewId());chip.setText(preset.name());chip.setCheckable(true);chip.setTag(preset.color());
            android.graphics.drawable.GradientDrawable dot=new android.graphics.drawable.GradientDrawable();dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);dot.setColor(preset.color());dot.setSize(dp(20),dp(20));chip.setChipIcon(dot);chip.setChipIconVisible(true);chip.setOnClickListener(v->chooseLabelColor(preset.color()));labelPresets.addView(chip);
        }
        customColor=button("Custom color");customColor.setTag("launcher-custom-color");editor.addView(customColor);customColor.setOnClickListener(v->ColorPickerDialog.show(this,"Custom label color","App label",customLabelColor,color->{if(!closed&&!busy)chooseLabelColor(color);}));
        editor.addView(text("Choose a preset or open Custom color for hex and RGB controls. Hidden labels stay hidden.",14));
        nativeColor.setOnCheckedChangeListener((v,checked)->{draftColor=checked?0:customLabelColor;mark();saveDraft();});
        MaterialButton defaults=button("Use native appearance");editor.addView(defaults);defaults.setOnClickListener(v->{if(busy)return;draftScale=100;draftHide=false;draftColor=0;nativeColor.setChecked(true);scale.setProgress(30);hide.setChecked(false);mark();saveDraft();});
        editor.addView(text("Selections save a draft. Enable launcher customization to apply. App drawer and widgets retain their native appearance. Folder settings have their own page.",14));pages.register("launcher","icons","Icons & labels",(View)editor.getParent());
        LinearLayout artworkEditor=panel(content,"Icon packs & shapes");artworkEditor.addView(text("Icon shape",18));
        for(int row=0;row<3;row++){LinearLayout choices=new LinearLayout(this);artworkEditor.addView(choices);for(int col=0;col<2;col++){int shape=row*2+col;if(shape>=LauncherIconShapes.NAMES.length)break;MaterialButton choice=button(LauncherIconShapes.NAMES[shape]);choice.setTag("icon-shape-"+shape);choice.setCheckable(true);choice.setContentDescription("Icon shape: "+LauncherIconShapes.NAMES[shape]);
            android.graphics.drawable.Drawable sample=new android.graphics.drawable.Drawable(){final android.graphics.Paint paint=new android.graphics.Paint(3);@Override public void draw(android.graphics.Canvas canvas){paint.setColor(getColor(R.color.primary));canvas.drawPath(LauncherIconShapes.path(shape==0?2:shape,new android.graphics.RectF(getBounds())),paint);}@Override public void setAlpha(int alpha){paint.setAlpha(alpha);}@Override public void setColorFilter(android.graphics.ColorFilter filter){paint.setColorFilter(filter);}@Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}@Override public int getIntrinsicWidth(){return dp(36);}@Override public int getIntrinsicHeight(){return dp(36);}};choice.setIcon(sample);choice.setIconSize(dp(32));choices.addView(choice,new LinearLayout.LayoutParams(0,-2,1));shapeChoices.add(choice);choice.setOnClickListener(v->{if(busy||!draftPack.isEmpty())return;draftShape=shape;mark();saveDraft();loadIconPreview();});}}
        shapeHint=text("",14);shapeHint.setTag("icon-shape-hint");artworkEditor.addView(shapeHint);
        artworkEditor.addView(text("Icon pack",18));packSelection=text("",16);packSelection.setTag("icon-pack-selection");artworkEditor.addView(packSelection);
        choosePack=button("Choose installed icon pack");choosePack.setTag("icon-pack-choose");artworkEditor.addView(choosePack);choosePack.setOnClickListener(v->pickIconPack());refreshPacks=button("Refresh icon packs");artworkEditor.addView(refreshPacks);refreshPacks.setOnClickListener(v->pickIconPack());
        packSummary=text("",14);packSummary.setTag("icon-pack-summary");artworkEditor.addView(packSummary);iconSamples=new LinearLayout(this);artworkEditor.addView(iconSamples);
        MaterialButton nativeArt=button("Use native icons & shape");nativeArt.setTag("icon-artwork-native");artworkEditor.addView(nativeArt);nativeArt.setOnClickListener(v->{if(busy)return;draftPack="";draftShape=0;mark();saveDraft();loadIconPreview();});
        artworkEditor.addView(text("Native icon shapes or matched pack icons apply on home, dock, open folders and the app drawer. Selecting a pack disables shape overrides and preserves its artwork. Badges stay native. Deep shortcuts and the folder Add button retain their own artwork. Packs use static appfilter mappings; unsupported or unmatched icons fall back to native artwork. Supported floating app transitions reuse cached custom artwork; unavailable or unmatched artwork stays native. Enable launcher customization to apply the draft.",14));pages.register("launcher","artwork","Icon packs & shapes",(View)artworkEditor.getParent());
        LinearLayout folderEditor=panel(content,"Folders");folderPreview=new LauncherFolderPreview(this);folderEditor.addView(folderPreview,new LinearLayout.LayoutParams(-1,dp(190)));folderEditor.addView(text("Illustrative folder preview",14));
        folderEditor.addView(text("Background blur",18));nativeBlur=new MaterialSwitch(this);nativeBlur.setTag("folder-blur-native");nativeBlur.setText("Use native folder blur");nativeBlur.setChecked(draftFolderBlur<0);folderEditor.addView(nativeBlur);
        folderBlurLabel=text("",14);folderBlurLabel.setTag("folder-blur-value");folderEditor.addView(folderBlurLabel);folderBlur=new SeekBar(this);folderBlur.setTag("folder-blur");folderBlur.setMax(25);folderBlur.setProgress(draftFolderBlur<0?20:draftFolderBlur);folderEditor.addView(folderBlur);
        folderBlur.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar bar,int value,boolean user){if(user){draftFolderBlur=value;if(nativeBlur.isChecked())nativeBlur.setChecked(false);mark();}}public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){saveDraft();}});
        nativeBlur.setOnCheckedChangeListener((v,checked)->{draftFolderBlur=checked?-1:folderBlur.getProgress();mark();saveDraft();});
        folderEditor.addView(text("0 removes folder blur; 25 retains the native maximum. Live blur follows the folder animation. On launchers using a cached wallpaper background, first use may need a moment and a folder close/reopen; low blur can reveal pixelation. Native dimming remains in place.",14));
        folderLabels=new FolderColor(folderEditor,false);folderTitle=new FolderColor(folderEditor,true);
        MaterialButton nativeFolder=button("Use native folder appearance");nativeFolder.setTag("folder-defaults");folderEditor.addView(nativeFolder);nativeFolder.setOnClickListener(v->{if(busy)return;draftFolderBlur=-1;draftFolderLabels=0;draftFolderTitle=0;nativeBlur.setChecked(true);folderLabels.nativeSwitch.setChecked(true);folderTitle.nativeSwitch.setChecked(true);mark();saveDraft();});
        layoutSlider(folderEditor,"Folder icon size",7,50,100,"launcher-folder-scale");
        layoutChoices(folderEditor,"Folder rows",0,new int[]{0,2,3,4,5,6},"launcher-folder-rows");layoutChoices(folderEditor,"Folder columns",1,new int[]{0,2,3,4,5,6},"launcher-folder-columns");
        folderEditor.addView(text("Rows and columns set the maximum page size; small folders may use fewer cells. Close and reopen a folder after applying. Additional apps stay on further folder pages.",14));
        folderEditor.addView(text("Folder title and app-label colors are independent of home-screen labels. Save a draft here, then enable launcher customization to apply.",14));pages.register("launcher","folders","Folders",(View)folderEditor.getParent());
        LinearLayout actions=panel(content,"Apply & restore");status=text("Checking installed launcher…",15);status.setTag("launcher-status");actions.addView(status);
        enable=button("Enable launcher customization");enable.setTag("launcher-enable");enable.setEnabled(false);actions.addView(enable);enable.setOnClickListener(v->apply(false));
        restore=button("Restore launcher appearance");restore.setTag("launcher-restore");actions.addView(restore);restore.setOnClickListener(v->apply(true));
        MaterialButton check=button("Check launcher runtime");actions.addView(check);check.setOnClickListener(v->client.check(message->{if(!closed)status.setText(message);}));
        actions.addView(text("Enable nubia-ly for Launcher (com.zte.mifavor.launcher) in Vector, then reboot. Existing System UI and Fingerprint scopes remain selected. Emergency stop and global Restore also stop launcher styling.",14));pages.register("launcher","preview","Launcher",(View)actions.getParent());
        LinearLayout nativeSettings=panel(content,"Launcher settings");nativeSettings.addView(text("Open the launcher's own settings for layout, folders and other native options. Changes made there remain managed by the launcher.",14));
        open=button("Open launcher settings");open.setTag("launcher-native-settings");nativeSettings.addView(open);open.setOnClickListener(v->openSettings());pages.register("launcher","","Launcher",(View)nativeSettings.getParent());
        LinearLayout support=panel(content,"Compatibility");MaterialButton technical=button("Technical details");technical.setTag("launcher-details-toggle");support.addView(technical);details=text("",13);details.setTag("launcher-details");support.addView(details);details.setVisibility(state!=null&&state.getBoolean("details")?View.VISIBLE:View.GONE);technical.setOnClickListener(v->details.setVisibility(details.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));pages.register("launcher","","Launcher",(View)support.getParent());
        pages.onChange(()->{enable.setText("Enable "+pages.title());restore.setText("Restore "+pages.title());});
        mark();loadIconPreview();pages.show("launcher",state==null?"":state.getString("page",""));
        worker.execute(()->{boolean allowed=LauncherCompatibility.supported(getApplicationContext());runOnUiThread(()->{if(closed)return;compatible=allowed;controls();status.setText(allowed?"APK verified. Choose styles, enable the Launcher scope in Vector, reboot, then apply.":"Custom controls blocked: the installed APK or device does not match the verified baseline.");});});
    }
    private int layoutValue(int field){return Integer.parseInt(draftLayout.encode().split(",")[field]);}
    private void changeLayout(int field,int value){if(busy)return;String[] fields=draftLayout.encode().split(",");fields[field]=Integer.toString(value);var updated=LauncherStyle.Layout.decode(String.join(",",fields));if(updated==null)return;draftLayout=updated;mark();saveDraft();}
    private void layoutSlider(LinearLayout parent,String title,int field,int minimum,int maximum,String tag){
        TextView label=text("",18);parent.addView(label);
        var slider=new com.google.android.material.slider.Slider(this);slider.setTag(tag);slider.setValueFrom(minimum);slider.setValueTo(maximum);slider.setStepSize(1);slider.setValue(Math.max(minimum,layoutValue(field)));parent.addView(slider);layoutControls.add(slider);
        Runnable update=()->{int value=layoutValue(field);label.setText(title+": "+(value==0?"Follow home-screen size":value+"%"));slider.setValue(Math.max(minimum,value));};sliderUpdates.add(update);update.run();
        slider.addOnChangeListener((view,value,user)->{if(user)changeLayout(field,Math.round(value));});
    }
    private void chooseGrid(){if(busy)return;String[] labels=new String[25];for(int i=0;i<25;i++)labels[i]=(4+i%5)+" × "+(6+i/5);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Home-screen grid · columns × rows").setItems(labels,(dialog,index)->{if(busy)return;gridPending=true;busy=true;controls();status.setText("Requesting native grid change…");client.applyGrid(6+index/5,4+index%5,message->{if(closed)return;gridPending=false;busy=false;controls();status.setText(message);});}).setNegativeButton("Cancel",null).show();
    }
    private void layoutChoices(LinearLayout parent,String title,int field,int[] values,String tag){parent.addView(text(title,18));var group=new com.google.android.material.chip.ChipGroup(this);group.setSingleSelection(true);group.setSelectionRequired(true);parent.addView(group);for(int value:values){var chip=new com.google.android.material.chip.Chip(this);chip.setText(value==0?"Native":field>=4?value+"%":Integer.toString(value));chip.setCheckable(true);chip.setChecked(layoutValue(field)==value);chip.setTag(tag+":"+value);layoutControls.add(chip);group.addView(chip);chip.setOnClickListener(v->changeLayout(field,value));}}
    private final java.util.List<Runnable> sliderUpdates=new java.util.ArrayList<>();
    private void mark(){for(Runnable update:sliderUpdates)update.run();if(indicatorPreview!=null)indicatorPreview.invalidate();for(View view:layoutControls)if(view instanceof MaterialButton button&&button.getTag() instanceof String tag&&tag.startsWith("layout-choice:")){String[] fields=tag.split(":");button.setChecked(layoutValue(Integer.parseInt(fields[1]))==Integer.parseInt(fields[2]));}if(packSelection!=null){packSelection.setText(draftPack.isEmpty()?"Native icons":draftPack);for(int i=0;i<shapeChoices.size();i++){var choice=shapeChoices.get(i);boolean selected=i==draftStyle().effectiveIconShape();choice.setEnabled(true);choice.setChecked(selected);choice.setEnabled(!busy&&draftPack.isEmpty());choice.setAlpha(draftPack.isEmpty()?1f:.45f);choice.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(selected?R.color.primary:R.color.surface_container_high)));choice.setTextColor(getColor(selected?R.color.on_primary:R.color.on_surface));choice.setIconTint(android.content.res.ColorStateList.valueOf(getColor(selected?R.color.on_primary:R.color.primary)));}shapeHint.setText(draftPack.isEmpty()?"Choose a shape for native app icons.":"Shapes are disabled while an icon pack is selected, preserving the pack’s artwork.");}if(folderPreview!=null){folderPreview.setStyle(draftStyle());folderBlurLabel.setText(draftFolderBlur<0?"Native blur":"Blur strength: "+draftFolderBlur+" / 25");folderLabels.mark();folderTitle.mark();}scaleLabel.setText("Icon size: "+draftScale+"%");preview.setStyle(draftStyle());labelSelection.setText(draftColor==0?"Native label color":ClockColorPolicy.hex(draftColor));labelSelection.setTextColor(draftColor==0?getColor(R.color.on_surface):draftColor);for(int i=0;i<labelPresets.getChildCount();i++){var chip=(com.google.android.material.chip.Chip)labelPresets.getChildAt(i);chip.setChecked((Integer)chip.getTag()==draftColor);}}
    private void chooseLabelColor(int color){if(busy||!ClockColorPolicy.valid(color))return;customLabelColor=color;if(nativeColor.isChecked())nativeColor.setChecked(false);else{draftColor=color;mark();saveDraft();}}
    private void saveDraft(){LauncherStyle draft=draftStyle();worker.execute(()->{boolean saved=LauncherPreferences.save(getApplicationContext(),draft);if(!saved)runOnUiThread(()->{if(!closed)status.setText("Draft could not be saved.");});});}
    private void apply(boolean restoring){
        if(busy||!compatible)return;busy=true;controls();LauncherStyle draft=restoring?LauncherStyle.STOCK:draftStyle();final String page=pages.page();status.setText("Applying this launcher page…");
        worker.execute(()->{boolean allowed=false;try{
            String id=GuardReader.inspectEnabled();boolean authority=RecoveryStore.enabledAllowed(getApplicationContext(),id);
            LauncherStyle merged=LauncherStyle.merge(page,authority?LauncherPreferences.active(getApplicationContext()):LauncherStyle.STOCK,draft);
            allowed=(authority||(RootGuardCommand.enableClock(SystemClock.elapsedRealtime()+8000)&&RecoveryStore.allowEnabled(getApplicationContext())))&&LauncherPreferences.activate(getApplicationContext(),merged);
        }catch(RuntimeException unavailable){}if(!allowed)LauncherPreferences.stop(getApplicationContext());boolean result=allowed;runOnUiThread(()->{if(closed)return;busy=false;controls();if(result)refreshLauncher("This page applied. Other launcher pages unchanged. Return home to see the result.");else status.setText("Could not enable. Check root, Recovery Guard and Vector activation.");});});
    }
    private void restore(){if(busy)return;busy=true;controls();worker.execute(()->{boolean stopped=LauncherPreferences.stop(getApplicationContext());runOnUiThread(()->{if(closed)return;busy=false;controls();if(stopped)refreshLauncher("Launcher styling disabled. Native appearance restored on refresh.");else status.setText("Could not save restoration. Use Emergency stop or Recovery Guard Action.");});});}
    private void refreshLauncher(String message){status.setText(message+" Checking launcher…");client.check(reply->{if(!closed)status.setText(message+"\n"+reply);});}
    private void controls(){for(View view:layoutControls)view.setEnabled(!busy&&compatible);choosePack.setEnabled(!busy);refreshPacks.setEnabled(!busy);for(var choice:shapeChoices)choice.setEnabled(!busy&&draftPack.isEmpty());folderBlur.setEnabled(!busy);nativeBlur.setEnabled(!busy);folderLabels.controls();folderTitle.controls();enable.setEnabled(!busy&&compatible);restore.setEnabled(!busy);scale.setEnabled(!busy);hide.setEnabled(!busy);nativeColor.setEnabled(!busy);customColor.setEnabled(!busy);for(int i=0;i<labelPresets.getChildCount();i++)labelPresets.getChildAt(i).setEnabled(!busy);}
    @Override protected void onResume(){super.onResume();inspect();}
    @Override protected void onStop(){client.cancel();if(gridPending){gridPending=false;busy=false;controls();}super.onStop();}
    @Override protected void onDestroy(){closed=true;client.cancel();worker.shutdown();super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle out){out.putString("layout",draftLayout.encode());out.putBoolean("details",details.getVisibility()==View.VISIBLE);out.putInt("scale",draftScale);out.putBoolean("hide",draftHide);out.putInt("color",draftColor);out.putInt("custom_label_color",customLabelColor);out.putInt("folder_blur",draftFolderBlur);out.putInt("folder_labels",draftFolderLabels);out.putInt("folder_title",draftFolderTitle);out.putString("icon_pack",draftPack);out.putInt("icon_shape",draftShape);out.putString("page",pages.page());super.onSaveInstanceState(out);}
    private void inspect(){
        open.setEnabled(false);
        try{var info=getPackageManager().getPackageInfo(PACKAGE,0);var activity=getPackageManager().getActivityInfo(new ComponentName(PACKAGE,SETTINGS),0);boolean allowed=info.applicationInfo!=null&&info.applicationInfo.enabled&&activity.enabled&&activity.exported&&(activity.permission==null||checkSelfPermission(activity.permission)==PackageManager.PERMISSION_GRANTED);open.setEnabled(allowed);
            diagnostic="Package: "+PACKAGE+"\nVersion: "+info.versionName+" ("+info.getLongVersionCode()+")\nVerified baseline: "+RESEARCH_VERSION+" (160000)\nSHA-256: "+LauncherCompatibility.SHA256+"\nNative settings: "+(allowed?"available":"unavailable")+"\nCosmetic hooks require exact APK hash, root guard authority and Launcher Vector scope. No provider writes, database edits or automatic launcher restarts.";
        }catch(PackageManager.NameNotFoundException unavailable){diagnostic="Supported launcher or native settings page not found.";}catch(RuntimeException unavailable){diagnostic="Launcher information unavailable.";}
        details.setText(diagnostic);
    }
    private void pickIconPack(){if(busy||closed)return;packSummary.setText("Finding installed icon packs…");worker.execute(()->{java.util.List<LauncherIconPacks.Pack> found;try{found=LauncherIconPacks.discover(getApplicationContext());}catch(RuntimeException unavailable){found=java.util.List.of();}var packs=found;runOnUiThread(()->{if(closed||busy)return;String[] names=new String[packs.size()+1];names[0]="Native icons";for(int i=0;i<packs.size();i++)names[i+1]=packs.get(i).name();packSummary.setText(packs.isEmpty()?"No icon packs found. Install an ADW/Nova-compatible pack, then refresh.":packs.size()+" installed icon packs");
            var dialog=new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Icon pack").setItems(names,(d,index)->{if(closed||busy)return;String selected=index==0?"":packs.get(index-1).id();if(selected.isEmpty()){chooseIconPack("");return;}packSummary.setText("Checking icon pack…");worker.execute(()->{boolean supported;try{LauncherIconPacks.load(getApplicationContext(),selected);supported=true;}catch(Exception unavailable){supported=false;}boolean valid=supported;runOnUiThread(()->{if(closed||busy)return;if(!valid){packSummary.setText("This pack has no readable static appfilter mappings.");return;}chooseIconPack(selected);});});}).setNegativeButton("Cancel",null).show();getLifecycle().addObserver(new androidx.lifecycle.DefaultLifecycleObserver(){@Override public void onDestroy(androidx.lifecycle.LifecycleOwner owner){dialog.dismiss();owner.getLifecycle().removeObserver(this);}});
        });});}
    void chooseIconPack(String pack){if(closed||busy||!LauncherIconPacks.validPackage(pack))return;draftPack=pack;mark();saveDraft();loadIconPreview();}
    private void loadIconPreview(){long ticket=++iconPreviewGeneration;String pack=draftPack;int shape=draftStyle().effectiveIconShape();iconSamples.removeAllViews();if(pack.isEmpty()){packSummary.setText("Native app icons · "+LauncherIconShapes.NAMES[shape]);return;}packSummary.setText("Loading icon pack preview…");worker.execute(()->{java.util.List<android.graphics.Bitmap> samples=new java.util.ArrayList<>();String summary;try{var loaded=LauncherIconPacks.load(getApplicationContext(),pack);summary=loaded.icons().size()+" static icon mappings · "+LauncherIconShapes.NAMES[shape];for(String component:loaded.icons().keySet()){var drawable=loaded.icon(ComponentName.unflattenFromString(component));if(drawable==null)continue;var source=LauncherIconShapes.raster(drawable,shape!=0);var bitmap=android.graphics.Bitmap.createBitmap(192,192,android.graphics.Bitmap.Config.ARGB_8888);LauncherIconShapes.draw(new android.graphics.Canvas(bitmap),source,new android.graphics.Rect(0,0,192,192),new android.graphics.Paint(3),shape);samples.add(bitmap);if(samples.size()==5)break;}}catch(Exception unavailable){summary="Icon pack unavailable. Choose another pack or Native icons.";}String result=summary;runOnUiThread(()->{if(closed||ticket!=iconPreviewGeneration)return;packSummary.setText(result);for(var bitmap:samples){ImageView sample=new ImageView(this);sample.setImageBitmap(bitmap);sample.setContentDescription("Icon pack preview");iconSamples.addView(sample,new LinearLayout.LayoutParams(0,dp(64),1));}});});}
    private void openSettings(){try{startActivity(new Intent().setComponent(new ComponentName(PACKAGE,SETTINGS)));}catch(RuntimeException unavailable){open.setEnabled(false);status.setText("Launcher settings could not be opened.");}}
    private LinearLayout panel(LinearLayout parent,String title){MaterialCardView card=new MaterialCardView(this);PersonalizationUi.panel(card);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(14);parent.addView(card,params);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(20),dp(20),dp(20));card.addView(body);body.addView(text(title,22));return body;}
    private MaterialButton button(String title){MaterialButton b=new MaterialButton(this);b.setText(title);b.setAllCaps(false);b.setCornerRadius(dp(28));return b;}
    private TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(getColor(size>=17?R.color.on_surface:R.color.on_surface_variant));t.setPadding(0,dp(4),0,dp(10));return t;}
    private int dp(float n){return PersonalizationUi.dp(this,n);}
}
