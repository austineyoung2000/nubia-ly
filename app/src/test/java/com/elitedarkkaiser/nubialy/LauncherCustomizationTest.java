package com.elitedarkkaiser.nubialy;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class LauncherCustomizationTest {
    @Test public void draftsDoNotChangeActiveAppearanceAndRestoreRetainsDraft(){
        var c=org.robolectric.RuntimeEnvironment.getApplication();LauncherStyle s=new LauncherStyle(80,true);
        assertTrue(LauncherPreferences.save(c,s));assertEquals(LauncherStyle.STOCK,LauncherPreferences.active(c));
        assertTrue(LauncherPreferences.activate(c,s));assertTrue(LauncherPreferences.save(c,new LauncherStyle(90,false)));assertEquals(s,LauncherPreferences.active(c));
        assertTrue(LauncherPreferences.stop(c));assertEquals(LauncherStyle.STOCK,LauncherPreferences.active(c));assertEquals(90,LauncherPreferences.draft(c).iconScale());
        assertFalse(LauncherPreferences.activate(c,new LauncherStyle(101,true)));assertEquals(LauncherStyle.STOCK,LauncherPreferences.active(c));
    }
    @Test public void textStateRestoresEvenWhenOriginalDrawingThrows() throws Exception {
        TextView view=new TextView(org.robolectric.RuntimeEnvironment.getApplication());view.setText("App label");view.setTextColor(0xffabcdef);view.getPaint().setColor(0xff123456);view.getPaint().setShadowLayer(3,1,2,0xff112233);
        var field=TextView.class.getDeclaredField("mCurTextColor");field.setAccessible(true);int color=view.getCurrentTextColor(),paint=view.getPaint().getColor();
        var state=LauncherDrawState.hide(view,field);
        try{assertEquals(0,view.getCurrentTextColor());assertEquals(0,view.getPaint().getColor());view.getPaint().clearShadowLayer();throw new IllegalStateException("original draw failed");}
        catch(IllegalStateException expected){}finally{state.restore();}
        assertEquals(3f,view.getPaint().getShadowLayerRadius(),0f);assertEquals(color,view.getCurrentTextColor());assertEquals(paint,view.getPaint().getColor());assertEquals("App label",view.getText().toString());
    }
    @Test public void iconTransformRestoresCanvasAndNeverChangesDrawableBounds(){
        Canvas c=new Canvas(Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888));ColorDrawable icon=new ColorDrawable(0xffffffff);icon.setBounds(10,20,50,60);Rect before=new Rect(icon.getBounds());int count=c.getSaveCount();
        int token=LauncherDrawState.scale(c,icon,70);assertTrue(c.getSaveCount()>count);assertEquals(before,icon.getBounds());c.restoreToCount(token);assertEquals(count,c.getSaveCount());assertEquals(before,icon.getBounds());
        try{LauncherDrawState.scale(c,icon,120);fail();}catch(IllegalArgumentException expected){}assertEquals(count,c.getSaveCount());
    }
    @Test public void launcherIdentityCannotBeBorrowedByAnotherUidOrPackage(){
        assertTrue(GuardBrokerPolicy.permitsLauncher(10040,10040,GuardBrokerPolicy.LAUNCHER_PACKAGE));
        assertFalse(GuardBrokerPolicy.permitsLauncher(10041,10040,GuardBrokerPolicy.LAUNCHER_PACKAGE));assertFalse(GuardBrokerPolicy.permitsLauncher(10040,10040,"com.android.systemui"));assertFalse(GuardBrokerPolicy.permitsLauncher(10040,-1,GuardBrokerPolicy.LAUNCHER_PACKAGE));
    }
}
