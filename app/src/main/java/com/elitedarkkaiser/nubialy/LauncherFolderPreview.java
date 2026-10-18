package com.elitedarkkaiser.nubialy;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Illustrative full-screen folder preview; no user wallpaper or layout is read. */
final class LauncherFolderPreview extends View {
    private LauncherStyle style=LauncherStyle.STOCK;private Bitmap backdrop;private int lastRadius=-2;
    LauncherFolderPreview(Context context){super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    void setStyle(LauncherStyle value){style=value;invalidate();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);if(getWidth()==0||getHeight()==0)return;
        int radius=style.folderBlur()<0?20:style.folderBlur();
        if(backdrop==null||lastRadius!=radius){int[] pixels=new int[80*60];for(int y=0;y<60;y++)for(int x=0;x<80;x++){boolean warm=x>y+14;int r=warm?230:50+x*2,g=warm?140+y:120+y,b=warm?80:190;pixels[y*80+x]=Color.rgb(r,g,b);}backdrop=Bitmap.createBitmap(FolderBlur.pixels(pixels,80,60,radius),80,60,Bitmap.Config.ARGB_8888);lastRadius=radius;}
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);c.drawBitmap(backdrop,null,new Rect(0,0,getWidth(),getHeight()),p);p.setColor(0x33000000);c.drawRect(0,0,getWidth(),getHeight(),p);
        float unit=Math.min(getWidth()/260f,getHeight()/180f);c.save();c.translate((getWidth()-260*unit)/2,(getHeight()-180*unit)/2);c.scale(unit,unit);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif-medium",0));p.setTextSize(20);p.setColor(LauncherStyle.labelTint(style.folderTitleColor(),Color.WHITE));c.drawText("Folder",130,30,p);
        int columns=style.layout().folderColumns()==0?3:style.layout().folderColumns(),rows=style.layout().folderRows()==0?3:style.layout().folderRows();
        int[] colors={0xffd3cbef,0xffb8c9bf,0xffe8c4ac};float cellW=230f/columns,cellH=125f/rows,size=Math.min(36,Math.min(cellW*.65f,cellH*.62f));
        for(int i=0;i<columns*rows;i++){float x=15+cellW*(i%columns+.5f),y=45+cellH*(i/columns+.5f);p.setColor(colors[i%3]);c.drawRoundRect(x-size/2,y-size/2,x+size/2,y+size/2,size*.22f,size*.22f,p);p.setTextSize(Math.min(9,cellH*.2f));p.setColor(LauncherStyle.labelTint(style.folderLabelColor(),Color.WHITE));c.drawText("App",x,y+size/2+Math.min(10,cellH*.22f),p);}c.restore();
    }
}
