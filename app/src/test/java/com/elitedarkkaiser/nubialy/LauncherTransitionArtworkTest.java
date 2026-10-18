package com.elitedarkkaiser.nubialy;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=36)
public final class LauncherTransitionArtworkTest {
 private static class Host {
  private View r;
  private void icon(Drawable main,Drawable badge,Supplier<Drawable> source,int inset){}
  private void setOriginalDrawableBackground(Supplier<Drawable> source){}
 }
 private static class Ambiguous {
  private View r;
  private void icon(Drawable a,Drawable b,Supplier<Drawable> s,int i){}
  private void another(Drawable a,Drawable b,Supplier<Drawable> s,int i){}
  private void setOriginalDrawableBackground(Supplier<Drawable> s){}
 }
 private static class WrongSource {
  private Object r;
  private void icon(Drawable a,Drawable b,Supplier<Drawable> s,int i){}
  private void setOriginalDrawableBackground(Supplier<Drawable> s){}
 }
 private static class Native extends Drawable {
  int draws,alpha=177;ColorFilter filter;boolean fail;
  @Override public void draw(Canvas c){draws++;if(fail)throw new IllegalStateException();}
  @Override public void setAlpha(int a){alpha=a;}
  @Override public int getAlpha(){return alpha;}
  @Override public void setColorFilter(ColorFilter f){filter=f;}
  @Override public ColorFilter getColorFilter(){return filter;}
  @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
  @Override public int getIntrinsicWidth(){return 96;}
  @Override public int getIntrinsicHeight(){return 96;}
 }
 @Test public void resolvesOnlyTheInspectedSignatures()throws Exception {
  var targets=LauncherTransitionArtwork.resolve(Host.class);assertEquals("r",targets.source().getName());assertEquals("icon",targets.setIcon().getName());
  assertEquals("setOriginalDrawableBackground",targets.setBackground().getName());
  for(Class<?> c:new Class<?>[]{Ambiguous.class,WrongSource.class})try{LauncherTransitionArtwork.resolve(c);fail();}catch(ReflectiveOperationException expected){}
 }
 @Test public void customArtworkReplacesStockUntilAuthorityExpires(){
  var original=new Native();original.setBounds(1,2,97,98);var bounds=new Rect(original.getBounds());var allowed=new AtomicBoolean(true);
  Bitmap bitmap=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);var canvas=new Canvas(Bitmap.createBitmap(200,200,Bitmap.Config.ARGB_8888));
  for(int shape=0;shape<=4;shape++){
   Drawable icon=LauncherTransitionArtwork.wrap(original,bitmap,shape,allowed::get);icon.setBounds(5,6,101,102);icon.setAlpha(128);
   int before=original.draws;icon.draw(canvas);assertEquals(before,original.draws);assertEquals(bounds,original.getBounds());assertEquals(177,original.getAlpha());assertEquals(96,icon.getIntrinsicWidth());
   allowed.set(false);icon.draw(canvas);assertEquals(before+1,original.draws);assertEquals(bounds,original.getBounds());assertEquals(177,original.getAlpha());allowed.set(true);
  }
 }
 @Test public void fallbackRestoresNativeStateEvenOnFailure(){
  var original=new Native();original.setBounds(2,3,98,99);var bounds=new Rect(original.getBounds());ColorFilter nativeFilter=new PorterDuffColorFilter(0xff123456,PorterDuff.Mode.SRC_IN);original.setColorFilter(nativeFilter);
  Bitmap bitmap=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);Drawable icon=LauncherTransitionArtwork.wrap(original,bitmap,0,()->false);icon.setBounds(0,0,50,50);icon.setAlpha(80);icon.setColorFilter(new PorterDuffColorFilter(0xff654321,PorterDuff.Mode.SRC_IN));
  original.fail=true;try{icon.draw(new Canvas());fail();}catch(IllegalStateException expected){}
  assertEquals(bounds,original.getBounds());assertEquals(177,original.getAlpha());assertSame(nativeFilter,original.getColorFilter());
  assertSame(original,LauncherTransitionArtwork.wrap(original,null,0,()->true));
 }
}
