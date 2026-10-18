package com.elitedarkkaiser.nubialy;
import android.graphics.*;
/** Masks only icon artwork; badges and text are drawn separately by the launcher. */
public final class LauncherIconShapes {
    public static final String[] NAMES={"Native","Circle","Rounded square","Squircle","Teardrop"};
    private LauncherIconShapes(){}
    public static Path path(int shape,RectF b){
        Path p=new Path();float r=Math.min(b.width(),b.height());
        switch(shape){case 1->p.addOval(b,Path.Direction.CW);case 2->p.addRoundRect(b,r*.22f,r*.22f,Path.Direction.CW);
            case 3->{float x=b.centerX(),y=b.centerY(),a=b.width()/2,c=b.height()/2;p.moveTo(x+a,y);for(int i=1;i<=64;i++){double t=i*Math.PI*2/64,cos=Math.cos(t),sin=Math.sin(t);p.lineTo(x+(float)(a*Math.copySign(Math.sqrt(Math.abs(cos)),cos)),y+(float)(c*Math.copySign(Math.sqrt(Math.abs(sin)),sin)));}p.close();}
            case 4->p.addRoundRect(b,new float[]{r*.5f,r*.5f,r*.08f,r*.08f,r*.5f,r*.5f,r*.5f,r*.5f},Path.Direction.CW);
            default->p.addRect(b,Path.Direction.CW);}
        return p;
    }
    public static Bitmap raster(android.graphics.drawable.Drawable source,boolean unmask){
        Bitmap image=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);Rect original=new Rect(source.getBounds());
        try{if(unmask&&source instanceof android.graphics.drawable.AdaptiveIconDrawable adaptive){for(var layer:new android.graphics.drawable.Drawable[]{adaptive.getBackground(),adaptive.getForeground()}){if(layer==null)continue;Rect bounds=new Rect(layer.getBounds());try{layer.setBounds(-48,-48,240,240);layer.draw(canvas);}finally{layer.setBounds(bounds);}}}
            else{source.setBounds(0,0,192,192);source.draw(canvas);}return image;
        }finally{source.setBounds(original);}
    }
    public static void draw(Canvas canvas,Bitmap image,Rect bounds,Paint nativePaint,int shape){int save=canvas.save();try{if(shape>0)canvas.clipPath(path(shape,new RectF(bounds)));canvas.drawBitmap(image,null,bounds,nativePaint);}finally{canvas.restoreToCount(save);}}
}
