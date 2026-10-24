package com.elitedarkkaiser.nubialy;
/** Merge only the visible editor scope into the last applied snapshot. Drafts never grant authority. */
public final class AppearanceScope {
 private AppearanceScope(){}
 public record Snapshot(StatusBarStyle bar,int color,int date,ClockSnapshot clock) {
  public static final Snapshot STOCK=new Snapshot(StatusBarStyle.STOCK,ClockColorPolicy.DEFAULT,0,ClockSnapshot.STOCK);
 }
 private static boolean owns(String section,String page,String owner,String detail){return owner.equals(section)&&(page.isEmpty()||detail.equals(page));}
 private static int bits(int active,int draft,int mask){return (active&~mask)|(draft&mask);}
 public static Snapshot merge(String section,String page,Snapshot active,Snapshot draft){
  if(section==null||page==null||active==null||draft==null)throw new IllegalArgumentException();
  if(page.startsWith("tab_")){Snapshot result=active;for(String scope:EditorNavigation.scopes(section,page)){if(scope.equals(page))return active;result=merge(section,scope,result,draft);}return result;}
  var a=active.bar();var d=draft.bar();var x=a.surfaces();var y=d.surfaces();
  int hiddenMask=(owns(section,page,"quick_settings","labels")?3:0)|(owns(section,page,"lock_screen","clock")?4:0)|(owns(section,page,"lock_screen","date")?8:0);
  int optionMask=(owns(section,page,"quick_settings","top_layout")?3:0)|(owns(section,page,"quick_settings","label_layout")?60:0)|(owns(section,page,"quick_settings","brightness_layout")?448:0)|(owns(section,page,"quick_settings","landscape")?32256:0);
  SurfaceStyle surfaces=new SurfaceStyle(
   bits(x.hidden(),y.hidden(),hiddenMask),
   owns(section,page,"quick_settings","labels")?y.primary():x.primary(),
   owns(section,page,"quick_settings","labels")?y.secondary():x.secondary(),
   owns(section,page,"lock_screen","clock")?y.clock():x.clock(),
   owns(section,page,"lock_screen","date")?y.date():x.date(),
   owns(section,page,"quick_settings","labels")?y.primaryPosition():x.primaryPosition(),
   owns(section,page,"quick_settings","labels")?y.secondaryPosition():x.secondaryPosition(),
   owns(section,page,"lock_screen","clock")?y.clockPosition():x.clockPosition(),
   owns(section,page,"lock_screen","date")?y.datePosition():x.datePosition(),
   owns(section,page,"quick_settings","tiles")?y.tileShape():x.tileShape(),
   owns(section,page,"quick_settings","tiles")?y.largeTileShape():x.largeTileShape(),
   owns(section,page,"lock_screen","widgets")?y.widgetsHidden():x.widgetsHidden(),
   owns(section,page,"lock_screen","fingerprint")?y.fingerprintStyle():x.fingerprintStyle(),
   owns(section,page,"lock_screen","fingerprint")?y.fingerprintHidden():x.fingerprintHidden(),
   owns(section,page,"lock_screen","fonts")?y.clockFont():x.clockFont(),
   owns(section,page,"lock_screen","fingerprint")?y.fingerprintAnimation():x.fingerprintAnimation(),
   owns(section,page,"quick_settings","style")?y.qsStyle():x.qsStyle(),
   owns(section,page,"quick_settings","brightness")?y.brightnessStyle():x.brightnessStyle(),
   owns(section,page,"volume","styles")?y.volumeStyle():x.volumeStyle(),
   owns(section,page,"notifications","styles")?y.notificationStyle():x.notificationStyle(),
   owns(section,page,"status_bar","connectivity")?y.wifiStyle():x.wifiStyle(),
   owns(section,page,"status_bar","connectivity")?y.signalStyle():x.signalStyle(),
   owns(section,page,"power_menu","styles")?y.powerStyle():x.powerStyle(),
   owns(section,page,"notifications","appearance")?y.notificationAppearance():x.notificationAppearance(),
   owns(section,page,"notifications","behavior")?y.notificationBehavior():x.notificationBehavior(),
   owns(section,page,"lock_screen","charging")?y.chargingInfo():x.chargingInfo(),
   owns(section,page,"status_bar","battery")?y.batteryStyle():x.batteryStyle(),
   owns(section,page,"status_bar","battery")?y.batteryPercent():x.batteryPercent(),
   owns(section,page,"status_bar","battery")?y.batteryCharge():x.batteryCharge(),
   owns(section,page,"notifications","icons")?y.notificationCount():x.notificationCount(),
   owns(section,page,"notifications","icons")?y.notificationLimit():x.notificationLimit(),
   owns(section,page,"quick_settings","grid")?y.qsColumns():x.qsColumns(),
   owns(section,page,"quick_settings","grid")?y.qsRows():x.qsRows(),
   bits(x.qsOptions(),y.qsOptions(),optionMask),
   owns(section,page,"status_bar","iconpacks")?y.iconPack():x.iconPack(),
   owns(section,page,"quick_settings","iconpacks")?y.qsIconPack():x.qsIconPack()).withQsTheme(section.equals("quick_settings")?SurfaceStyle.QsTheme.merge(x.qsTheme(),y.qsTheme(),page):x.qsTheme()).withPalette(SurfaceStyle.Palette.merge(x.palette(),y.palette(),section,page));
  int mask=(owns(section,page,"status_bar","icons")?15:0)|(owns(section,page,"status_bar","inventory")?112:0);
  StatusBarStyle result=new StatusBarStyle(bits(a.mask(),d.mask(),mask),
   owns(section,page,"status_bar","icons")?d.systemScale():a.systemScale(),
   owns(section,page,"status_bar","icons")?d.notificationScale():a.notificationScale(),
   owns(section,page,"status_bar","battery")?d.batteryScale():a.batteryScale(),
   owns(section,page,"status_bar","icons")?d.systemOpacity():a.systemOpacity(),
   owns(section,page,"status_bar","icons")?d.notificationOpacity():a.notificationOpacity(),
   owns(section,page,"status_bar","icons")?d.batteryOpacity():a.batteryOpacity(),
   owns(section,page,"status_bar","inventory")?d.hiddenSlots():a.hiddenSlots(),
   owns(section,page,"status_bar","icons")?d.systemTransform():a.systemTransform(),
   owns(section,page,"status_bar","icons")?d.notificationTransform():a.notificationTransform(),
   owns(section,page,"status_bar","icons")?d.batteryTransform():a.batteryTransform(),
   owns(section,page,"status_bar","inventory")?d.fanAppearance():a.fanAppearance(),
   owns(section,page,"status_bar","inventory")?d.pumpAppearance():a.pumpAppearance(),
   owns(section,page,"status_bar","inventory")?d.refreshAppearance():a.refreshAppearance(),surfaces);
  var ac=active.clock();var dc=draft.clock();
  ClockSnapshot clock=new ClockSnapshot(
   owns(section,page,"status_bar","color")?dc.color():ac.color(),
   owns(section,page,"status_bar","typography")?dc.size():ac.size(),
   owns(section,page,"status_bar","typography")?dc.bold():ac.bold(),
   owns(section,page,"status_bar","typography")?dc.seconds():ac.seconds(),
   owns(section,page,"status_bar","typography")?dc.italic():ac.italic(),
   owns(section,page,"status_bar","typography")?dc.spacing():ac.spacing(),
   owns(section,page,"status_bar","typography")?dc.family():ac.family(),
   owns(section,page,"status_bar","typography")?dc.hidden():ac.hidden());
  return new Snapshot(result,owns(section,page,"status_bar","icons")?draft.color():active.color(),owns(section,page,"status_bar","date")?draft.date():active.date(),clock);
 }
}
