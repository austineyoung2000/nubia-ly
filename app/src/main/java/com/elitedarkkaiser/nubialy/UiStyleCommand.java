package com.elitedarkkaiser.nubialy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
/** No caller-supplied paths, shell fragments or automatic replays. All calls run on the editor worker. */
final class UiStyleCommand {
 private static final AtomicBoolean busy=new AtomicBoolean();
 static String run(String operation){
  if(!UiStyles.command(operation))return "Unsupported operation.";
  if(!busy.compareAndSet(false,true))return "UI style operation is still running. Check status shortly.";
  Process p=null;boolean deferred=false;
  try{
   p=new ProcessBuilder("su","-c","sh /data/adb/modules/nubia_ly_guard/uictl.sh "+operation).redirectErrorStream(true).start();p.getOutputStream().close();
   if(!p.waitFor(30,TimeUnit.SECONDS)){Process pending=p;p=null;deferred=true;new Thread(()->{try{pending.waitFor();}catch(InterruptedException e){Thread.currentThread().interrupt();}finally{close(pending);busy.set(false);}},"nubia-ly-ui-completion").start();return "Operation not confirmed yet. Check status before retrying.";}
   byte[] output=p.getInputStream().readNBytes(4097);if(output.length>4096)return "Response exceeded its limit. Check status.";
   String message=new String(output,StandardCharsets.UTF_8).trim();return (p.exitValue()==0?"":"Not confirmed: ")+(message.isEmpty()?"Install the UI test guard, reboot and check status.":message);
  }catch(Exception error){if(p!=null&&p.isAlive()){Process pending=p;p=null;deferred=true;new Thread(()->{try{pending.waitFor();}catch(InterruptedException e){Thread.currentThread().interrupt();}finally{close(pending);busy.set(false);}},"nubia-ly-ui-completion").start();}return "UI operation unavailable. Install the UI test guard and grant root access.";}
  finally{if(p!=null)close(p);if(!deferred)busy.set(false);}
 }
 private static void close(Process p){try{p.getInputStream().close();p.getOutputStream().close();p.getErrorStream().close();}catch(Exception ignored){}}
}
