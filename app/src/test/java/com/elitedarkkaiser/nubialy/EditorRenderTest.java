package com.elitedarkkaiser.nubialy;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowBuild;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

/** Render the real editor hierarchy, including navigation, at a phone viewport. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="w440dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class EditorRenderTest {
 @Test public void phoneEditorsKeepPreviewProportionsAndActionsVisible() throws Exception {
  ShadowBuild.setModel("NX809J");
  for(String[] route:new String[][]{{"lock_screen","clock"},{"quick_settings","tiles"},{"quick_settings","colors"},{"quick_settings","header"},{"quick_settings","grid"}}){
   var controller=Robolectric.buildActivity(ClockEditorActivity.class,new Intent().putExtra("section",route[0])).setup();
   var activity=controller.get();EditorPages pages=ReflectionHelpers.getField(activity,"pages");pages.show(route[0],route[1]);
   var root=(android.view.ViewGroup)activity.findViewById(android.R.id.content);measure(root);
   View artwork=root.findViewWithTag("editor-preview:"+route[0]);assertNotNull(artwork);assertEquals(2f,artwork.getWidth()/(float)artwork.getHeight(),.02f);
   View host=root.findViewWithTag("editor-preview-host"),tabs=root.findViewWithTag("editor-tabs"),actions=root.findViewWithTag("editor-actions");
   assertTrue("Preview precedes tabs",host.getBottom()<=tabs.getTop());assertTrue("Actions stay on screen",actions.getBottom()<=900);assertTrue("Controls have room",actions.getTop()-tabs.getBottom()>250);
   Bitmap bitmap=Bitmap.createBitmap(440,900,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));File output=new File("build/editor-previews",route[0]+"-"+route[1]+".png");output.getParentFile().mkdirs();try(var stream=new FileOutputStream(output)){bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);}bitmap.recycle();controller.pause().stop().destroy();
  }
 }
 private void measure(View root){root.measure(View.MeasureSpec.makeMeasureSpec(440,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));root.layout(0,0,440,900);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();root.measure(View.MeasureSpec.makeMeasureSpec(440,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));root.layout(0,0,440,900);}
}
