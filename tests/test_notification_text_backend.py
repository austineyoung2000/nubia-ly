"""Notification text draw scope and native restoration checks."""
from pathlib import Path
import ast, subprocess, tempfile
root=Path(__file__).resolve().parents[1]
m=ast.parse((root/'tests/test_statusbar_backend.py').read_text())
base=ast.literal_eval(next(n.value for n in m.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='sources' for t in n.targets)))
sources={k:v for k,v in base.items() if k.startswith('de/robv/android/xposed/')}
sources["de/robv/android/xposed/XC_MethodHook.java"]=sources["de/robv/android/xposed/XC_MethodHook.java"].replace("public class XC_MethodHook {","public class XC_MethodHook {public XC_MethodHook(){}public XC_MethodHook(int priority){}")
sources.update({
'android/os/Looper.java':'package android.os;public class Looper {static final Looper M=new Looper();public static boolean off;public static Looper myLooper(){return off?null:M;}public static Looper getMainLooper(){return M;}}',
'android/view/View.java':'package android.view;public class View {public View parent;public boolean attached=true;public int invalidations;public View getParent(){return parent;}public boolean isAttachedToWindow(){return attached;}public int getWidth(){return 100;}public int getHeight(){return 40;}public void invalidate(){invalidations++;}}',
'android/graphics/Canvas.java':'package android.graphics;public class Canvas {public int layers,restores,draws;public int saveLayer(float a,float b,float c,float d,Paint p){layers++;return 1;}public void restoreToCount(int n){restores++;}}',
'android/graphics/Paint.java':'package android.graphics;public class Paint {public void setColorFilter(PorterDuffColorFilter f){}}',
'android/graphics/PorterDuff.java':'package android.graphics;public class PorterDuff {public enum Mode {SRC_IN}}',
'android/graphics/PorterDuffColorFilter.java':'package android.graphics;public class PorterDuffColorFilter {public PorterDuffColorFilter(int c,PorterDuff.Mode m){}}',
'android/widget/TextView.java':'package android.widget;public class TextView extends android.view.View {public void onDraw(android.graphics.Canvas c){de.robv.android.xposed.XposedBridge.run(this,TextView.class,"onDraw",new Class<?>[]{android.graphics.Canvas.class},new Object[]{c},()->{c.draws++;});}}',
'android/widget/EditText.java':'package android.widget;public class EditText extends TextView {}',
'com/android/systemui/statusbar/notification/row/ExpandableNotificationRow.java':'package com.android.systemui.statusbar.notification.row;public class ExpandableNotificationRow extends android.view.View {}',
'com/elitedarkkaiser/nubialy/xposed/ClockColorBackend.java':'package com.elitedarkkaiser.nubialy.xposed;public class ClockColorBackend {public static boolean allow=true;public static boolean authorized(){return allow;}}',
'com/elitedarkkaiser/nubialy/xposed/NotificationTextTest.java':'''package com.elitedarkkaiser.nubialy.xposed;
public class NotificationTextTest {static void ok(boolean b){if(!b)throw new AssertionError();}public static void main(String[] args){var row=new com.android.systemui.statusbar.notification.row.ExpandableNotificationRow();var text=new android.widget.TextView();text.parent=row;var other=new android.widget.TextView();var input=new android.widget.EditText();input.parent=row;NotificationTextBackend.begin(0xff00ff00);var c=new android.graphics.Canvas();text.onDraw(c);ok(c.draws==1&&c.layers==1&&c.restores==1);c=new android.graphics.Canvas();other.onDraw(c);input.onDraw(c);ok(c.layers==0);android.os.Looper.off=true;text.onDraw(c);android.os.Looper.off=false;ClockColorBackend.allow=false;text.onDraw(c);ClockColorBackend.allow=true;row.attached=false;text.onDraw(c);row.attached=true;ok(c.layers==0);ok(NotificationTextBackend.end());ok(text.invalidations==1);text.onDraw(c);ok(c.layers==0);ok(de.robv.android.xposed.XposedBridge.hooks.isEmpty());System.out.println("Notification text scope, authority, thread, detach, and restoration passed");}}'''
})
with tempfile.TemporaryDirectory(prefix='nubialy-notification-text-') as folder:
 folder=Path(folder)
 for name,content in sources.items():
  p=folder/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content)
 files=[str(p) for p in folder.rglob('*.java')]+[str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/xposed/NotificationTextBackend.java')]
 subprocess.run(['java','com.sun.tools.javac.Main','-d',str(folder/'classes'),*files],check=True)
 subprocess.run(['java','-cp',str(folder/'classes'),'com.elitedarkkaiser.nubialy.xposed.NotificationTextTest'],check=True)
