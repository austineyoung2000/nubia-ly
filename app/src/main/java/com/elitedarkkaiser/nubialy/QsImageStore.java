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
 public static String importImage(Context c,Uri uri)throws Exception {
  byte[] original;try(InputStream in=c.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Image unavailable");original=in.readNBytes(16*1024*1024+1);}
  if(original.length>16*1024*1024)throw new IOException("Choose an image smaller than 16 MB");
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
