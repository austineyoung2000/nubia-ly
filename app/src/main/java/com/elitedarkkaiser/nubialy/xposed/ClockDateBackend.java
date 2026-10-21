package com.elitedarkkaiser.nubialy.xposed;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.widget.TextView;
import com.elitedarkkaiser.nubialy.ClockDatePolicy;
import de.robv.android.xposed.*;
import java.util.*;
/** Append to the inspected stock formatter; retain its spans, locale/timezone and tick lifecycle. */
final class ClockDateBackend {
    // Only verified cleanup may clear a restoration-only failure; apply/hook failures stay latched.
    private static boolean hardFailed;
    private static final Map<TextView,State> states=new WeakHashMap<>();
    private static final List<XC_MethodHook.Unhook> hooks=new ArrayList<>();
    private static long generation;
    private record Frame(long generation,State state){}
    private static int mode, captured, restored;private static boolean active,writing;private static volatile boolean failed;private static Class<?> installed;
    private static final class State {CharSequence stockText,appliedText,stockDescription,appliedDescription;String date;Locale locale;int era,year,day;}
    static void begin(int selected){if(!ClockDatePolicy.valid(selected))throw new IllegalArgumentException();if(!end())throw new IllegalStateException("Date restoration incomplete");mode=selected;captured=restored=0;active=ClockDatePolicy.base(mode)!=0;}
    private static void install(Class<?> clock)throws Exception {
        if(installed!=null){if(installed!=clock)throw new IllegalStateException();return;}
        if(clock.getSuperclass()!=TextView.class||clock.getDeclaredField("mCalendar").getType()!=Calendar.class||clock.getDeclaredField("mLocale").getType()!=Locale.class
            ||clock.getDeclaredMethod("getSmallTime").getReturnType()!=CharSequence.class)throw new IllegalStateException();
        hooks.add(XposedBridge.hookMethod(clock.getDeclaredMethod("getSmallTime"),new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                if(active&&!failed&&p.thisObject instanceof TextView view&&states.containsKey(view))p.setObjectExtra("nubia-ly:date-frame",new Frame(generation,states.get(view)));
            }
            @Override protected void afterHookedMethod(MethodHookParam p){
                Object frame=p.getObjectExtra("nubia-ly:date-frame");
                if(!active||failed||!(p.thisObject instanceof TextView view)||!(frame instanceof Frame f)||f.generation()!=generation||states.get(view)!=f.state())return;
                try {
                    if(Looper.myLooper()!=Looper.getMainLooper()||p.hasThrowable()||!(p.getResult() instanceof CharSequence text))throw new IllegalStateException();
                    State s=states.get(view);s.stockText=text;s.appliedText=compose(text,date(view,s));p.setResult(s.appliedText);
                }catch(Throwable unavailable){failed=true;hardFailed=true;}
            }
        }));installed=clock;
    }
    private static CharSequence compose(CharSequence stock,String date){
        String separator=ClockDatePolicy.separatorText(mode);
        // Appending the original CharSequence retains stock time spans in either position.
        return ClockDatePolicy.position(mode)==0
            ?new SpannableStringBuilder(stock).append(separator).append(date)
            :new SpannableStringBuilder(date).append(separator).append(stock);
    }
    private static String date(TextView view,State s){
        Calendar calendar=(Calendar)XposedHelpers.getObjectField(view,"mCalendar");Locale locale=(Locale)XposedHelpers.getObjectField(view,"mLocale");if(locale==null)locale=Locale.getDefault();
        int era=calendar.get(Calendar.ERA),year=calendar.get(Calendar.YEAR),day=calendar.get(Calendar.DAY_OF_YEAR);
        if(s.date==null||s.era!=era||s.year!=year||s.day!=day||!locale.equals(s.locale)){
            s.date=ClockDatePolicy.format(mode,calendar,locale);s.era=era;s.year=year;s.day=day;s.locale=locale;
        }return s.date;
    }
    static void track(TextView view){
        if(!active||writing||states.containsKey(view))return;
        if(failed||Looper.myLooper()!=Looper.getMainLooper())throw new IllegalStateException("Clock date unavailable");
        try{install(view.getClass());State s=new State();s.stockText=view.getText();s.stockDescription=view.getContentDescription();states.put(view,s);captured++;writing=true;XposedHelpers.callMethod(view,"updateClock");}
        catch(Throwable unavailable){failed=true;hardFailed=true;throw new IllegalStateException("Clock date unavailable",unavailable);}finally{writing=false;}
    }
    static void afterUpdate(TextView view){
        if(!active||failed||Looper.myLooper()!=Looper.getMainLooper())return;State s=states.get(view);if(s==null)return;
        try {
            CharSequence description=view.getContentDescription();if(description==null)return;
            if(!TextUtils.equals(description,s.appliedDescription))s.stockDescription=description;
            s.appliedDescription=compose(s.stockDescription,date(view,s));view.setContentDescription(s.appliedDescription);
        }catch(Throwable unavailable){failed=true;hardFailed=true;}
    }
    static boolean restore(TextView view){
        State s=states.get(view);if(s==null)return true;writing=true;
        try {
            states.remove(view);
            if(s.appliedText!=null&&TextUtils.equals(view.getText(),s.appliedText)){view.setText(s.stockText);if(!TextUtils.equals(view.getText(),s.stockText))throw new IllegalStateException();}
            if(s.appliedDescription!=null&&TextUtils.equals(view.getContentDescription(),s.appliedDescription)){view.setContentDescription(s.stockDescription);if(!TextUtils.equals(view.getContentDescription(),s.stockDescription))throw new IllegalStateException();}
            XposedHelpers.callMethod(view,"updateClock");restored++;return true;
        }catch(Throwable unavailable){states.put(view,s);failed=true;return false;}finally{writing=false;}
    }
    static boolean end(){generation++;active=false;for(TextView v:new ArrayList<>(states.keySet()))restore(v);for(var h:new ArrayList<>(hooks))try{h.unhook();hooks.remove(h);}catch(Throwable unavailable){failed=true;hardFailed=true;}installed=null;if(!hardFailed&&states.isEmpty()&&hooks.isEmpty())failed=false;return !failed&&states.isEmpty();}
    static boolean healthy(){return !failed;}
    static String status(){return (failed?"Clock date blocked; check remaining state and disable nubia-ly in Vector/reboot if incomplete. ":"")+"Clock date: "+ClockDatePolicy.LABELS[ClockDatePolicy.base(mode)]+"; captured: "+captured+"; restored: "+restored+"; date remaining: "+states.size()+".";}
}
