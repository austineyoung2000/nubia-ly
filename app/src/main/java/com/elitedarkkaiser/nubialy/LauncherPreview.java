package com.elitedarkkaiser.nubialy;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Illustrative launcher artwork. Never reads the user's home-screen layout. */
final class LauncherPreview extends View {
    private LauncherStyle style=LauncherStyle.STOCK;
    void setStyle(LauncherStyle value){style=value;invalidate();}
    LauncherPreview(Context context) { super(context); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    @Override protected void onDraw(Canvas canvas) { super.onDraw(canvas); paint(canvas,getWidth(),getHeight(),style); }
    static void paint(Canvas c,float width,float height){paint(c,width,height,LauncherStyle.STOCK);}
    static void paint(Canvas c,float width,float height,LauncherStyle style) {
        if(width<=0||height<=0)return;
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        float scale=Math.min(width/250f,height/110f),left=(width-250*scale)/2,top=(height-110*scale)/2;
        c.save();c.translate(left,top);c.scale(scale,scale);
        p.setColor(0xff33333c);c.drawRoundRect(0,0,250,110,24,24,p);
        int[] colors={0xffd3cbef,0xffb8c9bf,0xffe8c4ac,0xffb9c5e5};
        for(int row=0;row<2;row++)for(int col=0;col<4;col++){
            float x=30+col*62,y=25+row*50;
            float r=12*style.iconScale()/100f;p.setColor(colors[(col+row)%colors.length]);c.drawRoundRect(x-r,y-r,x+r,y+r,8,8,p);
            if(!style.hideLabels())p.setColor(LauncherStyle.labelTint(style.labelColor(),0xffc2bec9));else p.setColor(0x00000000);c.drawRoundRect(x-14,y+17,x+14,y+20,1.5f,1.5f,p);
        }
        c.restore();
    }
}
