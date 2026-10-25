package com.elitedarkkaiser.nubialy;
import android.content.Context;import android.graphics.*;import android.view.View;import java.util.function.Supplier;
/** Illustrative Settings layout, not live device data. */
public final class SettingsLayoutPreview extends View {
 private final Supplier<SettingsStyle> read;
 public SettingsLayoutPreview(Context c,Supplier<SettingsStyle> s){super(c);read=s;setContentDescription("Original Settings appearance preview");}
 @Override protected void onDraw(Canvas c){super.onDraw(c);SettingsStyle s=read.get();int save=c.save();try{c.scale(getWidth()/180f,getHeight()/250f);Paint p=new Paint(3);p.setColor(s.background());c.drawRoundRect(0,0,180,250,20,20,p);p.setColor(s.title());p.setTextSize(14);c.drawText("Settings",14,27,p);int y=44;for(int i=0;i<5;i++){p.setColor(s.card());c.drawRoundRect(12,y,168,y+24,4,4,p);p.setColor(s.accent());c.drawCircle(25,y+12,6,p);p.setColor(s.title());c.drawRoundRect(40,y+7,110,y+10,1,1,p);p.setColor(s.summary());c.drawRoundRect(40,y+15,135,y+17,1,1,p);y+=29;}p.setColor(s.card());c.drawRoundRect(12,224,168,241,9,9,p);p.setColor(s.summary());p.setTextSize(8);c.drawText("Search settings",28,235,p);}finally{c.restoreToCount(save);}}
}
