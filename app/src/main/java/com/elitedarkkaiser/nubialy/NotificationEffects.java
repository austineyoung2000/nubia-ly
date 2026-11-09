package com.elitedarkkaiser.nubialy;
import android.graphics.*;
import java.util.function.BooleanSupplier;
/** Static overlays over native notification backgrounds; native blur is never flattened. */
public final class NotificationEffects {
    public static final String[] NAMES={"Stock","Duoline","CyberPunk","Fluid","iOS","Layers"};
    private NotificationEffects(){}
    public static boolean draw(int style,Canvas canvas,RectF b,float[] corners,float density,BooleanSupplier allowed,QsOutlineRenderer.NativeDraw stock)throws Throwable{
        if(style<1||style>5||!allowed.getAsBoolean()||b.isEmpty()||corners==null||corners.length!=8||!Float.isFinite(density)||density<=0)return false;
        for(float r:corners)if(!Float.isFinite(r)||r<0)return false;
        stock.draw();if(!allowed.getAsBoolean())return true;
        int save=canvas.saveLayer(b,null);boolean complete=false;
        try{canvas.clipPath(outline(b,corners,0));Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);float u=Math.min(density,Math.min(b.width(),b.height())/12f);
            switch(style){
                case 1->{p.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,0x90ffffff,0x60000000,Shader.TileMode.CLAMP));canvas.drawRect(b.left,b.top,b.right,b.top+3*u,p);canvas.drawRect(b.left,b.bottom-3*u,b.right,b.bottom,p);}
                case 2->{p.setColor(0x45000000);canvas.drawRect(b,p);p.setColor(0x28ffffff);Path facets=new Path();facets.moveTo(b.left,b.top+6*u);facets.lineTo(b.centerX()-3*u,b.top+6*u);facets.lineTo(b.centerX()+3*u,b.bottom-3*u);facets.lineTo(b.left,b.bottom-3*u);facets.close();facets.moveTo(b.centerX()+3*u,b.top+3*u);facets.lineTo(b.right,b.top+3*u);facets.lineTo(b.right,b.bottom-6*u);facets.lineTo(b.centerX()+9*u,b.bottom-6*u);facets.close();canvas.drawPath(facets,p);}
                case 3->{p.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,0x20ffffff,0x08000000,Shader.TileMode.CLAMP));canvas.drawRect(b,p);}
                case 4->{p.setColor(0x50000000);canvas.drawRect(b.left,b.bottom-12*u,b.right,b.bottom,p);p.setColor(0x28ffffff);canvas.drawRect(b.left+12*u,b.bottom-8*u,b.right-12*u,b.bottom-4*u,p);p.setColor(0x40ffffff);canvas.drawRect(b.left+24*u,b.bottom-4*u,b.right-24*u,b.bottom,p);}
                case 5->{p.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,0x65ffffff,0x60000000,Shader.TileMode.CLAMP));Path ring=outline(b,corners,0);ring.setFillType(Path.FillType.EVEN_ODD);ring.addPath(outline(b,corners,4*u));canvas.drawPath(ring,p);}
                default->throw new IllegalArgumentException();
            }
            if(!allowed.getAsBoolean())return true;complete=true;return true;
        }finally{try{if(!complete)canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);}finally{canvas.restoreToCount(save);}}
    }
    /** Tint only the background pass; preserve native clipping and leave content untouched. */
    public static void color(Canvas canvas,RectF b,float[] corners,int first,int second,int mode){
        if(mode<1||mode>3||b.isEmpty()||corners==null||corners.length!=8)return;
        for(float r:corners)if(!Float.isFinite(r)||r<0)return;
        int a=(first==0?0xff9b83d6:first)&0x00ffffff|0x70000000;
        int z=(second==0?0xff4c9ac7:second)&0x00ffffff|0x70000000;
        int save=canvas.save();try{canvas.clipPath(outline(b,corners,0));Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            if(mode==2)p.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,a,z,Shader.TileMode.CLAMP));
            if(mode==3){p.setColor(a);canvas.drawRect(b.left,b.top,b.right,b.centerY(),p);p.setColor(z);canvas.drawRect(b.left,b.centerY(),b.right,b.bottom,p);}else{if(mode==1)p.setColor(a);canvas.drawRect(b,p);}
        }finally{canvas.restoreToCount(save);}
    }
    private static Path outline(RectF bounds,float[] corners,float inset){RectF b=new RectF(bounds);b.inset(inset,inset);Path p=new Path();if(b.isEmpty())return p;float[] radii=corners.clone();for(int i=0;i<8;i++)radii[i]=Math.max(0,radii[i]-inset);p.addRoundRect(b,radii,Path.Direction.CW);return p;}
}
