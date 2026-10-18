"""Root guard nudges never mint authority; service waits for unlocked owner and exits after bounded checks."""
from pathlib import Path
import tempfile,subprocess,os
root=Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory(prefix='nubialy-unlock-') as work:
 w=Path(work);module=w/'module';module.mkdir();state=w/'state';guard=w/'guard';tools=w/'bin';tools.mkdir();log=w/'calls'
 for p in (root/'recovery-module').glob('*.sh'):
  text=p.read_text().replace('/data/local/tmp/nubia-ly-guard',str(guard)).replace('/data/adb/nubia_ly_guard',str(state)).replace(') >/dev/null 2>&1 &',')')
  (module/p.name).write_text(text)
 (tools/'getprop').write_text('#!/bin/sh\ncase "$1" in ro.product.model) echo NX809J;;ro.build.version.sdk) echo 36;;sys.boot_completed) echo 1;;esac\n')
 (tools/'id').write_text('#!/bin/sh\necho 0\n')
 (tools/'am').write_text('#!/bin/sh\nif [ "$1" = get-started-user-state ]; then echo "${TEST_UNLOCK:-RUNNING_UNLOCKED}";else echo "$*" >> "$CALL_LOG";fi\n')
 (tools/'sleep').write_text('#!/bin/sh\necho wait >> "$WAIT_LOG"\n')
 for p in tools.iterdir():p.chmod(0o755)
 env=dict(os.environ,PATH=str(tools)+':'+os.environ['PATH'],CALL_LOG=str(log),WAIT_LOG=str(w/'waits'))
 def run(command,ok=True,script='guardctl.sh',extra=None):
  r=subprocess.run(['sh',str(module/script),command],env=dict(env,**(extra or {})),capture_output=True,text=True,timeout=5);assert (r.returncode==0)==ok,(r.stdout,r.stderr);return r
 run('resume_app');assert not state.exists() and not log.exists()
 run('enable_clock');original=(state/'clock_id').read_text();run('resume_app');assert 'RESUME_AFTER_UNLOCK' in log.read_text();assert (state/'clock_id').read_text()==original
 log.unlink();(module/'disable').touch();run('resume_app');assert not log.exists();(module/'disable').unlink()
 # Replayed receipt from another boot cannot wake the app.
 good=(guard/'enabled').read_text();(guard/'enabled').write_text(good.replace('clock-enabled','other'));run('resume_app',False);assert not log.exists();(guard/'enabled').write_text(good)
 run('resume',script='service.sh');assert log.read_text().count('RESUME_AFTER_UNLOCK')==3
 log.unlink();(w/'waits').unlink();run('resume',script='service.sh',extra={'TEST_UNLOCK':'RUNNING_LOCKED'});assert 'RESUME_AFTER_UNLOCK' not in log.read_text();assert len((w/'waits').read_text().splitlines())==180
 run('stop');log.unlink();run('resume_app');assert not log.exists();assert not (state/'clock_id').exists() and not (guard/'enabled').exists()
print('Unlock resume shell passed: existing authority only, current boot/identity, disable/stop fences, unlock gate and bounded boot nudges')
