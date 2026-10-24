package com.elitedarkkaiser.nubialy;
import android.content.Context;
import android.os.Build;
import java.io.*;
import java.security.MessageDigest;
import java.util.HexFormat;
/** Called only on a worker; the research APK is not permission to target other firmware. */
public final class SidebarCompatibility {
    private SidebarCompatibility(){}
    public static boolean supported(Context context){try{
        if(Build.VERSION.SDK_INT!=36||!("NX809J".equals(Build.MODEL)||"NX809J".equals(Build.DEVICE)))return false;
        var info=context.getPackageManager().getApplicationInfo(SidebarStyle.PACKAGE,0);File f=new File(info.sourceDir);
        if(!f.isFile()||f.length()<1||f.length()>100L*1024*1024)return false;
        MessageDigest hash=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)hash.update(b,0,n);}
        return SidebarStyle.APK_SHA256.equals(HexFormat.of().formatHex(hash.digest()));
    }catch(Exception unavailable){return false;}}
}
