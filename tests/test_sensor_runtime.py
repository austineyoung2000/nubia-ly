"""Actual SensorService guard runtime: stale reads, expiry, revocation and busy admission."""
from pathlib import Path
import ast,tempfile,subprocess
root=Path(__file__).resolve().parents[1]
module=ast.parse((root/'tests/test_inventory_client.py').read_text())
sources=ast.literal_eval(next(n.value for n in module.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='sources' for t in n.targets)))
sources.pop('com/elitedarkkaiser/nubialy/InventoryTest.java')
sources['com/elitedarkkaiser/nubialy/FingerprintAnimationArtwork.java']='package com.elitedarkkaiser.nubialy;public class FingerprintAnimationArtwork {public record Frames(int id){}public static Frames load(int id){return id==0?null:new Frames(id);}}'
sources['com/elitedarkkaiser/nubialy/xposed/SensorAnimationBackend.java']='package com.elitedarkkaiser.nubialy.xposed;import com.elitedarkkaiser.nubialy.*;class SensorAnimationBackend {static int id,ends;static long deadline;static void install(ClassLoader l){}static boolean apply(int value,long until,FingerprintAnimationArtwork.Frames frames,int speed){id=value;deadline=until;return value==0||frames!=null&&frames.id()==value;}static void end(){ends++;}static String status(){return "animation";}}'

