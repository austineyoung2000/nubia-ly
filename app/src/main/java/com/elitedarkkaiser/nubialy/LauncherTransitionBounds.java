package com.elitedarkkaiser.nubialy;
import android.app.Activity;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
/** Only the floating animation's freshly computed output rectangle; no workspace geometry writes. */
public final class LauncherTransitionBounds {
    private LauncherTransitionBounds(){}
    public static Method resolve(Class<?> floating) throws NoSuchMethodException {
        Method found=null;
        for(Method method:floating.getDeclaredMethods()){
            Class<?>[] p=method.getParameterTypes();
            if(Modifier.isStatic(method.getModifiers())&&method.getReturnType()==void.class&&p.length==5
                &&Activity.class.isAssignableFrom(p[0])&&p[1]==View.class&&p[2]==boolean.class&&p[3]==RectF.class&&p[4]==Rect.class){
                if(found!=null)throw new NoSuchMethodException("Ambiguous launcher animation bounds");found=method;
            }
        }
        if(found==null)throw new NoSuchMethodException("Launcher animation bounds unavailable");return found;
    }
    public static boolean adjust(RectF bounds,int percentage){
        if(bounds==null||percentage<70||percentage>=100||!Float.isFinite(bounds.left)||!Float.isFinite(bounds.top)||!Float.isFinite(bounds.right)||!Float.isFinite(bounds.bottom)||bounds.isEmpty())return false;
        float dx=bounds.width()*(1-percentage/100f)/2,dy=bounds.height()*(1-percentage/100f)/2;
        if(!Float.isFinite(dx)||!Float.isFinite(dy))return false;
        bounds.inset(dx,dy);return true;
    }
}
