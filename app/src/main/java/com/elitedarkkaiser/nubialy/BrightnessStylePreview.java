package com.elitedarkkaiser.nubialy;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
/** Illustrative static track and native-style clipped progress, with no display access. */
final class BrightnessStylePreview extends View {
    private final int style;private final java.util.function.Supplier<SurfaceStyle.Palette> palette;
    BrightnessStylePreview(Context context,int style){this(context,style,()->SurfaceStyle.Palette.STOCK);}
    BrightnessStylePreview(Context context,int style,java.util.function.Supplier<SurfaceStyle.Palette> palette){super(context);this.style=style;this.palette=palette;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);int w=getWidth(),h=getHeight(),inset=PersonalizationUi.dp(getContext(),4);if(w<12||h<12)return;
        for(int i=0;i<2;i++){GradientDrawable nativeLeaf=new GradientDrawable();GradientDrawable d=nativeLeaf;d.setColor(i==0?0xff73707e:0xffd3cbef);d.setCornerRadius(h/3f);d.setBounds(inset,h/4,w-inset,h*3/4);GradientDrawable colored=BrightnessColors.copy(d,i==1,palette.get());if(colored!=null)d=colored;final GradientDrawable artwork=d;int save=c.save();if(i==1)c.clipRect(inset,0,w*0.6f,h);
            try{if(!BrightnessEffects.draw(style,c,artwork,getResources().getDisplayMetrics().density,()->true,()->artwork.draw(c)))artwork.draw(c);}catch(Throwable unavailable){artwork.draw(c);}finally{c.restoreToCount(save);}
        }
    }
}
