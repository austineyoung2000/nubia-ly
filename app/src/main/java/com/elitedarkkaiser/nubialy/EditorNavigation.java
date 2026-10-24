package com.elitedarkkaiser.nubialy;
/** Shared routes for visible tabs and their exact Apply/Restore scopes. */
public final class EditorNavigation {
 private EditorNavigation(){}
 public static String[][] tabs(String section){return switch(section){
 case "quick_settings"->new String[][]{{"tab_tiles","Tiles","style,tiles,iconpacks"},{"tab_colors","Colors","colors"},{"tab_header","Header","header"},{"tab_layout","Layout","grid,landscape,top_layout,label_layout,labels"},{"tab_brightness","Brightness","brightness,brightness_layout"},{"tab_background","Background","background"}};
 case "status_bar"->new String[][]{{"tab_clock","Clock","typography,color,date"},{"tab_colors","Colors","colors"},{"tab_icons","Icons","icons,inventory"},{"tab_battery","Battery","battery"},{"tab_artwork","Artwork","iconpacks,connectivity"}};
 case "lock_screen"->new String[][]{{"tab_clock","Clock","clock,date,fonts"},{"tab_fingerprint","Fingerprint","fingerprint"},{"tab_widgets","Widgets","widgets"},{"tab_charging","Charging","charging"}};
 case "launcher"->new String[][]{{"icons","Icons","icons"},{"folders","Folders","folders"},{"grid","Grid","grid"},{"dock","Dock","dock"},{"artwork","Artwork","artwork"},{"indicator","Indicator","indicator"},{"tools","Tools","tools"}};
 case "notifications"->new String[][]{{"tab_styles","Styles","styles,appearance"},{"tab_controls","Controls","behavior,icons"}};
 default->new String[][]{{"styles","Styles","styles"}};};}
 public static String tab(String section,String page){for(String[] t:tabs(section))if(t[0].equals(page)||java.util.Arrays.asList(t[2].split(",")).contains(page))return t[0];return tabs(section)[0][0];}
 public static String[] scopes(String section,String page){for(String[] t:tabs(section))if(t[0].equals(page))return t[2].split(",");return new String[]{page};}
 public static boolean contains(String section,String tab,String panel){return java.util.Arrays.asList(scopes(section,tab)).contains(panel);}
}
