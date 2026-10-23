package com.elitedarkkaiser.nubialy;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

/** Shared app-only surfaces. These previews never read or change system appearance. */
final class PersonalizationUi {
    private PersonalizationUi() {}
    static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static MaterialToolbar toolbar(Activity a,String title){
        MaterialToolbar bar=new MaterialToolbar(a);bar.setTitle(title);
        bar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        bar.setNavigationContentDescription("Back");bar.setNavigationOnClickListener(v->a.finish());return bar;
    }
    static void panel(MaterialCardView card){
        card.setRadius(dp(card.getContext(),32));card.setCardElevation(0);card.setStrokeWidth(0);
        card.setCardBackgroundColor(card.getContext().getColor(R.color.surface));
    }
    static LinearLayout dashboard(Activity a,java.util.function.Consumer<String> open){
        LinearLayout dashboard=new LinearLayout(a);dashboard.setOrientation(LinearLayout.VERTICAL);
        boolean stacked=a.getResources().getConfiguration().fontScale>1.3f||a.getResources().getConfiguration().screenWidthDp<360;
        LinearLayout row=new LinearLayout(a);row.setOrientation(stacked?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);dashboard.addView(row);
        MaterialCardView lock=tile(a,"Lock screen","Clock, fonts & fingerprint",0,()->open.accept("lock_screen"),stacked?120:238);
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(stacked?-1:0,-2,stacked?0:1);left.setMargins(0,0,stacked?0:dp(a,12),dp(a,12));row.addView(lock,left);
        LinearLayout side=new LinearLayout(a);side.setOrientation(LinearLayout.VERTICAL);row.addView(side,new LinearLayout.LayoutParams(stacked?-1:0,-2,stacked?0:1));
        side.addView(tile(a,"Quick Settings","Tiles & labels",1,()->open.accept("quick_settings"),62),new LinearLayout.LayoutParams(-1,-2));
        LinearLayout.LayoutParams lower=new LinearLayout.LayoutParams(-1,-2);lower.topMargin=dp(a,12);
        side.addView(tile(a,"Status bar","Clock, icons & battery",2,()->open.accept("status_bar"),62),lower);
        LinearLayout volumeRow=new LinearLayout(a);volumeRow.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams volumeRowParams=new LinearLayout.LayoutParams(-1,-2);volumeRowParams.topMargin=dp(a,12);dashboard.addView(volumeRow,volumeRowParams);
        LinearLayout.LayoutParams volume=new LinearLayout.LayoutParams(stacked?-1:0,-2,stacked?0:1);volume.setMargins(0,0,stacked?0:dp(a,12),dp(a,12));volumeRow.addView(tile(a,"Volume","Slider styles",3,()->open.accept("volume"),stacked?120:238),volume);
        LinearLayout notificationColumn=new LinearLayout(a);notificationColumn.setOrientation(LinearLayout.VERTICAL);
        if(!stacked){volumeRow.addView(notificationColumn,new LinearLayout.LayoutParams(0,-2,1));}else dashboard.addView(notificationColumn,new LinearLayout.LayoutParams(-1,-2));
        notificationColumn.addView(tile(a,"Notifications","Card styles & layers",4,()->open.accept("notifications"),62),new LinearLayout.LayoutParams(-1,-2));
        LinearLayout.LayoutParams powerParams=new LinearLayout.LayoutParams(-1,-2);powerParams.topMargin=dp(a,12);notificationColumn.addView(tile(a,"Power menu","Button styles",5,()->open.accept("power_menu"),62),powerParams);
        LinearLayout.LayoutParams bootParams=new LinearLayout.LayoutParams(-1,-2);bootParams.topMargin=dp(a,12);dashboard.addView(tile(a,"Boot animations","Systemless startup styles",6,()->open.accept("boot_animation"),62),bootParams);
        LinearLayout.LayoutParams launcherParams=new LinearLayout.LayoutParams(-1,-2);launcherParams.topMargin=dp(a,12);dashboard.addView(tile(a,"Launcher","Home screen & launcher settings",7,()->open.accept("launcher"),90),launcherParams);
        LinearLayout.LayoutParams uiParams=new LinearLayout.LayoutParams(-1,-2);uiParams.topMargin=dp(a,12);dashboard.addView(tile(a,"UI styles","Framework, Settings & System UI",8,()->open.accept("ui_styles"),90),uiParams);
        return dashboard;
    }
    private static MaterialCardView tile(Activity a,String title,String summary,int kind,Runnable action,int previewHeight){
        MaterialCardView card=new MaterialCardView(a);panel(card);card.setClickable(true);card.setFocusable(true);card.setOnClickListener(v->action.run());
        card.setContentDescription(title+". "+summary);card.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(a,18),dp(a,18),dp(a,18),dp(a,18));
        body.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        body.addView(new Artwork(a,kind),new LinearLayout.LayoutParams(-1,dp(a,previewHeight)));
        TextView heading=new TextView(a);heading.setText(title);heading.setTextSize(19);heading.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));heading.setTextColor(a.getColor(R.color.on_surface));heading.setPadding(0,dp(a,12),0,dp(a,5));body.addView(heading);
        TextView caption=new TextView(a);caption.setText(summary);caption.setTextSize(12);caption.setTextColor(a.getColor(R.color.on_surface_variant));body.addView(caption);card.addView(body);return card;
    }
    private static final class Artwork extends View {
        private final int kind;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Artwork(Context c,int kind){super(c);this.kind=kind;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
            if(kind==7){LauncherPreview.paint(canvas,w,h);return;}
            if(kind==6){p.setColor(0xffd3cbef);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif-medium",0));p.setTextSize(Math.min(h*.6f,w*.12f));canvas.drawText("BOOT",w/2,h*.65f,p);return;}
            if(kind==5){PowerStylePreview.paint(canvas,0,w,h,getResources().getDisplayMetrics().density);return;}
            if(kind==3){VolumeStylePreview.paint(canvas,0,w,h,getResources().getDisplayMetrics().density);return;}
            canvas.save();canvas.scale(w/160f,h/((kind==0||kind==3)?238f:62f));
            if(kind==0){
                p.setShader(new LinearGradient(0,0,160,238,new int[]{0xff5d567d,0xff302d43,0xff444c54},null,Shader.TileMode.CLAMP));canvas.drawRoundRect(0,0,160,238,26,26,p);p.setShader(null);
                p.setColor(0xff9c91bf);canvas.drawCircle(133,195,72,p);p.setColor(0xff6f7c84);canvas.drawCircle(13,243,67,p);
                p.setTypeface(Typeface.create("sans-serif-light",0));p.setTextAlign(Paint.Align.CENTER);p.setColor(0xffeee7ff);p.setTextSize(55);canvas.drawText("10",80,88,p);canvas.drawText("26",80,144,p);
                p.setTextSize(11);canvas.drawText("Thursday, 15 October",80,173,p);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);canvas.drawRoundRect(73,203,87,215,3,3,p);canvas.drawArc(76,196,84,210,180,180,false,p);p.setStyle(Paint.Style.FILL);
            }else if(kind==1){
                for(int i=0;i<4;i++){p.setColor(i<2?0xffd3cbef:0xff44444e);float x=(i%2)*81,y=(i/2)*32;canvas.drawRoundRect(x,y,x+73,y+25,12,12,p);p.setColor(i<2?0xff3e3655:0xffa5a1ae);canvas.drawCircle(x+15,y+12,4,p);canvas.drawRoundRect(x+27,y+10,x+59,y+14,2,2,p);}
            }else if(kind==4){NotificationStylePreview.paint(canvas,0,160,62,1);
            }else{
                p.setColor(0xffd3cbef);p.setTypeface(Typeface.create("sans-serif-medium",0));p.setTextSize(20);p.setTextAlign(Paint.Align.LEFT);canvas.drawText("10:26",4,36,p);
                for(int i=0;i<3;i++)canvas.drawRoundRect(92+i*9,28-i*4,97+i*9,38,2,2,p);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);canvas.drawRoundRect(127,24,153,38,4,4,p);p.setStyle(Paint.Style.FILL);canvas.drawRoundRect(130,27,147,35,2,2,p);
            }canvas.restore();
        }
    }
}