sources['android/os/Handler.java']='''package android.os;public class Handler {public Handler(Looper l){}public boolean post(Runnable r){r.run();return true;}public static java.util.Map<Runnable,Long> tasks=new java.util.IdentityHashMap<>();public boolean postDelayed(Runnable r,long d){tasks.put(r,d);return true;}public void removeCallbacks(Runnable r){tasks.remove(r);}}'''
sources['android/os/Bundle.java']=sources['android/os/Bundle.java'].replace('public void putString(', 'public void putBoolean(String k,boolean v){values.put(k,v);}public void putLong(String k,long v){values.put(k,v);}public void putString(')
sources['android/content/Intent.java']=sources['android/content/Intent.java'].replace('public static final int','public static final String ACTION_SCREEN_ON="on",ACTION_SCREEN_OFF="off";public String getAction(){return "";}public String getStringExtra(String k){return (String)extras.get(k);}public long getLongExtra(String k,long d){return (Long)extras.getOrDefault(k,d);}public static final int')
sources['android/content/BroadcastReceiver.java']=sources['android/content/BroadcastReceiver.java'].replace('private int code;', 'public boolean isOrderedBroadcast(){return true;}public PendingResult goAsync(){return new PendingResult();}public static class PendingResult {public void setResultExtras(android.os.Bundle b){}public void setResultCode(int c){}public void finish(){}}private int code;')
sources['android/content/IntentFilter.java']='package android.content;public class IntentFilter {public IntentFilter(String s){}public void addAction(String s){}}'
sources['android/net/Uri.java']='package android.net;public class Uri {public static Uri parse(String u){return new Uri();}}'
sources['android/content/Context.java']='''package android.content;public class Context {public static final int RECEIVER_EXPORTED=2,RECEIVER_NOT_EXPORTED=4;public java.util.function.Function<android.os.Bundle,android.os.Bundle> answer;public class Resolver {public android.os.Bundle call(android.net.Uri u,String method,String arg,android.os.Bundle b){if(!method.equals("inspect_fingerprint_enabled"))throw new AssertionError();return answer.apply(b);}}public Resolver getContentResolver(){return new Resolver();}public ClassLoader getClassLoader(){return getClass().getClassLoader();}public void registerReceiver(BroadcastReceiver r,IntentFilter f,String p,android.os.Handler h,int flags){}public void registerReceiver(BroadcastReceiver r,IntentFilter f,int flags){}public void sendOrderedBroadcast(Intent i,String permission,BroadcastReceiver receiver,android.os.Handler h,int code,String data,android.os.Bundle extras){}}'''
sources['com/elitedarkkaiser/nubialy/xposed/SensorFingerprintBackend.java']='''package com.elitedarkkaiser.nubialy.xposed;class SensorFingerprintBackend {static int applies,ends,refreshes;static long deadline;static boolean install(ClassLoader l){return true;}static boolean apply(int s,boolean h,long until){applies++;deadline=until;return true;}static void refresh(){refreshes++;}static boolean end(){ends++;return true;}static String status(){return "backend";}}'''
sources['com/elitedarkkaiser/nubialy/xposed/RuntimeTest.java']='''package com.elitedarkkaiser.nubialy.xposed;
import android.os.*;import android.content.*;import java.util.concurrent.*;import java.util.function.*;
public class RuntimeTest {
static void check(boolean b){if(!b)throw new AssertionError();}
static Bundle response(Bundle request,int status){Bundle b=new Bundle();b.putString("nonce",request.getString("nonce"));b.putInt("version",19);b.putInt("status",status);b.putString("permit_id","12345678-1234-4234-8234-123456789abc");b.putLong("deadline_ms",SystemClock.now+10000);b.putInt("fingerprint_style",188);b.putInt("fingerprint_animation",40);return b;}
static Object field(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
static void revoke(Object runtime)throws Exception{var m=runtime.getClass().getDeclaredMethod("revoke");m.setAccessible(true);m.invoke(runtime);}
static CountDownLatch request(Object runtime)throws Exception{var done=new CountDownLatch(1);var m=runtime.getClass().getDeclaredMethod("read",Consumer.class);m.setAccessible(true);m.invoke(runtime,(Consumer<String>)s->done.countDown());return done;}
static void await(CountDownLatch l)throws Exception{check(l.await(3,TimeUnit.SECONDS));}
public static void main(String[] args)throws Exception{var context=new Context();var ctor=SensorFingerprintRuntime.class.getDeclaredConstructor(Context.class);ctor.setAccessible(true);var runtime=ctor.newInstance(context);try{
context.answer=b->response(b,2);await(request(runtime));check(SensorFingerprintBackend.applies==1&&SensorAnimationBackend.id==40);check(Handler.tasks.get((Runnable)field(runtime,"poll"))==8000L);long deadline=SensorFingerprintBackend.deadline;Object expiry=field(runtime,"expiry");
context.answer=b->response(b,3);await(request(runtime));check(SensorFingerprintBackend.applies==1&&SensorFingerprintBackend.deadline==deadline&&field(runtime,"expiry")==expiry);
context.answer=b->response(b,1);int ends=SensorFingerprintBackend.ends;await(request(runtime));check(SensorFingerprintBackend.ends>ends);
for(int mode=0;mode<6;mode++){final int type=mode;context.answer=b->{Bundle r=response(b,2);if(type==0)r.putString("nonce","bad");if(type==1)r.putInt("version",18);if(type==2)r.putInt("fingerprint_style",189);if(type==3)r.putLong("deadline_ms",SystemClock.now+15001);if(type==4)SystemClock.now+=2001;if(type==5)r.putInt("fingerprint_animation",41);return r;};int applied=SensorFingerprintBackend.applies;await(request(runtime));check(SensorFingerprintBackend.applies==applied);}
context.answer=b->response(b,2);await(request(runtime));check(SensorFingerprintBackend.applies==2);
CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);context.answer=b->{entered.countDown();try{release.await();}catch(Exception e){throw new RuntimeException(e);}return response(b,2);};var done=request(runtime);await(entered);ends=SensorFingerprintBackend.ends;((Runnable)field(runtime,"expiry")).run();check(SensorFingerprintBackend.ends>ends);release.countDown();await(done);check(SensorFingerprintBackend.applies==2);
context.answer=b->{Bundle r=response(b,2);r.putInt("fingerprint_style",0);r.putInt("fingerprint_animation",0);return r;};await(request(runtime));check(!Handler.tasks.containsKey((Runnable)field(runtime,"poll")));
var off=runtime.getClass().getDeclaredField("screenOff");off.setAccessible(true);off.setBoolean(runtime,true);context.answer=b->{throw new AssertionError("Screen-off must not query broker");};await(request(runtime));check(!Handler.tasks.containsKey((Runnable)field(runtime,"poll")));off.setBoolean(runtime,false);
context.answer=b->response(b,2);await(request(runtime));check(Handler.tasks.get((Runnable)field(runtime,"poll"))==8000L);revoke(runtime);check(!Handler.tasks.containsKey((Runnable)field(runtime,"poll")));
System.out.println("Sensor runtime passed: idle/screen-off silence, event restart, eight-second renewal, cancellation; fresh authority, busy without renewal, blocked/replayed/version/size/latency rejection, independent expiry and stale-worker fencing");
}finally{((ExecutorService)field(runtime,"worker")).shutdownNow();}}}
'''
with tempfile.TemporaryDirectory(prefix='nubialy-sensor-runtime-') as temp:
    temp=Path(temp)
    for name,content in sources.items():
        p=temp/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content)
    names=['GuardBrokerPolicy','SystemUiGuardProtocol','PreviewPolicy','ClockEnabledPolicy']
    files=[str(p) for p in temp.rglob('*.java')]+[str(root/'app/src/main/java/com/elitedarkkaiser/nubialy'/(n+'.java')) for n in names]+[str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/xposed/SensorFingerprintRuntime.java')]
    files.append(str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/FingerprintStyles.java'))
    files.append(str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/FingerprintAnimations.java'))
    subprocess.run(['java','com.sun.tools.javac.Main','-d',str(temp/'classes'),*files],check=True)
    subprocess.run(['java','-cp',str(temp/'classes'),'com.elitedarkkaiser.nubialy.xposed.RuntimeTest'],check=True)
