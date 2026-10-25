package com.elitedarkkaiser.nubialy;

import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.util.*;
/** Worker-only sidebar resolver, including activity aliases and standard generated pack artwork. */
public final class SidebarIconPack {
    private final LauncherIconPacks.Loaded pack;
    private final Decorations decorations;
    private final Map<String,String> aliases;
    public record Decorations(List<String> backs,String mask,String front,float scale){
        public Decorations { backs=List.copyOf(backs); }
        public static final Decorations EMPTY=new Decorations(List.of(),"","",1);
        public boolean any(){return !backs.isEmpty()||!mask.isEmpty()||!front.isEmpty();}
    }
    public SidebarIconPack(LauncherIconPacks.Loaded pack,Decorations decorations){this.pack=pack;this.decorations=decorations;this.aliases=packageMappings(pack.icons());}
    public static SidebarIconPack load(Context context,String id)throws Exception{
        var pack=LauncherIconPacks.load(context,id);Decorations decor=Decorations.EMPTY;Resources r=pack.resources();
        int xml=r.getIdentifier("appfilter","xml",id);
        if(xml!=0){try(XmlResourceParser parser=r.getXml(xml)){decor=parse(parser);}}
        else{int raw=r.getIdentifier("appfilter","raw",id);try(InputStream in=raw==0?r.getAssets().open("appfilter.xml"):r.openRawResource(raw)){decor=parse(in);}}
        return new SidebarIconPack(pack,decor);
    }
    public Drawable icon(ComponentName component,Drawable stock){
        Drawable exact=pack.icon(component);if(exact!=null)return exact;
        // A launcher activity alias is safe only when the package has one artwork choice.
        String name=aliases.get(component.getPackageName());Drawable mapped=resource(name);if(mapped!=null)return mapped;
        return decorations.any()&&stock!=null?compose(component.getPackageName(),stock):null;
    }
    public static String uniquePackageMapping(Map<String,String> icons,String pkg){return packageMappings(icons).get(pkg);}
    private static Map<String,String> packageMappings(Map<String,String> icons){Map<String,String> result=new HashMap<>();Set<String> ambiguous=new HashSet<>();for(var e:icons.entrySet()){ComponentName c=ComponentName.unflattenFromString(e.getKey());if(c==null)continue;String pkg=c.getPackageName();String before=result.putIfAbsent(pkg,e.getValue());if(before!=null&&!before.equals(e.getValue()))ambiguous.add(pkg);}for(String pkg:ambiguous)result.remove(pkg);return Map.copyOf(result);}
    private Drawable resource(String name){try{if(name==null||name.isEmpty())return null;int id=pack.resources().getIdentifier(name,"drawable",pack.id());if(id==0)id=pack.resources().getIdentifier(name,"mipmap",pack.id());return id==0?null:pack.resources().getDrawable(id,null);}catch(RuntimeException unavailable){return null;}}
    private Drawable compose(String pkg,Drawable stock){
        Drawable back=decorations.backs().isEmpty()?null:resource(decorations.backs().get(Math.floorMod(pkg.hashCode(),decorations.backs().size()))),mask=resource(decorations.mask()),front=resource(decorations.front());
        if(back==null&&mask==null&&front==null)return null;
        Bitmap result=Bitmap.createBitmap(160,160,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(result);
        paint(back,canvas,0,160);int layer=canvas.saveLayer(0,0,160,160,null);float inset=80*(1-decorations.scale());paint(stock,canvas,Math.round(inset),Math.round(160-inset));
        if(mask!=null){Bitmap cut=Bitmap.createBitmap(160,160,Bitmap.Config.ARGB_8888);paint(mask,new Canvas(cut),0,160);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setBlendMode(BlendMode.DST_OUT);canvas.drawBitmap(cut,0,0,p);cut.recycle();}
        canvas.restoreToCount(layer);paint(front,canvas,0,160);return new BitmapDrawable(pack.resources(),result);
    }
    private static void paint(Drawable d,Canvas c,int start,int end){if(d==null)return;Rect old=new Rect(d.getBounds());try{d.setBounds(start,start,end,end);d.draw(c);}finally{d.setBounds(old);}}
    public static Decorations parse(InputStream input)throws Exception{XmlPullParser p=Xml.newPullParser();p.setInput(new Bounded(input),"UTF-8");return parse(p);}
    private static Decorations parse(XmlPullParser p)throws Exception{
        List<String> backs=new ArrayList<>();String mask="",front="";float scale=1;int count=0;
        for(int event=p.getEventType();event!=XmlPullParser.END_DOCUMENT;event=p.nextToken()){
            if(++count>100000||event==XmlPullParser.DOCDECL)throw new IOException("Unsupported icon-pack XML");if(event!=XmlPullParser.START_TAG)continue;
            String tag=p.getName().toLowerCase(Locale.ROOT);
            if(tag.equals("iconback")||tag.equals("iconmask")||tag.equals("iconupon"))for(int i=0;i<p.getAttributeCount();i++){
                if(!p.getAttributeName(i).matches("img[0-9]+"))continue;String name=p.getAttributeValue(i);if(name==null||!name.matches("[A-Za-z0-9_]{1,160}"))continue;
                if(tag.equals("iconback")){if(backs.size()<32)backs.add(name);}else if(tag.equals("iconmask"))mask=name;else front=name;
            }
            if(tag.equals("scale")){try{float v=Float.parseFloat(p.getAttributeValue(null,"factor"));if(Float.isFinite(v)&&v>=.2f&&v<=1)scale=v;}catch(RuntimeException ignored){}}
        }return new Decorations(backs,mask,front,scale);
    }
    private static final class Bounded extends FilterInputStream {
        int remaining=4*1024*1024;Bounded(InputStream in){super(in);}
        @Override public int read()throws IOException{if(remaining--<=0)throw new IOException("Icon-pack XML too large");return in.read();}
        @Override public int read(byte[] b,int off,int len)throws IOException{if(remaining<=0)throw new IOException("Icon-pack XML too large");int n=in.read(b,off,Math.min(len,remaining));if(n>0)remaining-=n;return n;}
    }
}
