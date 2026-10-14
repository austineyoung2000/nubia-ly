"""Production inventory client: nonce/version/deadline and cancellation before publishing choices."""
from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
sources={
'android/os/SystemClock.java':'package android.os;public class SystemClock {public static long now=100;public static long elapsedRealtime(){return now;}}',
'android/os/Looper.java':'package android.os;public class Looper {public static Looper getMainLooper(){return new Looper();}}',
'android/os/Handler.java':'package android.os;public class Handler {public Handler(Looper l){}public boolean postDelayed(Runnable r,long d){return true;}public void removeCallbacks(Runnable r){}}',
'android/os/Bundle.java':'''package android.os;public class Bundle {java.util.Map<String,Object> values=new java.util.HashMap<>();public void putString(String k,String v){values.put(k,v);}public String getString(String k){return (String)values.get(k);}public String getString(String k,String d){return (String)values.getOrDefault(k,d);}public void putInt(String k,int v){values.put(k,v);}public int getInt(String k,int d){return (Integer)values.getOrDefault(k,d);}public long getLong(String k,long d){return (Long)values.getOrDefault(k,d);}public boolean getBoolean(String k,boolean d){return (Boolean)values.getOrDefault(k,d);}public void putStringArrayList(String k,java.util.ArrayList v){values.put(k,v);}public java.util.ArrayList<String> getStringArrayList(String k){return (java.util.ArrayList<String>)values.get(k);}}''',
'android/content/Intent.java':'package android.content;public class Intent {public static final int FLAG_RECEIVER_REGISTERED_ONLY=1;public java.util.Map<String,Object> extras=new java.util.HashMap<>();public Intent(String action){}public Intent setPackage(String p){return this;}public Intent addFlags(int f){return this;}public Intent putExtra(String k,Object v){extras.put(k,v);return this;}}',
'android/content/BroadcastReceiver.java':'package android.content;public abstract class BroadcastReceiver {private int code;private android.os.Bundle result;public abstract void onReceive(Context c,Intent i);public int getResultCode(){return code;}public android.os.Bundle getResultExtras(boolean create){return result;}public void deliver(Context c,int code,android.os.Bundle result){this.code=code;this.result=result;onReceive(c,new Intent("reply"));}}',
'android/content/Context.java':'package android.content;public class Context {public BroadcastReceiver receiver;public Intent intent;public Context getApplicationContext(){return this;}public void sendOrderedBroadcast(Intent i,String permission,BroadcastReceiver receiver,android.os.Handler h,int code,String data,android.os.Bundle extras){this.receiver=receiver;intent=i;}}',
'com/elitedarkkaiser/nubialy/BuildConfig.java':'package com.elitedarkkaiser.nubialy;public class BuildConfig {public static final int VERSION_CODE=19;}',
'com/elitedarkkaiser/nubialy/InventoryTest.java':'''package com.elitedarkkaiser.nubialy;import android.os.*;import android.content.*;public class InventoryTest {static int publishes;static String message;static void check(boolean b){if(!b)throw new AssertionError(message);}static Bundle reply(Context c){var b=new Bundle();b.putString("nonce",(String)c.intent.extras.get("nonce"));b.putInt("version",19);b.putString("phase","INVENTORY");b.putStringArrayList("statusbar_slots",new java.util.ArrayList<>(java.util.List.of("alarm_clock","bluetooth")));return b;}static void request(SystemUiGuardClient client){client.inventory(slots->{check(slots.size()==2);publishes++;},text->message=text);}public static void main(String[] args){var context=new Context();var client=new SystemUiGuardClient(context);request(client);context.receiver.deliver(context,1201,reply(context));check(publishes==1);check(message.contains("loaded"));
 request(client);var wrong=reply(context);wrong.putString("nonce","bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");context.receiver.deliver(context,1201,wrong);check(publishes==1);
 request(client);wrong=reply(context);wrong.putInt("version",18);context.receiver.deliver(context,1201,wrong);check(publishes==1);
 request(client);wrong=reply(context);wrong.putStringArrayList("statusbar_slots",new java.util.ArrayList<>(java.util.List.of(123)));context.receiver.deliver(context,1201,wrong);check(publishes==1);
 request(client);wrong=reply(context);wrong.putStringArrayList("statusbar_slots",new java.util.ArrayList<>(java.util.List.of("bad slot")));context.receiver.deliver(context,1201,wrong);check(publishes==1);
 request(client);var receiver=context.receiver;var late=reply(context);client.cancel();receiver.deliver(context,1201,late);check(publishes==1);
 request(client);late=reply(context);SystemClock.now+=9000;context.receiver.deliver(context,1201,late);check(publishes==1);
 request(client);context.receiver.deliver(context,1201,reply(context));check(publishes==2);System.out.println("Inventory client passed: validated publication, nonce/version/deadline rejection, malformed list and cancellation fences");}}
'''
}
with tempfile.TemporaryDirectory(prefix='nubialy-inventory-') as work:
    work=Path(work)
    for name,content in sources.items():
        p=work/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content)
    files=[str(p) for p in work.rglob('*.java')]+[str(root/'app/src/main/java/com/elitedarkkaiser/nubialy'/n) for n in ['SystemUiGuardClient.java','SystemUiGuardProtocol.java','GuardBrokerPolicy.java','ClockColorPolicy.java','StatusBarStyle.java']]
    subprocess.run(['java','com.sun.tools.javac.Main','-d',str(work/'classes'),*files],check=True)
    subprocess.run(['java','-cp',str(work/'classes'),'com.elitedarkkaiser.nubialy.InventoryTest'],check=True)
