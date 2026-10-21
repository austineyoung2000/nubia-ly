"""Unknown launcher versions remain available; invalid packages/platforms stay rejected."""
from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
pkg='com/elitedarkkaiser/nubialy/'
sources={
'android/content/Context.java':'''package android.content;public class Context {public static Info info=new Info();public static boolean missing;public PM getPackageManager(){return new PM();}public static class PM {public Info getPackageInfo(String p,int f){if(missing)throw new IllegalStateException();return info;}} public static class Info {public String versionName;public long code;public App applicationInfo=new App();}public static class App {public String sourceDir;}}''',
'android/os/Build.java':'package android.os;public class Build {public static String MODEL="NX809J",DEVICE="NX809J";public static class VERSION {public static int SDK_INT=36;}}',
pkg+'CompatibilityPolicy.java':'package com.elitedarkkaiser.nubialy;public class CompatibilityPolicy {public static boolean allowed=true;public static boolean isLauncherPlatform(String a,String b,int c){return allowed;}}',
pkg+'GuardBrokerPolicy.java':'package com.elitedarkkaiser.nubialy;public class GuardBrokerPolicy {public static String LAUNCHER_PACKAGE="com.zte.mifavor.launcher";}',
pkg+'EligibilityTest.java':'''package com.elitedarkkaiser.nubialy;import android.content.Context;public class EligibilityTest {static void check(boolean b){if(!b)throw new AssertionError();}public static void main(String[] a)throws Exception{var c=new Context();var f=java.io.File.createTempFile("launcher-eligibility", ".apk");try{Context.info.applicationInfo.sourceDir=f.toString();java.nio.file.Files.write(f.toPath(),new byte[]{1,2,3});for(String version:new String[]{"16.0.010.000.2604151532","16.0.020.000.2603171131","future-unknown",null}){Context.info.versionName=version;Context.info.code=999999;check(LauncherCompatibility.supported(c));}CompatibilityPolicy.allowed=false;check(!LauncherCompatibility.supported(c));CompatibilityPolicy.allowed=true;Context.missing=true;check(!LauncherCompatibility.supported(c));Context.missing=false;Context.info.applicationInfo=null;check(!LauncherCompatibility.supported(c));Context.info.applicationInfo=new Context.App();Context.info.applicationInfo.sourceDir=f.toString();try(var file=new java.io.RandomAccessFile(f,"rw")){file.setLength(0);}check(!LauncherCompatibility.supported(c));try(var file=new java.io.RandomAccessFile(f,"rw")){file.setLength(100L*1024*1024+1);}check(!LauncherCompatibility.supported(c));f.delete();check(!LauncherCompatibility.supported(c));System.out.println("Launcher eligibility: unknown version/code and APK bytes accepted; platform, missing package/file, empty and oversized file rejected");}finally{f.delete();}}}'''
}
with tempfile.TemporaryDirectory() as folder:
 folder=Path(folder)
 for name,content in sources.items():
  p=folder/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content)
 files=[str(p) for p in folder.rglob('*.java')]+[str(root/'app/src/main/java'/pkg/'LauncherCompatibility.java')]
 subprocess.run(['java','com.sun.tools.javac.Main','-d',str(folder/'classes'),*files],check=True)
 subprocess.run(['java','-cp',str(folder/'classes'),'com.elitedarkkaiser.nubialy.EligibilityTest'],check=True)
