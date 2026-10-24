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
 @Test public void contentTintAndScaleWorkWithoutAnOuterViewDraw(){
  var root=new FrameLayout(RuntimeEnvironment.getApplication());var clock=new Clock();root.addView(clock);clock.layout(0,0,100,50);QsHeaderScope.headers.add(root);
  try{Bitmap bitmap=Bitmap.createBitmap(100,50,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);int before=canvas.getSaveCount();var frame=QsHeaderAppearance.content(clock,canvas,QsTheme.STOCK.with(15,Color.MAGENTA).with(18,70));assertNotNull(frame);assertFalse(frame.replace());Paint paint=new Paint();paint.setColor(Color.WHITE);canvas.drawRect(0,0,100,50,paint);canvas.restoreToCount(frame.save());assertEquals(before,canvas.getSaveCount());assertEquals(Color.MAGENTA,bitmap.getPixel(50,25));assertEquals(Color.TRANSPARENT,bitmap.getPixel(1,1));assertEquals(1f,clock.getScaleX(),0);assertNull(QsHeaderAppearance.content(new Clock(),canvas,QsTheme.STOCK));}finally{QsHeaderScope.headers.clear();}
 }
 @Test public void customClocksUseNativeTextAndLeaveTheNativeViewUntouched(){
  var root=new FrameLayout(RuntimeEnvironment.getApplication());var clock=new Clock();clock.setText("08:42");clock.layout(0,0,160,100);root.addView(clock);QsHeaderScope.headers.add(root);var nativeFace=clock.getTypeface();float nativeSize=clock.getTextSize();
  try{for(int style=0;style<4;style++){Bitmap bitmap=Bitmap.createBitmap(160,100,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);var frame=QsHeaderAppearance.content(clock,canvas,QsTheme.STOCK.with(21,18).with(22,style));assertNotNull(frame);assertTrue(frame.replace());canvas.restoreToCount(frame.save());int[] pixels=new int[16000];bitmap.getPixels(pixels,0,160,0,0,160,100);assertTrue(java.util.Arrays.stream(pixels).anyMatch(p->(p>>>24)!=0));assertEquals("08:42",clock.getText().toString());assertEquals(nativeFace,clock.getTypeface());assertEquals(nativeSize,clock.getTextSize(),0f);}}finally{QsHeaderScope.headers.clear();}
 }
}
