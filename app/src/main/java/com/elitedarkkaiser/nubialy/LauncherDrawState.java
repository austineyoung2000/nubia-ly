package com.elitedarkkaiser.nubialy;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.widget.TextView;
import java.lang.reflect.Field;
/** Temporary state for one synchronous draw. No setters that invalidate or persist settings. */
public final class LauncherDrawState {
    private LauncherDrawState(){}
    public static final class TextState {
        private final TextView view;private final Field field;private final int color;private final Paint paint;private final float radius,dx,dy;private final int shadowColor;
        private TextState(TextView v,Field f) throws IllegalAccessException{view=v;field=f;color=f.getInt(v);paint=new Paint(v.getPaint());radius=v.getPaint().getShadowLayerRadius();dx=v.getPaint().getShadowLayerDx();dy=v.getPaint().getShadowLayerDy();shadowColor=v.getPaint().getShadowLayerColor();}
        public void restore() throws IllegalAccessException {field.setInt(view,color);view.getPaint().set(paint);if(radius>0)view.getPaint().setShadowLayer(radius,dx,dy,shadowColor);else view.getPaint().clearShadowLayer();}
    }
    public static TextState hide(TextView view,Field currentColor) throws IllegalAccessException{
        TextState state=new TextState(view,currentColor);
        try{currentColor.setInt(view,0);view.getPaint().setColor(0);return state;}
        catch(RuntimeException|IllegalAccessException error){state.restore();throw error;}
    }
    public static int scale(Canvas canvas,Drawable drawable,int percentage){
        if(!new LauncherStyle(percentage,false).valid())throw new IllegalArgumentException("Invalid launcher scale");
        int save=canvas.save();try{var bounds=drawable.getBounds();float value=percentage/100f;canvas.scale(value,value,bounds.exactCenterX(),bounds.exactCenterY());return save;}
        catch(RuntimeException failure){canvas.restoreToCount(save);throw failure;}
    }
}
