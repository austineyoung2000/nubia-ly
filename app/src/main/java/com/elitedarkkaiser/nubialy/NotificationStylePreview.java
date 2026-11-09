package com.elitedarkkaiser.nubialy;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
/** Local sample cards only, never notification contents. */
final class NotificationStylePreview extends View {
    private final int style;
    NotificationStylePreview(Context context,int style){super(context);this.style=style;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    static void paint(Canvas c,int style,float w,float h,float density){paint(c,style,w,h,density,0);}
    static void paint(Canvas c,int style,float w,float h,float density,int appearance){paint(c,style,w,h,density,appearance,SurfaceStyle.Palette.STOCK);}
    static void paint(Canvas c,int style,float w,float h,float density,int appearance,SurfaceStyle.Palette palette){if(w<16||h<16)return;Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        for(int i=0;i<2;i++){float margin=w*.04f,gap=h*.04f,top=i*h/2+gap;RectF b=new RectF(margin,top,w-margin,(i+1)*h/2-gap);float radius=Math.min(b.height()/3,16*density);int customRadius=NotificationControls.radius(appearance);if(customRadius!=0)radius=Math.max(radius,customRadius*density);float[] radii=new float[8];java.util.Arrays.fill(radii,radius);GradientDrawable bg=new GradientDrawable();bg.setColor(0xff66616f);bg.setCornerRadius(radius);bg.setBounds(Math.round(b.left),Math.round(b.top),Math.round(b.right),Math.round(b.bottom));
            int opacity=NotificationControls.opacity(appearance);int layer=c.saveLayerAlpha(b.left,b.top,b.right,b.bottom,opacity==0?255:opacity*255/100);
            try{bg.draw(c);if(palette.notificationMode()!=0)NotificationEffects.color(c,b,radii,palette.notificationFirst(),palette.notificationSecond(),palette.notificationMode());NotificationEffects.draw(style,c,b,radii,density,()->true,()->{});}catch(Throwable unavailable){bg.draw(c);}finally{c.restoreToCount(layer);}
            float y=b.top+b.height()*.38f;p.setColor(palette.notificationText()==0?0xffd3cbef:palette.notificationText());c.drawCircle(b.left+b.height()*.28f,y,b.height()*.09f,p);float x=b.left+b.height()*.5f;p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(Math.max(2,b.height()*.055f));c.drawLine(x,y,b.left+b.width()*.7f,y,p);p.setColor(palette.notificationText()==0?0xffc8c1d1:palette.notificationText());c.drawLine(x,y+b.height()*.24f,b.left+b.width()*.86f,y+b.height()*.24f,p);
        }
    }
    @Override protected void onDraw(Canvas c){super.onDraw(c);paint(c,style,getWidth(),getHeight(),getResources().getDisplayMetrics().density);}
}
