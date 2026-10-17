package com.elitedarkkaiser.nubialy;

import android.os.*;
import android.content.Intent;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.concurrent.*;

/** One animated preview at a time; choosing a preview only edits the editor draft. */
public final class FingerprintAnimationPickerActivity extends AppCompatActivity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private int selected;private long generation;private androidx.appcompat.app.AlertDialog dialog;private FingerprintAnimationArtwork.Preview playing;
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle state){super.onCreate(state);if(!CompatibilityPolicy.isSupportedDevice(Build.MODEL,Build.DEVICE)||!CompatibilityPolicy.isSupportedSdk(Build.VERSION.SDK_INT)){finish();return;}
        selected=getIntent().getIntExtra("selection",0);if(!FingerprintAnimations.valid(selected))selected=0;FingerprintAnimationArtwork.resources(getResources());
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(getColor(R.color.background));
        MaterialToolbar toolbar=new MaterialToolbar(this);toolbar.setTitle("Fingerprint animation");toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);toolbar.setNavigationContentDescription("Back");toolbar.setNavigationOnClickListener(v->finish());root.addView(toolbar,new LinearLayout.LayoutParams(-1,dp(64)));
        TextView info=new TextView(this);info.setText("Tap to preview · "+FingerprintAnimations.NAMES[selected]);info.setPadding(dp(20),dp(8),dp(20),dp(16));root.addView(info);
        GridView grid=new GridView(this);grid.setNumColumns(3);grid.setHorizontalSpacing(dp(12));grid.setVerticalSpacing(dp(12));grid.setPadding(dp(16),0,dp(16),dp(24));grid.setClipToPadding(false);grid.setAdapter(new Animations());root.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
        root.setOnApplyWindowInsetsListener((view,insets)->{android.graphics.Insets b=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());view.setPadding(b.left,b.top,b.right,b.bottom);return insets;});setContentView(root);root.requestApplyInsets();grid.setSelection(selected);
    }
    private void choose(int id){setResult(RESULT_OK,new Intent().putExtra("selection",id));finish();}
    private void preview(int id){if(id==0){choose(0);return;}long ticket=++generation;
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(8),dp(16),dp(16));ImageView image=new ImageView(this);content.addView(image,new LinearLayout.LayoutParams(-1,dp(240)));TextView note=new TextView(this);note.setText("Loading preview…");content.addView(note);
        dialog=new MaterialAlertDialogBuilder(this).setTitle(FingerprintAnimations.NAMES[id]).setView(content).setNegativeButton("Back",null).setPositiveButton("Use animation",(d,which)->choose(id)).create();
        dialog.setOnDismissListener(d->{generation++;if(playing!=null){playing.stop();playing=null;}});dialog.show();dialog.getButton(-1).setEnabled(false);
        worker.execute(()->{FingerprintAnimationArtwork.Frames frames=null;try{frames=FingerprintAnimationArtwork.load(id);}catch(Throwable unavailable){}FingerprintAnimationArtwork.Frames result=frames;main.post(()->{if(ticket!=generation||isFinishing()||dialog==null||!dialog.isShowing())return;if(result==null){note.setText("This preview could not be loaded.");return;}playing=new FingerprintAnimationArtwork.Preview(result);image.setImageDrawable(playing);playing.start();note.setText("The effect plays during fingerprint scanning. Its position and scan duration follow your phone’s native animation. Keep a stock fingerprint animation enabled in phone settings.");dialog.getButton(-1).setEnabled(true);});});
    }
    @Override protected void onPause(){if(dialog!=null)dialog.dismiss();super.onPause();}
    @Override protected void onDestroy(){generation++;worker.shutdownNow();super.onDestroy();}
    private final class Animations extends BaseAdapter {
        public int getCount(){return FingerprintAnimations.NAMES.length;}public Object getItem(int p){return FingerprintAnimations.NAMES[p];}public long getItemId(int p){return p;}
        public View getView(int position,View convert,ViewGroup parent){MaterialCardView card;LinearLayout body;
            if(convert instanceof MaterialCardView old){card=old;body=(LinearLayout)card.getChildAt(0);}else{card=new MaterialCardView(FingerprintAnimationPickerActivity.this);card.setRadius(dp(24));card.setCardElevation(0);card.setCardBackgroundColor(getColor(R.color.surface));card.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);card.setFocusable(true);body=new LinearLayout(FingerprintAnimationPickerActivity.this);body.setOrientation(LinearLayout.VERTICAL);body.setGravity(Gravity.CENTER);body.setPadding(dp(12),dp(12),dp(12),dp(12));ImageView image=new ImageView(FingerprintAnimationPickerActivity.this);image.setScaleType(ImageView.ScaleType.FIT_CENTER);body.addView(image,new LinearLayout.LayoutParams(-1,dp(66)));TextView name=new TextView(FingerprintAnimationPickerActivity.this);name.setTextSize(12);name.setGravity(Gravity.CENTER);body.addView(name);card.addView(body);}
            card.setLayoutParams(new AbsListView.LayoutParams(-1,dp(124)));card.setStrokeWidth(dp(position==selected?3:2));card.setStrokeColor(position==selected?getColor(R.color.primary):0xff454454);card.setOnClickListener(v->preview(position));card.setContentDescription(FingerprintAnimations.NAMES[position]+(position==selected?", selected":""));body.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            ImageView image=(ImageView)body.getChildAt(0);int icon=position==0?R.drawable.rising_fp_udfps_icon_default:getResources().getIdentifier("rising_anim_preview_"+position,"drawable",getPackageName());image.setImageResource(icon);((TextView)body.getChildAt(1)).setText(FingerprintAnimations.NAMES[position]);return card;
        }
    }
}
