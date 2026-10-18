package com.elitedarkkaiser.nubialy;
import android.graphics.Canvas;
import android.graphics.Rect;
import java.lang.reflect.*;
import java.util.*;
/** Themed drawables replace the base bitmap method rather than delegating to it. */
public final class LauncherArtworkMethods {
 private LauncherArtworkMethods(){}
 public static Method override(Class<?> base,Class<?> candidate){
  if(candidate==base||!base.isAssignableFrom(candidate))return null;
  try{Method method=candidate.getDeclaredMethod("c",Canvas.class,Rect.class);return method.getReturnType()==void.class&&!Modifier.isStatic(method.getModifiers())?method:null;}catch(ReflectiveOperationException missing){return null;}
 }
 public static List<Method> themed(ClassLoader loader,Class<?> base){
  List<Method> result=new ArrayList<>();for(String name:new String[]{"s1.c","u1.c"})try{Method method=override(base,Class.forName(name,false,loader));if(method!=null)result.add(method);}catch(ClassNotFoundException|LinkageError missing){}return result;
 }
}
