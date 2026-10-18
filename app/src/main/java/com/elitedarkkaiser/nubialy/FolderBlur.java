package com.elitedarkkaiser.nubialy;

/** Bounded separable box blur for the launcher's small wallpaper copy. Never modifies input. */
public final class FolderBlur {
    private FolderBlur(){}
    public static int[] pixels(int[] source,int width,int height,int radius){
        if(width<1||height<1||(long)width*height>524288||source.length!=width*height||radius<0||radius>25)throw new IllegalArgumentException("Invalid folder blur input");
        int[] out=source.clone();if(radius==0)return out;int[] temp=new int[out.length];
        // Three box passes approximate a smooth Gaussian without native resources or per-draw work.
        for(int pass=0;pass<3;pass++){line(out,temp,width,height,radius,false);line(temp,out,width,height,radius,true);}return out;
    }
    private static void line(int[] in,int[] out,int width,int height,int radius,boolean vertical){
        int count=vertical?width:height,length=vertical?height:width,window=radius*2+1;
        for(int row=0;row<count;row++){
            long a=0,r=0,g=0,b=0;
            for(int k=-radius;k<=radius;k++){int c=in[index(row,clamp(k,length),width,vertical)];a+=c>>>24;r+=(c>>16)&255;g+=(c>>8)&255;b+=c&255;}
            for(int x=0;x<length;x++){
                out[index(row,x,width,vertical)]=((int)(a/window)<<24)|((int)(r/window)<<16)|((int)(g/window)<<8)|(int)(b/window);
                int old=in[index(row,clamp(x-radius,length),width,vertical)],next=in[index(row,clamp(x+radius+1,length),width,vertical)];
                a+=(next>>>24)-(old>>>24);r+=((next>>16)&255)-((old>>16)&255);g+=((next>>8)&255)-((old>>8)&255);b+=(next&255)-(old&255);
            }
        }
    }
    private static int clamp(int x,int length){return Math.max(0,Math.min(length-1,x));}
    private static int index(int row,int x,int width,boolean vertical){return vertical?x*width+row:row*width+x;}
}
