package com.elitedarkkaiser.nubialy.xposed;
import android.content.Context;import android.graphics.*;import android.view.View;import android.widget.*;
import com.elitedarkkaiser.nubialy.SurfaceStyle.Palette;
import org.junit.Test;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class IndicatorColorTest {
 public static class ModernStatusBarWifiView extends LinearLayout {ModernStatusBarWifiView(Context c){super(c);}}
 public static class ModernStatusBarMobileView extends LinearLayout {ModernStatusBarMobileView(Context c){super(c);}}
 public static class BatteryMeterView extends LinearLayout {BatteryMeterView(Context c){super(c);}}
 @Test public void categoriesTintIndependentlyAndNotificationsNeverQualify(){Context c=RuntimeEnvironment.getApplication();FrameLayout header=new FrameLayout(c);QsHeaderScope.headers.add(header);try{
  View[] views={new ModernStatusBarWifiView(c),new ModernStatusBarMobileView(c),new BatteryMeterView(c),new com.android.systemui.statusbar.StatusBarIconView(c)};Palette p=Palette.STOCK.with(0,Color.RED).with(1,Color.GREEN).with(2,Color.BLUE).with(3,Color.MAGENTA);
  for(int i=0;i<views.length;i++){View view=views[i];header.addView(view);view.layout(0,0,40,30);assertEquals(i,IndicatorColorBackend.role(view));Bitmap bitmap=Bitmap.createBitmap(40,30,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);int before=canvas.getSaveCount();int save=IndicatorColorBackend.layer(view,canvas,p);assertTrue(save>0);canvas.drawColor(Color.WHITE);canvas.restoreToCount(save);assertEquals(before,canvas.getSaveCount());assertEquals(p.get(i),bitmap.getPixel(20,15));assertEquals(-1,IndicatorColorBackend.layer(view,canvas,Palette.STOCK));}
  var notification=new com.android.systemui.statusbar.StatusBarIconView(c);notification.notification=true;header.addView(notification);assertEquals(-1,IndicatorColorBackend.role(notification));var container=new com.android.systemui.statusbar.phone.NotificationIconContainer(c);header.addView(container);var fakeWifi=new ModernStatusBarWifiView(c);container.addView(fakeWifi);assertEquals(-1,IndicatorColorBackend.role(fakeWifi));var nested=new com.android.systemui.statusbar.StatusBarIconView(c);((LinearLayout)views[0]).addView(nested);assertEquals(-1,IndicatorColorBackend.role(nested));assertEquals(-1,IndicatorColorBackend.role(new ModernStatusBarWifiView(c)));
 }finally{QsHeaderScope.headers.clear();}}
}
