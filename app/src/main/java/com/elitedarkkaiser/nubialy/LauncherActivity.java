package com.elitedarkkaiser.nubialy;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/** Dedicated launcher landing page. Native settings only until reversible hooks are validated. */
public final class LauncherActivity extends AppCompatActivity {
    static final String PACKAGE="com.zte.mifavor.launcher";
    static final String SETTINGS="com.android.launcher3.settings.SettingsActivityMFV";
    static final String RESEARCH_VERSION="16.0.010.000.2604151532";
    private TextView status,details;
    private MaterialButton open;
    private String diagnostic="";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll=new ScrollView(this);
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20),dp(16),dp(20),dp(24));scroll.addView(content);setContentView(scroll);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(scroll,(v,insets)->{
            var bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;
        });
        content.addView(PersonalizationUi.toolbar(this,"Launcher"));
        LinearLayout hero=panel(content,"Your home screen");
        hero.addView(new LauncherPreview(this),new LinearLayout.LayoutParams(-1,dp(150)));
        hero.addView(text("Icons, labels, folders and layout",16));
        LinearLayout nativeSettings=panel(content,"Launcher settings");
        nativeSettings.addView(text("Open your launcher's own settings for the options available on your installed version. Changes made there are managed by the launcher.",14));
        open=button("Open launcher settings");open.setTag("launcher-native-settings");nativeSettings.addView(open);
        open.setOnClickListener(v->openSettings());
        LinearLayout support=panel(content,"Customization support");
        status=text("Checking installed launcher…",15);status.setTag("launcher-status");support.addView(status);
        support.addView(text("Custom nubia-ly launcher controls are not enabled in this build. Launcher settings are separate from Enable customization and Restore stock appearance.",14));
        MaterialButton technical=button("Technical details");technical.setTag("launcher-details-toggle");support.addView(technical);
        details=text("",13);details.setTag("launcher-details");details.setVisibility(View.GONE);support.addView(details);
        boolean expanded=state!=null&&state.getBoolean("details");details.setVisibility(expanded?View.VISIBLE:View.GONE);
        technical.setOnClickListener(v->details.setVisibility(details.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));
    }
    @Override protected void onResume(){super.onResume();inspect();}
    @Override protected void onSaveInstanceState(Bundle out){out.putBoolean("details",details.getVisibility()==View.VISIBLE);super.onSaveInstanceState(out);}

    private void inspect(){
        open.setEnabled(false);
        try {
            var info=getPackageManager().getPackageInfo(PACKAGE,0);
            var activity=getPackageManager().getActivityInfo(new ComponentName(PACKAGE,SETTINGS),0);
            boolean allowed=info.applicationInfo!=null&&info.applicationInfo.enabled&&activity.enabled&&activity.exported
                &&(activity.permission==null||checkSelfPermission(activity.permission)==PackageManager.PERMISSION_GRANTED);
            open.setEnabled(allowed);
            boolean baseline=info.getLongVersionCode()==160000&&RESEARCH_VERSION.equals(info.versionName);
            status.setText(baseline?"Research launcher version detected. Custom controls still require validation.":"Installed launcher differs from the research version. Custom controls remain unavailable.");
            diagnostic="Package: "+PACKAGE+"\nVersion: "+info.versionName+" ("+info.getLongVersionCode()+")"
                +"\nResearch version: "+RESEARCH_VERSION+" (160000)"
                +"\nVersion matching does not verify APK contents or hook compatibility."
                +"\nNative settings: "+(allowed?"available":"unavailable")
                +"\nNo launcher provider writes, root commands, database edits or launcher restarts are performed by this page.";
        } catch(PackageManager.NameNotFoundException unavailable){status.setText("Supported launcher or its settings page was not found.");diagnostic="Expected package: "+PACKAGE+"\nExpected settings: "+SETTINGS;}
          catch(RuntimeException unavailable){status.setText("Launcher information is unavailable.");diagnostic="Inspection failed safely: "+unavailable.getClass().getSimpleName();}
        details.setText(diagnostic);
    }
    private void openSettings(){
        try { startActivity(new Intent().setComponent(new ComponentName(PACKAGE,SETTINGS))); }
        catch(RuntimeException unavailable){open.setEnabled(false);status.setText("Launcher settings could not be opened. No settings were changed by nubia-ly.");}
    }
    private LinearLayout panel(LinearLayout parent,String title){
        MaterialCardView card=new MaterialCardView(this);PersonalizationUi.panel(card);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(14);parent.addView(card,params);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(20),dp(20),dp(20));card.addView(body);body.addView(text(title,22));return body;
    }
    private MaterialButton button(String title){MaterialButton button=new MaterialButton(this);button.setText(title);button.setAllCaps(false);button.setCornerRadius(dp(28));return button;}
    private TextView text(String value,int size){TextView text=new TextView(this);text.setText(value);text.setTextSize(size);text.setTextColor(getColor(size>=17?R.color.on_surface:R.color.on_surface_variant));text.setPadding(0,dp(4),0,dp(10));return text;}
    private int dp(float value){return PersonalizationUi.dp(this,value);}
}
