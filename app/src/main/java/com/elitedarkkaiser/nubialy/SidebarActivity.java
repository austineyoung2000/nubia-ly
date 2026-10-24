package com.elitedarkkaiser.nubialy;

import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.*;
import java.util.concurrent.*;

/** One draft and ordered save worker. The panel is a sample, never a live overlay. */
public final class SidebarActivity extends AppCompatActivity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private SidebarStyle draft;
    private LinearLayout body,preview,actions;
    private TextView status,appSummary,packSummary;
    private boolean closed,busy;
    private String page="";
    private final List<View> controls=new ArrayList<>();
    @Override public void onCreate(Bundle state){super.onCreate(state);draft=state==null?SidebarPreferences.draft(this):SidebarStyle.decode(state.getString("draft"));if(draft==null)draft=SidebarStyle.DEFAULT;page=state==null?"":state.getString("page","");render();}
    private int dp(int x){return PersonalizationUi.dp(this,x);}
    private TextView text(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(getColor(R.color.on_surface));v.setPadding(0,dp(6),0,dp(6));return v;}
    private MaterialButton button(LinearLayout parent,String title,Runnable action){MaterialButton b=new MaterialButton(this);b.setText(title);b.setCornerRadius(dp(24));parent.addView(b,new LinearLayout.LayoutParams(-1,-2));b.setOnClickListener(v->action.run());controls.add(b);return b;}
    private LinearLayout card(String title){MaterialCardView c=new MaterialCardView(this);PersonalizationUi.panel(c);LinearLayout inner=new LinearLayout(this);inner.setOrientation(1);inner.setPadding(dp(20),dp(14),dp(20),dp(14));inner.addView(text(title,21));c.addView(inner);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);body.addView(c,p);return inner;}
    private void render(){controls.clear();LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(getColor(R.color.background));setContentView(root);
        var toolbar=PersonalizationUi.toolbar(this,page.isEmpty()?"Sidebar":switch(page){case "appearance"->"Colors & blur";case "layout"->"Layout & size";case "icons"->"App icons";case "styles"->"UI styles";case "styles_qs"->"Quick Settings styles";case "styles_power"->"Power menu styles";case "styles_volume"->"Volume styles";default->"Apps";});toolbar.setNavigationOnClickListener(v->back());root.addView(toolbar);
        ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(16),0,dp(16),dp(12));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        preview=card("Preview");updatePreview();
        if(page.isEmpty()){
            LinearLayout overview=card("Make it yours");button(overview,"UI styles",()->open("styles"));button(overview,"Colors, blur & theme",()->open("appearance"));button(overview,"Layout & size",()->open("layout"));button(overview,"App icons",()->open("icons"));button(overview,"Choose apps",()->open("apps"));
            overview.addView(text("Enable nubia-ly for FloatAssist in Vector and reboot once. Changes appear when the sidebar opens. Data Dock and stock controls are retained.",14));
        }else if(page.equals("styles")){
            LinearLayout c=card("Match your UI");
            button(c,"Follow applied Quick Settings style",()->selectSurface(300));button(c,"Follow applied power menu style",()->selectSurface(301));button(c,"Follow applied volume style",()->selectSurface(302));
            c.addView(text("The same renderers used in those editors style the sidebar surfaces. Color, blur and dimensions stay independent.",14));
            button(c,"Quick Settings style gallery",()->open("styles_qs"));button(c,"Power menu style gallery",()->open("styles_power"));button(c,"Volume style gallery",()->open("styles_volume"));
        }else if(page.startsWith("styles_")){
            boolean qs=page.equals("styles_qs"),power=page.equals("styles_power");
            LinearLayout c=card(qs?"Quick Settings looks":power?"Power menu looks":"Volume looks");
            button(c,"Follow applied style",()->selectSurface(qs?300:power?301:302));
            c.addView(text("Samples use a neutral tone to show the effect. Your sidebar keeps its chosen color and blur.",14));
            surfaceCards(c,qs?QsTileEffects.NAMES:power?NotificationEffects.NAMES:VolumeEffects.NAMES,qs?0:power?100:200);
        }else if(page.equals("appearance")){
            LinearLayout c=card("Style");String[] styles={"Follow overall UI colors","Tonal","Glass","Custom"};for(int i=0;i<styles.length;i++){final int k=i;button(c,(draft.theme()==i?"✓  ":"")+styles[i],()->{draft=draft.with(9,k);if(k==1)draft=draft.with(0,0xff25232b).with(2,100).with(3,0);if(k==2)draft=draft.with(2,55).with(3,90);changed();render();});}
            button(c,"Panel color",()->color(0));button(c,"Text color",()->color(1));slider(c,"Opacity",2,draft.opacity(),0,100);slider(c,"Background blur",3,draft.blur(),0,150);slider(c,"Corner radius",8,draft.corner(),0,48);
            c.addView(text("Follow overall UI uses your applied Quick Settings inactive background and primary label colors. Blur affects the scene behind the panel; support depends on the ROM.",14));
        }else if(page.equals("layout")){
            LinearLayout c=card("Panel dimensions");slider(c,"Width · 0 = stock",4,draft.width(),0,400);slider(c,"Height · 0 = stock",5,draft.height(),0,800);
            button(c,"Restore stock dimensions",()->{draft=draft.with(4,0).with(5,0);changed();render();});
            slider(c,"Columns · 1 = list",6,draft.columns(),1,4);slider(c,"App icon size",7,draft.iconSize(),70,130);
        }else if(page.equals("icons")){
            LinearLayout c=card("Icon artwork");packSummary=text(draft.iconPack().equals("@launcher")?"Follow nubia-ly Launcher editor":draft.iconPack().isEmpty()?"Stock app icons":draft.iconPack(),15);c.addView(packSummary);
            button(c,"Follow nubia-ly launcher icon pack",()->{draft=draft.withPack("@launcher");changed();render();});button(c,"Choose installed icon pack",this::pickPack);button(c,"Stock app icons",()->{draft=draft.withPack("");changed();render();});
            c.addView(text("For Nova or another launcher, choose the same installed icon pack here. Private per-app launcher overrides are not imported. Apps missing from a pack keep their stock icon.",14));
        }else {
            LinearLayout c=card("Additional apps");appSummary=text(draft.apps().size()+" apps added",16);c.addView(appSummary);button(c,"Add or remove apps",this::pickApps);
            c.addView(text("Choose any enabled app with a launcher entry in your current profile. Added apps open normally. This does not force unsupported floating-window behavior. Your stock sidebar apps remain available.",14));
        }
        actions=new LinearLayout(this);actions.setOrientation(1);actions.setPadding(dp(16),0,dp(16),dp(8));status=text("Draft changes apply only after Enable / Apply.",13);actions.addView(status);button(actions,"Enable / Apply sidebar",()->apply(false));button(actions,"Restore stock sidebar",()->apply(true));root.addView(actions);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{var b=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());v.setPadding(b.left,b.top,b.right,b.bottom);return insets;});setBusy(busy);
    }
    private SidebarStyle previewStyle(){var applied=StatusBarPreferences.read(this).surfaces();int id=switch(draft.surface()){case 300->applied.qsStyle();case 301->applied.powerStyle()==0?0:100+applied.powerStyle();case 302->applied.volumeStyle()==0?0:200+applied.volumeStyle();default->draft.surface();};return draft.withSurface(id);}
    private void selectSurface(int id){draft=draft.withSurface(id);changed();render();}
    private void surfaceCards(LinearLayout parent,String[] names,int offset){
        for(int i=0;i<names.length;i++){
            int id=i==0?0:offset+i;MaterialCardView choice=new MaterialCardView(this);PersonalizationUi.panel(choice);choice.setStrokeWidth(dp(draft.surface()==id?2:1));choice.setStrokeColor(getColor(draft.surface()==id?R.color.primary:R.color.surface_container_high));
            LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(12),dp(10),dp(12),dp(10));
            View sample=new View(this);sample.setBackground(new SidebarSurfaceDrawable(draft.withSurface(id).with(0,0xff67626f).with(2,100),getResources().getDisplayMetrics().density,null,()->true));content.addView(sample,new LinearLayout.LayoutParams(-1,dp(72)));content.addView(text((draft.surface()==id?"✓  ":"")+names[i],16));choice.addView(content);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(10);parent.addView(choice,lp);choice.setTag("sidebar-surface-"+id);choice.setContentDescription(names[i]);choice.setClickable(true);choice.setFocusable(true);choice.setOnClickListener(v->selectSurface(id));controls.add(choice);
        }
    }
    private void open(String p){page=p;render();}
    private void back(){if(page.isEmpty())finish();else if(page.startsWith("styles_"))open("styles");else open("");}
    @Override public void onBackPressed(){back();}
    private void color(int key){ColorPickerDialog.show(this,key==0?"Panel color":"Text color","Sidebar",key==0?draft.color():draft.text(),value->{draft=draft.with(key,value).with(9,3);changed();});}
    private void slider(LinearLayout c,String title,int key,int value,int low,int high){TextView label=text(title+": "+value,14);c.addView(label);SeekBar b=new SeekBar(this);b.setMax(high-low);b.setProgress(value-low);c.addView(b);controls.add(b);b.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar v){}public void onStopTrackingTouch(SeekBar v){changed();}public void onProgressChanged(SeekBar v,int n,boolean user){if(!user)return;int x=n+low;if(key==4&&x>0)x=Math.max(160,x);if(key==5&&x>0)x=Math.max(240,x);draft=draft.with(key,x);label.setText(title+": "+x);updatePreview();}});}
    private void changed(){SidebarStyle saved=draft;worker.execute(()->SidebarPreferences.save(getApplicationContext(),saved));updatePreview();}
    private void updatePreview(){if(preview==null)return;while(preview.getChildCount()>1)preview.removeViewAt(1);LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(12),dp(12),dp(12),dp(12));panel.setBackground(new SidebarSurfaceDrawable(previewStyle(),getResources().getDisplayMetrics().density,null,()->true));TextView dock=text("⇄  Data Dock",16);dock.setTextColor(draft.text());panel.addView(dock);for(int row=0;row<2;row++){LinearLayout cells=new LinearLayout(this);for(int col=0;col<draft.columns();col++){TextView cell=text("▣\nApp",13);cell.setGravity(Gravity.CENTER);cell.setTextColor(draft.text());cells.addView(cell,new LinearLayout.LayoutParams(0,dp(56),1));}panel.addView(cells);}int w=draft.width()==0?220:draft.width();LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(Math.min(dp(Math.min(300,w)),getResources().getDisplayMetrics().widthPixels-dp(100)),-2);p.gravity=Gravity.END;preview.addView(panel,p);preview.addView(text("Illustration · live blur and launcher artwork appear on device",12));}
    private void setBusy(boolean value){busy=value;for(View v:controls)v.setEnabled(!value);}
    private void apply(boolean restore){if(busy)return;SidebarStyle saved=draft;setBusy(true);status.setText(restore?"Restoring…":"Checking recovery and saving…");worker.execute(()->{boolean ok=false;try{if(restore)ok=SidebarPreferences.stop(this);else if(SidebarCompatibility.supported(this)){String id=GuardReader.inspectEnabled();boolean allowed=RecoveryStore.enabledAllowed(this,id)||(RootGuardCommand.enableClock(SystemClock.elapsedRealtime()+8000)&&RecoveryStore.allowEnabled(this));ok=allowed&&SidebarPreferences.activate(this,saved);}}catch(RuntimeException ignored){}boolean result=ok;runOnUiThread(()->{if(closed)return;setBusy(false);status.setText(result?(restore?"Stock sidebar selected. Reopen the panel to refresh.":"Saved. Enable FloatAssist scope in Vector, reboot once, then open the sidebar. This is an experimental device test."):"Could not enable. Requires NX809J / Android 16, the inspected FloatAssist APK, Vector activation and Recovery Guard.");});});}
    private void pickPack(){setBusy(true);worker.execute(()->{List<LauncherIconPacks.Pack> packs=LauncherIconPacks.discover(this);runOnUiThread(()->{if(closed)return;setBusy(false);if(packs.isEmpty()){status.setText("No supported icon packs installed.");return;}new MaterialAlertDialogBuilder(this).setTitle("Icon pack").setItems(packs.stream().map(LauncherIconPacks.Pack::name).toArray(String[]::new),(d,n)->{draft=draft.withPack(packs.get(n).id());changed();render();}).setNegativeButton("Cancel",null).show();});});}
    private record App(String id,String label){}
    private void pickApps(){setBusy(true);worker.execute(()->{Map<String,App> found=new HashMap<>();var pm=getPackageManager();for(var r:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)){if(r.activityInfo==null||!r.activityInfo.enabled||!r.activityInfo.applicationInfo.enabled)continue;String id=r.activityInfo.packageName;found.putIfAbsent(id,new App(id,r.loadLabel(pm).toString()));}List<App> apps=new ArrayList<>(found.values());apps.sort(Comparator.comparing(App::label,String.CASE_INSENSITIVE_ORDER));runOnUiThread(()->{if(closed)return;setBusy(false);showApps(apps);});});}
    private void showApps(List<App> all){LinkedHashSet<String> selected=new LinkedHashSet<>(draft.apps());List<App> visible=new ArrayList<>(all);LinearLayout content=new LinearLayout(this);content.setOrientation(1);EditText search=new EditText(this);search.setHint("Search apps");content.addView(search);ListView list=new ListView(this);content.addView(list,new LinearLayout.LayoutParams(-1,dp(350)));ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_multiple_choice,new ArrayList<>());list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);list.setAdapter(adapter);Runnable fill=()->{adapter.clear();list.clearChoices();String q=search.getText().toString().toLowerCase(Locale.ROOT);visible.clear();for(App app:all)if(app.label().toLowerCase(Locale.ROOT).contains(q)||app.id().contains(q)){visible.add(app);adapter.add(app.label());}for(int i=0;i<visible.size();i++)list.setItemChecked(i,selected.contains(visible.get(i).id()));};fill.run();list.setOnItemClickListener((p,v,n,id)->{String pkg=visible.get(n).id();if(list.isItemChecked(n)){if(selected.size()>=100){list.setItemChecked(n,false);Toast.makeText(this,"Up to 100 additional apps",Toast.LENGTH_SHORT).show();}else selected.add(pkg);}else selected.remove(pkg);});search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){fill.run();}public void afterTextChanged(android.text.Editable e){}});new MaterialAlertDialogBuilder(this).setTitle("Sidebar apps").setView(content).setPositiveButton("Use selection",(d,n)->{draft=draft.withApps(new ArrayList<>(selected));changed();render();}).setNegativeButton("Cancel",null).show();}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("draft",draft.encode());b.putString("page",page);}
    @Override protected void onDestroy(){closed=true;worker.shutdown();super.onDestroy();}
}
