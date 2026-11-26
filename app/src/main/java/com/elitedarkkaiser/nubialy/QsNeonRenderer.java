package com.elitedarkkaiser.nubialy;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.util.LruCache;
import java.util.Arrays;
import java.util.function.BooleanSupplier;

/** Draw-only neon decoration; never changes the native drawable or its state. */
public final class QsNeonRenderer {
    private static final int FILL_ALPHA = 76; // 30% opacity, independent of the outline and glow.
    private static final int MAX_BITMAP_BYTES = 4 * 1024 * 1024;
    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(MAX_BITMAP_BYTES) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getAllocationByteCount(); }
    };
    private QsNeonRenderer() {}
    public static void clear() { CACHE.evictAll(); }

    public static boolean draw(Canvas canvas, GradientDrawable tile, float density,
            int first, int second, int intensity, BooleanSupplier permitted,
            QsOutlineRenderer.NativeDraw nativeDraw) throws Throwable {
        Rect bounds = tile.getBounds();
        if (!permitted.getAsBoolean() || bounds.isEmpty() || !Float.isFinite(density) || density <= 0
                || intensity <= 0 || intensity > 100 || bounds.width() > 2048 || bounds.height() > 2048
                || (tile.getShape() != GradientDrawable.RECTANGLE && tile.getShape() != GradientDrawable.OVAL)) return false;
        float[] radii = tile.getCornerRadii();
        if (radii == null) { radii = new float[8]; Arrays.fill(radii, tile.getCornerRadius()); }
        if (radii.length != 8) return false;
        for (float radius : radii) if (!Float.isFinite(radius) || radius < 0) return false;
        float blur = (2f + 14f * intensity / 100f) * density;
        int padding = (int) Math.ceil(blur * 2f);
        int width = bounds.width(), height = bounds.height();
        int bw = Math.max(1, (int) Math.ceil((width + 2f * padding) * .5f));
        int bh = Math.max(1, (int) Math.ceil((height + 2f * padding) * .5f));
        if ((long) bw * bh * 4 > MAX_BITMAP_BYTES) return false;
        String key = width + ":" + height + ":" + tile.getShape() + ":" + Arrays.toString(radii)
                + ":" + density + ":" + first + ":" + second + ":" + intensity;
        Bitmap bitmap = CACHE.get(key);
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
            Canvas software = new Canvas(bitmap); software.scale(.5f, .5f); software.translate(padding, padding);
            Path local = path(tile.getShape(), new RectF(0, 0, width, height), radii);
            Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
            glow.setStyle(Paint.Style.STROKE);
            glow.setStrokeWidth(2f * density);
            glow.setMaskFilter(new BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL));
            glow.setShader(new LinearGradient(0, 0, Math.max(1, width), 0, first, second, Shader.TileMode.CLAMP));
            glow.setAlpha(intensity * 65 * 255 / 10000);
            software.drawPath(local, glow);
            CACHE.put(key, bitmap);
        }
        RectF extent = new RectF(bounds); extent.inset(-padding, -padding);
        int saved = canvas.saveLayer(extent, null); boolean complete = false;
        try {
            if (!permitted.getAsBoolean()) return false;
            Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG); bitmapPaint.setAlpha(tile.getAlpha());
            canvas.drawBitmap(bitmap, null, extent, bitmapPaint);
            int fillLayer = canvas.saveLayerAlpha(new RectF(bounds), FILL_ALPHA);
            try { nativeDraw.draw(); }
            finally { canvas.restoreToCount(fillLayer); }
            if (!permitted.getAsBoolean()) return false;
            Path outline = path(tile.getShape(), new RectF(bounds), radii);
            int clip = canvas.save();
            try {
                canvas.clipPath(outline);
                Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG); edge.setStyle(Paint.Style.STROKE);
                edge.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.top,
                        first, second, Shader.TileMode.CLAMP));
                for (int layer = 5; layer >= 1; layer--) {
                    edge.setStrokeWidth(layer * 2f * density);
                    int alpha = tile.getAlpha() * intensity / 100;
                    edge.setAlpha(layer == 1 ? alpha * 9 / 10 : alpha / 10);
                    canvas.drawPath(outline, edge);
                }
            } finally { canvas.restoreToCount(clip); }
            if (!permitted.getAsBoolean()) return false;
            complete = true; return true;
        } finally {
            try { if (!complete) canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); }
            finally { canvas.restoreToCount(saved); }
        }
    }
    private static Path path(int shape, RectF bounds, float[] radii) {
        Path path = new Path();
        if (shape == GradientDrawable.OVAL) path.addOval(bounds, Path.Direction.CW);
        else path.addRoundRect(bounds, radii, Path.Direction.CW);
        return path;
    }
}
