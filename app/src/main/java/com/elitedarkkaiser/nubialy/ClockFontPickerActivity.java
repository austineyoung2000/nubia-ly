package com.elitedarkkaiser.nubialy;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/** Preview first; Apply returns a draft to the editor and never arms customization. */
public final class ClockFontPickerActivity extends AppCompatActivity {
    private int selected;private TextView sample;private MaterialButton selector;private PopupWindow popup;
    private int dp(float n){return PersonalizationUi.dp(this,n);}
    @Override public void onCreate(Bundle state){super.onCreate(state);
        selected=state==null?getIntent().getIntExtra("selection",0):state.getInt("selection",0);if(!LockClockFonts.valid(selected))selected=0;
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.addView(PersonalizationUi.toolbar(this,"Clock font styles"),new LinearLayout.LayoutParams(-1,dp(64)));
        ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(24),dp(32),dp(24),dp(32));scroll.addView(body);
        MaterialCardView card=new MaterialCardView(this);PersonalizationUi.panel(card);sample=new TextView(this);sample.setText("The quick brown\nfox jumps over the\nlazy dog\n\nABCDEFGHIJKLM\nNOPQRSTUVWXYZ\nabcdefghijklm\nnopqrstuvwxyz\n1234567890\n\n12:34");sample.setTextSize(23);sample.setTextColor(getColor(R.color.on_surface));sample.setPadding(dp(24),dp(28),dp(24),dp(28));sample.setLineSpacing(dp(2),1.05f);card.addView(sample);body.addView(card,new LinearLayout.LayoutParams(-1,-2));
        selector=new MaterialButton(this, null,com.google.android.material.R.attr.materialButtonOutlinedStyle);selector.setTag("font-selector");selector.setAllCaps(false);selector.setCornerRadius(dp(28));selector.setMinHeight(dp(48));LinearLayout.LayoutParams selectParams=new LinearLayout.LayoutParams(-2,-2);selectParams.topMargin=dp(20);body.addView(selector,selectParams);selector.setOnClickListener(v->showFonts());
        TextView note=new TextView(this);note.setText("For "+(getIntent().getStringExtra("target")==null?"lock-screen":getIntent().getStringExtra("target"))+" text clocks. Apply returns to the editor; apply that page to use the font.");note.setTextSize(14);note.setTextColor(getColor(R.color.on_surface_variant));note.setPadding(0,dp(16),0,dp(20));body.addView(note);
        MaterialButton apply=new MaterialButton(this);apply.setTag("font-apply");apply.setText("Apply");apply.setAllCaps(false);apply.setCornerRadius(dp(28));LinearLayout.LayoutParams applyParams=new LinearLayout.LayoutParams(-2,-2);applyParams.gravity=Gravity.END;body.addView(apply,applyParams);apply.setOnClickListener(v->{setResult(RESULT_OK,new Intent().putExtra("selection",selected));finish();});
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{var bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()|androidx.core.view.WindowInsetsCompat.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});setContentView(root);root.requestApplyInsets();redraw();
    }
    private void redraw(){var face=LockClockFonts.FACES[selected];sample.setTypeface(Typeface.create(face.family(),face.style()));selector.setText(face.name()+"  ▾");selector.setContentDescription("Choose clock font. Selected: "+face.name());}
    private void showFonts(){
        ListView list=new ListView(this);list.setTag("font-list");list.setDivider(null);list.setPadding(dp(8),dp(8),dp(8),dp(8));list.setAdapter(new BaseAdapter(){
            @Override public int getCount(){return LockClockFonts.FACES.length;}
            @Override public Object getItem(int i){return LockClockFonts.FACES[i];}
            @Override public long getItemId(int i){return i;}
            @Override public View getView(int i,View recycled,ViewGroup parent){TextView row=recycled instanceof TextView?(TextView)recycled:new TextView(ClockFontPickerActivity.this);var face=LockClockFonts.FACES[i];row.setText(face.name()+(selected==i?"  ✓":""));row.setTypeface(Typeface.create(face.family(),face.style()));row.setTextColor(getColor(R.color.on_surface));row.setTextSize(18);row.setPadding(dp(18),dp(16),dp(18),dp(16));row.setMinHeight(dp(56));row.setContentDescription(face.name()+(selected==i?", selected":""));return row;}
        });
        int available=getResources().getDisplayMetrics().widthPixels-dp(48);
        popup=new PopupWindow(list,Math.min(available,dp(320)),Math.min(dp(380),getWindow().getDecorView().getHeight()/2),true);
        android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable();background.setColor(getColor(R.color.surface_container_high));background.setCornerRadius(dp(28));popup.setBackgroundDrawable(background);popup.setElevation(dp(8));popup.setOutsideTouchable(true);
        list.setOnItemClickListener((parent,view,position,id)->{selected=position;redraw();popup.dismiss();});popup.showAsDropDown(selector,0,dp(4));list.setSelection(selected);
    }
    @Override protected void onSaveInstanceState(Bundle state){state.putInt("selection",selected);super.onSaveInstanceState(state);}
    @Override protected void onPause(){if(popup!=null)popup.dismiss();super.onPause();}
}
