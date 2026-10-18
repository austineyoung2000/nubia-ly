package com.elitedarkkaiser.nubialy;
import android.graphics.Canvas;
import android.graphics.Rect;
import org.junit.Test;
import static org.junit.Assert.*;
public final class LauncherArtworkMethodsTest {
 static class BitmapIcon {protected void c(Canvas canvas,Rect bounds){}}
 static class ThemedIcon extends BitmapIcon {@Override protected void c(Canvas canvas,Rect bounds){}}
 static class InheritedIcon extends BitmapIcon {}
 static class Unrelated {protected void c(Canvas canvas,Rect bounds){}}
 @Test public void overriddenThemedPathIsHookedWithoutMatchingUnrelatedClasses(){
  var method=LauncherArtworkMethods.override(BitmapIcon.class,ThemedIcon.class);assertNotNull(method);assertEquals(ThemedIcon.class,method.getDeclaringClass());
  assertNull(LauncherArtworkMethods.override(BitmapIcon.class,InheritedIcon.class));assertNull(LauncherArtworkMethods.override(BitmapIcon.class,Unrelated.class));assertNull(LauncherArtworkMethods.override(BitmapIcon.class,BitmapIcon.class));
 }
}
