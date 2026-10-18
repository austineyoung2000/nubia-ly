package com.elitedarkkaiser.nubialy;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public final class LauncherActivityTest {
    private void install(boolean exported,String permission){
        var pm=shadowOf(org.robolectric.RuntimeEnvironment.getApplication().getPackageManager());
        PackageInfo info=new PackageInfo();info.packageName=LauncherActivity.PACKAGE;info.versionName=LauncherActivity.RESEARCH_VERSION;info.setLongVersionCode(160000);
        info.applicationInfo=new ApplicationInfo();info.applicationInfo.packageName=info.packageName;info.applicationInfo.enabled=true;info.applicationInfo.targetSdkVersion=35;
        ActivityInfo settings=new ActivityInfo();settings.packageName=info.packageName;settings.name=LauncherActivity.SETTINGS;settings.applicationInfo=info.applicationInfo;settings.enabled=true;settings.exported=exported;settings.permission=permission;info.activities=new ActivityInfo[]{settings};pm.installPackage(info);
    }
    @Test public void opensOnlyVerifiedNativeSettingsComponent(){
        install(true,null);
        var controller=Robolectric.buildActivity(LauncherActivity.class).setup();var a=controller.get();
        View button=a.findViewById(android.R.id.content).findViewWithTag("launcher-native-settings");assertTrue(button.isEnabled());button.performClick();
        Intent intent=shadowOf(a).getNextStartedActivity();assertEquals(new ComponentName(LauncherActivity.PACKAGE,LauncherActivity.SETTINGS),intent.getComponent());
        controller.pause().stop().destroy();
    }
    @Test public void protectedSettingsAreNotOpened(){
        install(true,"android.permission.BIND_WALLPAPER");
        var controller=Robolectric.buildActivity(LauncherActivity.class).setup();var a=controller.get();
        assertFalse(a.findViewById(android.R.id.content).findViewWithTag("launcher-native-settings").isEnabled());assertNull(shadowOf(a).getNextStartedActivity());controller.pause().stop().destroy();
    }
    @Test public void missingLauncherFailsClosedAndDetailsStayCollapsed(){
        var controller=Robolectric.buildActivity(LauncherActivity.class).setup();var a=controller.get();View root=a.findViewById(android.R.id.content);
        assertFalse(root.findViewWithTag("launcher-native-settings").isEnabled());assertEquals(View.GONE,root.findViewWithTag("launcher-details").getVisibility());
        root.findViewWithTag("launcher-details-toggle").performClick();assertEquals(View.VISIBLE,root.findViewWithTag("launcher-details").getVisibility());controller.recreate();
        assertEquals(View.VISIBLE,controller.get().findViewById(android.R.id.content).findViewWithTag("launcher-details").getVisibility());controller.pause().stop().destroy();
    }
    @Test public void iconsPageBackAndRecreationKeepOneDraft(){
        var controller=Robolectric.buildActivity(LauncherActivity.class).setup();var a=controller.get();View root=a.findViewById(android.R.id.content);
        root.findViewWithTag("launcher-icons").performClick();android.widget.SeekBar scale=root.findViewWithTag("launcher-scale");
        com.google.android.material.materialswitch.MaterialSwitch hide=root.findViewWithTag("launcher-hide-labels");hide.setChecked(true);
        a.getOnBackPressedDispatcher().onBackPressed();assertFalse(a.isFinishing());assertTrue(hide.isChecked());
        root.findViewWithTag("launcher-icons").performClick();assertTrue(hide.isChecked());controller.recreate();
        assertTrue(((com.google.android.material.materialswitch.MaterialSwitch)controller.get().findViewById(android.R.id.content).findViewWithTag("launcher-hide-labels")).isChecked());
        controller.pause().stop().destroy();
    }
    @Test public void sharedColorDialogCancelsDraftAndCommitsOnlyUseColor(){
        var controller=Robolectric.buildActivity(LauncherActivity.class).setup();var a=controller.get();View root=a.findViewById(android.R.id.content);root.findViewWithTag("launcher-icons").performClick();
        root.findViewWithTag("launcher-custom-color").performClick();var dialog=(androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
        com.google.android.material.textfield.TextInputEditText hex=dialog.getWindow().getDecorView().findViewWithTag("color-picker-hex");com.google.android.material.textfield.TextInputLayout field=dialog.getWindow().getDecorView().findViewWithTag("color-picker-field");
        hex.setText("#12");dialog.getButton(-1).performClick();assertTrue(dialog.isShowing());assertNotNull(field.getError());
        hex.setText("#123456");assertEquals(18f,((com.google.android.material.slider.Slider)dialog.getWindow().getDecorView().findViewWithTag("color-picker-red")).getValue(),0f);
        dialog.getButton(-2).performClick();assertTrue(((com.google.android.material.materialswitch.MaterialSwitch)root.findViewWithTag("launcher-native-label-color")).isChecked());assertEquals("Native label color",((TextView)root.findViewWithTag("launcher-label-selection")).getText().toString());
        root.findViewWithTag("launcher-custom-color").performClick();dialog=(androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();hex=dialog.getWindow().getDecorView().findViewWithTag("color-picker-hex");hex.setText("#123456");dialog.getButton(-1).performClick();assertFalse(dialog.isShowing());assertEquals("#123456",((TextView)root.findViewWithTag("launcher-label-selection")).getText().toString());
        controller.recreate();assertEquals("#123456",((TextView)controller.get().findViewById(android.R.id.content).findViewWithTag("launcher-label-selection")).getText().toString());controller.pause().stop().destroy();
    }
    @Test public void folderPageKeepsIndependentColorsAndNativeDefaults(){
        var controller=Robolectric.buildActivity(LauncherActivity.class).setup();var activity=controller.get();View root=activity.findViewById(android.R.id.content);root.findViewWithTag("launcher-folders").performClick();
        root.findViewWithTag("folder-title-custom").performClick();var dialog=(androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();com.google.android.material.textfield.TextInputEditText hex=dialog.getWindow().getDecorView().findViewWithTag("color-picker-hex");hex.setText("#123456");dialog.getButton(-1).performClick();assertEquals("#123456",((TextView)root.findViewWithTag("folder-title-selection")).getText().toString());assertEquals("Native color",((TextView)root.findViewWithTag("folder-labels-selection")).getText().toString());
        root.findViewWithTag("folder-labels-custom").performClick();dialog=(androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();hex=dialog.getWindow().getDecorView().findViewWithTag("color-picker-hex");hex.setText("#ABCDEF");dialog.getButton(-1).performClick();
        ((android.widget.SeekBar)root.findViewWithTag("folder-blur")).setProgress(5);((com.google.android.material.materialswitch.MaterialSwitch)root.findViewWithTag("folder-blur-native")).setChecked(false);
        controller.recreate();root=controller.get().findViewById(android.R.id.content);assertEquals("#123456",((TextView)root.findViewWithTag("folder-title-selection")).getText().toString());assertEquals("#ABCDEF",((TextView)root.findViewWithTag("folder-labels-selection")).getText().toString());assertEquals("Blur strength: 5 / 25",((TextView)root.findViewWithTag("folder-blur-value")).getText().toString());assertEquals("Native label color",((TextView)root.findViewWithTag("launcher-label-selection")).getText().toString());
        root.findViewWithTag("folder-defaults").performClick();assertEquals("Native blur",((TextView)root.findViewWithTag("folder-blur-value")).getText().toString());assertEquals("Native color",((TextView)root.findViewWithTag("folder-title-selection")).getText().toString());assertEquals("Native color",((TextView)root.findViewWithTag("folder-labels-selection")).getText().toString());controller.pause().stop().destroy();
    }
    @Test public void artworkPageKeepsShapeDraftAndCanRestoreNative(){
        var controller=Robolectric.buildActivity(LauncherActivity.class).setup();var activity=controller.get();View root=activity.findViewById(android.R.id.content);root.findViewWithTag("launcher-artwork").performClick();root.findViewWithTag("icon-shape-3").performClick();assertTrue(((com.google.android.material.button.MaterialButton)root.findViewWithTag("icon-shape-3")).isChecked());assertEquals(LauncherStyle.STOCK,LauncherPreferences.active(activity));controller.recreate();root=controller.get().findViewById(android.R.id.content);assertTrue(((com.google.android.material.button.MaterialButton)root.findViewWithTag("icon-shape-3")).isChecked());root.findViewWithTag("icon-artwork-native").performClick();assertTrue(((com.google.android.material.button.MaterialButton)root.findViewWithTag("icon-shape-0")).isChecked());assertEquals("Native icons",((TextView)root.findViewWithTag("icon-pack-selection")).getText().toString());controller.pause().stop().destroy();
    }
    @Test public void dashboardLauncherTileHasItsOwnRoute(){
        var controller=Robolectric.buildActivity(androidx.appcompat.app.AppCompatActivity.class);var a=controller.get();a.setTheme(R.style.Theme_NubiaLy);controller.setup();
        String[] route={null};ViewGroup dashboard=PersonalizationUi.dashboard(a,value->route[0]=value);View launcher=find(dashboard,"Launcher. Home screen & launcher settings");assertNotNull(launcher);launcher.performClick();assertEquals("launcher",route[0]);controller.pause().stop().destroy();
    }
    private View find(View view,String description){if(description.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),description);if(found!=null)return found;}return null;}
}
