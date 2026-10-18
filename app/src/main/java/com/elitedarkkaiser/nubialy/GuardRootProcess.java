package com.elitedarkkaiser.nubialy;

import java.io.*;
import java.util.concurrent.*;

/** One deadline covers process completion and bounded output, including a pipe that never closes. */
final class GuardRootProcess {
    record Result(int exitCode,byte[] output){}
    private GuardRootProcess(){}
    static Result capture(Process process,long timeoutMs,int maximum)throws Exception{
        if(timeoutMs<=0||maximum<1||maximum>8192)throw new IllegalArgumentException("Invalid root read bounds");
        long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        FutureTask<byte[]> output=new FutureTask<>(()->{byte[] bytes=process.getInputStream().readNBytes(maximum+1);if(bytes.length>maximum)throw new IOException("Oversized root output");return bytes;});
        Thread reader=new Thread(output,"nubia-guard-root-output");reader.setDaemon(true);reader.start();
        try{
            long left=deadline-System.nanoTime();if(left<=0||!process.waitFor(left,TimeUnit.NANOSECONDS))throw new TimeoutException("Root process timed out");
            left=deadline-System.nanoTime();if(left<=0)throw new TimeoutException("Root output timed out");
            return new Result(process.exitValue(),output.get(left,TimeUnit.NANOSECONDS));
        }catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw interrupted;}
        finally{
            output.cancel(true);
            // A stream close can itself block behind a pipe read. Cleanup must not extend the deadline.
            Thread cleanup=new Thread(()->{try{if(process.isAlive())process.destroyForcibly();}catch(RuntimeException ignored){}close(process.getInputStream());close(process.getErrorStream());close(process.getOutputStream());},"nubia-guard-root-cleanup");cleanup.setDaemon(true);cleanup.start();
        }
    }
    private static void close(Closeable stream){try{stream.close();}catch(IOException|RuntimeException ignored){}}
}
