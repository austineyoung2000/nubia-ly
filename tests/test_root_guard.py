"""Verify the production root caller's timeout quarantine using a fake su executable."""
from pathlib import Path
import os,subprocess,tempfile
root=Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory(prefix='nubialy-root-channel-') as w:
    w=Path(w);(w/'android/os').mkdir(parents=True);(w/'com/elitedarkkaiser/nubialy').mkdir(parents=True);(w/'bin').mkdir()
    (w/'android/os/SystemClock.java').write_text('package android.os;public class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000;}}')
    (w/'bin/su').write_text('#!/bin/sh\nprintf "%s\\n" "$2" >> "$TEST_ROOT_LOG"\ncase "$2" in *enable_clock) sleep 0.3 ;; esac\nexit 0\n');(w/'bin/su').chmod(0o755)
    (w/'com/elitedarkkaiser/nubialy/RootGuardTest.java').write_text('''package com.elitedarkkaiser.nubialy;import android.os.SystemClock;public class RootGuardTest{static void check(boolean b){if(!b)throw new AssertionError();}public static void main(String[]a)throws Exception{check(!RootGuardCommand.enableClock(SystemClock.elapsedRealtime()+70));check(!RootGuardCommand.enableClock(SystemClock.elapsedRealtime()+1000));Thread.sleep(500);check(RootGuardCommand.enableClock(SystemClock.elapsedRealtime()+1000));check(RootGuardCommand.stopClock(SystemClock.elapsedRealtime()+1000));System.out.println("Production root caller checks passed: dispatched timeout, quarantine, no duplicate launch and completion release");}}''')
    files=[str(p) for p in w.rglob('*.java')]+[str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/RootGuardCommand.java')]
    subprocess.run(['java','com.sun.tools.javac.Main','-d',str(w/'classes'),*files],check=True)
    log=w/'calls';subprocess.run(['java','-cp',str(w/'classes'),'com.elitedarkkaiser.nubialy.RootGuardTest'],env=dict(os.environ,PATH=f'{w / "bin"}:{os.environ["PATH"]}',TEST_ROOT_LOG=str(log)),check=True)
    calls=log.read_text().splitlines();assert len(calls)==3 and calls[-1].endswith('guardctl.sh stop'),calls
