package com.elitedarkkaiser.nubialy.xposed;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import com.elitedarkkaiser.nubialy.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Independent launcher authority. Polls only while a launcher Activity is resumed. */
final class LauncherRuntime {
    private final Application context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Set<Activity> resumed=Collections.newSetFromMap(new WeakHashMap<>());
    private boolean ready,busy,screenOff,needsPolling;
    private long generation;
    private Runnable expiry;
    private String authority="checking APK";
    private final Runnable poll=new Runnable(){@Override public void run(){if(resumed.isEmpty()||screenOff)return;read(ignored->{if(needsPolling&&!resumed.isEmpty()&&!screenOff)main.postDelayed(this,3000);});}};
    private LauncherRuntime(Application app){context=app;}
    static void install(Application app){new LauncherRuntime(app).start();}
    private void start(){
        context.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            public void onActivityResumed(Activity a){resumed.add(a);LauncherBackend.observe(a.getWindow().getDecorView());main.removeCallbacks(poll);if(!screenOff)main.post(poll);}
            public void onActivityPaused(Activity a){resumed.remove(a);if(resumed.isEmpty()){main.removeCallbacks(poll);revoke();}}
            public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){resumed.remove(a);}
        });
        IntentFilter screen=new IntentFilter(Intent.ACTION_SCREEN_OFF);screen.addAction(Intent.ACTION_SCREEN_ON);
        context.registerReceiver(new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){screenOff=Intent.ACTION_SCREEN_OFF.equals(i.getAction());main.removeCallbacks(poll);if(screenOff)revoke();else if(!resumed.isEmpty())main.post(poll);}},screen,Context.RECEIVER_NOT_EXPORTED);
        context.registerReceiver(new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){
            String nonce=i.getStringExtra("nonce");
            if(!isOrderedBroadcast()||!PreviewPolicy.uuid(nonce)||!SystemUiGuardProtocol.admitted(i.getLongExtra("request_deadline_ms",0),SystemClock.elapsedRealtime()))return;
            PendingResult result=goAsync();boolean[] answered={false};Runnable[] timeout={null};
            Consumer<String> finish=message->{if(answered[0])return;answered[0]=true;main.removeCallbacks(timeout[0]);try{Bundle b=new Bundle();b.putString("nonce",nonce);b.putInt("version",BuildConfig.VERSION_CODE);b.putString("launcher_status",message);result.setResultExtras(b);result.setResultCode(SystemUiGuardProtocol.RESPONSE);}finally{result.finish();}};
            timeout[0]=()->{revoke();finish.accept("Launcher guard read timed out. "+status());};main.postDelayed(timeout[0],2500);
            read(finish);
        }},new IntentFilter(GuardBrokerPolicy.LAUNCHER_ACTION),SystemUiGuardProtocol.PERMISSION,null,Context.RECEIVER_EXPORTED);
        worker.execute(()->{boolean match=LauncherCompatibility.supported(context);main.post(()->{
            ready=match&&LauncherBackend.install(context.getClassLoader());authority=ready?"APK verified":"unsupported APK or hooks";
            if(ready&&!resumed.isEmpty()&&!screenOff){for(Activity a:resumed)LauncherBackend.observe(a.getWindow().getDecorView());main.removeCallbacks(poll);main.post(poll);}
        });});
    }
    private String status(){return authority+". "+LauncherBackend.status();}
    private void revoke(){needsPolling=false;generation++;if(expiry!=null)main.removeCallbacks(expiry);expiry=null;LauncherBackend.end();}
    private void read(Consumer<String> reply){
        if(!ready||busy){if(reply!=null)reply.accept(status());return;}
        busy=true;long ticket=generation,started=SystemClock.elapsedRealtime();String nonce=UUID.randomUUID().toString();
        worker.execute(()->{
            Bundle answer=null;try{Bundle request=new Bundle();request.putString("nonce",nonce);request.putInt("version",BuildConfig.VERSION_CODE);answer=context.getContentResolver().call(Uri.parse("content://"+GuardBrokerPolicy.AUTHORITY),GuardBrokerPolicy.LAUNCHER_METHOD,null,request);}catch(Throwable unavailable){}
            Bundle result=answer;main.post(()->{
                busy=false;if(ticket!=generation){if(reply!=null)reply.accept(status());return;}
                try{
                    long now=SystemClock.elapsedRealtime();boolean valid=result!=null&&now-started<=2000&&SystemUiGuardProtocol.accepts(nonce,result.getString("nonce"),BuildConfig.VERSION_CODE,result.getInt("version",-1),now-started);
                    if(valid&&result.getInt("status",-1)==GuardBrokerPolicy.BUSY){authority="guard busy; current deadline retained";needsPolling=true;return;}
                    long until=valid?result.getLong("deadline_ms",0):0;
                    LauncherStyle selected=new LauncherStyle(valid?result.getInt("launcher_scale",-1):-1,valid&&result.getBoolean("launcher_labels"));
                    valid=valid&&result.getInt("status",-1)==GuardBrokerPolicy.READABLE&&PreviewPolicy.uuid(result.getString("permit_id"))&&until>now&&until-now<=ClockEnabledPolicy.HEALTH_MS&&selected.valid();
                    if(!valid||screenOff||resumed.isEmpty()){authority=valid?"waiting for launcher foreground":"guard blocked";revoke();}
                    else{authority="guard readable";needsPolling=selected.custom();LauncherBackend.apply(selected,until);if(expiry!=null)main.removeCallbacks(expiry);long epoch=generation;expiry=()->{if(epoch==generation){authority="guard expired";revoke();}};main.postDelayed(expiry,until-now);}
                }catch(Throwable unavailable){authority="guard unavailable";revoke();}
                finally{if(reply!=null)reply.accept(status());}
            });
        });
    }
}
