package com.elitedarkkaiser.nubialy;
import android.content.Context;import android.os.Build;import java.io.*;import java.security.*;import java.util.HexFormat;
public final class SettingsCompatibility {
 private SettingsCompatibility(){}
 static String verificationKey(File f,String firmware){return SettingsStyle.SHA256+"|"+f.getAbsolutePath()+"|"+f.length()+"|"+f.lastModified()+"|"+firmware;}
 static boolean cached(File marker,String key){try{return marker.isFile()&&marker.length()<4096&&key.equals(java.nio.file.Files.readString(marker.toPath()));}catch(Exception e){return false;}}
 public static boolean supported(Context c){try{if(Build.VERSION.SDK_INT!=36||!("NX809J".equals(Build.MODEL)||"NX809J".equals(Build.DEVICE)))return false;File f=new File(c.getPackageManager().getApplicationInfo(SettingsStyle.PACKAGE,0).sourceDir);if(!f.isFile()||f.length()==0||f.length()>350_000_000)return false;String key=verificationKey(f,Build.FINGERPRINT);File marker=new File(c.getCacheDir(),"nubialy-settings-verified");try{if(cached(marker,key))return true;}catch(Exception ignored){}try(InputStream in=new FileInputStream(f)){MessageDigest hash=MessageDigest.getInstance("SHA-256");byte[] bytes=new byte[65536];for(int n;(n=in.read(bytes))!=-1;)hash.update(bytes,0,n);boolean verified=SettingsStyle.SHA256.equals(HexFormat.of().formatHex(hash.digest()));if(verified)try{java.nio.file.Files.writeString(marker.toPath(),key);}catch(Exception ignored){}return verified;}}catch(Exception e){return false;}}
}
