package com.elitedarkkaiser.nubialy.xposed;
import android.graphics.*;
import android.widget.*;
import com.elitedarkkaiser.nubialy.SurfaceStyle.QsTheme;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class QsHeaderDrawTest {
 public static class Clock extends TextView {Clock(){super(RuntimeEnvironment.getApplication());}}
 @Test public void headerTintDoesNotFlattenBatteryTextAndIconColors()throws Exception {
  var root=new FrameLayout(RuntimeEnvironment.getApplication());var icons=new QsHeaderAppearanceScopeTest.StatusIconContainer(root.getContext());var battery=new com.zte.mifavor.views.MFVBatteryViewLayout(root.getContext());root.addView(icons);icons.addView(battery);QsHeaderScope.headers.add(root);var field=IndicatorColorBackend.class.getDeclaredField("palette");field.setAccessible(true);Object old=field.get(null);field.set(null,com.elitedarkkaiser.nubialy.SurfaceStyle.Palette.STOCK.with(2,Color.MAGENTA).with(8,Color.WHITE));
  try{var theme=QsTheme.STOCK.with(17,Color.CYAN);Bitmap bitmap=Bitmap.createBitmap(100,50,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);icons.layout(0,0,100,50);int saves=canvas.getSaveCount();var group=QsHeaderAppearance.content(icons,canvas,theme);assertNotNull(group);Paint p=new Paint();p.setColor(Color.GREEN);canvas.drawRect(0,0,100,50,p);canvas.restoreToCount(group.save());assertEquals(Color.GREEN,bitmap.getPixel(50,25));for(var view:new android.view.View[]{battery.mBatteryIconView,battery.mBatteryLevelInsideView,battery.mBatteryLevelOutsideView}){view.layout(0,0,100,50);bitmap.eraseColor(Color.TRANSPARENT);var frame=QsHeaderAppearance.content(view,canvas,theme);assertNotNull(frame);p.setColor(Color.GREEN);canvas.drawRect(0,0,100,50,p);canvas.restoreToCount(frame.save());assertEquals(view instanceof TextView?Color.WHITE:Color.MAGENTA,bitmap.getPixel(50,25));assertEquals(saves,canvas.getSaveCount());}QsHeaderAppearance.begin(theme.with(23,Color.YELLOW));assertEquals(Color.YELLOW,IndicatorColorBackend.headerTint(battery.mBatteryLevelInsideView,Color.CYAN));assertEquals(Color.MAGENTA,IndicatorColorBackend.headerTint(battery.mBatteryIconView,Color.CYAN));assertEquals(Color.YELLOW,QsHeaderAppearance.batteryTextTint(battery.mBatteryIconView));assertEquals(0,QsHeaderAppearance.batteryTextTint(new ImageView(root.getContext())));QsHeaderAppearance.end();field.set(null,com.elitedarkkaiser.nubialy.SurfaceStyle.Palette.STOCK);assertEquals(Color.CYAN,IndicatorColorBackend.headerTint(battery.mBatteryIconView,Color.CYAN));assertNull(QsHeaderAppearance.content(new ImageView(root.getContext()),canvas,theme));}finally{QsHeaderAppearance.end();field.set(null,old);QsHeaderScope.headers.clear();}
 }
 @Test public void contentTintAndScaleWorkWithoutAnOuterViewDraw(){
  var root=new FrameLayout(RuntimeEnvironment.getApplication());var clock=new Clock();root.addView(clock);clock.layout(0,0,100,50);QsHeaderScope.headers.add(root);
  try{Bitmap bitmap=Bitmap.createBitmap(100,50,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);int before=canvas.getSaveCount();var frame=QsHeaderAppearance.content(clock,canvas,QsTheme.STOCK.with(15,Color.MAGENTA).with(18,70));assertNotNull(frame);assertFalse(frame.replace());Paint paint=new Paint();paint.setColor(Color.WHITE);canvas.drawRect(0,0,100,50,paint);canvas.restoreToCount(frame.save());assertEquals(before,canvas.getSaveCount());assertEquals(Color.MAGENTA,bitmap.getPixel(50,25));assertEquals(Color.TRANSPARENT,bitmap.getPixel(1,1));assertEquals(1f,clock.getScaleX(),0);assertNull(QsHeaderAppearance.content(new Clock(),canvas,QsTheme.STOCK));}finally{QsHeaderScope.headers.clear();}
 }
 @Test public void customClocksUseNativeTextAndLeaveTheNativeViewUntouched(){
  var root=new FrameLayout(RuntimeEnvironment.getApplication());var clock=new Clock();clock.setText("08:42");clock.layout(0,0,160,100);root.addView(clock);QsHeaderScope.headers.add(root);var nativeFace=clock.getTypeface();float nativeSize=clock.getTextSize();
  try{for(int style=0;style<4;style++){Bitmap bitmap=Bitmap.createBitmap(160,100,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);var frame=QsHeaderAppearance.content(clock,canvas,QsTheme.STOCK.with(21,18).with(22,style));assertNotNull(frame);assertTrue(frame.replace());canvas.restoreToCount(frame.save());int[] pixels=new int[16000];bitmap.getPixels(pixels,0,160,0,0,160,100);assertTrue(java.util.Arrays.stream(pixels).anyMatch(p->(p>>>24)!=0));assertEquals("08:42",clock.getText().toString());assertEquals(nativeFace,clock.getTypeface());assertEquals(nativeSize,clock.getTextSize(),0f);}}finally{QsHeaderScope.headers.clear();}
 }
}
