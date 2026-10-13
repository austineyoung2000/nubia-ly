package com.elitedarkkaiser.nubialy;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Only fresh fixed probe/lifecycle reads are exposed. No paths, root commands or writes accepted. */
public final class GuardBrokerProvider extends ContentProvider {
    private final AtomicBoolean busy = new AtomicBoolean();
    @Override public boolean onCreate() {
        return CompatibilityPolicy.isNx809j(Build.MODEL, Build.DEVICE) && Build.VERSION.SDK_INT == 36;
    }
    @Override public Bundle call(String method, String arg, Bundle extras) {
        enforceSystemUi();
        if ((!GuardBrokerPolicy.METHOD.equals(method) && !GuardBrokerPolicy.PREVIEW_METHOD.equals(method) && !GuardBrokerPolicy.CLOCK_METHOD.equals(method) && !GuardBrokerPolicy.ENABLED_METHOD.equals(method) && !GuardBrokerPolicy.STARTUP_METHOD.equals(method))
                || arg != null || extras == null)
            throw new IllegalArgumentException("Unsupported guard request");
        String nonce = extras.getString("nonce");
        int version = extras.getInt("version", -1);
        if (!SystemUiGuardProtocol.accepts(nonce, nonce, BuildConfig.VERSION_CODE, version, 0))
            throw new IllegalArgumentException("Invalid guard request");
        Bundle reply = new Bundle();
        reply.putString("nonce", nonce);
        reply.putInt("version", BuildConfig.VERSION_CODE);
        if (!busy.compareAndSet(false, true)) {
            reply.putInt("status", GuardBrokerPolicy.BUSY);
            return reply;
        }
        // Keep authentication before any identity change. File access uses this app's domain.
        int callerPid = Binder.getCallingPid();
        long identity = Binder.clearCallingIdentity();
        try {
            if (GuardBrokerPolicy.ENABLED_METHOD.equals(method) || GuardBrokerPolicy.STARTUP_METHOD.equals(method)) {
                String id = GuardReader.inspectEnabled();
                boolean allowed = RecoveryStore.enabledAllowed(getContext(), id);
                if (allowed && GuardBrokerPolicy.STARTUP_METHOD.equals(method)) allowed = RecoveryStore.claimStartup(getContext(), callerPid);
                reply.putInt("status", allowed ? GuardBrokerPolicy.READABLE : GuardBrokerPolicy.BLOCKED);
                if (allowed) {
                    reply.putString("permit_id", id);
                    reply.putLong("deadline_ms", android.os.SystemClock.elapsedRealtime() + ClockEnabledPolicy.HEALTH_MS);
                    reply.putInt("clock_color", ClockColorPreferences.read(getContext()));
                    reply.putInt("clock_size", ClockColorPreferences.size(getContext()));
                    reply.putBoolean("clock_bold", ClockColorPreferences.bold(getContext()));
                }
            } else if (GuardBrokerPolicy.PREVIEW_METHOD.equals(method) || GuardBrokerPolicy.CLOCK_METHOD.equals(method)) {
                PreviewPolicy.Permit permit;
                if (GuardBrokerPolicy.CLOCK_METHOD.equals(method)) {
                    permit = GuardReader.inspectClock();
                    if (!RecoveryStore.clockAllowed(getContext(), permit)) permit = null;
                } else permit = RecoveryStore.lifecycleAllowed(getContext()) ? GuardReader.inspectPreview() : null;
                reply.putInt("status", permit != null ? GuardBrokerPolicy.READABLE : GuardBrokerPolicy.BLOCKED);
                if (permit != null) {
                    reply.putString("permit_id", permit.id());
                    reply.putLong("deadline_ms", permit.deadlineMs());
                }
            } else {
                reply.putInt("status", GuardReader.inspectSnapshot().readable()
                    ? GuardBrokerPolicy.READABLE : GuardBrokerPolicy.BLOCKED);
            }
            return reply;
        } finally {
            Binder.restoreCallingIdentity(identity);
            busy.set(false);
        }
    }
    private void enforceSystemUi() {
        int systemUiUid;
        try {
            systemUiUid = getContext().getPackageManager().getApplicationInfo("com.android.systemui", 0).uid;
        } catch (Exception unavailable) { throw new SecurityException("System UI identity unavailable"); }
        if (!GuardBrokerPolicy.permits(Binder.getCallingUid(), systemUiUid, getCallingPackage()))
            throw new SecurityException("System UI caller required");
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) { throw unsupported(); }
    @Override public Uri insert(Uri uri, ContentValues values) { throw unsupported(); }
    @Override public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) { throw unsupported(); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { throw unsupported(); }
    @Override public String getType(Uri uri) { return null; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        throw new FileNotFoundException("No guard files exported");
    }
    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Only fixed guard inspection is supported");
    }
}
