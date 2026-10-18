package com.elitedarkkaiser.nubialy;

import android.content.Intent;
import android.view.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBuild;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=36)
public final class AppearanceEditorNavigationTest {
    @Test public void applyActionsFollowTheVisiblePage(){
        ShadowBuild.setModel("NX809J");var controller=Robolectric.buildActivity(ClockEditorActivity.class,new Intent().putExtra("section","quick_settings")).setup();
        var decor=controller.get().getWindow().getDecorView();var enable=(android.widget.TextView)decor.findViewWithTag("editor-enable");var restore=(android.widget.TextView)decor.findViewWithTag("editor-restore");
        assertEquals("Enable Quick Settings",enable.getText().toString());
        decor.findViewWithTag("editor-route:quick_settings:tiles").performClick();assertEquals("Enable Tile shapes",enable.getText().toString());assertEquals("Restore Tile shapes",restore.getText().toString());
        controller.get().getOnBackPressedDispatcher().onBackPressed();assertEquals("Enable Quick Settings",enable.getText().toString());
        assertNotNull(decor.findViewWithTag("qs-iconpack-style:1"));assertNotNull(decor.findViewWithTag("iconpack-style:1"));controller.pause().stop().destroy();
    }
    @Test public void everySectionOpensItsDetailPagesAndBackKeepsOverview(){
        ShadowBuild.setModel("NX809J");
        String[][] routes={{"lock_screen","clock","date","fonts","fingerprint","widgets","charging"},{"quick_settings","grid","landscape","top_layout","label_layout","brightness_layout","style","brightness","tiles","labels"},{"volume","styles"},{"notifications","styles","appearance","behavior","icons"},{"power_menu","styles"},{"status_bar","typography","date","color","icons","battery","connectivity","inventory"}};
        for(String[] section:routes){
            var controller=Robolectric.buildActivity(ClockEditorActivity.class,new Intent().putExtra("section",section[0])).setup();var activity=controller.get();assertFalse(activity.isFinishing());EditorPages router=ReflectionHelpers.getField(activity,"pages");
            for(int i=1;i<section.length;i++){
                View row=activity.getWindow().getDecorView().findViewWithTag("editor-route:"+section[0]+":"+section[i]);assertNotNull(row);assertTrue(row.isShown());row.performClick();assertEquals(section[i],router.page());activity.getOnBackPressedDispatcher().onBackPressed();assertEquals("",router.page());assertFalse(activity.isFinishing());
            }
            controller.pause().stop().destroy();
        }
    }
    @Test public void outlineSelectionSavesOnlyADraft() throws Exception {
        ShadowBuild.setModel("NX809J");var controller=Robolectric.buildActivity(ClockEditorActivity.class,new Intent().putExtra("section","quick_settings")).setup();var activity=controller.get();
        var recovery=activity.getSharedPreferences("recovery",android.content.Context.MODE_PRIVATE);var before=recovery.getAll();
        activity.getWindow().getDecorView().findViewWithTag("editor-route:quick_settings:style").performClick();
        activity.getWindow().getDecorView().findViewWithTag("qs-style:1").performClick();
        java.util.concurrent.ExecutorService worker=ReflectionHelpers.getField(activity,"worker");worker.submit(()->{}).get(3,java.util.concurrent.TimeUnit.SECONDS);
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(1,StatusBarPreferences.read(activity).surfaces().qsStyle());assertEquals(before,recovery.getAll());
        activity.getOnBackPressedDispatcher().onBackPressed();EditorPages router=ReflectionHelpers.getField(activity,"pages");assertEquals("",router.page());controller.pause().stop().destroy();
    }
    @Test public void brightnessSelectionSavesOnlyADraft() throws Exception {
        ShadowBuild.setModel("NX809J");var controller=Robolectric.buildActivity(ClockEditorActivity.class,new Intent().putExtra("section","quick_settings")).setup();var activity=controller.get();
        var recovery=activity.getSharedPreferences("recovery",android.content.Context.MODE_PRIVATE);var before=recovery.getAll();
        activity.getWindow().getDecorView().findViewWithTag("editor-route:quick_settings:brightness").performClick();activity.getWindow().getDecorView().findViewWithTag("brightness-style:3").performClick();
        java.util.concurrent.ExecutorService worker=ReflectionHelpers.getField(activity,"worker");worker.submit(()->{}).get(3,java.util.concurrent.TimeUnit.SECONDS);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertEquals(3,StatusBarPreferences.read(activity).surfaces().brightnessStyle());assertEquals(before,recovery.getAll());controller.pause().stop().destroy();
    }
    @Test public void volumeCardSelectionOnlySavesDraft()throws Exception{
        ShadowBuild.setModel("NX809J");var controller=Robolectric.buildActivity(ClockEditorActivity.class,new Intent().putExtra("section","volume")).setup();var activity=controller.get();var recovery=activity.getSharedPreferences("recovery",android.content.Context.MODE_PRIVATE);var before=recovery.getAll();activity.getWindow().getDecorView().findViewWithTag("editor-route:volume:styles").performClick();activity.getWindow().getDecorView().findViewWithTag("volume-style:4").performClick();java.util.concurrent.ExecutorService worker=ReflectionHelpers.getField(activity,"worker");worker.submit(()->{}).get(3,java.util.concurrent.TimeUnit.SECONDS);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(4,StatusBarPreferences.read(activity).surfaces().volumeStyle());assertEquals(before,recovery.getAll());controller.pause().stop().destroy();
    }
    @Test public void notificationSelectionOnlySavesDraft()throws Exception{
        ShadowBuild.setModel("NX809J");var controller=Robolectric.buildActivity(ClockEditorActivity.class,new Intent().putExtra("section","notifications")).setup();var activity=controller.get();var recovery=activity.getSharedPreferences("recovery",android.content.Context.MODE_PRIVATE);var before=recovery.getAll();activity.getWindow().getDecorView().findViewWithTag("editor-route:notifications:styles").performClick();activity.getWindow().getDecorView().findViewWithTag("notification-style:2").performClick();java.util.concurrent.ExecutorService worker=ReflectionHelpers.getField(activity,"worker");worker.submit(()->{}).get(3,java.util.concurrent.TimeUnit.SECONDS);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(2,StatusBarPreferences.read(activity).surfaces().notificationStyle());assertEquals(before,recovery.getAll());controller.pause().stop().destroy();
    }
    @Test public void recreationRestoresTheFocusedPage(){
        ShadowBuild.setModel("NX809J");var controller=Robolectric.buildActivity(ClockEditorActivity.class,new Intent().putExtra("section","lock_screen")).setup();var activity=controller.get();activity.getWindow().getDecorView().findViewWithTag("editor-route:lock_screen:widgets").performClick();controller.recreate();EditorPages router=ReflectionHelpers.getField(controller.get(),"pages");assertEquals("lock_screen",router.section());assertEquals("widgets",router.page());controller.pause().stop().destroy();
    }
    @Test public void connectivityAndPowerChoicesStayInTheSharedDraft()throws Exception{
        ShadowBuild.setModel("NX809J");var controller=Robolectric.buildActivity(ClockEditorActivity.class,new Intent().putExtra("section","status_bar")).setup();var activity=controller.get();var before=activity.getSharedPreferences("recovery",0).getAll();var decor=activity.getWindow().getDecorView();decor.findViewWithTag("wifi-style:1").performClick();decor.findViewWithTag("signal-style:8").performClick();decor.findViewWithTag("power-style:3").performClick();java.util.concurrent.ExecutorService worker=ReflectionHelpers.getField(activity,"worker");worker.submit(()->{}).get(3,java.util.concurrent.TimeUnit.SECONDS);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();SurfaceStyle s=StatusBarPreferences.read(activity).surfaces();assertEquals(1,s.wifiStyle());assertEquals(8,s.signalStyle());assertEquals(3,s.powerStyle());assertEquals(before,activity.getSharedPreferences("recovery",0).getAll());controller.pause().stop().destroy();
    }

