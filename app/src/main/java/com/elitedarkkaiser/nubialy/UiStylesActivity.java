package com.elitedarkkaiser.nubialy;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.*;
import java.util.concurrent.*;
/** One draft selection, one ordered worker, explicit preview/keep/restore. */
public final class UiStylesActivity extends AppCompatActivity {
 private final ExecutorService worker=Executors.newSingleThreadExecutor();private final List<MaterialCardView> choices=new ArrayList<>();
 private String selected="nights";private TextView status;private MaterialButton apply,keep,restore,check;private boolean busy,closed;
 @Override protected void onCreate(Bundle state){super.onCreate(state);
  selected=getSharedPreferences("ui-style-draft",MODE_PRIVATE).getString("selection","nights");if(UiStyles.find(selected)==null)selected="nights";
  LinearLayout shell=new LinearLayout(this);shell.setOrientation(1);shell.setBackgroundColor(getColor(R.color.background));shell.addView(PersonalizationUi.toolbar(this,"UI styles"));ScrollView scroll=new ScrollView(this);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(20),dp(8),dp(20),dp(28));scroll.addView(body);setContentView(shell);
  TextView intro=text("Framework, Nubia framework, Settings & System UI",20);body.addView(intro);
  body.addView(text("Rising UI styles adapted to Nubia resources. Sample colors below; wallpaper accents and light/dark mode can change the result. Your existing per-page tweaks remain active.",14));
  for(UiStyles.Entry entry:UiStyles.ENTRIES){MaterialCardView card=new MaterialCardView(this);PersonalizationUi.panel(card);card.setClickable(true);card.setFocusable(true);card.setContentDescription(entry.name());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);body.addView(card,lp);LinearLayout inner=new LinearLayout(this);inner.setOrientation(1);inner.setPadding(dp(16),dp(12),dp(16),dp(12));card.addView(inner);inner.addView(text(entry.name(),18));
   View preview=new View(this){final Paint paint=new Paint(3);@Override protected void onDraw(Canvas canvas){float w=getWidth(),h=getHeight();paint.setColor(entry.background());canvas.drawRoundRect(0,0,w,h,dp(18),dp(18),paint);paint.setColor(entry.card());canvas.drawRoundRect(dp(12),dp(12),w/2-dp(6),h-dp(12),dp(14),dp(14),paint);paint.setColor(entry.accent());canvas.drawRoundRect(w/2+dp(6),dp(12),w-dp(12),h/2,dp(14),dp(14),paint);paint.setColor(0xffeeeeee);paint.setTextSize(dp(14));canvas.drawText("Settings",dp(24),h/2+dp(6),paint);canvas.drawText("Quick Settings",w/2+dp(18),h-dp(20),paint);}};
   inner.addView(preview,new LinearLayout.LayoutParams(-1,dp(92)));choices.add(card);card.setOnClickListener(v->{selected=entry.id();getSharedPreferences("ui-style-draft",MODE_PRIVATE).edit().putString("selection",selected).apply();redraw();});
  }
  LinearLayout controls=panel(body);controls.addView(text("Apply & restore",22));controls.addView(text("Install the UI test guard and reboot once. Preview applies all four targets together. Inspect Settings and the shade, then Keep within 90 seconds. Test styles reset on reboot. Guard Action also restores them.",14));
  status=text("Check the recovery guard before applying.",14);controls.addView(status);apply=button(controls,"Preview selected style",()->run("preview_"+selected));keep=button(controls,"Keep this preview",()->run("confirm"));restore=button(controls,"Restore native UI style",()->run("restore"));check=button(controls,"Check UI style",()->run("status"));redraw();
 }
 @Override protected void onStart(){super.onStart();if(check!=null)run("status");}
 private void run(String operation){if(busy)return;busy=true;controls();status.setText("Working…");worker.execute(()->{String reply=UiStyleCommand.run(operation);runOnUiThread(()->{if(closed||isFinishing()||isDestroyed())return;busy=false;status.setText(reply);controls();});});}
 private void controls(){apply.setEnabled(!busy);keep.setEnabled(!busy);restore.setEnabled(!busy);check.setEnabled(!busy);for(View v:choices)v.setEnabled(!busy);}
 private void redraw(){for(int i=0;i<choices.size();i++){var card=choices.get(i);boolean chosen=UiStyles.ENTRIES[i].id().equals(selected);card.setStrokeWidth(chosen?dp(2):0);card.setStrokeColor(getColor(R.color.primary));card.setSelected(chosen);}}
 private LinearLayout panel(LinearLayout body){MaterialCardView card=new MaterialCardView(this);PersonalizationUi.panel(card);body.addView(card,new LinearLayout.LayoutParams(-1,-2));LinearLayout inner=new LinearLayout(this);inner.setOrientation(1);inner.setPadding(dp(18),dp(18),dp(18),dp(18));card.addView(inner);return inner;}
 private TextView text(String value,int size){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(getColor(size>=18?R.color.on_surface:R.color.on_surface_variant));v.setPadding(0,dp(5),0,dp(12));return v;}
 private MaterialButton button(LinearLayout parent,String title,Runnable action){MaterialButton b=new MaterialButton(this);b.setText(title);b.setAllCaps(false);b.setCornerRadius(dp(28));b.setOnClickListener(v->action.run());parent.addView(b,new LinearLayout.LayoutParams(-1,-2));return b;}
 private int dp(float value){return PersonalizationUi.dp(this,value);}
 @Override protected void onDestroy(){closed=true;worker.shutdown();super.onDestroy();}
}
