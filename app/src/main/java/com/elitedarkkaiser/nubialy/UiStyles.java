package com.elitedarkkaiser.nubialy;
/** Named upstream styles; IDs are also the guard command allowlist. */
final class UiStyles {
 record Entry(String id,String name,int background,int card,int accent) {}
 static final Entry[] ENTRIES={
  new Entry("espresso","Espresso",0xff1f1c18,0xff3a342c,0xffc6b39f),
  new Entry("snow","Snow Paint Drop",0xff1a161d,0xff362c3a,0xffc0a8ff),
  new Entry("vivid","Vivid",0xff000000,0xff171717,0xffc0a8ff),
  new Entry("pacleggers","Pacleggers",0xff1f2127,0xff3d414d,0xffc0a8ff),
  new Entry("amalgamation","Shishu Amalgamation",0xff0a0c11,0xff202737,0xffc0a8ff),
  new Entry("illusions","Shishu Illusions",0xff121212,0xff292929,0xffc0a8ff),
  new Entry("immensity","Shishu Immensity",0xff070707,0xff2d2d2d,0xffc0a8ff),
  new Entry("nights","Shishu Nights",0xff101014,0xff2e2e3a,0xffc0a8ff),
  new Entry("solarized","Solarized Dark",0xff002b36,0xff073642,0xff268bd2)
 };
 static Entry find(String id){for(Entry e:ENTRIES)if(e.id().equals(id))return e;return null;}
 static boolean command(String operation){return operation!=null&&(operation.equals("status")||operation.equals("restore")||operation.equals("confirm")||(operation.startsWith("preview_")&&find(operation.substring(8))!=null));}
}
