"""Production root-read transport: one deadline must cover process exit and output EOF."""
from pathlib import Path
import tempfile,subprocess
root=Path(__file__).resolve().parents[1]
source='''package com.elitedarkkaiser.nubialy;
import java.io.*;import java.util.concurrent.*;
public final class GuardRootProcessTest {
static void check(boolean value){if(!value)throw new AssertionError();}
static final class Blocked extends InputStream {boolean closed;public synchronized int read(){while(!closed)try{wait();}catch(InterruptedException ignored){}return -1;}public synchronized void close(){closed=true;notifyAll();}}
static final class Exited extends Process {final Blocked pipe=new Blocked();public OutputStream getOutputStream(){return new ByteArrayOutputStream();}public InputStream getInputStream(){return pipe;}public InputStream getErrorStream(){return InputStream.nullInputStream();}public int waitFor(){return 0;}public boolean waitFor(long t,TimeUnit u){return true;}public int exitValue(){return 0;}public boolean isAlive(){return false;}public void destroy(){}}
public static void main(String[]args)throws Exception{
var normal=new ProcessBuilder("sh","-c","printf guard-receipt").redirectErrorStream(true).start();var good=GuardRootProcess.capture(normal,2000,100);check(good.exitCode()==0&&new String(good.output()).equals("guard-receipt"));
var refused=new ProcessBuilder("sh","-c","exit 7").redirectErrorStream(true).start();check(GuardRootProcess.capture(refused,2000,100).exitCode()==7);
var oversized=new ProcessBuilder("sh","-c","printf 123456").redirectErrorStream(true).start();try{GuardRootProcess.capture(oversized,2000,3);throw new AssertionError();}catch(ExecutionException expected){check(expected.getCause() instanceof IOException);}
var waiting=new ProcessBuilder("sh","-c","exec sleep 10").redirectErrorStream(true).start();long start=System.nanoTime();try{GuardRootProcess.capture(waiting,150,100);throw new AssertionError();}catch(TimeoutException expected){}check(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)<2000);check(waiting.waitFor(2,TimeUnit.SECONDS));
var heldPipe=new Exited();start=System.nanoTime();try{GuardRootProcess.capture(heldPipe,150,100);throw new AssertionError();}catch(TimeoutException expected){}check(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)<2000);
var interrupted=new ProcessBuilder("sh","-c","exec sleep 10").redirectErrorStream(true).start();Thread.currentThread().interrupt();try{GuardRootProcess.capture(interrupted,2000,100);throw new AssertionError();}catch(InterruptedException expected){check(Thread.currentThread().isInterrupted());}finally{Thread.interrupted();}check(interrupted.waitFor(2,TimeUnit.SECONDS));
System.out.println("Production root receipt transport passed: normal/nonzero/oversize, hung su, exited parent with held pipe, cleanup and interrupt preservation");}}
'''
with tempfile.TemporaryDirectory(prefix='nubia-root-read-') as temp:
    temp=Path(temp);p=temp/'com/elitedarkkaiser/nubialy/GuardRootProcessTest.java';p.parent.mkdir(parents=True);p.write_text(source)
    subprocess.run(['java','com.sun.tools.javac.Main','-d',str(temp/'classes'),str(p),str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/GuardRootProcess.java')],check=True)
    subprocess.run(['java','-cp',str(temp/'classes'),'com.elitedarkkaiser.nubialy.GuardRootProcessTest'],check=True,timeout=15)
