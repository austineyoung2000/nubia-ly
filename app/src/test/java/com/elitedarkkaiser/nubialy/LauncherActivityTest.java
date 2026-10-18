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
    @Test public void dashboardLauncherTileHasItsOwnRoute(){
        var controller=Robolectric.buildActivity(androidx.appcompat.app.AppCompatActivity.class);var a=controller.get();a.setTheme(R.style.Theme_NubiaLy);controller.setup();
        String[] route={null};ViewGroup dashboard=PersonalizationUi.dashboard(a,value->route[0]=value);View launcher=find(dashboard,"Launcher. Home screen & launcher settings");assertNotNull(launcher);launcher.performClick();assertEquals("launcher",route[0]);controller.pause().stop().destroy();
    }
    private View find(View view,String description){if(description.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),description);if(found!=null)return found;}return null;}
}
