package com.elitedarkkaiser.nubialy.xposed;

import android.app.Application;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import com.elitedarkkaiser.nubialy.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class SidebarRuntimeTest {
    public static class Broker extends ContentProvider {
        final AtomicInteger calls=new AtomicInteger();volatile int status=GuardBrokerPolicy.READABLE;volatile boolean badNonce;
        @Override public boolean onCreate(){return true;}
        @Override public Bundle call(String method,String arg,Bundle input){assertEquals(SidebarStyle.METHOD,method);calls.incrementAndGet();Bundle out=new Bundle();out.putString("nonce",badNonce?"invalid":input.getString("nonce"));out.putInt("version",BuildConfig.VERSION_CODE);out.putInt("status",status);out.putString("permit_id","12345678-1234-4234-8234-123456789abc");out.putLong("deadline_ms",SystemClock.elapsedRealtime()+ClockEnabledPolicy.HEALTH_MS);out.putString("sidebar_style",SidebarStyle.DEFAULT.withPack("").with(3,0).encode());return out;}
        @Override public Cursor query(Uri u,String[] p,String s,String[] a,String sort){return null;}
        @Override public String getType(Uri u){return null;}
        @Override public Uri insert(Uri u,ContentValues v){return null;}
        @Override public int delete(Uri u,String s,String[] a){return 0;}
        @Override public int update(Uri u,ContentValues v,String s,String[] a){return 0;}
    }
    private static class Fixture implements AutoCloseable {
        final Broker broker=new Broker();final SidebarRuntime runtime;final Application app=RuntimeEnvironment.getApplication();
        Fixture(){var info=new android.content.pm.ProviderInfo();info.authority=GuardBrokerPolicy.AUTHORITY;broker.attachInfo(app,info);ShadowContentResolver.registerProviderInternal(GuardBrokerPolicy.AUTHORITY,broker);runtime=ReflectionHelpers.callConstructor(SidebarRuntime.class,ReflectionHelpers.ClassParameter.from(Application.class,app));}
        void call(String method){ReflectionHelpers.callInstanceMethod(runtime,method);}
        boolean active(){return ReflectionHelpers.callInstanceMethod(runtime,"active");}
        void settle()throws Exception {ExecutorService worker=ReflectionHelpers.getField(runtime,"worker");worker.submit(()->{}).get(3,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();}
        View panel(){View v=new View(app);v.setBackground(new ColorDrawable(Color.RED));ReflectionHelpers.callInstanceMethod(runtime,"background",ReflectionHelpers.ClassParameter.from(View.class,v));return v;}
        @Override public void close(){call("onHidden");call("revoke");Handler handler=ReflectionHelpers.getField(runtime,"main");handler.removeCallbacksAndMessages(null);ExecutorService worker=ReflectionHelpers.getField(runtime,"worker");worker.shutdownNow();}
    }
    @Test public void warmsWithoutOpenAndCloseDoesNotRemoveAppliedStyle()throws Exception {
        try(Fixture f=new Fixture()){f.call("read");f.settle();assertTrue(f.active());assertFalse((Boolean)ReflectionHelpers.getField(f.runtime,"visible"));View panel=f.panel();var applied=panel.getBackground();assertTrue(applied instanceof SidebarSurfaceDrawable);f.call("onHidden");assertTrue(f.active());assertSame(applied,panel.getBackground());ReflectionHelpers.callInstanceMethod(f.runtime,"onShown",ReflectionHelpers.ClassParameter.from(Object.class,null));assertTrue(f.active());assertEquals(1,f.broker.calls.get());}
    }
    @Test public void busyRenewalKeepsCurrentStyleAndInvalidReplyRestoresStock()throws Exception {
        try(Fixture f=new Fixture()){f.call("read");f.settle();View panel=f.panel();var applied=panel.getBackground();f.broker.status=GuardBrokerPolicy.BUSY;f.call("read");f.settle();assertTrue(f.active());assertSame(applied,panel.getBackground());f.broker.status=GuardBrokerPolicy.READABLE;f.broker.badNonce=true;f.call("read");f.settle();assertFalse(f.active());assertTrue(panel.getBackground() instanceof ColorDrawable);assertEquals(Color.RED,((ColorDrawable)panel.getBackground()).getColor());}
    }
    @Test public void expiredAuthorityIsNeverReusedFromArtworkCache()throws Exception {
        try(Fixture f=new Fixture()){f.call("read");f.settle();View panel=f.panel();ReflectionHelpers.setField(f.runtime,"deadline",SystemClock.elapsedRealtime()-1);assertFalse(f.active());f.broker.status=GuardBrokerPolicy.BLOCKED;Runnable expire=ReflectionHelpers.getField(f.runtime,"expire");expire.run();f.settle();assertFalse(f.active());assertTrue(panel.getBackground() instanceof ColorDrawable);assertNull(ReflectionHelpers.getField(f.runtime,"cachedStyle"));}
    }
}
