package com.elitedarkkaiser.nubialy;

import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public final class EditorPagesTest {
    @Test public void detailNavigationRetainsViewsAndBackReturnsToOverview(){
        var controller=Robolectric.buildActivity(AppCompatActivity.class);var activity=controller.get();activity.setTheme(R.style.Theme_NubiaLy);controller.setup();
        LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);ScrollView scroll=new ScrollView(activity);scroll.addView(root);activity.setContentView(scroll);
        var bar=PersonalizationUi.toolbar(activity,"Lock screen");root.addView(bar);
        var router=new EditorPages(activity,bar,scroll);View preview=new View(activity),overview=new View(activity);EditText draft=new EditText(activity);draft.setText("keep my draft");root.addView(preview);root.addView(overview);root.addView(draft);
        router.register("lock_screen","preview","Preview",preview);router.register("lock_screen","","Lock screen",overview);router.register("lock_screen","clock","Clock appearance",draft);
        router.show("lock_screen","");assertEquals(View.GONE,overview.getVisibility());assertEquals(View.VISIBLE,draft.getVisibility());
        router.show("lock_screen","clock");assertEquals("Lock screen",bar.getTitle());assertEquals(View.VISIBLE,preview.getVisibility());
        activity.getOnBackPressedDispatcher().onBackPressed();assertTrue(activity.isFinishing());assertEquals("tab_clock",router.page());assertEquals(View.GONE,overview.getVisibility());assertEquals("keep my draft",draft.getText().toString());
        router.show("lock_screen","clock");assertEquals("keep my draft",draft.getText().toString());controller.pause().stop().destroy();
    }
    @Test public void invalidRouteFallsBackToSafeOverview(){
        var controller=Robolectric.buildActivity(AppCompatActivity.class);var activity=controller.get();activity.setTheme(R.style.Theme_NubiaLy);controller.setup();var bar=PersonalizationUi.toolbar(activity,"");var scroll=new ScrollView(activity);var router=new EditorPages(activity,bar,scroll);View view=new View(activity);router.register("status_bar","","Status bar",view);router.show("unknown","unknown");assertEquals("status_bar",router.section());assertEquals("tab_clock",router.page());assertEquals(View.GONE,view.getVisibility());controller.pause().stop().destroy();
    }
}
