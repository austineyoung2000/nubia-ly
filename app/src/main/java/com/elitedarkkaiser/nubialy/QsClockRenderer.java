package com.elitedarkkaiser.nubialy;
import android.graphics.*;
/** Header clock artwork uses the native clock's text, so format/timezone updates stay native. */
public final class QsClockRenderer {
 public static final String[] NAMES={"Native","Digital","Stacked","Outline"};
 private QsClockRenderer(){}
 public static void draw(Canvas canvas,RectF area,CharSequence time,Paint source,int font,int style,int color){
  if(area.isEmpty()||time==null||time.length()==0)return;Paint paint=new Paint(source);paint.setAntiAlias(true);paint.setColor(color);paint.setTextAlign(Paint.Align.CENTER);
  if(font!=0){var face=LockClockFonts.FACES[font];paint.setTypeface(Typeface.create(face.family(),face.style()));}
  if(font==0&&style==1)paint.setTypeface(Typeface.MONOSPACE);else if(font==0&&style==2)paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
  String value=time.toString();String[] lines=style==2?value.split("[:：]",2):new String[]{value};float unit=area.height()/lines.length;float size=unit*.84f;paint.setTextSize(size);
  float max=0;for(String line:lines)max=Math.max(max,paint.measureText(line));if(max>area.width())paint.setTextSize(size*area.width()/max);
  if(style==3){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1,paint.getTextSize()/40));}
  Paint.FontMetrics metrics=paint.getFontMetrics();for(int i=0;i<lines.length;i++){float y=area.top+unit*(i+.5f)-(metrics.ascent+metrics.descent)/2;canvas.drawText(lines[i],area.centerX(),y,paint);}
 }
}
