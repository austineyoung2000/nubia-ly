package com.elitedarkkaiser.nubialy.xposed;
import android.graphics.drawable.GradientDrawable;
import android.os.Looper;
import android.view.ViewGroup;
import de.robv.android.xposed.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;
/** Modify only the inspected circle tile's existing gradient geometry; preserve colors and tile models. */
final class QsShapeBackend {
    // Only verified cleanup may clear a restoration-only failure; apply/hook failures stay latched.
    private static boolean hardFailed;
    private static final String WIDE="com.zte.qs.tileimpl.QSTileViewWide",CIRCLE="com.zte.qs.tileimpl.QSTileViewCircle";
    private static final Map<GradientDrawable,State> states=new WeakHashMap<>();
    private static final List<XC_MethodHook.Unhook> hooks=new ArrayList<>();
    private static String failure="none";private static boolean active,writing;private static volatile boolean failed;private static long generation;private static int largeSelected,selected,captured,restored;
    private static final class State {final WeakReference<ViewGroup> owner;int shape;float radius;float[] corners;State(ViewGroup owner,GradientDrawable d){this.owner=new WeakReference<>(owner);shape=d.getShape();radius=d.getCornerRadius();corners=copy(d.getCornerRadii());}}
    private record Frame(long generation,State state){}
    private static float[] copy(float[] values){return values==null?null:values.clone();}
    static void begin(int shape){begin(shape,0);}
    static void begin(int shape,int largeShape){if(!(largeShape>=0&&largeShape<=4||largeShape>=100&&largeShape<=150))throw new IllegalArgumentException();largeSelected=largeShape;if(!(shape>=0&&shape<=4||shape>=100&&shape<=150))throw new IllegalArgumentException();if(!end())throw new IllegalStateException("Tile shape restoration incomplete");selected=shape;captured=restored=0;if(shape==0&&largeShape==0)return;
        XC_MethodHook callback=new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){if(!active||writing)return;State state=states.get(p.thisObject);if(state!=null)p.setObjectExtra("nubia-ly:qs-shape",new Frame(generation,state));}
            @Override protected void afterHookedMethod(MethodHookParam p){Object extra=p.getObjectExtra("nubia-ly:qs-shape");if(!(extra instanceof Frame frame)||!active||writing||frame.generation()!=generation||states.get(p.thisObject)!=frame.state())return;
                try{if(p.hasThrowable()||Looper.myLooper()!=Looper.getMainLooper())throw new IllegalStateException();GradientDrawable d=(GradientDrawable)p.thisObject;State s=frame.state();switch(((Method)p.method).getName()){case "setShape"->s.shape=d.getShape();case "setCornerRadius"->{s.radius=d.getCornerRadius();s.corners=null;}case "setCornerRadii"->{s.corners=copy(d.getCornerRadii());s.radius=d.getCornerRadius();}}paint(d,s);}
                catch(Throwable unavailable){failed=true;hardFailed=true;}
            }
        };
        try{hooks.add(XposedHelpers.findAndHookMethod(GradientDrawable.class,"setShape",int.class,callback));hooks.add(XposedHelpers.findAndHookMethod(GradientDrawable.class,"setCornerRadius",float.class,callback));hooks.add(XposedHelpers.findAndHookMethod(GradientDrawable.class,"setCornerRadii",float[].class,callback));hooks.add(XposedHelpers.findAndHookMethod(android.graphics.drawable.Drawable.class,"setBounds",int.class,int.class,int.class,int.class,callback));active=true;}
        catch(Throwable unavailable){failed=true;hardFailed=true;end();throw new IllegalStateException("Tile shape hooks unavailable",unavailable);}
    }
    static void track(ViewGroup root){if(!active||failed||!root.isAttachedToWindow()||(!root.getClass().getName().equals(CIRCLE)&&!root.getClass().getName().equals(WIDE)))return;
        if((root.getClass().getName().equals(WIDE)?largeSelected:selected)==0)return;
        try{Object value=XposedHelpers.callMethod(root,"getColorBackgroundDrawable");if(!(value instanceof GradientDrawable d))throw new IllegalStateException("Circle background type");
            for(GradientDrawable old:new ArrayList<>(states.keySet()))if(states.get(old).owner.get()==root&&old!=d&&!restore(old))return;
            State state=states.get(d);if(state!=null&&state.owner.get()!=root)throw new IllegalStateException("Shared background instance");
            if(state==null){state=new State(root,d);if(d.mutate()!=d)throw new IllegalStateException();states.put(d,state);captured++;}paint(d,state);
        }catch(Throwable unavailable){failed=true;hardFailed=true;}
    }
    private static void paint(GradientDrawable d,State state){writing=true;try{
        ViewGroup owner=state.owner.get();if(owner==null)return;int selected=owner.getClass().getName().equals(WIDE)?largeSelected:QsShapeBackend.selected;
        int width=d.getBounds().width(),height=d.getBounds().height();if(selected!=1&&(width<=0||height<=0))return;
        float factor=selected>=100?(selected-100)/100f:(selected==1?0f:selected==2?0.24f:0.5f);
        float radius=Math.min(width,height)*factor;
        float[] corners=selected==3?new float[]{radius,radius,0,0,radius,radius,0,0}:new float[]{radius,radius,radius,radius,radius,radius,radius,radius};
        d.setShape(GradientDrawable.RECTANGLE);d.setCornerRadii(corners);
    }finally{writing=false;}}
    private static boolean restore(GradientDrawable d){State s=states.get(d);if(s==null)return true;writing=true;try{
        d.setShape(s.shape);d.setCornerRadius(s.radius);if(s.corners!=null)d.setCornerRadii(copy(s.corners));
        if(d.getShape()!=s.shape||!Arrays.equals(d.getCornerRadii(),s.corners)||(s.corners==null&&d.getCornerRadius()!=s.radius))throw new IllegalStateException("Gradient geometry read-back mismatch");states.remove(d);restored++;return true;
    }catch(Throwable unavailable){failed=true;failure="restore "+unavailable.getClass().getSimpleName()+": "+String.valueOf(unavailable.getMessage());return false;}finally{writing=false;}}
    static void release(ViewGroup owner){for(GradientDrawable d:new ArrayList<>(states.keySet()))if(states.get(d).owner.get()==owner)restore(d);}
    static boolean end(){generation++;active=false;for(GradientDrawable d:new ArrayList<>(states.keySet()))restore(d);for(var h:new ArrayList<>(hooks))try{h.unhook();hooks.remove(h);}catch(Throwable unavailable){failed=true;hardFailed=true;}if(!hardFailed&&states.isEmpty()&&hooks.isEmpty())failed=false;return !failed&&states.isEmpty();}
    static boolean healthy(){return !failed;}
    private static String label(int s){return s>=100?"Custom "+(s-100)+"%":new String[]{"Stock","Square","Rounded square","Leaf","Pill"}[s];}
    static String status(){return "Large QS tiles: "+label(largeSelected)+"; Small QS tiles: "+label(selected)+"; captured: "+captured+"; restored: "+restored+"; remaining: "+states.size()+(failed?"; shape blocked: "+failure+".":".");}
}
