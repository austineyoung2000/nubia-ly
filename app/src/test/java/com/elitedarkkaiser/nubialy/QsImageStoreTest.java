package com.elitedarkkaiser.nubialy;
import android.graphics.*;
import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class QsImageStoreTest {
 @Test public void importsBoundedPrivateJpegAndRejectsInvalidInput()throws Exception{
  var c=RuntimeEnvironment.getApplication();var source=java.io.File.createTempFile("qs-source", ".png",c.getCacheDir());var bitmap=Bitmap.createBitmap(1200,800,Bitmap.Config.ARGB_8888);bitmap.eraseColor(0xff123456);try(var out=new java.io.FileOutputStream(source)){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();
  String hash=QsImageStore.importImage(c,Uri.fromFile(source));assertTrue(QsImageStore.hash(hash));var file=QsImageStore.file(c,hash);assertTrue(file.isFile());assertTrue(file.length()<=QsImageStore.MAX_BYTES);assertEquals(hash,QsImageStore.digest(java.nio.file.Files.readAllBytes(file.toPath())));var opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.toString(),opts);assertTrue(opts.outWidth<=1024&&opts.outHeight<=1024);
  java.nio.file.Files.write(source.toPath(),new byte[]{1,2,3});try{QsImageStore.importImage(c,Uri.fromFile(source));fail();}catch(java.io.IOException expected){}source.delete();
  try{QsImageStore.file(c,"../secret");fail();}catch(IllegalArgumentException expected){}
 }
}
