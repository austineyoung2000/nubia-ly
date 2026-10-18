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

/** One launcher draft, explicit apply and reversible drawing; no launcher settings writes. */
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
    private boolean closed,busy,compatible;
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
    private String diagnostic="";
    private LauncherStyle draftStyle(){return new LauncherStyle(draftScale,draftHide,draftColor,draftFolderBlur,draftFolderLabels,draftFolderTitle);}
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
        if(!draftStyle().valid()){draftScale=100;draftHide=false;draftColor=0;draftFolderBlur=-1;draftFolderLabels=0;draftFolderTitle=0;}
        customLabelColor=draftColor==0?ClockColorPolicy.DEFAULT:draftColor;
        if(state!=null&&ClockColorPolicy.valid(state.getInt("custom_label_color",customLabelColor)))customLabelColor=state.getInt("custom_label_color",customLabelColor);
        client=new LauncherServiceClient(this);
        ScrollView scroll=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20),dp(16),dp(20),dp(24));scroll.addView(content);setContentView(scroll);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(scroll,(v,insets)->{var bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
        var toolbar=PersonalizationUi.toolbar(this,"Launcher");content.addView(toolbar);pages=new EditorPages(this,toolbar,scroll);
        LinearLayout hero=panel(content,"Your home screen");preview=new LauncherPreview(this);preview.setStyle(draftStyle());hero.addView(preview,new LinearLayout.LayoutParams(-1,dp(150)));hero.addView(text("Draft preview · home-screen shortcuts only",14));pages.register("launcher","preview","Launcher",(View)hero.getParent());
        LinearLayout overview=panel(content,"Home screen");MaterialButton icons=button("Icons & labels");icons.setTag("launcher-icons");overview.addView(icons);icons.setOnClickListener(v->pages.show("launcher","icons"));MaterialButton folders=button("Folders");folders.setTag("launcher-folders");overview.addView(folders);folders.setOnClickListener(v->pages.show("launcher","folders"));pages.register("launcher","","Launcher",(View)overview.getParent());
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
        LinearLayout folderEditor=panel(content,"Folders");folderPreview=new LauncherFolderPreview(this);folderEditor.addView(folderPreview,new LinearLayout.LayoutParams(-1,dp(190)));folderEditor.addView(text("Illustrative folder preview",14));
        folderEditor.addView(text("Background blur",18));nativeBlur=new MaterialSwitch(this);nativeBlur.setTag("folder-blur-native");nativeBlur.setText("Use native folder blur");nativeBlur.setChecked(draftFolderBlur<0);folderEditor.addView(nativeBlur);
        folderBlurLabel=text("",14);folderBlurLabel.setTag("folder-blur-value");folderEditor.addView(folderBlurLabel);folderBlur=new SeekBar(this);folderBlur.setTag("folder-blur");folderBlur.setMax(25);folderBlur.setProgress(draftFolderBlur<0?20:draftFolderBlur);folderEditor.addView(folderBlur);
        folderBlur.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar bar,int value,boolean user){if(user){draftFolderBlur=value;if(nativeBlur.isChecked())nativeBlur.setChecked(false);mark();}}public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){saveDraft();}});
        nativeBlur.setOnCheckedChangeListener((v,checked)->{draftFolderBlur=checked?-1:folderBlur.getProgress();mark();saveDraft();});
        folderEditor.addView(text("0 removes the blur from the launcher's small wallpaper copy; 25 is the strongest setting. At low blur the small copy may look pixelated. First use prepares the wallpaper in the background: close and reopen the folder after a moment. Native dimming remains in place.",14));
        folderLabels=new FolderColor(folderEditor,false);folderTitle=new FolderColor(folderEditor,true);
        MaterialButton nativeFolder=button("Use native folder appearance");nativeFolder.setTag("folder-defaults");folderEditor.addView(nativeFolder);nativeFolder.setOnClickListener(v->{if(busy)return;draftFolderBlur=-1;draftFolderLabels=0;draftFolderTitle=0;nativeBlur.setChecked(true);folderLabels.nativeSwitch.setChecked(true);folderTitle.nativeSwitch.setChecked(true);mark();saveDraft();});
        folderEditor.addView(text("Folder title and app-label colors are independent of home-screen labels. Save a draft here, then enable launcher customization to apply.",14));pages.register("launcher","folders","Folders",(View)folderEditor.getParent());
        LinearLayout actions=panel(content,"Apply & restore");status=text("Checking installed launcher…",15);status.setTag("launcher-status");actions.addView(status);
        enable=button("Enable launcher customization");enable.setTag("launcher-enable");enable.setEnabled(false);actions.addView(enable);enable.setOnClickListener(v->apply());
        restore=button("Restore launcher appearance");restore.setTag("launcher-restore");actions.addView(restore);restore.setOnClickListener(v->restore());
        MaterialButton check=button("Check launcher runtime");actions.addView(check);check.setOnClickListener(v->client.check(message->{if(!closed)status.setText(message);}));
        actions.addView(text("Enable nubia-ly for Launcher (com.zte.mifavor.launcher) in Vector, then reboot. Existing System UI and Fingerprint scopes remain selected. Emergency stop and global Restore also stop launcher styling.",14));pages.register("launcher","preview","Launcher",(View)actions.getParent());
        LinearLayout nativeSettings=panel(content,"Launcher settings");nativeSettings.addView(text("Open the launcher's own settings for layout, folders and other native options. Changes made there remain managed by the launcher.",14));
        open=button("Open launcher settings");open.setTag("launcher-native-settings");nativeSettings.addView(open);open.setOnClickListener(v->openSettings());pages.register("launcher","","Launcher",(View)nativeSettings.getParent());
        LinearLayout support=panel(content,"Compatibility");MaterialButton technical=button("Technical details");technical.setTag("launcher-details-toggle");support.addView(technical);details=text("",13);details.setTag("launcher-details");support.addView(details);details.setVisibility(state!=null&&state.getBoolean("details")?View.VISIBLE:View.GONE);technical.setOnClickListener(v->details.setVisibility(details.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));pages.register("launcher","","Launcher",(View)support.getParent());
        mark();pages.show("launcher",state==null?"":state.getString("page",""));
        worker.execute(()->{boolean allowed=LauncherCompatibility.supported(getApplicationContext());runOnUiThread(()->{if(closed)return;compatible=allowed;controls();status.setText(allowed?"APK verified. Choose styles, enable the Launcher scope in Vector, reboot, then apply.":"Custom controls blocked: the installed APK or device does not match the verified baseline.");});});
    }
    private void mark(){if(folderPreview!=null){folderPreview.setStyle(draftStyle());folderBlurLabel.setText(draftFolderBlur<0?"Native blur":"Blur strength: "+draftFolderBlur+" / 25");folderLabels.mark();folderTitle.mark();}scaleLabel.setText("Icon size: "+draftScale+"%");preview.setStyle(draftStyle());labelSelection.setText(draftColor==0?"Native label color":ClockColorPolicy.hex(draftColor));labelSelection.setTextColor(draftColor==0?getColor(R.color.on_surface):draftColor);for(int i=0;i<labelPresets.getChildCount();i++){var chip=(com.google.android.material.chip.Chip)labelPresets.getChildAt(i);chip.setChecked((Integer)chip.getTag()==draftColor);}}
    private void chooseLabelColor(int color){if(busy||!ClockColorPolicy.valid(color))return;customLabelColor=color;if(nativeColor.isChecked())nativeColor.setChecked(false);else{draftColor=color;mark();saveDraft();}}
    private void saveDraft(){LauncherStyle draft=draftStyle();worker.execute(()->{boolean saved=LauncherPreferences.save(getApplicationContext(),draft);if(!saved)runOnUiThread(()->{if(!closed)status.setText("Draft could not be saved.");});});}
    private void apply(){
        if(busy||!compatible)return;busy=true;controls();LauncherStyle draft=draftStyle();status.setText("Enabling launcher customization…");
        worker.execute(()->{boolean allowed=false;try{
            String id=GuardReader.inspectEnabled();boolean authority=RecoveryStore.enabledAllowed(getApplicationContext(),id);
            allowed=LauncherPreferences.save(getApplicationContext(),draft)&&(authority||(RootGuardCommand.enableClock(SystemClock.elapsedRealtime()+8000)&&RecoveryStore.allowEnabled(getApplicationContext())))&&LauncherPreferences.activate(getApplicationContext(),draft);
        }catch(RuntimeException unavailable){}if(!allowed)LauncherPreferences.stop(getApplicationContext());boolean result=allowed;runOnUiThread(()->{if(closed)return;busy=false;controls();if(result)refreshLauncher("Saved and enabled. Return to the home screen to see the result.");else status.setText("Could not enable. Check root, Recovery Guard and Vector activation.");});});
    }
    private void restore(){if(busy)return;busy=true;controls();worker.execute(()->{boolean stopped=LauncherPreferences.stop(getApplicationContext());runOnUiThread(()->{if(closed)return;busy=false;controls();if(stopped)refreshLauncher("Launcher styling disabled. Native appearance restored on refresh.");else status.setText("Could not save restoration. Use Emergency stop or Recovery Guard Action.");});});}
    private void refreshLauncher(String message){status.setText(message+" Checking launcher…");client.check(reply->{if(!closed)status.setText(message+"\n"+reply);});}
    private void controls(){folderBlur.setEnabled(!busy);nativeBlur.setEnabled(!busy);folderLabels.controls();folderTitle.controls();enable.setEnabled(!busy&&compatible);restore.setEnabled(!busy);scale.setEnabled(!busy);hide.setEnabled(!busy);nativeColor.setEnabled(!busy);customColor.setEnabled(!busy);for(int i=0;i<labelPresets.getChildCount();i++)labelPresets.getChildAt(i).setEnabled(!busy);}
    @Override protected void onResume(){super.onResume();inspect();}
    @Override protected void onStop(){client.cancel();super.onStop();}
    @Override protected void onDestroy(){closed=true;client.cancel();worker.shutdown();super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle out){out.putBoolean("details",details.getVisibility()==View.VISIBLE);out.putInt("scale",draftScale);out.putBoolean("hide",draftHide);out.putInt("color",draftColor);out.putInt("custom_label_color",customLabelColor);out.putInt("folder_blur",draftFolderBlur);out.putInt("folder_labels",draftFolderLabels);out.putInt("folder_title",draftFolderTitle);out.putString("page",pages.page());super.onSaveInstanceState(out);}
    private void inspect(){
        open.setEnabled(false);
        try{var info=getPackageManager().getPackageInfo(PACKAGE,0);var activity=getPackageManager().getActivityInfo(new ComponentName(PACKAGE,SETTINGS),0);boolean allowed=info.applicationInfo!=null&&info.applicationInfo.enabled&&activity.enabled&&activity.exported&&(activity.permission==null||checkSelfPermission(activity.permission)==PackageManager.PERMISSION_GRANTED);open.setEnabled(allowed);
            diagnostic="Package: "+PACKAGE+"\nVersion: "+info.versionName+" ("+info.getLongVersionCode()+")\nVerified baseline: "+RESEARCH_VERSION+" (160000)\nSHA-256: "+LauncherCompatibility.SHA256+"\nNative settings: "+(allowed?"available":"unavailable")+"\nCosmetic hooks require exact APK hash, root guard authority and Launcher Vector scope. No provider writes, database edits or automatic launcher restarts.";
        }catch(PackageManager.NameNotFoundException unavailable){diagnostic="Supported launcher or native settings page not found.";}catch(RuntimeException unavailable){diagnostic="Launcher information unavailable.";}
        details.setText(diagnostic);
    }
    private void openSettings(){try{startActivity(new Intent().setComponent(new ComponentName(PACKAGE,SETTINGS)));}catch(RuntimeException unavailable){open.setEnabled(false);status.setText("Launcher settings could not be opened.");}}
    private LinearLayout panel(LinearLayout parent,String title){MaterialCardView card=new MaterialCardView(this);PersonalizationUi.panel(card);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(14);parent.addView(card,params);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(20),dp(20),dp(20));card.addView(body);body.addView(text(title,22));return body;}
    private MaterialButton button(String title){MaterialButton b=new MaterialButton(this);b.setText(title);b.setAllCaps(false);b.setCornerRadius(dp(28));return b;}
    private TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(getColor(size>=17?R.color.on_surface:R.color.on_surface_variant));t.setPadding(0,dp(4),0,dp(10));return t;}
    private int dp(float n){return PersonalizationUi.dp(this,n);}
}
