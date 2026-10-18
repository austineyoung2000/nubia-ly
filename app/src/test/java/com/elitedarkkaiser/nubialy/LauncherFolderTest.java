package com.elitedarkkaiser.nubialy;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.View;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class LauncherFolderTest {
    private static final String FOLDER="com.android.launcher3.folder.Folder";
    @Test public void folderSelectionIsIndependentAndOlderProfilesDefaultToNative(){
        var c=org.robolectric.RuntimeEnvironment.getApplication();var s=new LauncherStyle(70,true,0xff123456,0,0xffabcdef,0xfffedcba);
        assertTrue(s.valid());assertTrue(LauncherPreferences.save(c,s));assertEquals(s,LauncherPreferences.draft(c));assertEquals(LauncherStyle.STOCK,LauncherPreferences.active(c));
        assertTrue(LauncherPreferences.activate(c,s));assertEquals(s,LauncherPreferences.active(c));assertTrue(LauncherPreferences.stop(c));assertEquals(LauncherStyle.STOCK,LauncherPreferences.active(c));assertEquals(s,LauncherPreferences.draft(c));
        c.getSharedPreferences("launcher_editor",0).edit().clear().putInt("draft_scale",80).putInt("draft_color",0xff123456).commit();assertEquals(new LauncherStyle(80,false,0xff123456),LauncherPreferences.draft(c));
        for(LauncherStyle invalid:List.of(new LauncherStyle(100,false,0,26,0,0),new LauncherStyle(100,false,0,-2,0,0),new LauncherStyle(100,false,0,5,0x55112233,0),new LauncherStyle(100,false,0,5,0,0x55112233)))assertFalse(invalid.valid());
    }
    @Test public void onlyVerifiedFolderTitleAndAppItemsMatch(){
        assertEquals(2,LauncherFolderScope.kind(true,false,List.of(FOLDER),-1,-1));assertEquals(1,LauncherFolderScope.kind(false,true,List.of("com.android.launcher3.CellLayout",FOLDER),42,0));assertEquals(1,LauncherFolderScope.kind(false,true,List.of(FOLDER),42,6));
        assertEquals(0,LauncherFolderScope.kind(false,true,List.of(FOLDER),Integer.MAX_VALUE,0));assertEquals(0,LauncherFolderScope.kind(false,true,List.of(FOLDER),-100,0));assertEquals(0,LauncherFolderScope.kind(false,true,List.of(FOLDER),42,2));assertEquals(0,LauncherFolderScope.kind(false,false,List.of(FOLDER),42,0));assertEquals(0,LauncherFolderScope.kind(true,false,List.of("com.android.launcher3.Workspace"),42,0));assertEquals(0,LauncherFolderScope.kind(false,true,List.of("com.android.launcher3.allapps.AllAppsContainerView",FOLDER),42,0));
    }
    @Test public void blurZeroCopiesInputAndPositiveBlurSmoothsEdgesWithoutChangingSolidColors(){
        int[] solid=new int[63];Arrays.fill(solid,0xff123456);assertArrayEquals(solid,FolderBlur.pixels(solid,9,7,25));
        int[] edge=new int[81];for(int i=0;i<81;i++)edge[i]=i%9<4?0xff000000:0xffffffff;int[] before=edge.clone();assertArrayEquals(edge,FolderBlur.pixels(edge,9,9,0));assertNotSame(edge,FolderBlur.pixels(edge,9,9,0));int[] blurred=FolderBlur.pixels(edge,9,9,2);assertArrayEquals(before,edge);assertTrue((blurred[4*9+3]&255)>0);assertTrue((blurred[4*9+4]&255)<255);for(int c:blurred)assertEquals(255,c>>>24);
        try{FolderBlur.pixels(edge,9,9,26);fail();}catch(IllegalArgumentException expected){}try{FolderBlur.pixels(edge,10,9,1);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void liveBlurPreservesAnimationAndRespectsNativeAndGuardStates(){
        assertEquals(0,FolderSurfaceState.radius(100,0,true,true));assertEquals(100,FolderSurfaceState.radius(100,25,true,true));assertEquals(20,FolderSurfaceState.radius(50,10,true,true));assertEquals(0,FolderSurfaceState.radius(0,10,true,true));
        assertEquals(100,FolderSurfaceState.radius(100,-1,true,true));assertEquals(100,FolderSurfaceState.radius(100,10,false,true));assertEquals(100,FolderSurfaceState.radius(100,10,true,false));assertEquals(100,FolderSurfaceState.radius(100,26,true,true));assertEquals(5000,FolderSurfaceState.radius(5000,0,true,true));
    }
    @Test public void surfaceRestoreKeepsLatestNativeRadiusAndRetiresOwnershipOnOtherWrites(){
        FolderSurfaceState state=new FolderSurfaceState();Object surface=new Object(),controller=new Object(),folder=new Object();state.remember(surface,controller,folder,100,0);state.remember(surface,controller,folder,40,0);var restore=state.drain();assertEquals(1,restore.size());assertEquals(40,restore.get(0).nativeRadius());assertSame(surface,restore.get(0).surface());assertSame(folder,restore.get(0).folder());assertTrue(state.drain().isEmpty());
        state.remember(surface,controller,folder,100,0);state.nativeWrite(surface);assertTrue(state.drain().isEmpty());state.remember(surface,controller,folder,100,100);assertTrue(state.drain().isEmpty());
    }
    @Test public void revocationRestoresNativeBitmapWithoutOverwritingNewLauncherBackground(){
        var c=org.robolectric.RuntimeEnvironment.getApplication();View view=new View(c);Bitmap custom=Bitmap.createBitmap(4,4,Bitmap.Config.ARGB_8888),nativeBitmap=Bitmap.createBitmap(4,4,Bitmap.Config.ARGB_8888);custom.eraseColor(Color.RED);nativeBitmap.eraseColor(Color.BLUE);
        BitmapDrawable selected=new BitmapDrawable(c.getResources(),custom);selected.setAlpha(100);view.setBackground(selected);LauncherFolderBackgrounds state=new LauncherFolderBackgrounds();state.remember(view,nativeBitmap);state.restore();assertSame(nativeBitmap,((BitmapDrawable)view.getBackground()).getBitmap());assertEquals(100,view.getBackground().getAlpha());assertFalse(nativeBitmap.isRecycled());assertFalse(custom.isRecycled());
        view.setBackground(selected);state.remember(view,nativeBitmap);ColorDrawable newer=new ColorDrawable(Color.GREEN);view.setBackground(newer);state.restore();assertSame(newer,view.getBackground());
    }
}