    @Test public void batterySelectionIsDraftOnlyAndSurvivesRecreation()throws Exception{
        ShadowBuild.setModel("NX809J");var controller=Robolectric.buildActivity(ClockEditorActivity.class,new Intent().putExtra("section","status_bar")).setup();var activity=controller.get();var before=activity.getSharedPreferences("recovery",0).getAll();activity.getWindow().getDecorView().findViewWithTag("editor-route:status_bar:battery").performClick();activity.getWindow().getDecorView().findViewWithTag("battery-style:23").performClick();java.util.concurrent.ExecutorService worker=ReflectionHelpers.getField(activity,"worker");worker.submit(()->{}).get(3,java.util.concurrent.TimeUnit.SECONDS);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(23,StatusBarPreferences.read(activity).surfaces().batteryStyle());assertEquals(before,activity.getSharedPreferences("recovery",0).getAll());controller.recreate();EditorPages router=ReflectionHelpers.getField(controller.get(),"pages");assertEquals("battery",router.page());assertTrue(controller.get().getWindow().getDecorView().findViewWithTag("battery-style:23").isSelected());controller.pause().stop().destroy();
    }

    @Test public void notificationAndBatteryControlsShareDraftAndPersist()throws Exception{
        ShadowBuild.setModel("NX809J");var controller=Robolectric.buildActivity(ClockEditorActivity.class,new Intent().putExtra("section","notifications")).setup();var activity=controller.get();var before=activity.getSharedPreferences("recovery",0).getAll();var decor=activity.getWindow().getDecorView();decor.findViewWithTag("editor-route:notifications:icons").performClick();decor.findViewWithTag("notification-overflow-count").performClick();decor.findViewWithTag("notification-limit:3").performClick();decor.findViewWithTag("battery-percent:2").performClick();decor.findViewWithTag("battery-hide-charge").performClick();java.util.concurrent.ExecutorService worker=ReflectionHelpers.getField(activity,"worker");worker.submit(()->{}).get(3,java.util.concurrent.TimeUnit.SECONDS);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();SurfaceStyle s=StatusBarPreferences.read(activity).surfaces();assertEquals(1,s.notificationCount());assertEquals(3,s.notificationLimit());assertEquals(2,s.batteryPercent());assertEquals(1,s.batteryCharge());assertEquals(before,activity.getSharedPreferences("recovery",0).getAll());controller.recreate();EditorPages router=ReflectionHelpers.getField(controller.get(),"pages");assertEquals("icons",router.page());assertEquals("notifications",router.section());assertTrue(((com.google.android.material.chip.Chip)controller.get().getWindow().getDecorView().findViewWithTag("battery-percent:2")).isChecked());controller.pause().stop().destroy();
    }

