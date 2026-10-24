package com.elitedarkkaiser.nubialy.xposed;
import android.content.Context;import android.view.View;import android.widget.*;import java.lang.reflect.*;import java.util.Set;
import org.junit.Test;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class QsHeaderAppearanceScopeTest {
 public static class Clock extends TextView {public Clock(Context c){super(c);}}
 public static class DateView extends TextView {public DateView(Context c){super(c);}}
 public static class StatusIconContainer extends LinearLayout {public StatusIconContainer(Context c){super(c);}}
 private int role(View v)throws Exception{Method m=QsHeaderAppearance.class.getDeclaredMethod("role",View.class);m.setAccessible(true);return (Integer)m.invoke(null,v);}
 @SuppressWarnings("unchecked") @Test public void onlyRecognizedViewsInsideTheCapturedHeaderAreStyled()throws Exception{
  Context c=RuntimeEnvironment.getApplication();FrameLayout header=new FrameLayout(c),outside=new FrameLayout(c);Clock clock=new Clock(c),foreignClock=new Clock(c);DateView date=new DateView(c);StatusIconContainer icons=new StatusIconContainer(c);Clock nested=new Clock(c);icons.addView(nested);header.addView(clock);header.addView(date);header.addView(icons);outside.addView(foreignClock);
  Field f=QsHeaderAppearance.class.getDeclaredField("headers");f.setAccessible(true);Set<View> headers=(Set<View>)f.get(null);headers.clear();headers.add(header);
  assertEquals(0,role(clock));assertEquals(1,role(date));assertEquals(2,role(icons));assertEquals(-1,role(foreignClock));assertEquals(-1,role(nested));assertEquals(-1,role(header));headers.clear();assertEquals(-1,role(clock));
 }
}
