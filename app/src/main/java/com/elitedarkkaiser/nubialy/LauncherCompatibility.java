package com.elitedarkkaiser.nubialy;
import android.content.Context;
import android.os.Build;
import java.io.File;
/** Launcher versions stay open; runtime adapters still check the hooks they use. */
public final class LauncherCompatibility {
    private LauncherCompatibility(){}
    public static boolean supported(Context c){
        if(!CompatibilityPolicy.isLauncherPlatform(Build.MODEL,Build.DEVICE,Build.VERSION.SDK_INT))return false;
        try{
            var p=c.getPackageManager().getPackageInfo(GuardBrokerPolicy.LAUNCHER_PACKAGE,0);
            if(p.applicationInfo==null)return false;
            File f=new File(p.applicationInfo.sourceDir);
            return f.isFile()&&f.canRead()&&f.length()>0&&f.length()<=100L*1024*1024;
        }catch(Exception unavailable){return false;}
    }
}
