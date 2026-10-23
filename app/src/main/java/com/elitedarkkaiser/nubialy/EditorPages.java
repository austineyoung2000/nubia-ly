package com.elitedarkkaiser.nubialy;
import android.app.Activity;
import android.view.*;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import java.util.*;
/** One editor, visible tabs, one draft/worker and fixed scoped actions. */
final class EditorPages {
 private record Panel(String section,String page,String title,View view){}
 private final AppCompatActivity activity;private final MaterialToolbar toolbar;private final ScrollView scroll;
 private final List<Panel> panels=new ArrayList<>();private final Map<String,Integer> positions=new HashMap<>();
 private String section="status_bar",page="",title="Clock";private Runnable changed=()->{};private TabLayout tabs;private boolean selecting;
 void onChange(Runnable callback){changed=callback;changed.run();}String title(){return title;}String section(){return section;}String page(){return page;}
 EditorPages(AppCompatActivity activity,MaterialToolbar toolbar,ScrollView scroll){this.activity=activity;this.toolbar=toolbar;this.scroll=scroll;toolbar.setNavigationOnClickListener(v->activity.finish());activity.getOnBackPressedDispatcher().addCallback(activity,new OnBackPressedCallback(true){@Override public void handleOnBackPressed(){activity.finish();}});}
 void register(String section,String page,String title,View view){panels.add(new Panel(section,page,title,view));}
 void show(String section,String requested){positions.put(this.section+":"+page,scroll.getScrollY());if(panels.stream().noneMatch(p->p.section.equals(section))){show("status_bar","");return;}this.section=section;page=EditorNavigation.tab(section,requested);String area=switch(section){case "launcher"->"Launcher";case "lock_screen"->"Lock screen";case "quick_settings"->"Quick Settings";case "volume"->"Volume";case "notifications"->"Notifications";case "power_menu"->"Power menu";default->"Status bar";};
  for(Panel p:panels)p.view.setVisibility(p.section.equals(section)&&(p.page.equals("preview")||EditorNavigation.contains(section,page,p.page))?View.VISIBLE:View.GONE);
  toolbar.setTitle(area);for(String[] t:EditorNavigation.tabs(section))if(t[0].equals(page))title=t[1];
  if(tabs!=null){selecting=true;tabs.removeAllTabs();for(String[] t:EditorNavigation.tabs(section))tabs.addTab(tabs.newTab().setText(t[1]).setTag(t[0]),t[0].equals(page));selecting=false;}
  changed.run();int y=positions.getOrDefault(section+":"+page,0);scroll.post(()->scroll.scrollTo(0,y));
 }
 private static void detach(View v){if(v.getParent() instanceof ViewGroup p)p.removeView(v);}
 void layout(MaterialButton apply,MaterialButton restore,TextView status,View diagnostics){
  detach(scroll);detach(toolbar);detach(apply);detach(restore);detach(status);detach(diagnostics);
  LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(activity.getColor(R.color.background));
  androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(scroll,null);scroll.setPadding(0,0,0,0);
  activity.setContentView(root);root.addView(toolbar,new LinearLayout.LayoutParams(-1,-2));
  tabs=new TabLayout(activity);tabs.setTabMode(TabLayout.MODE_SCROLLABLE);tabs.setTabGravity(TabLayout.GRAVITY_START);tabs.setTabTextColors(activity.getColor(R.color.on_surface),activity.getColor(R.color.primary));tabs.setSelectedTabIndicatorColor(activity.getColor(R.color.primary));root.addView(tabs,new LinearLayout.LayoutParams(-1,-2));
  tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){public void onTabSelected(TabLayout.Tab t){if(!selecting)show(section,(String)t.getTag());}public void onTabUnselected(TabLayout.Tab t){}public void onTabReselected(TabLayout.Tab t){}});
  root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout footer=new LinearLayout(activity);footer.setOrientation(LinearLayout.VERTICAL);footer.setPadding(dp(20),dp(4),dp(20),dp(8));footer.setBackgroundColor(activity.getColor(R.color.surface));status.setTextSize(12);status.setMaxLines(2);status.setEllipsize(android.text.TextUtils.TruncateAt.END);footer.addView(status,new LinearLayout.LayoutParams(-1,-2));LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);row.addView(restore,new LinearLayout.LayoutParams(0,-2,1));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,-2,1);ap.leftMargin=dp(12);row.addView(apply,ap);footer.addView(row);root.addView(footer);
  toolbar.getMenu().add("Dashboard").setIcon(android.R.drawable.ic_menu_view).setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM);toolbar.getMenu().add("Help & diagnostics").setIcon(android.R.drawable.ic_menu_info_details).setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM);
  toolbar.setOnMenuItemClickListener(item->{if(item.getTitle().equals("Dashboard")){activity.finish();return true;}detach(diagnostics);ScrollView body=new ScrollView(activity);body.addView(diagnostics);new com.google.android.material.dialog.MaterialAlertDialogBuilder(activity).setTitle("Help & diagnostics").setView(body).setPositiveButton("Close",null).show();return true;});
  androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{var bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});show(section,page);
 }
 private int dp(int n){return PersonalizationUi.dp(activity,n);}
}
