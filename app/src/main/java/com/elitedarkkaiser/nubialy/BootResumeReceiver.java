package com.elitedarkkaiser.nubialy;
import android.content.*;
import android.os.*;
import java.util.concurrent.atomic.AtomicBoolean;
/** Starts no Activity or root process; resumes existing enabled authority after user unlock only. */
public final class BootResumeReceiver extends BroadcastReceiver {
 public static final String ACTION="com.elitedarkkaiser.nubialy.RESUME_AFTER_UNLOCK";
 private static final AtomicBoolean busy=new AtomicBoolean();
 @Override public void onReceive(Context context,Intent intent){
  if(intent==null||!(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())||ACTION.equals(intent.getAction()))||!CompatibilityPolicy.isSupportedDevice(Build.MODEL,Build.DEVICE)||!CompatibilityPolicy.isSupportedSdk(Build.VERSION.SDK_INT))return;
  UserManager user=context.getSystemService(UserManager.class);if(user==null||!user.isUserUnlocked()||android.os.Process.myUid()/100000!=0||!busy.compareAndSet(false,true))return;
  PendingResult pending=goAsync();Context app=context.getApplicationContext();Handler main=new Handler(Looper.getMainLooper());
  // File reads and private preference access never block a broadcast's main thread.
  new Thread(()->{boolean allowed=false;try{allowed=RecoveryStore.enabledAllowed(app,GuardReader.inspectEnabled());}catch(RuntimeException unavailable){}final boolean resume=allowed;main.post(()->{if(!resume){busy.set(false);pending.finish();return;}
   try{SystemUiGuardClient client=new SystemUiGuardClient(app);client.preview("clock_resume",result->{try{new FingerprintServiceClient(app).check(ignored->{});}finally{busy.set(false);pending.finish();}});}catch(RuntimeException unavailable){busy.set(false);pending.finish();}
  });},"nubia-ly-unlock-resume").start();
 }
}
