package com.elitedarkkaiser.nubialy;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.function.Supplier;

/** An illustrative app-only draft, with no timers or device access. */
final class EditorPreview extends View {
    record Draft(StatusBarStyle bar,int color,int iconColor,int size,boolean bold,boolean italic,boolean hidden,int family,float spacing){}
    private int tileStyle;
    private final String section;private final Supplier<Draft> draft;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    EditorPreview(Context context,String section,Supplier<Draft> draft){super(context);this.section=section;this.draft=draft;setContentDescription("Illustrative "+section.replace('_',' ')+" draft preview");}
    @Override protected void onDraw(Canvas c){super.onDraw(c);if(getWidth()==0)return;Draft d=draft.get();SurfaceStyle s=d.bar.surfaces();tileStyle=s.qsStyle();float height=section.equals("lock_screen")?280:section.equals("quick_settings")?180:section.equals("volume")?220:section.equals("notifications")?180:100;
        c.save();c.scale(getWidth()/360f,getHeight()/height);
        paint.setShader(new LinearGradient(0,0,360,height,new int[]{0xff494253,0xff272932,0xff414a50},null,Shader.TileMode.CLAMP));c.drawRoundRect(0,0,360,height,28,28,paint);paint.setShader(null);
        if(section.equals("lock_screen")){
            paint.setColor(0xff64677b);c.drawCircle(344,298,125,paint);paint.setColor(0xff6e5b75);c.drawCircle(0,320,114,paint);
            var face=LockClockFonts.FACES[s.clockFont()];paint.setTypeface(Typeface.create(face.family(),face.style()));
            if(!s.hide(2))drawText(c,"12:34",180,100,54,s.appearance(2),s.position(2),0xfff1e8ff);
            paint.setTypeface(Typeface.create("sans-serif",0));if(!s.hide(3))drawText(c,"Thursday, 15 October",180,130,13,s.appearance(3),s.position(3),0xffeee6f5);
            String widgets="";if((s.widgetsHidden()&1)==0)widgets+="21°  ";if((s.widgetsHidden()&2)==0)widgets+="Alarm 7:00  ";if((s.widgetsHidden()&4)==0)widgets+="85%";
            plain(c,widgets,180,160,12,0xffd9d1e1);if(s.chargingInfo()!=0){plain(c,"Super charging · 85%",180,181,10,0xffded3fa);plain(c,"Battery 4.420 V · +2,350 mA · 36.5 °C",180,197,9,0xffded3fa);}
            if(s.fingerprintHidden()==0){var icon=s.fingerprintStyle()==0?getContext().getDrawable(R.drawable.rising_fp_udfps_icon_default):new com.elitedarkkaiser.nubialy.xposed.FingerprintGlyphDrawable(s.fingerprintStyle(),0xffded3fa);if(icon!=null){icon.setBounds(159,209,201,251);icon.draw(c);}}
        }else if(section.equals("quick_settings")){
            for(int i=0;i<4;i++){float x=20+(i%2)*164,y=18+(i/2)*65;paint.setColor(i==0?0xffd3cbef:0xff55545f);shape(c,x,y,156,55,s.largeTileShape());
                paint.setTypeface(Typeface.create("sans-serif-medium",0));if(!s.hide(0))drawText(c,new String[]{"Wi-Fi","Bluetooth","Torch","Auto-rotate"}[i],x+78,y+28,13,s.appearance(0),s.position(0),i==0?0xff332c49:0xfff1edf7);
                if(!s.hide(1))drawText(c,i==0?"Connected":"Off",x+78,y+43,9,s.appearance(1),s.position(1),i==0?0xff514965:0xffc8c2d0);
            }
            for(int i=0;i<6;i++){paint.setColor(i==0?0xffd3cbef:0xff68646f);shape(c,23+i*54,151,34,22,s.tileShape());}
        }else if(section.equals("notifications")){NotificationStylePreview.paint(c,s.notificationStyle(),360,height,1,s.notificationAppearance());
        }else if(section.equals("power_menu")){PowerStylePreview.paint(c,s.powerStyle(),360,height,1);
        }else if(section.equals("volume")){VolumeStylePreview.paint(c,s.volumeStyle(),360,height,1);
        }else{
            paint.setTypeface(Typeface.create(ClockStylePolicy.familyName(d.family),(d.bold?1:0)|(d.italic?2:0)));
            if(!d.hidden){paint.setTextAlign(Paint.Align.LEFT);paint.setTextSize(22*(d.size==0?1:d.size/100f));paint.setColor(d.color);c.drawText("12:34",20,57,paint);}
            paint.setTypeface(Typeface.create("sans-serif-medium",0));
            if(!d.bar.selected(StatusBarStyle.HIDE_NOTIFICATIONS))drawText(c,"●  ●",129,56,13,StatusBarStyle.appearance(d.bar.notificationScale(),d.bar.notificationOpacity()),d.bar.notificationTransform(),0xffeee8f5);
            if(!d.bar.selected(StatusBarStyle.HIDE_SYSTEM))drawText(c,"VoLTE",239,56,13,StatusBarStyle.appearance(d.bar.systemScale(),d.bar.systemOpacity()),d.bar.systemTransform(),d.bar.selected(StatusBarStyle.TINT)?d.iconColor:0xffeee8f5);
            BatteryControlsPreview.draw(c,d.bar,315,50,false);
        }
        paint.setAlpha(255);c.restore();
    }
    private void plain(Canvas c,String text,float x,float y,float size,int color){paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(size);paint.setColor(color);paint.setAlpha(255);c.drawText(text,x,y,paint);}
    private void drawText(Canvas c,String text,float x,float y,float size,int appearance,int transform,int color){c.save();c.translate(x+StatusBarStyle.horizontal(transform),y+StatusBarStyle.vertical(transform));c.rotate(StatusBarStyle.rotation(transform));float scale=StatusBarStyle.indicatorScale(appearance)/100f;c.scale(scale,scale);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(size);paint.setColor(color);paint.setAlpha(StatusBarStyle.indicatorOpacity(appearance)*255/100);c.drawText(text,0,0,paint);c.restore();paint.setAlpha(255);}
    private void shape(Canvas canvas,float x,float y,float w,float h,int shape){
        android.graphics.drawable.GradientDrawable tile=new android.graphics.drawable.GradientDrawable();tile.setColor(paint.getColor());tile.setBounds(Math.round(x),Math.round(y),Math.round(x+w),Math.round(y+h));
        if(shape==3)tile.setCornerRadii(new float[]{22,22,0,0,22,22,0,0});else tile.setCornerRadius(shape==1?0:shape==2?10:shape==4?h/2:h/2);
        if(tileStyle==0)tile.draw(canvas);else try{if(!QsTileEffects.draw(tileStyle,canvas,tile,1f,()->true,()->tile.draw(canvas)))tile.draw(canvas);}catch(Throwable unavailable){tile.draw(canvas);}
    }
}
