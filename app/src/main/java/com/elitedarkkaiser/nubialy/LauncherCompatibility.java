package com.elitedarkkaiser.nubialy;
import android.content.Context;
import android.os.Build;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
/** Called on workers only. Exact baseline; unknown firmware APKs keep stock. */
public final class LauncherCompatibility {
    public static final String SHA256="cce32bcfa1fce5a4c208ea510594a1c7d0e6ad614588334a12753f2819cf3f7c";
    private LauncherCompatibility(){}
    public static boolean supported(Context c){
        if(!CompatibilityPolicy.isLauncherPlatform(Build.MODEL,Build.DEVICE,Build.VERSION.SDK_INT))return false;
        try{var p=c.getPackageManager().getPackageInfo(GuardBrokerPolicy.LAUNCHER_PACKAGE,0);
            if(!LauncherProbePolicy.ENABLED&&(p.getLongVersionCode()!=160000||!"16.0.010.000.2604151532".equals(p.versionName)))return false;
            if(p.applicationInfo==null)return false;
            File f=new File(p.applicationInfo.sourceDir);if(f.length()<=0||f.length()>100*1024*1024)return false;
            if(LauncherProbePolicy.ENABLED)return f.isFile()&&f.canRead();
            MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] b=new byte[65536];long total=0;
            try(FileInputStream in=new FileInputStream(f)){int n;while((n=in.read(b))!=-1){total+=n;if(total>100*1024*1024)return false;digest.update(b,0,n);}}
            StringBuilder hex=new StringBuilder();for(byte value:digest.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",value&255));return SHA256.contentEquals(hex);
        }catch(Exception unavailable){return false;}
    }
}
