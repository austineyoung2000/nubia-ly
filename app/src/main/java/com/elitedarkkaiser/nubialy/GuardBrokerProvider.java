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
        return CompatibilityPolicy.isSupportedDevice(Build.MODEL, Build.DEVICE) && CompatibilityPolicy.isSupportedSdk(Build.VERSION.SDK_INT);
    }
    @Override public Bundle call(String method, String arg, Bundle extras) {
        if(GuardBrokerPolicy.LAUNCHER_METHOD.equals(method)) enforceLauncher(); else if(GuardBrokerPolicy.FINGERPRINT_METHOD.equals(method)) enforceFingerprint(); else enforceSystemUi();
        if ((!GuardBrokerPolicy.LAUNCHER_METHOD.equals(method) && !GuardBrokerPolicy.FINGERPRINT_METHOD.equals(method) && !GuardBrokerPolicy.METHOD.equals(method) && !GuardBrokerPolicy.PREVIEW_METHOD.equals(method) && !GuardBrokerPolicy.CLOCK_METHOD.equals(method) && !GuardBrokerPolicy.ENABLED_METHOD.equals(method) && !GuardBrokerPolicy.STARTUP_METHOD.equals(method))
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
            if(GuardBrokerPolicy.LAUNCHER_METHOD.equals(method)) {
                String id=GuardReader.inspectEnabled();boolean allowed=RecoveryStore.enabledAllowed(getContext(),id);
                LauncherStyle style=allowed?LauncherPreferences.active(getContext()):LauncherStyle.STOCK;
                if(allowed&&style.custom())allowed=RecoveryStore.claimLauncherStartup(getContext(),callerPid);
                reply.putInt("status",allowed?GuardBrokerPolicy.READABLE:GuardBrokerPolicy.BLOCKED);
                if(allowed){reply.putString("permit_id",id);reply.putLong("deadline_ms",android.os.SystemClock.elapsedRealtime()+ClockEnabledPolicy.HEALTH_MS);reply.putString("launcher_layout",style.layout().encode());reply.putInt("launcher_scale",style.iconScale());reply.putBoolean("launcher_labels",style.hideLabels());reply.putInt("launcher_label_color",style.labelColor());reply.putInt("folder_blur",style.folderBlur());reply.putInt("folder_labels",style.folderLabelColor());reply.putInt("folder_title",style.folderTitleColor());reply.putString("icon_pack",style.iconPack());reply.putInt("icon_shape",style.iconShape());}
            } else if(GuardBrokerPolicy.FINGERPRINT_METHOD.equals(method)) {
                String id=GuardReader.inspectEnabled();boolean allowed=RecoveryStore.enabledAllowed(getContext(),id);
                SurfaceStyle style=allowed?StatusBarPreferences.active(getContext(),id).surfaces():SurfaceStyle.STOCK;
                if(allowed&&(style.fingerprintStyle()!=0||style.fingerprintHidden()!=0||style.fingerprintAnimation()!=0))allowed=RecoveryStore.claimSensorStartup(getContext(),callerPid);
                reply.putInt("status",allowed?GuardBrokerPolicy.READABLE:GuardBrokerPolicy.BLOCKED);
                if(allowed){reply.putString("permit_id",id);reply.putLong("deadline_ms",android.os.SystemClock.elapsedRealtime()+ClockEnabledPolicy.HEALTH_MS);reply.putInt("fingerprint_animation",style.fingerprintAnimation());reply.putInt("fingerprint_speed",style.fingerprintSpeed());reply.putInt("fingerprint_style",style.fingerprintStyle());reply.putBoolean("fingerprint_hidden",style.fingerprintHidden()!=0);}
            } else if (GuardBrokerPolicy.ENABLED_METHOD.equals(method) || GuardBrokerPolicy.STARTUP_METHOD.equals(method)) {
                String id = GuardReader.inspectEnabled();
                boolean allowed = RecoveryStore.enabledAllowed(getContext(), id);
                if (allowed && GuardBrokerPolicy.STARTUP_METHOD.equals(method)) allowed = RecoveryStore.claimStartup(getContext(), callerPid);
                reply.putInt("status", allowed ? GuardBrokerPolicy.READABLE : GuardBrokerPolicy.BLOCKED);
                if (allowed) {
                    reply.putString("permit_id", id);
                    reply.putLong("deadline_ms", android.os.SystemClock.elapsedRealtime() + ClockEnabledPolicy.HEALTH_MS);
                    ClockSnapshot clock=StatusBarPreferences.activeClock(getContext(),id);
                    reply.putInt("clock_color", clock.color());
                    reply.putInt("clock_size", clock.size());
                    reply.putBoolean("clock_bold", clock.bold());
                    reply.putBoolean("clock_seconds", clock.seconds());
                    reply.putBoolean("clock_italic", clock.italic());
                    reply.putInt("clock_spacing", clock.spacing());
                    reply.putInt("clock_family", clock.family());
                    reply.putBoolean("clock_hidden", clock.hidden());
                    StatusBarStyle bar = StatusBarPreferences.active(getContext(), id);
                    reply.putInt("statusbar_color",StatusBarPreferences.activeColor(getContext(), id));
                    reply.putInt("clock_date",StatusBarPreferences.activeDate(getContext(),id));
                    reply.putInt("statusbar_system_opacity",bar.systemOpacity());reply.putInt("statusbar_notification_opacity",bar.notificationOpacity());reply.putInt("statusbar_battery_opacity",bar.batteryOpacity());reply.putString("statusbar_hidden_slots",bar.slotsText());reply.putInt("statusbar_system_transform",bar.systemTransform());reply.putInt("statusbar_notification_transform",bar.notificationTransform());reply.putInt("statusbar_battery_transform",bar.batteryTransform());reply.putInt("statusbar_fan_appearance",bar.fanAppearance());reply.putInt("statusbar_pump_appearance",bar.pumpAppearance());reply.putInt("statusbar_refresh_appearance",bar.refreshAppearance());reply.putString("surface_style",bar.surfaces().encode());
                    reply.putInt("statusbar_mask",bar.mask()); reply.putInt("statusbar_system_scale",bar.systemScale());
                    reply.putInt("statusbar_notification_scale",bar.notificationScale()); reply.putInt("statusbar_battery_scale",bar.batteryScale());
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
    private void enforceLauncher(){
        int uid;try{uid=getContext().getPackageManager().getApplicationInfo(GuardBrokerPolicy.LAUNCHER_PACKAGE,0).uid;}catch(Exception unavailable){throw new SecurityException("Launcher identity unavailable");}
        if(!GuardBrokerPolicy.permitsLauncher(Binder.getCallingUid(),uid,getCallingPackage()))throw new SecurityException("Launcher caller required");
    }
    private void enforceFingerprint(){
        int uid;try{uid=getContext().getPackageManager().getApplicationInfo(GuardBrokerPolicy.SENSOR_PACKAGE,0).uid;}catch(Exception unavailable){throw new SecurityException("Fingerprint identity unavailable");}
        if(!GuardBrokerPolicy.permitsFingerprint(Binder.getCallingUid(),uid,getCallingPackage()))throw new SecurityException("Fingerprint caller required");
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
        enforceSystemUi();
        var segments=uri.getPathSegments();
        if(!"r".equals(mode)||segments.size()!=2||!"qs-image".equals(segments.get(0))||!QsImageStore.hash(segments.get(1)))throw new FileNotFoundException("Unknown image");
        long identity=Binder.clearCallingIdentity();
        try{
            String id=GuardReader.inspectEnabled();
            if(!RecoveryStore.enabledAllowed(getContext(),id))throw new FileNotFoundException("Image authority unavailable");
            var theme=StatusBarPreferences.active(getContext(),id).surfaces().qsTheme();
            if(theme.header()==0||!segments.get(1).equals(theme.image()))throw new FileNotFoundException("Image not active");
            java.io.File file=QsImageStore.file(getContext(),theme.image());
            if(!file.isFile()||file.length()<=0||file.length()>QsImageStore.MAX_BYTES)throw new FileNotFoundException("Image unavailable");
            return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
        }finally{Binder.restoreCallingIdentity(identity);}
    }
    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Only fixed guard inspection is supported");
    }
}
