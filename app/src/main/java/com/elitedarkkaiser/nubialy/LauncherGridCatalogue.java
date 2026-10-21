package com.elitedarkkaiser.nubialy;
import java.util.*;
/** Additive native grid choices; the launcher owns all actual selection and item reflow. */
public final class LauncherGridCatalogue {
 private final Map<Integer,Object> owned=new HashMap<>();private Map<Integer,Object> map;
 public boolean enable(Map<Integer,Object> target){
  if(target==null||target.get(100)==null||target.get(101)==null)return false;
  if(map!=target){restore();map=target;}
  Object nativeProfile=target.get(101);
  for(int rows=6;rows<=8;rows++)for(int columns=4;columns<=7;columns++){int key=(rows<<4)|columns;if(!target.containsKey(key)){target.put(key,nativeProfile);owned.put(key,nativeProfile);}}
  return true;
 }
 public void restore(){if(map!=null)for(var entry:owned.entrySet())if(map.get(entry.getKey())==entry.getValue())map.remove(entry.getKey());owned.clear();map=null;}
 public static boolean choice(int rows,int columns){return rows>=6&&rows<=8&&columns>=4&&columns<=7;}
 public int added(){return owned.size();}
}