    @org.junit.Test public void gridDraftSurvivesRecreationWithoutEnabling() throws Exception {
        ShadowBuild.setModel("NX809J");var controller=Robolectric.buildActivity(ClockEditorActivity.class,new Intent().putExtra("section","quick_settings")).setup();var activity=controller.get();var before=activity.getSharedPreferences("recovery",0).getAll();var decor=activity.getWindow().getDecorView();
        decor.findViewWithTag("editor-route:quick_settings:grid").performClick();assertNull(decor.findViewWithTag("qs-columns:4"));assertNull(decor.findViewWithTag("qs-rows:3"));assertNotNull(decor.findViewWithTag("qs-columns:2"));decor.findViewWithTag("qs-columns:5").performClick();decor.findViewWithTag("qs-rows:4").performClick();
        java.util.concurrent.ExecutorService worker=ReflectionHelpers.getField(activity,"worker");worker.submit(()->{}).get(3,java.util.concurrent.TimeUnit.SECONDS);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertEquals(5,StatusBarPreferences.read(activity).surfaces().qsColumns());assertEquals(4,StatusBarPreferences.read(activity).surfaces().qsRows());assertEquals(before,activity.getSharedPreferences("recovery",0).getAll());controller.recreate();
        EditorPages router=ReflectionHelpers.getField(controller.get(),"pages");assertEquals("grid",router.page());assertTrue(controller.get().getWindow().getDecorView().findViewWithTag("qs-columns:5").isSelected());assertTrue(controller.get().getWindow().getDecorView().findViewWithTag("qs-rows:4").isSelected());controller.pause().stop().destroy();
    }

    @Test public void expandedQsControlsShareDraftWithoutEnabling()throws Exception{
        ShadowBuild.setModel("NX809J");var controller=Robolectric.buildActivity(ClockEditorActivity.class,new Intent().putExtra("section","quick_settings")).setup();var a=controller.get();var before=a.getSharedPreferences("recovery",0).getAll();var decor=a.getWindow().getDecorView();
        for(String tag:new String[]{"qs-top-layout:2","qs-label-position:1","qs-label-lines:3","qs-brightness-position:2","qs-brightness-icons:1","qs-landscape-columns:6","qs-landscape-rows:5"}){assertNotNull(tag,decor.findViewWithTag(tag));decor.findViewWithTag(tag).performClick();}
        java.util.concurrent.ExecutorService worker=ReflectionHelpers.getField(a,"worker");worker.submit(()->{}).get(3,java.util.concurrent.TimeUnit.SECONDS);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();SurfaceStyle s=StatusBarPreferences.read(a).surfaces();assertEquals(2,s.qsTopLayout());assertEquals(1,s.qsLabelPosition());assertEquals(3,s.qsLabelLines());assertEquals(2,s.qsBrightnessPosition());assertTrue(s.qsHideBrightnessIcons());assertEquals(6,s.qsLandscapeColumns());assertEquals(5,s.qsLandscapeRows());assertEquals(before,a.getSharedPreferences("recovery",0).getAll());controller.recreate();assertTrue(controller.get().getWindow().getDecorView().findViewWithTag("qs-top-layout:2").isSelected());controller.pause().stop().destroy();
    }

}
