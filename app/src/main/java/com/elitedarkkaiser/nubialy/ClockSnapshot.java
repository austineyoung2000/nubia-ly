package com.elitedarkkaiser.nubialy;
/** Immutable clock selection captured at Enable, independent of subsequent draft edits. */
public record ClockSnapshot(int color,int size,boolean bold,boolean seconds,boolean italic,int spacing,int family,boolean hidden){
 public static final ClockSnapshot STOCK=new ClockSnapshot(0,0,false,false,false,0,0,false);
 public boolean valid(){return (color==0||ClockColorPolicy.valid(color))&&ClockStylePolicy.valid(size)&&ClockStylePolicy.validSpacing(spacing)&&ClockStylePolicy.validFamily(family);}
 public String encode(){return color+","+size+","+((bold?1:0)|(seconds?2:0)|(italic?4:0)|(hidden?8:0))+","+spacing+","+family;}
 public static ClockSnapshot decode(String text){if(text==null||text.length()>80)return null;String[] f=text.split(",",-1);if(f.length!=5)return null;int[] n=new int[5];try{for(int i=0;i<5;i++){if(!f[i].matches("-?[0-9]{1,10}"))return null;n[i]=Integer.parseInt(f[i]);}}catch(NumberFormatException bad){return null;}if(n[2]<0||n[2]>15)return null;ClockSnapshot value=new ClockSnapshot(n[0],n[1],(n[2]&1)!=0,(n[2]&2)!=0,(n[2]&4)!=0,n[3],n[4],(n[2]&8)!=0);return value.valid()?value:null;}
}
