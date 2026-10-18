package com.elitedarkkaiser.nubialy;

import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.elitedarkkaiser.nubialy.xposed.FingerprintGlyphDrawable;

/** A full-screen, recycled gallery. Selection edits a draft; it cannot grant cosmetic authority. */
public final class FingerprintPickerActivity extends AppCompatActivity {
    private int selected;
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        if(!CompatibilityPolicy.isSupportedDevice(Build.MODEL,Build.DEVICE)||!CompatibilityPolicy.isSupportedSdk(Build.VERSION.SDK_INT)){finish();return;}
        selected=getIntent().getIntExtra("selection",0);if(!FingerprintStyles.valid(selected))selected=0;
        FingerprintGlyphDrawable.artworkResources(getResources());
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.background));
        MaterialToolbar toolbar=new MaterialToolbar(this);toolbar.setTitle("UDFPS icon picker");toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);toolbar.setNavigationContentDescription("Back");toolbar.setNavigationOnClickListener(v->finish());toolbar.getMenu().add("Artwork credits").setOnMenuItemClickListener(item->{showCredits();return true;});root.addView(toolbar,new LinearLayout.LayoutParams(-1,dp(64)));
        TextView caption=new TextView(this);caption.setText("Choose an icon · "+FingerprintStyles.NAMES[selected]);caption.setTextSize(14);caption.setPadding(dp(24),dp(4),dp(24),dp(16));root.addView(caption);
        GridView grid=new GridView(this);grid.setNumColumns(3);grid.setHorizontalSpacing(dp(16));grid.setVerticalSpacing(dp(16));grid.setPadding(dp(20),dp(4),dp(20),dp(24));grid.setClipToPadding(false);grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);grid.setSelector(android.R.color.transparent);grid.setAdapter(new FingerprintIconAdapter(this,selected,this::chooseIcon));
        root.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
        // Keep the toolbar and last row clear of system bars, including edge-to-edge enforcement.
        root.setOnApplyWindowInsetsListener((view,insets)->{android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());view.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
        setContentView(root);root.requestApplyInsets();grid.setSelection(selected);
    }
    private void chooseIcon(int icon){if(!FingerprintStyles.valid(icon))return;setResult(RESULT_OK,new Intent().putExtra("selection",icon));finish();}
    private void showCredits(){try(java.io.InputStream input=getAssets().open("licenses/rising-udfps.txt")){String notice=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Artwork credits").setMessage(notice).setPositiveButton("Close",null).show();}catch(java.io.IOException unavailable){android.widget.Toast.makeText(this,"Artwork credits unavailable",android.widget.Toast.LENGTH_SHORT).show();}}
}
