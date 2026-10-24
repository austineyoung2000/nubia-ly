package com.elitedarkkaiser.nubialy;

import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Animatable;
import android.os.SystemClock;

/** Bounded single-atlas cache; playback never decodes or recycles shared frames. */
public final class FingerprintAnimationArtwork {
    private static Resources resources;
    private static Frames cache;
    public record Frames(int id,Bitmap bitmap) {
        public Rect source(int frame){return new Rect((frame%8)*256,(frame/8)*256,(frame%8+1)*256,(frame/8+1)*256);}
    }
    public static synchronized void resources(Resources value){if(resources!=value){resources=value;cache=null;}}
    public static synchronized Frames load(int id){
        if(!FingerprintAnimations.valid(id))throw new IllegalArgumentException();if(id==0)return null;
        if(cache!=null&&cache.id()==id)return cache;if(resources==null)throw new IllegalStateException("Animation resources unavailable");
        int resource=resources.getIdentifier("rising_anim_atlas_"+id,"drawable","com.elitedarkkaiser.nubialy");
        if(resource==0)throw new IllegalStateException("Animation atlas missing");
        BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inPreferredConfig=Bitmap.Config.ARGB_8888;
        Bitmap bitmap=BitmapFactory.decodeResource(resources,resource,options);
        int count=FingerprintAnimations.DURATIONS[id].length;
        if(bitmap==null||bitmap.getWidth()!=2048||bitmap.getHeight()!=((count+7)/8)*256||bitmap.getAllocationByteCount()>16*1024*1024)throw new IllegalStateException("Animation atlas invalid");
        cache=new Frames(id,bitmap);return cache;
    }
    public static synchronized void clear(){cache=null;}
    public static final class Preview extends Drawable implements Animatable,Runnable {
        private final int speed;private final Frames frames;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);private long started;private boolean running;
        public Preview(Frames frames){this(frames,100);}
        public Preview(Frames frames,int speed){this.frames=frames;this.speed=FingerprintAnimations.validSpeed(speed)?speed:100;}
        @Override public void draw(Canvas canvas){canvas.drawBitmap(frames.bitmap(),frames.source(FingerprintAnimations.frame(frames.id(),(SystemClock.uptimeMillis()-started)*speed/100)),getBounds(),paint);}
        @Override public void start(){stop();running=true;started=SystemClock.uptimeMillis();run();}
        @Override public void stop(){running=false;unscheduleSelf(this);}
        @Override public boolean isRunning(){return running;}
        @Override public void run(){if(!running)return;invalidateSelf();scheduleSelf(this,SystemClock.uptimeMillis()+16);}
        @Override public void setAlpha(int value){paint.setAlpha(value);invalidateSelf();}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getIntrinsicWidth(){return 256;}
        @Override public int getIntrinsicHeight(){return 256;}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
