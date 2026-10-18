package com.elitedarkkaiser.nubialy;

import android.text.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.*;
import java.util.function.IntConsumer;

/** Shared clock/icon/launcher color editor. Only Use color commits the local picker draft. */
final class ColorPickerDialog {
    private ColorPickerDialog(){}
    static AlertDialog show(AppCompatActivity activity,String title,String sampleText,int initial,IntConsumer use){
        if(!ClockColorPolicy.valid(initial))throw new IllegalArgumentException("Opaque color required");
        LinearLayout picker=new LinearLayout(activity);picker.setOrientation(LinearLayout.VERTICAL);picker.setPadding(dp(activity,24),dp(activity,8),dp(activity,24),dp(activity,8));
        TextView sample=text(activity,sampleText,36);sample.setTag("color-picker-preview");picker.addView(sample);
        TextInputLayout field=new TextInputLayout(activity);field.setHint("Hex color · RRGGBB");field.setTag("color-picker-field");
        TextInputEditText hex=new TextInputEditText(activity);hex.setTag("color-picker-hex");hex.setSingleLine(true);hex.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);field.addView(hex);picker.addView(field);
        Slider[] sliders=new Slider[3];String[] names={"Red","Green","Blue"};int[] draft={initial};boolean[] updating={false};
        for(int i=0;i<3;i++){picker.addView(text(activity,names[i],14));Slider slider=new Slider(activity);sliders[i]=slider;slider.setTag("color-picker-"+names[i].toLowerCase(java.util.Locale.ROOT));slider.setContentDescription(names[i]);slider.setValueFrom(0);slider.setValueTo(255);slider.setStepSize(1);picker.addView(slider);}
        Runnable update=()->{updating[0]=true;sample.setTextColor(draft[0]);hex.setText(ClockColorPolicy.hex(draft[0]));for(int i=0;i<3;i++)sliders[i].setValue((draft[0]>>(16-i*8))&255);field.setError(null);updating[0]=false;};
        for(int i=0;i<3;i++)sliders[i].addOnChangeListener((slider,value,fromUser)->{if(!fromUser||updating[0])return;draft[0]=0xff000000|((int)sliders[0].getValue()<<16)|((int)sliders[1].getValue()<<8)|(int)sliders[2].getValue();update.run();});
        hex.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void afterTextChanged(Editable s){}public void onTextChanged(CharSequence s,int start,int before,int count){if(updating[0])return;Integer parsed=ClockColorPolicy.parse(s.toString());field.setError(parsed==null?"Enter six hex digits, such as C0A8FF.":null);if(parsed!=null){draft[0]=parsed;updating[0]=true;sample.setTextColor(parsed);for(int i=0;i<3;i++)sliders[i].setValue((parsed>>(16-i*8))&255);updating[0]=false;}}});
        update.run();ScrollView scroll=new ScrollView(activity);scroll.addView(picker);
        AlertDialog dialog=new MaterialAlertDialogBuilder(activity).setTitle(title).setView(scroll).setNegativeButton("Cancel",null).setPositiveButton("Use color",null).create();
        DefaultLifecycleObserver lifecycle=new DefaultLifecycleObserver(){@Override public void onDestroy(LifecycleOwner owner){dialog.dismiss();}};
        activity.getLifecycle().addObserver(lifecycle);dialog.setOnDismissListener(ignored->activity.getLifecycle().removeObserver(lifecycle));
        dialog.show();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{Integer parsed=ClockColorPolicy.parse(hex.getText()==null?null:hex.getText().toString());if(parsed==null){field.setError("Enter six hex digits.");return;}use.accept(parsed);dialog.dismiss();});return dialog;
    }
    private static TextView text(AppCompatActivity a,String value,int size){TextView text=new TextView(a);text.setText(value);text.setTextSize(size);text.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));text.setTextColor(a.getColor(R.color.on_surface));text.setPadding(0,dp(a,4),0,dp(a,10));return text;}
    private static int dp(AppCompatActivity a,float value){return PersonalizationUi.dp(a,value);}
}
