package com.elitedarkkaiser.nubialy;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.*;
import java.util.function.*;
/** Color rows use the app's single hex/RGB picker and native reset. */
final class PaletteEditor {
 private final AppCompatActivity a;private final Supplier<SurfaceStyle.Palette> read;private final Consumer<SurfaceStyle.Palette> save;private final List<Runnable> labels=new ArrayList<>();
 PaletteEditor(AppCompatActivity a,Supplier<SurfaceStyle.Palette> read,Consumer<SurfaceStyle.Palette> save){this.a=a;this.read=read;this.save=save;}
 private int dp(int n){return PersonalizationUi.dp(a,n);}
 void refresh(){for(Runnable update:labels)update.run();}
 private void row(LinearLayout parent,String title,int key){LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(dp(52));parent.addView(row,new LinearLayout.LayoutParams(-1,-2));TextView label=new TextView(a);label.setText(title);label.setTextSize(14);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));MaterialButton value=new MaterialButton(a);value.setTag("palette-color:"+key);value.setAllCaps(false);value.setCornerRadius(dp(14));value.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));value.setStrokeWidth(dp(1));value.setStrokeColor(ColorStateList.valueOf(a.getColor(R.color.surface_container_high)));value.setTextColor(a.getColor(R.color.primary));row.addView(value,new LinearLayout.LayoutParams(-2,dp(48)));labels.add(()->{int color=read.get().get(key);value.setText(color==0?"Native":ClockColorPolicy.hex(color));value.setContentDescription(title+", "+value.getText());});value.setOnClickListener(v->ColorPickerDialog.show(a,title,"Appearance color",read.get().get(key)==0?a.getColor(R.color.primary):read.get().get(key),color->{save.accept(read.get().with(key,color));refresh();}));MaterialButton reset=new MaterialButton(a);reset.setText("↺");reset.setMinWidth(0);reset.setMinimumWidth(0);reset.setPadding(0,0,0,0);reset.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));reset.setTextColor(a.getColor(R.color.on_surface_variant));reset.setContentDescription("Use native "+title);row.addView(reset,new LinearLayout.LayoutParams(dp(48),dp(48)));reset.setOnClickListener(v->{save.accept(read.get().with(key,0));refresh();});}
 void status(LinearLayout parent){row(parent,"Wi-Fi",0);row(parent,"Mobile signal & data",1);row(parent,"Battery icon",2);row(parent,"Battery percentage text",8);row(parent,"Other system icons",3);TextView note=new TextView(a);note.setText("Notification icons keep their own appearance. Applied colors also follow your status-bar artwork into the QS header.");note.setPadding(0,dp(12),0,dp(8));parent.addView(note);refresh();}
 void brightness(LinearLayout parent){row(parent,"Track background",4);row(parent,"Progress / first accent",5);MaterialSwitch twoTone=new MaterialSwitch(a);twoTone.setTag("brightness-two-tone");twoTone.setText("Two-tone progress");twoTone.setChecked(read.get().brightnessTwoTone()!=0);parent.addView(twoTone);twoTone.setOnCheckedChangeListener((v,checked)->{save.accept(read.get().with(7,checked?1:0));refresh();});row(parent,"Second accent",6);refresh();}
}
