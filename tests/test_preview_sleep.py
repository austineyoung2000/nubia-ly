"""Persistent display sleep retains authority, never renews it, and still stops on expiry."""
from pathlib import Path
import tempfile, subprocess
root=Path(__file__).resolve().parents[1]
pkg='com/elitedarkkaiser/nubialy/'
sources={
'android/os/Looper.java':'package android.os; public class Looper {public static Looper getMainLooper(){return new Looper();}}',
'android/os/SystemClock.java':'package android.os; public class SystemClock {public static long time=1000;public static long elapsedRealtime(){return time;}}',
'android/os/Handler.java':'''package android.os;public class Handler {public Handler(Looper l){}public boolean post(Runnable r){r.run();return true;}public boolean postDelayed(Runnable r,long delay){return true;}public void removeCallbacks(Runnable r){}}''',
'android/os/Bundle.java':'''package android.os;public class Bundle {java.util.Map<String,Object> values=new java.util.HashMap<>();public void putString(String k,String v){values.put(k,v);}public String getString(String k){return (String)values.get(k);}public String getString(String k,String d){return (String)values.getOrDefault(k,d);}public void putInt(String k,int v){values.put(k,v);}public int getInt(String k,int d){return (Integer)values.getOrDefault(k,d);}public void putLong(String k,long v){values.put(k,v);}public long getLong(String k,long d){return (Long)values.getOrDefault(k,d);}public void putBoolean(String k,boolean v){values.put(k,v);}public boolean getBoolean(String k,boolean d){return (Boolean)values.getOrDefault(k,d);}public void putStringArrayList(String k,java.util.ArrayList<String> v){values.put(k,v);}}''',
'android/net/Uri.java':'package android.net;public class Uri {public static Uri parse(String s){return new Uri();}}',
'android/content/Context.java':'''package android.content;public class Context {public Resolver getContentResolver(){return new Resolver();}public static class Resolver {public android.os.Bundle call(android.net.Uri u,String m,String a,android.os.Bundle b){throw new AssertionError("Sleep must not request authority");}}}''',
pkg+'BuildConfig.java':'package com.elitedarkkaiser.nubialy;public class BuildConfig {public static final int VERSION_CODE=32;}',
pkg+'xposed/ClockColorBackend.java':'''package com.elitedarkkaiser.nubialy.xposed;class ClockColorBackend {static int ends;static boolean beginAllowed=true,retryCleanup;static boolean begin(Object... a){return beginAllowed;}static boolean healthy(){return true;}static boolean refreshHealth(long d){return true;}static void end(){ends++;}static int color(){return 0;}static boolean cleanupRetryable(){return retryCleanup;}static String startupFailure(){return "fixture failure";}static String status(){return "style";}}''',
pkg+'xposed/ClockCaptureBackend.java':'package com.elitedarkkaiser.nubialy.xposed;class ClockCaptureBackend {static boolean begin(){return true;}static void end(){}static String status(){return "capture";}}',
pkg+'xposed/StatusBarBackend.java':'package com.elitedarkkaiser.nubialy.xposed;class StatusBarBackend {static java.util.ArrayList<String> inventory(){return new java.util.ArrayList<>();}}',
pkg+'xposed/SurfaceBackend.java':'package com.elitedarkkaiser.nubialy.xposed;class SurfaceBackend {static int refreshes;static boolean valid=true;static void refresh(){refreshes++;}static boolean healthy(){return valid;}}',
pkg+'xposed/SleepTest.java':'''package com.elitedarkkaiser.nubialy.xposed;
import com.elitedarkkaiser.nubialy.*;import android.os.*;
public class SleepTest {
static void check(boolean b){if(!b)throw new AssertionError();}
static void set(Object o,String n,Object v)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
static PreviewLifecycle prime(PreviewRuntime r,boolean enabled)throws Exception{var f=PreviewRuntime.class.getDeclaredField("session");f.setAccessible(true);var session=(PreviewLifecycle)f.get(r);long t=session.begin(enabled);check(session.activate(t,new PreviewPolicy.Permit("12345678-1234-4234-8234-123456789abc",10000),1000));set(r,"enabledMode",enabled);set(r,"visual",true);return session;}
public static void main(String[] a)throws Exception{
var r=new PreviewRuntime(new android.content.Context());var s=prime(r,true);long gen=s.generation(),deadline=s.deadline();var tick=(Runnable)()->{};var expiry=(Runnable)()->{};set(r,"tick",tick);set(r,"expiry",expiry);
r.request("screen_off",0,b->check(b.getString("phase").equals("ACTIVE")));check(s.generation()==gen&&s.deadline()==deadline&&ClockColorBackend.ends==0&&SurfaceBackend.refreshes==1);
r.request("clock_resume",2000,b->check(b.getString("phase").equals("ACTIVE")));check(ClockColorBackend.ends==0&&s.deadline()==deadline&&SurfaceBackend.refreshes==2);
SystemClock.time=10000;r.request("screen_off",0,b->check(!b.getString("phase").equals("ACTIVE")));check(ClockColorBackend.ends>0);
SystemClock.time=1000;prime(r,false);r.request("screen_off",0,b->check(b.getString("phase").equals("STOPPED")));
prime(r,true);SurfaceBackend.valid=false;r.request("screen_off",0,b->check(b.getString("phase").equals("STOPPED")));
SurfaceBackend.valid=true;prime(r,true);r.request("stop",0,b->{});r.request("clock_resume",2000,b->check(b.getString("phase").equals("STOPPED")));
System.out.println("Display sleep: enabled authority retained without renewal; expiry, temporary stop, failure and explicit stop preserved");}}
'''
}
with tempfile.TemporaryDirectory() as folder:
    folder=Path(folder)
    for name,content in sources.items():
        p=folder/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content)
    names=['PreviewLifecycle','PreviewPolicy','ClockEnabledPolicy','ClockColorPolicy','ClockStylePolicy','ClockDatePolicy','SystemUiGuardProtocol','GuardBrokerPolicy','StatusBarStyle','SurfaceStyle','NotificationControls','LockClockFonts']
    files=[str(p) for p in folder.rglob('*.java')]+[str(root/'app/src/main/java'/pkg/(n+'.java')) for n in names]+[str(root/'app/src/main/java'/pkg/'xposed/PreviewRuntime.java')]
    files.append(str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/FingerprintStyles.java'))
    files.append(str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/FingerprintAnimations.java'))
    subprocess.run(['java','com.sun.tools.javac.Main','-d',str(folder/'classes'),*files],check=True)
    subprocess.run(['java','-cp',str(folder/'classes'),'com.elitedarkkaiser.nubialy.xposed.SleepTest'],check=True)
