package com.elitedarkkaiser.nubialy;
import android.content.Context;import android.os.Build;import java.io.*;import java.security.*;import java.util.HexFormat;
public final class SettingsCompatibility {
 private SettingsCompatibility(){}
 public static boolean supported(Context c){try{if(Build.VERSION.SDK_INT!=36||!("NX809J".equals(Build.MODEL)||"NX809J".equals(Build.DEVICE)))return false;File f=new File(c.getPackageManager().getApplicationInfo(SettingsStyle.PACKAGE,0).sourceDir);if(!f.isFile()||f.length()==0||f.length()>350_000_000)return false;try(InputStream in=new FileInputStream(f)){MessageDigest hash=MessageDigest.getInstance("SHA-256");byte[] bytes=new byte[65536];for(int n;(n=in.read(bytes))!=-1;)hash.update(bytes,0,n);return SettingsStyle.SHA256.equals(HexFormat.of().formatHex(hash.digest()));}}catch(Exception e){return false;}}
}
