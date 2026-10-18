"""Exercise production runtime contention, bounded resume, denial and cancellation."""
from pathlib import Path
import tempfile, subprocess
root=Path(__file__).resolve().parents[1]
pkg='com/elitedarkkaiser/nubialy/'
import ast
fixture=ast.parse((root/'tests/test_preview_sleep.py').read_text())
sources=eval(compile(ast.Expression(next(n.value for n in fixture.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='sources' for t in n.targets))),'<fixture>','eval'),{'pkg':pkg})
del sources[pkg+'xposed/SleepTest.java']
sources['android/content/Context.java']='package android.content;public class Context {public static java.util.function.Function<android.os.Bundle,android.os.Bundle> answer;public Resolver getContentResolver(){return new Resolver();}public static class Resolver {public android.os.Bundle call(android.net.Uri u,String m,String a,android.os.Bundle b){return answer.apply(b);}}}'
sources[pkg+'xposed/RecoveryTest.java']=r'''package com.elitedarkkaiser.nubialy.xposed;
import com.elitedarkkaiser.nubialy.*;import android.os.*;import android.content.Context;import java.util.concurrent.*;
public class RecoveryTest {
static final String ID="12345678-1234-4234-8234-123456789abc";
static void check(boolean b){if(!b)throw new AssertionError();}
static Object get(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
static Bundle response(Bundle q,int status){var b=new Bundle();b.putString("nonce",q.getString("nonce"));b.putInt("version",32);b.putInt("status",status);b.putString("permit_id",ID);b.putLong("deadline_ms",SystemClock.time+15000);return b;}
static Bundle request(PreviewRuntime r,String op)throws Exception{var latch=new CountDownLatch(1);Bundle[] out={null};r.request(op,SystemClock.time+8000,b->{out[0]=b;latch.countDown();});check(latch.await(3,TimeUnit.SECONDS));return out[0];}
static void flush(PreviewRuntime r)throws Exception{((ExecutorService)get(r,"worker")).submit(()->{}).get(3,TimeUnit.SECONDS);}
static void run(PreviewRuntime r,String field)throws Exception{var task=(Runnable)get(r,field);check(task!=null);task.run();flush(r);}
public static void main(String[] args)throws Exception{
var r=new PreviewRuntime(new Context());try{
Context.answer=q->response(q,GuardBrokerPolicy.READABLE);check(request(r,"clock_enable").getString("phase").equals("ACTIVE"));
var session=(PreviewLifecycle)get(r,"session");long deadline=session.deadline();int ends=ClockColorBackend.ends;
Context.answer=q->response(q,GuardBrokerPolicy.BUSY);check(request(r,"status").getString("phase").equals("ACTIVE"));check(session.deadline()==deadline&&ClockColorBackend.ends==ends);
Context.answer=q->null;check(request(r,"status").getString("phase").equals("ACTIVE"));check(session.deadline()==deadline);
// An actual overlapping diagnostic must not revoke the active permit.
var entered=new CountDownLatch(1);var release=new CountDownLatch(1);Context.answer=q->{entered.countDown();try{release.await(3,TimeUnit.SECONDS);}catch(Exception e){throw new RuntimeException(e);}return response(q,GuardBrokerPolicy.READABLE);};
r.request("status",SystemClock.time+8000,b->{});check(entered.await(3,TimeUnit.SECONDS));check(request(r,"status").getString("phase").equals("ACTIVE"));release.countDown();flush(r);
SystemClock.time=session.deadline();run(r,"expiry");check(session.state(SystemClock.time)!=PreviewLifecycle.Phase.ACTIVE);check(get(r,"recovery")!=null);
Context.answer=q->response(q,GuardBrokerPolicy.READABLE);run(r,"recovery");check(session.state(SystemClock.time)==PreviewLifecycle.Phase.ACTIVE&&session.deadline()>deadline);
// Explicit denial cannot trigger recovery.
Context.answer=q->response(q,GuardBrokerPolicy.BLOCKED);check(request(r,"status").getString("phase").equals("STOPPED"));check(get(r,"recovery")==null);
Context.answer=q->response(q,GuardBrokerPolicy.READABLE);request(r,"clock_enable");Context.answer=q->{var b=response(q,GuardBrokerPolicy.READABLE);b.putString("nonce","wrong");return b;};check(request(r,"status").getString("phase").equals("STOPPED"));check(get(r,"recovery")==null);
// Recovery is bounded and every attempt requests fresh authority.
Context.answer=q->response(q,GuardBrokerPolicy.READABLE);request(r,"clock_enable");SystemClock.time=session.deadline();run(r,"expiry");Context.answer=q->null;run(r,"recovery");run(r,"recovery");run(r,"recovery");check(get(r,"recovery")==null&&session.state(SystemClock.time)!=PreviewLifecycle.Phase.ACTIVE);
Context.answer=q->response(q,GuardBrokerPolicy.READABLE);request(r,"clock_enable");SystemClock.time=session.deadline();run(r,"expiry");var late=(Runnable)get(r,"recovery");request(r,"stop");late.run();flush(r);check(session.state(SystemClock.time)==PreviewLifecycle.Phase.STOPPED&&get(r,"recovery")==null);check(request(r,"clock_resume").getString("phase").equals("STOPPED"));
System.out.println("Runtime recovery: contention retains deadline; fresh resume works; retries bounded; denial and explicit stop stay stopped");
}finally{((ExecutorService)get(r,"worker")).shutdownNow();}}
}'''

with tempfile.TemporaryDirectory() as folder:
    folder=Path(folder)
    for name,content in sources.items():
        p=folder/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content)
    names=['PreviewLifecycle','PreviewPolicy','ClockEnabledPolicy','ClockColorPolicy','ClockStylePolicy','ClockDatePolicy','SystemUiGuardProtocol','GuardBrokerPolicy','StatusBarStyle','SurfaceStyle','NotificationControls','LockClockFonts']
    files=[str(p) for p in folder.rglob('*.java')]+[str(root/'app/src/main/java'/pkg/(n+'.java')) for n in names]+[str(root/'app/src/main/java'/pkg/'xposed/PreviewRuntime.java')]
    files.append(str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/FingerprintStyles.java'))
    files.append(str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/FingerprintAnimations.java'))
    subprocess.run(['java','com.sun.tools.javac.Main','-d',str(folder/'classes'),*files],check=True)
    subprocess.run(['java','-cp',str(folder/'classes'),'com.elitedarkkaiser.nubialy.xposed.RecoveryTest'],check=True)
