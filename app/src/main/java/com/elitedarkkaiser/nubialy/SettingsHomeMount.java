package com.elitedarkkaiser.nubialy;
import android.view.*;import android.widget.FrameLayout;
/** Owns one exact list replacement. Restoration never depends on a fragment's newer list field. */
public final class SettingsHomeMount {
 private final View nativeList;private final ViewGroup parent;private final int index,visibility;private final ViewGroup.LayoutParams params;private final FrameLayout frame;private View custom;private boolean installed;
 public SettingsHomeMount(View list){if(!(list.getParent() instanceof ViewGroup p))throw new IllegalArgumentException("Detached Settings list");nativeList=list;parent=p;index=p.indexOfChild(list);params=list.getLayoutParams();visibility=list.getVisibility();frame=new FrameLayout(list.getContext());}
 public void install(View content){if(installed)throw new IllegalStateException();custom=content;try{parent.removeView(nativeList);parent.addView(frame,Math.min(index,parent.getChildCount()),params);installed=true;frame.addView(nativeList,new FrameLayout.LayoutParams(-1,-1));frame.addView(content,new FrameLayout.LayoutParams(-1,-1));nativeList.setVisibility(View.INVISIBLE);parent.requestLayout();}catch(Throwable failure){restore();throw new IllegalStateException("Settings list swap failed",failure);}}
 public boolean mounted(){return installed&&frame.getParent()==parent&&nativeList.getParent()==frame&&custom!=null&&custom.getParent()==frame;}
 public void discard(){restore(false);}
 public void restore(){restore(true);}
 private void restore(boolean reattach){boolean frameOwned=frame.getParent()==parent;if(custom!=null&&custom.getParent()==frame)frame.removeView(custom);if(nativeList.getParent()==frame)frame.removeView(nativeList);if(frameOwned)parent.removeView(frame);if(nativeList.getParent()==null){nativeList.setLayoutParams(params);if(reattach)parent.addView(nativeList,Math.min(index,parent.getChildCount()),params);}nativeList.setVisibility(visibility);installed=false;custom=null;parent.requestLayout();}
}
