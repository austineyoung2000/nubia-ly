package com.elitedarkkaiser.nubialy;
import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import java.io.*;
import java.security.MessageDigest;
/** Private, bounded image imports. Only immutable SHA-256 names cross into System UI. */
public final class QsImageStore {
 public static final int MAX_BYTES=256*1024;
 private QsImageStore(){}
 public static boolean hash(String value){return value!=null&&value.matches("[a-f0-9]{64}");}
 public static String digest(byte[] data)throws Exception {StringBuilder s=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(data))s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
 public static File file(Context c,String hash){if(!hash(hash))throw new IllegalArgumentException();return new File(new File(c.getFilesDir(),"qs-images"),hash+".jpg");}
 private static boolean imageSignature(byte[] b){
  if(b.length<12)return false;
  boolean png=(b[0]&255)==137&&b[1]==80&&b[2]==78&&b[3]==71&&b[4]==13&&b[5]==10&&b[6]==26&&b[7]==10;
  boolean jpeg=(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255;
  boolean webp=b[0]==82&&b[1]==73&&b[2]==70&&b[3]==70&&b[8]==87&&b[9]==69&&b[10]==66&&b[11]==80;
  boolean gif=b[0]==71&&b[1]==73&&b[2]==70&&b[3]==56&&(b[4]==55||b[4]==57)&&b[5]==97;
  boolean bmp=b[0]==66&&b[1]==77;
  return png||jpeg||webp||gif||bmp;
 }
 public static String importImage(Context c,Uri uri)throws Exception {
  byte[] original;try(InputStream in=c.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Image unavailable");original=in.readNBytes(16*1024*1024+1);}
  if(original.length>16*1024*1024)throw new IOException("Choose an image smaller than 16 MB");
  if(!imageSignature(original))throw new IOException("Choose a JPEG, PNG, WebP, GIF or BMP image");
  BitmapFactory.Options info=new BitmapFactory.Options();info.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(original,0,original.length,info);
  if(info.outWidth<=0||info.outHeight<=0||info.outWidth>32000||info.outHeight>32000)throw new IOException("Unsupported image dimensions");
  BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=1;while(info.outWidth/opts.inSampleSize>1024||info.outHeight/opts.inSampleSize>1024)opts.inSampleSize*=2;
  Bitmap bitmap=BitmapFactory.decodeByteArray(original,0,original.length,opts);if(bitmap==null)throw new IOException("Cannot decode image");
  byte[] data;try{ByteArrayOutputStream out=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,85,out);data=out.toByteArray();if(data.length>MAX_BYTES){out.reset();bitmap.compress(Bitmap.CompressFormat.JPEG,55,out);data=out.toByteArray();}}finally{bitmap.recycle();}
  if(data.length==0||data.length>MAX_BYTES)throw new IOException("Choose a smaller or simpler image");
  String hash=digest(data);File target=file(c,hash);if(!target.getParentFile().isDirectory()&&!target.getParentFile().mkdirs())throw new IOException("Image storage unavailable");
  File tmp=File.createTempFile("import-",".tmp",target.getParentFile());try{try(FileOutputStream out=new FileOutputStream(tmp)){out.write(data);out.getFD().sync();}if(!tmp.renameTo(target)&&!target.isFile())throw new IOException("Image save failed");}finally{tmp.delete();}
  return hash;
 }
}
