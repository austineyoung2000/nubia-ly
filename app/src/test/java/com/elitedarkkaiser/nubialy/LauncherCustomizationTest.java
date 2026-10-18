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
        var c=org.robolectric.RuntimeEnvironment.getApplication();LauncherStyle s=new LauncherStyle(80,true,0xffd3cbef);
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
    @Test public void hexColorsAreStrictAndPreserveNativeLabelAlpha() throws Exception {
        assertEquals(Integer.valueOf(0xffd3cbef),LauncherStyle.parseLabelHex("#D3CBEF"));assertEquals(Integer.valueOf(0xff000000),LauncherStyle.parseLabelHex("000000"));
        for(String invalid:new String[]{"#123","#11223344","#GGGGGG","red","12;3456"})assertNull(LauncherStyle.parseLabelHex(invalid));
        assertEquals(0x33d3cbef,LauncherStyle.labelTint(0xffd3cbef,0x33112233));assertEquals(0x00d3cbef,LauncherStyle.labelTint(0xffd3cbef,0x00112233));assertEquals(0x33112233,LauncherStyle.labelTint(0,0x33112233));
        assertFalse(new LauncherStyle(70,false,0x33d3cbef).valid());assertTrue(new LauncherStyle(70,false,0xff000000).valid());
        TextView view=new TextView(org.robolectric.RuntimeEnvironment.getApplication());view.setText("App");view.setTextColor(0x33112233);int paint=view.getPaint().getColor();var f=TextView.class.getDeclaredField("mCurTextColor");f.setAccessible(true);
        var frame=LauncherDrawState.tint(view,f,LauncherStyle.labelTint(0xffd3cbef,view.getCurrentTextColor()));try{assertEquals(0x33d3cbef,view.getCurrentTextColor());}finally{frame.restore();}assertEquals(0x33112233,view.getCurrentTextColor());assertEquals(paint,view.getPaint().getColor());
    }
    public static class BoundsHelper {
        public static void target(android.app.Activity a,android.view.View v,boolean opening,android.graphics.RectF out,android.graphics.Rect local){}
        public static void wrapper(android.app.Activity a,android.view.View v,boolean opening,android.graphics.RectF out){}
    }
    public static class AmbiguousBounds {
        public static void first(android.app.Activity a,android.view.View v,boolean opening,android.graphics.RectF out,android.graphics.Rect local){}
        public static void second(android.app.Activity a,android.view.View v,boolean opening,android.graphics.RectF out,android.graphics.Rect local){}
    }
    @Test public void floatingAnimationTargetMatchesSeventyPercentWithoutMovingCenter(){
        RectF target=new RectF(100,200,200,300);assertTrue(LauncherTransitionBounds.adjust(target,70));
        assertEquals(70f,target.width(),0.001f);assertEquals(70f,target.height(),0.001f);assertEquals(150f,target.centerX(),0f);assertEquals(250f,target.centerY(),0f);
        RectF stock=new RectF(100,200,200,300),copy=new RectF(stock);assertFalse(LauncherTransitionBounds.adjust(stock,100));assertEquals(copy,stock);
        assertFalse(LauncherTransitionBounds.adjust(stock,69));assertEquals(copy,stock);assertFalse(LauncherTransitionBounds.adjust(new RectF(),70));assertFalse(LauncherTransitionBounds.adjust(new RectF(Float.NaN,0,50,50),70));
    }
    @Test public void animationHookSignatureMustBeUniqueAndComplete() throws Exception {
        assertEquals("target",LauncherTransitionBounds.resolve(BoundsHelper.class).getName());
        try{LauncherTransitionBounds.resolve(AmbiguousBounds.class);fail();}catch(NoSuchMethodException expected){}
        try{LauncherTransitionBounds.resolve(String.class);fail();}catch(NoSuchMethodException expected){}
    }
    @Test public void launcherIdentityCannotBeBorrowedByAnotherUidOrPackage(){
        assertTrue(GuardBrokerPolicy.permitsLauncher(10040,10040,GuardBrokerPolicy.LAUNCHER_PACKAGE));
        assertFalse(GuardBrokerPolicy.permitsLauncher(10041,10040,GuardBrokerPolicy.LAUNCHER_PACKAGE));assertFalse(GuardBrokerPolicy.permitsLauncher(10040,10040,"com.android.systemui"));assertFalse(GuardBrokerPolicy.permitsLauncher(10040,-1,GuardBrokerPolicy.LAUNCHER_PACKAGE));
    }
}
