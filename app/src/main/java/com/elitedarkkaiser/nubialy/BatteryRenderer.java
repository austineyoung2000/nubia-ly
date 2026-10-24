package com.elitedarkkaiser.nubialy;
import android.graphics.*;
import androidx.core.graphics.PathParser;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
/** Shared preview/live renderer. Paths keep their aspect ratio inside the supplied native footprint. */
public final class BatteryRenderer {
 private record Paths(BatteryGeometry.Shape geometry,Path perimeter,Path fill,Path bolt,Path plus,RectF fillBounds){}
 private static final ConcurrentHashMap<Integer,Paths> cache=new ConcurrentHashMap<>();
 private BatteryRenderer(){}
 private static Paths paths(int style){return cache.computeIfAbsent(style,id->{var geometry=BatteryGeometry.shape(id);if(geometry==null)throw new IllegalArgumentException();Path perimeter=path(geometry.perimeter()),fill=path(geometry.fill()),bolt=path(geometry.bolt()),plus=path(geometry.plus());RectF bounds=new RectF();fill.computeBounds(bounds,true);if(bounds.isEmpty())throw new IllegalArgumentException();return new Paths(geometry,perimeter,fill,bolt,plus,bounds);});}
 private static Path path(String data){Path p=PathParser.createPathFromPathData(data);if(p==null||p.isEmpty())throw new IllegalArgumentException("Missing battery path");return p;}
 public static boolean draw(int style,Canvas c,RectF bounds,int level,boolean charging,boolean saver,boolean percent,int foreground,int frame,BooleanSupplier allowed){
  return draw(style,c,bounds,level,charging,saver,percent,foreground,frame,0,allowed);
 }
 public static boolean draw(int style,Canvas c,RectF bounds,int level,boolean charging,boolean saver,boolean percent,int foreground,int frame,int textColor,BooleanSupplier allowed){
  if(!BatteryStyles.valid(style)||style==0||level<0||level>100||bounds==null||bounds.isEmpty()||!Float.isFinite(bounds.width())||!Float.isFinite(bounds.height())||!allowed.getAsBoolean())return false;
  if(style==BatteryStyles.HIDDEN)return allowed.getAsBoolean();
  int save=c.saveLayer(bounds,null);boolean complete=false;
  try{c.clipRect(bounds);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);boolean circle=style==2||style==3||style==4||style==9||style==10;Paths paths=circle||style==BatteryStyles.TEXT||style==24?null:paths(style);
   float width=circle?24:style==BatteryStyles.TEXT?36:style==24?24:paths.geometry.width(),height=circle?24:style==BatteryStyles.TEXT?14:style==24?12:paths.geometry.height();float scale=Math.min(bounds.width()/width,bounds.height()/height);if(!Float.isFinite(scale)||scale<=0)return false;
   c.translate(bounds.centerX()-width*scale/2,bounds.centerY()-height*scale/2);c.scale(scale,scale);int fg=foreground,dim=(foreground&0x00ffffff)|((Color.alpha(foreground)*45/255)<<24);
   if(style==BatteryStyles.TEXT){text(c,p,(charging?"ϟ ":saver?"+ ":"")+java.text.NumberFormat.getPercentInstance().format(level/100d),width/2,height/2,height*.82f,width,textColor==0?fg:textColor);}
   else if(circle){float radius=style==9||style==10?10.1f:9.2f,stroke=24f/6.5f;p.setColor(dim);if(style==4){c.drawCircle(12,12,radius,p);p.setColor(fg);c.drawCircle(12,12,radius*level/100f,p);}else{p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(stroke);p.setColor(frame);if(style==3||style==10)p.setPathEffect(new DashPathEffect(new float[]{3,2},0));RectF ring=new RectF(12-radius,12-radius,12+radius,12+radius);c.drawArc(ring,-90,360,false,p);p.setColor(fg);if(level>0)c.drawArc(ring,-90,360*level/100f,false,p);p.setStyle(Paint.Style.FILL);p.setPathEffect(null);}
    if(charging||saver)text(c,p,charging?"ϟ":"+",12,12,12,14,fg);else if(percent)text(c,p,Integer.toString(level),12,12,8.2f,15,textColor!=0?textColor:style==4?contrast(fg):fg);
   }else if(style==24){RectF pill=new RectF(.6f,.6f,23.4f,11.4f);p.setColor(dim);c.drawRoundRect(pill,5.4f,5.4f,p);int clip=c.save();try{Path rounded=new Path();rounded.addRoundRect(pill,5.4f,5.4f,Path.Direction.CW);c.clipPath(rounded);c.clipRect(pill.left,pill.top,pill.left+pill.width()*level/100f,pill.bottom);p.setColor(fg);c.drawRect(pill,p);}finally{c.restoreToCount(clip);}if(charging||saver)text(c,p,charging?"ϟ":"+",12,6,9,15,contrast(fg));else if(percent)text(c,p,Integer.toString(level),12,6,8,19,textColor==0?contrast(fg):textColor);
   }else{p.setColor(dim);c.drawPath(paths.fill,p);p.setColor(paths.geometry.perimeter().equals(paths.geometry.fill())?dim:frame);c.drawPath(paths.perimeter,p);RectF fill=paths.fillBounds;int clip=c.save();try{c.clipPath(paths.fill);float fraction=level/100f;switch(paths.geometry.direction()){case 1->c.clipRect(fill.left,fill.top,fill.left+fill.width()*fraction,fill.bottom);case 2->c.clipRect(fill.right-fill.width()*fraction,fill.top,fill.right,fill.bottom);default->c.clipRect(fill.left,fill.bottom-fill.height()*fraction,fill.right,fill.bottom);}p.setColor(fg);if(level>0)c.drawPath(paths.fill,p);}finally{c.restoreToCount(clip);}
    if(charging||saver){if(!charging&&paths.geometry.plus().equals(paths.geometry.perimeter()))text(c,p,"+",fill.centerX(),fill.centerY(),Math.min(9,fill.height()*.8f),fill.width()*.9f,contrast(fg));else{p.setColor(contrast(fg));c.drawPath(charging?paths.bolt:paths.plus,p);}}else if(percent){text(c,p,Integer.toString(level),fill.centerX(),fill.centerY(),Math.min(8,fill.height()*.72f),fill.width()*.90f,textColor==0?contrast(fg):textColor);}
   }
   if(!allowed.getAsBoolean())return false;complete=true;return true;
  }finally{try{if(!complete)c.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);}finally{c.restoreToCount(save);}}
 }
 private static int contrast(int color){return Color.luminance(color)>.45?Color.BLACK:Color.WHITE;}
 private static void text(Canvas c,Paint p,String label,float x,float y,float size,float width,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);float measured=p.measureText(label);if(measured>width&&measured>0)p.setTextSize(size*width/measured);Paint.FontMetrics metrics=p.getFontMetrics();c.drawText(label,x,y-(metrics.ascent+metrics.descent)/2,p);}
}
