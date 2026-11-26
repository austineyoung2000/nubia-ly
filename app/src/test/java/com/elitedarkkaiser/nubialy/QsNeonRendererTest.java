package com.elitedarkkaiser.nubialy;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class QsNeonRendererTest {
    @Test public void translucentFillPreservesNativeStateAndBrightNeonEdge() throws Throwable {
        for (boolean active : new boolean[]{false, true}) {
            GradientDrawable tile = new GradientDrawable();
            tile.setColor(active ? Color.BLUE : Color.RED);
            tile.setBounds(0, 0, 160, 160);
            tile.setCornerRadius(40);
            Bitmap image = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(image);
            assertTrue(QsNeonRenderer.draw(canvas, tile, 1f, Color.GREEN, Color.GREEN, 100,
                    () -> true, () -> tile.draw(canvas)));
            int center = image.getPixel(80, 80);
            assertEquals(76, Color.alpha(center), 5);
            assertTrue("Neon edge should remain brighter than the fill",
                    Color.alpha(image.getPixel(2, 80)) > Color.alpha(center));
            assertEquals(255, tile.getAlpha());
            assertEquals(active ? 255 : 0, Color.blue(center), 5);
            image.recycle();
            QsNeonRenderer.clear();
        }
    }
}
