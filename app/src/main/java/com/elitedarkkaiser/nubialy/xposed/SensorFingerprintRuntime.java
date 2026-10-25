package com.elitedarkkaiser.nubialy.xposed;

import android.content.*;
import android.net.Uri;
import android.os.*;
import com.elitedarkkaiser.nubialy.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Independent bounded guard reads in SensorService; no System UI authority is relayed. */
final class SensorFingerprintRuntime {
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private String authority="not checked";
    private String status(){String text="Fingerprint guard: "+authority+". "+SensorAnimationBackend.status()+" "+SensorFingerprintBackend.status();return text.length()>790?text.substring(0,789)+"…":text;}
    private boolean busy;private long generation;private Runnable expiry;
    private SensorFingerprintRuntime(Context context){this.context=context;}
    static void install(Context context){
        if(!SensorFingerprintBackend.install(context.getClassLoader()))return;
        SensorAnimationBackend.install(context.getClassLoader());
        SensorFingerprintRuntime runtime=new SensorFingerprintRuntime(context);
        IntentFilter filter=new IntentFilter(GuardBrokerPolicy.FINGERPRINT_ACTION);
        context.registerReceiver(new BroadcastReceiver(){@Override public void onReceive(Context c,Intent intent){
            if(!isOrderedBroadcast())return;
            String nonce=intent.getStringExtra("nonce");long requestDeadline=intent.getLongExtra("request_deadline_ms",0);
            if(!GuardBrokerPolicy.FINGERPRINT_ACTION.equals(intent.getAction())||!PreviewPolicy.uuid(nonce)||!SystemUiGuardProtocol.admitted(requestDeadline,SystemClock.elapsedRealtime()))return;
            PendingResult pending=goAsync();boolean[] answered={false};Runnable[] timeout={null};
            Consumer<String> finish=message->{if(answered[0])return;answered[0]=true;if(timeout[0]!=null)runtime.main.removeCallbacks(timeout[0]);try{Bundle reply=new Bundle();reply.putString("nonce",nonce);reply.putInt("version",BuildConfig.VERSION_CODE);reply.putString("fingerprint_status",message);pending.setResultExtras(reply);pending.setResultCode(SystemUiGuardProtocol.RESPONSE);}finally{pending.finish();}};
            timeout[0]=()->{runtime.authority="diagnostic read timed out";runtime.revoke();finish.accept(runtime.status());};runtime.main.postDelayed(timeout[0],2500);runtime.read(finish);
        }},filter,SystemUiGuardProtocol.PERMISSION,null,Context.RECEIVER_EXPORTED);
        IntentFilter screen=new IntentFilter(Intent.ACTION_SCREEN_ON);screen.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(new BroadcastReceiver(){@Override public void onReceive(Context c,Intent intent){SensorFingerprintBackend.refresh();if(Intent.ACTION_SCREEN_ON.equals(intent.getAction()))runtime.read(null);}},screen,Context.RECEIVER_NOT_EXPORTED);
        runtime.poll();
    }
    private void poll(){read(null);main.postDelayed(this::poll,3000);}
    private void revoke(){generation++;if(expiry!=null)main.removeCallbacks(expiry);expiry=null;SensorAnimationBackend.end();SensorFingerprintBackend.end();}
    private void read(Consumer<String> reply){
        if(busy){if(reply!=null)reply.accept("Fingerprint guard check pending. "+status());return;}
        busy=true;long ticket=generation,started=SystemClock.elapsedRealtime();String nonce=UUID.randomUUID().toString();
        try{worker.execute(()->{
            Bundle response=null;try{Bundle request=new Bundle();request.putString("nonce",nonce);request.putInt("version",BuildConfig.VERSION_CODE);response=context.getContentResolver().call(Uri.parse("content://"+GuardBrokerPolicy.AUTHORITY),GuardBrokerPolicy.FINGERPRINT_METHOD,null,request);}catch(Throwable unavailable){}
            Bundle result=response;FingerprintAnimationArtwork.Frames prepared=null;
            try{if(result!=null&&result.getInt("status",-1)==GuardBrokerPolicy.READABLE&&SystemUiGuardProtocol.accepts(nonce,result.getString("nonce"),BuildConfig.VERSION_CODE,result.getInt("version",-1),SystemClock.elapsedRealtime()-started)&&FingerprintAnimations.valid(result.getInt("fingerprint_animation",-1)))prepared=FingerprintAnimationArtwork.load(result.getInt("fingerprint_animation",-1));}catch(Throwable unavailable){}
            FingerprintAnimationArtwork.Frames frames=prepared;main.post(()->{busy=false;if(ticket!=generation){if(reply!=null)reply.accept(status());return;}try{
                long now=SystemClock.elapsedRealtime();boolean valid=result!=null&&now-started<=2000&&SystemUiGuardProtocol.accepts(nonce,result.getString("nonce"),BuildConfig.VERSION_CODE,result.getInt("version",-1),now-started)&&result.getInt("status",-1)==GuardBrokerPolicy.READABLE&&PreviewPolicy.uuid(result.getString("permit_id"));
                if(result!=null&&SystemUiGuardProtocol.accepts(nonce,result.getString("nonce"),BuildConfig.VERSION_CODE,result.getInt("version",-1),now-started)&&result.getInt("status",-1)==GuardBrokerPolicy.BUSY){authority="busy; existing deadline retained";SensorFingerprintBackend.refresh();return;}
                long until=valid?result.getLong("deadline_ms",0):0;int style=valid?result.getInt("fingerprint_style",-1):-1;int animation=valid?result.getInt("fingerprint_animation",-1):-1;
                valid=valid&&until>now&&until-now<=ClockEnabledPolicy.HEALTH_MS&&com.elitedarkkaiser.nubialy.FingerprintStyles.valid(style)&&FingerprintAnimations.valid(animation)&&FingerprintAnimations.validSpeed(result.getInt("fingerprint_speed",100));
                if(!valid){authority="blocked or stale";revoke();}else if((!SensorFingerprintBackend.apply(style,result.getBoolean("fingerprint_hidden",false),until)||!SensorAnimationBackend.apply(animation,until,frames,result.getInt("fingerprint_speed",100)))){authority="backend unavailable";revoke();}else{authority="readable";
                    if(expiry!=null)main.removeCallbacks(expiry);long epoch=generation;expiry=()->{if(epoch==generation){authority="expired";revoke();}};main.postDelayed(expiry,until-now);
                }
            }catch(Throwable unavailable){revoke();}finally{if(reply!=null)reply.accept(status());}});
        });}catch(Throwable unavailable){busy=false;revoke();if(reply!=null)reply.accept(status());}
    }
}
