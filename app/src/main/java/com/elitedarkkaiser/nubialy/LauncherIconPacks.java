package com.elitedarkkaiser.nubialy;
import android.content.*;
import android.content.pm.*;
import android.content.res.*;
import android.graphics.drawable.Drawable;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.util.*;
/** Standard static appfilter mappings. Resource work belongs on a worker, never inside a draw. */
public final class LauncherIconPacks {
    public static final String[] ACTIONS={"org.adw.ActivityStarter.THEMES","com.novalauncher.THEME","com.gau.go.launcherex.theme"};
    public record Pack(String id,String name){}
    public record Loaded(String id,Resources resources,Map<String,String> icons){
        public Drawable icon(ComponentName component){try{String name=icons.get(component.flattenToString());if(name==null)return null;int resource=resources.getIdentifier(name,"drawable",id);if(resource==0)resource=resources.getIdentifier(name,"mipmap",id);return resource==0?null:resources.getDrawable(resource,null);}catch(RuntimeException unavailable){return null;}}
    }
    private LauncherIconPacks(){}
    public static boolean validPackage(String id){return id!=null&&(id.isEmpty()||(id.length()<=200&&id.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")));}
    public static List<Pack> discover(Context context){Map<String,Pack> found=new HashMap<>();PackageManager pm=context.getPackageManager();for(String action:ACTIONS)for(ResolveInfo r:pm.queryIntentActivities(new Intent(action),0)){if(r.activityInfo==null||r.activityInfo.applicationInfo==null||!r.activityInfo.applicationInfo.enabled)continue;String id=r.activityInfo.packageName;found.put(id,new Pack(id,pm.getApplicationLabel(r.activityInfo.applicationInfo).toString()));}List<Pack> packs=new ArrayList<>(found.values());packs.sort(Comparator.comparing(Pack::name,String.CASE_INSENSITIVE_ORDER));return packs;}
    public static Loaded load(Context context,String id)throws Exception{
        if(!validPackage(id)||id.isEmpty())throw new IllegalArgumentException("Choose an installed icon pack");Resources resources=context.getPackageManager().getResourcesForApplication(id);
        int xml=resources.getIdentifier("appfilter","xml",id);Map<String,String> mappings;
        if(xml!=0){try(XmlResourceParser parser=resources.getXml(xml)){mappings=parse(parser);}}
        else {int raw=resources.getIdentifier("appfilter","raw",id);try(InputStream input=raw!=0?resources.openRawResource(raw):resources.getAssets().open("appfilter.xml")){mappings=parse(input);}}
        if(mappings.isEmpty())throw new IllegalArgumentException("No static app icons in this pack");return new Loaded(id,resources,Map.copyOf(mappings));
    }
    public static Map<String,String> parse(InputStream input)throws Exception{XmlPullParser parser=Xml.newPullParser();parser.setInput(new LimitedInputStream(input),"UTF-8");return parse(parser);}
    private static Map<String,String> parse(XmlPullParser parser)throws Exception{
        Map<String,String> result=new LinkedHashMap<>();int events=0;
        for(int event=parser.getEventType();event!=XmlPullParser.END_DOCUMENT;event=parser.nextToken()){
            if(++events>100000)throw new IOException("Icon pack mapping is too large");if(event==XmlPullParser.DOCDECL)throw new IOException("Document declarations unsupported");
            if(event!=XmlPullParser.START_TAG||!"item".equals(parser.getName()))continue;
            String component=parser.getAttributeValue(null,"component"),drawable=parser.getAttributeValue(null,"drawable");
            if(component==null||drawable==null||!drawable.matches("[a-zA-Z0-9_]{1,160}"))continue;
            if(component.startsWith("ComponentInfo{")&&component.endsWith("}"))component=component.substring(14,component.length()-1);
            ComponentName name=ComponentName.unflattenFromString(component);if(name==null||!validPackage(name.getPackageName())||name.getClassName().length()>400)continue;
            result.put(name.flattenToString(),drawable);if(result.size()>20000)throw new IOException("Too many icon mappings");
        }return result;
    }
    private static final class LimitedInputStream extends FilterInputStream {int remaining=4*1024*1024;LimitedInputStream(InputStream stream){super(stream);}@Override public int read()throws IOException{if(--remaining<0)throw new IOException("Icon pack mapping too large");return in.read();}@Override public int read(byte[] bytes,int off,int len)throws IOException{if(remaining<=0)throw new IOException("Icon pack mapping too large");int n=in.read(bytes,off,Math.min(len,remaining));if(n>0)remaining-=n;return n;}}
}
