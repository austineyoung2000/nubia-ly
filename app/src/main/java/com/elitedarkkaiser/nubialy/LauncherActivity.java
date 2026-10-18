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
    private EditText labelHex;
    private MaterialSwitch nativeColor;
    private String diagnostic="";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LauncherStyle draft=LauncherPreferences.draft(this);draftScale=state==null?draft.iconScale():state.getInt("scale",draft.iconScale());draftHide=state==null?draft.hideLabels():state.getBoolean("hide",draft.hideLabels());
        draftColor=state==null?draft.labelColor():state.getInt("color",draft.labelColor());
        if(!new LauncherStyle(draftScale,draftHide,draftColor).valid()){draftScale=100;draftHide=false;draftColor=0;}
        client=new LauncherServiceClient(this);
        ScrollView scroll=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20),dp(16),dp(20),dp(24));scroll.addView(content);setContentView(scroll);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(scroll,(v,insets)->{var bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
        var toolbar=PersonalizationUi.toolbar(this,"Launcher");content.addView(toolbar);pages=new EditorPages(this,toolbar,scroll);
        LinearLayout hero=panel(content,"Your home screen");preview=new LauncherPreview(this);preview.setStyle(new LauncherStyle(draftScale,draftHide,draftColor));hero.addView(preview,new LinearLayout.LayoutParams(-1,dp(150)));hero.addView(text("Draft preview · home-screen shortcuts only",14));pages.register("launcher","preview","Launcher",(View)hero.getParent());
        LinearLayout overview=panel(content,"Home screen");MaterialButton icons=button("Icons & labels");icons.setTag("launcher-icons");overview.addView(icons);icons.setOnClickListener(v->pages.show("launcher","icons"));pages.register("launcher","","Launcher",(View)overview.getParent());
        LinearLayout editor=panel(content,"Icons & labels");editor.addView(text("Visual icon size",18));scaleLabel=text("",14);editor.addView(scaleLabel);
        scale=new SeekBar(this);scale.setTag("launcher-scale");scale.setMax(30);scale.setProgress(draftScale-70);editor.addView(scale);
        scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int value,boolean user){if(user){draftScale=70+value;mark();}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){saveDraft();}});
        editor.addView(text("70–100% of the launcher's current icon size. Layout, icon positions and touch targets stay unchanged.",14));
        hide=new MaterialSwitch(this);hide.setText("Hide home-screen app labels");hide.setTag("launcher-hide-labels");hide.setChecked(draftHide);editor.addView(hide);hide.setOnCheckedChangeListener((v,checked)->{draftHide=checked;mark();saveDraft();});
        editor.addView(text("Label color",18));
        nativeColor=new MaterialSwitch(this);nativeColor.setText("Use native label color");nativeColor.setTag("launcher-native-label-color");nativeColor.setChecked(draftColor==0);editor.addView(nativeColor);
        labelHex=new EditText(this);labelHex.setTag("launcher-label-hex");labelHex.setSingleLine(true);labelHex.setHint("#RRGGBB");labelHex.setContentDescription("Home-screen label color, six hexadecimal digits");labelHex.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);labelHex.setText(draftColor==0?"#D3CBEF":LauncherStyle.labelName(draftColor));labelHex.setEnabled(draftColor!=0);editor.addView(labelHex);
        editor.addView(text("Enter six hex digits, for example #D3CBEF. The preview updates as you type. Hidden labels stay hidden.",14));
        labelHex.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){}public void afterTextChanged(android.text.Editable value){if(nativeColor.isChecked())return;Integer color=LauncherStyle.parseLabelHex(value.toString());labelHex.setError(color==null?"Enter six hex digits, such as #D3CBEF":null);if(color!=null){draftColor=color;mark();saveDraft();}controls();}});
        nativeColor.setOnCheckedChangeListener((v,checked)->{Integer color=LauncherStyle.parseLabelHex(labelHex.getText().toString());if(checked)color=0;labelHex.setError(color==null?"Enter six hex digits, such as #D3CBEF":null);if(color!=null){draftColor=color;mark();saveDraft();}controls();});
        MaterialButton defaults=button("Use native appearance");editor.addView(defaults);defaults.setOnClickListener(v->{if(busy)return;draftScale=100;draftHide=false;draftColor=0;nativeColor.setChecked(true);scale.setProgress(30);hide.setChecked(false);mark();saveDraft();});
        editor.addView(text("Selections save a draft. Enable launcher customization to apply. App drawer, folders and widgets retain their native appearance.",14));pages.register("launcher","icons","Icons & labels",(View)editor.getParent());
        LinearLayout actions=panel(content,"Apply & restore");status=text("Checking installed launcher…",15);status.setTag("launcher-status");actions.addView(status);
        enable=button("Enable launcher customization");enable.setTag("launcher-enable");enable.setEnabled(false);actions.addView(enable);enable.setOnClickListener(v->apply());
        restore=button("Restore launcher appearance");restore.setTag("launcher-restore");actions.addView(restore);restore.setOnClickListener(v->restore());
        MaterialButton check=button("Check launcher runtime");actions.addView(check);check.setOnClickListener(v->client.check(message->{if(!closed)status.setText(message);}));
        actions.addView(text("Enable nubia-ly for Launcher (com.zte.mifavor.launcher) in Vector, then reboot. Existing System UI and Fingerprint scopes remain selected. Emergency stop and global Restore also stop launcher styling.",14));pages.register("launcher","preview","Launcher",(View)actions.getParent());
        LinearLayout nativeSettings=panel(content,"Launcher settings");nativeSettings.addView(text("Open the launcher's own settings for layout, folders and other native options. Changes made there remain managed by the launcher.",14));
        open=button("Open launcher settings");open.setTag("launcher-native-settings");nativeSettings.addView(open);open.setOnClickListener(v->openSettings());pages.register("launcher","","Launcher",(View)nativeSettings.getParent());
        LinearLayout support=panel(content,"Compatibility");MaterialButton technical=button("Technical details");technical.setTag("launcher-details-toggle");support.addView(technical);details=text("",13);details.setTag("launcher-details");support.addView(details);details.setVisibility(state!=null&&state.getBoolean("details")?View.VISIBLE:View.GONE);technical.setOnClickListener(v->details.setVisibility(details.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));pages.register("launcher","","Launcher",(View)support.getParent());
        if(state!=null){labelHex.setText(state.getString("hex",labelHex.getText().toString()));nativeColor.setChecked(state.getBoolean("native_color",draftColor==0));}
        mark();pages.show("launcher",state==null?"":state.getString("page",""));
        worker.execute(()->{boolean allowed=LauncherCompatibility.supported(getApplicationContext());runOnUiThread(()->{if(closed)return;compatible=allowed;controls();status.setText(allowed?"APK verified. Choose styles, enable the Launcher scope in Vector, reboot, then apply.":"Custom controls blocked: the installed APK or device does not match the verified baseline.");});});
    }
    private void mark(){scaleLabel.setText("Icon size: "+draftScale+"%");preview.setStyle(new LauncherStyle(draftScale,draftHide,draftColor));}
    private boolean validLabelDraft(){return nativeColor.isChecked()||LauncherStyle.parseLabelHex(labelHex.getText().toString())!=null;}
    private void saveDraft(){LauncherStyle draft=new LauncherStyle(draftScale,draftHide,draftColor);worker.execute(()->{boolean saved=LauncherPreferences.save(getApplicationContext(),draft);if(!saved)runOnUiThread(()->{if(!closed)status.setText("Draft could not be saved.");});});}
    private void apply(){
        if(busy||!compatible||!validLabelDraft())return;busy=true;controls();LauncherStyle draft=new LauncherStyle(draftScale,draftHide,draftColor);status.setText("Enabling launcher customization…");
        worker.execute(()->{boolean allowed=false;try{
            String id=GuardReader.inspectEnabled();boolean authority=RecoveryStore.enabledAllowed(getApplicationContext(),id);
            allowed=LauncherPreferences.save(getApplicationContext(),draft)&&(authority||(RootGuardCommand.enableClock(SystemClock.elapsedRealtime()+8000)&&RecoveryStore.allowEnabled(getApplicationContext())))&&LauncherPreferences.activate(getApplicationContext(),draft);
        }catch(RuntimeException unavailable){}if(!allowed)LauncherPreferences.stop(getApplicationContext());boolean result=allowed;runOnUiThread(()->{if(closed)return;busy=false;controls();status.setText(result?"Saved and enabled. Return to the home screen to see the result. Use Check launcher runtime to verify hooks.":"Could not enable. Check root, Recovery Guard and Vector activation.");});});
    }
    private void restore(){if(busy)return;busy=true;controls();worker.execute(()->{boolean stopped=LauncherPreferences.stop(getApplicationContext());runOnUiThread(()->{if(closed)return;busy=false;controls();status.setText(stopped?"Launcher styling disabled. Native appearance returns on the next home-screen refresh.":"Could not save restoration. Use Emergency stop or Recovery Guard Action.");});});}
    private void controls(){enable.setEnabled(!busy&&compatible&&validLabelDraft());restore.setEnabled(!busy);scale.setEnabled(!busy);hide.setEnabled(!busy);nativeColor.setEnabled(!busy);labelHex.setEnabled(!busy&&!nativeColor.isChecked());}
    @Override protected void onResume(){super.onResume();inspect();}
    @Override protected void onStop(){client.cancel();super.onStop();}
    @Override protected void onDestroy(){closed=true;client.cancel();worker.shutdown();super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle out){out.putBoolean("details",details.getVisibility()==View.VISIBLE);out.putInt("scale",draftScale);out.putBoolean("hide",draftHide);out.putInt("color",draftColor);out.putString("hex",labelHex.getText().toString());out.putBoolean("native_color",nativeColor.isChecked());out.putString("page",pages.page());super.onSaveInstanceState(out);}
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
