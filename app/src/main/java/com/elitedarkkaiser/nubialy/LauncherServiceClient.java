package com.elitedarkkaiser.nubialy;
import android.content.*;
import android.os.*;
import java.util.UUID;
import java.util.function.Consumer;
/** Signature-protected diagnostic. It cannot enable a style; the service reads the guard itself. */
public final class LauncherServiceClient {
    private final Context context;private final Handler main=new Handler(Looper.getMainLooper());private long generation;private Runnable timeout;private Consumer<String> callback;
    public LauncherServiceClient(Context context){this.context=context;}
    public void check(Consumer<String> result){request(0,0,result);}
    public void applyGrid(int rows,int columns,Consumer<String> result){if(!LauncherGridCatalogue.choice(rows,columns)){result.accept("Unsupported grid");return;}request(rows,columns,result);}
    private void request(int rows,int columns,Consumer<String> result){cancel();callback=result;long ticket=generation,started=SystemClock.elapsedRealtime();String nonce=UUID.randomUUID().toString();timeout=()->finish(ticket,"Launcher did not reply. Enable nubia-ly for Launcher / com.zte.mifavor.launcher in Vector, then reboot.");main.postDelayed(timeout,SystemUiGuardProtocol.DEADLINE_MS);
        Intent intent=new Intent(GuardBrokerPolicy.LAUNCHER_ACTION).setPackage(GuardBrokerPolicy.LAUNCHER_PACKAGE).addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);intent.putExtra("nonce",nonce);if(rows!=0){intent.putExtra("grid_rows",rows);intent.putExtra("grid_columns",columns);}intent.putExtra("request_deadline_ms",started+SystemUiGuardProtocol.DEADLINE_MS);
        try{context.sendOrderedBroadcast(intent,null,new BroadcastReceiver(){@Override public void onReceive(Context ignored,Intent response){Bundle extras=getResultExtras(false);if(getResultCode()!=SystemUiGuardProtocol.RESPONSE||extras==null||!SystemUiGuardProtocol.accepts(nonce,extras.getString("nonce"),BuildConfig.VERSION_CODE,extras.getInt("version",-1),SystemClock.elapsedRealtime()-started)){finish(ticket,"No valid launcher reply. Check Vector scope and reboot.");return;}String status=extras.getString("launcher_status","");finish(ticket,status==null||status.isBlank()||status.length()>800?"Launcher diagnostic rejected":status);}},main,0,null,null);}catch(RuntimeException unavailable){finish(ticket,"Launcher diagnostic unavailable. Check Vector scope and reboot.");}
    }
    private void finish(long ticket,String message){if(ticket!=generation||callback==null)return;Consumer<String> cb=callback;callback=null;if(timeout!=null)main.removeCallbacks(timeout);timeout=null;cb.accept(message);}
    public void cancel(){generation++;callback=null;if(timeout!=null)main.removeCallbacks(timeout);timeout=null;}
}
