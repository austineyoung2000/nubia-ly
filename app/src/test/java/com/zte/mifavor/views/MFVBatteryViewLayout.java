package com.zte.mifavor.views;
import android.content.Context;import android.widget.*;
public class MFVBatteryViewLayout extends RelativeLayout {
 public final FrameLayout mBatteryContainer;public final ImageView mBatteryIconView,mInsideChargeView,mOutsideChargeView;public final TextView mBatteryLevelInsideView,mBatteryLevelOutsideView;
 public MFVBatteryViewLayout(Context c){super(c);mBatteryContainer=new FrameLayout(c);addView(mBatteryContainer);mBatteryIconView=new ImageView(c);mInsideChargeView=new ImageView(c);mOutsideChargeView=new ImageView(c);mBatteryLevelInsideView=new TextView(c);mBatteryLevelOutsideView=new TextView(c);mBatteryContainer.addView(mBatteryIconView);mBatteryContainer.addView(mInsideChargeView);mBatteryContainer.addView(mBatteryLevelInsideView);addView(mOutsideChargeView);addView(mBatteryLevelOutsideView);}
}
