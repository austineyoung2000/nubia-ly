package com.elitedarkkaiser.nubialy;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.function.Supplier;
/** Fixed charging/normal samples; never subscribes to battery data. */
final class BatteryControlsPreview extends View {
 private final Supplier<StatusBarStyle> draft;
 BatteryControlsPreview(Context context,Supplier<StatusBarStyle> draft){super(context);this.draft=draft;setContentDescription("Illustrative battery controls: normal and charging samples");}
 @Override protected void onDraw(Canvas c){super.onDraw(c);int save=c.save();try{c.scale(getWidth()/300f,getHeight()/90f);StatusBarStyle bar=draft.get();draw(c,bar,75,32,false);draw(c,bar,225,32,true);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xffd5cedf);p.setTextSize(11);p.setTextAlign(Paint.Align.CENTER);c.drawText("Normal · sample",75,75,p);c.drawText("Charging · sample",225,75,p);}finally{c.restoreToCount(save);}}
 static void draw(Canvas c,StatusBarStyle bar,float x,float y,boolean charging){SurfaceStyle s=bar.surfaces();if(bar.selected(StatusBarStyle.HIDE_BATTERY)||s.batteryStyle()==BatteryStyles.HIDDEN)return;int save=c.save();try{c.translate(x,y);c.scale(bar.batteryScale()/100f,bar.batteryScale()/100f);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(s.palette().batteryText()==0?0xffeee8f5:s.palette().batteryText());p.setTextSize(12);p.setTextAlign(Paint.Align.CENTER);int mode=s.batteryPercent();boolean outside=mode==0||mode==2,charge=charging&&s.batteryCharge()==0;float left=outside&&s.batteryStyle()!=BatteryStyles.TEXT?-13:0;BatteryRenderer.draw(s.batteryStyle()==0?7:s.batteryStyle(),c,new RectF(left-23,-11,left+23,11),85,charge,false,mode==1,s.palette().battery()==0?0xffeee8f5:s.palette().battery(),s.palette().battery()==0?0xffaaa2b6:s.palette().battery(),s.palette().batteryText(),()->true);if(outside&&s.batteryStyle()!=BatteryStyles.TEXT)c.drawText("85%",28,4,p);}finally{c.restoreToCount(save);}}
}
