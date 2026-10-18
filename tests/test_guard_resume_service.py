"""Production guard service: late first unlock, bounded retries and stop gates."""
from pathlib import Path
import os, subprocess, tempfile
root=Path(__file__).resolve().parents[1]
for mode in ('late','stopped','absent','disabled','never'):
 with tempfile.TemporaryDirectory(prefix='nubialy-resume-') as tmp:
  w=Path(tmp);module=w/'module';module.mkdir();bins=w/'bin';bins.mkdir()
  state=w/'state';state.mkdir()
  if mode!='absent':(state/'clock_id').write_text('existing-authority')
  if mode=='disabled':(module/'disable').touch()
  (module/'service.sh').write_text((root/'recovery-module/service.sh').read_text().replace('/data/adb/nubia_ly_guard',str(state))+'\nwait\n')
  (module/'guardctl.sh').write_text('#!/bin/sh\necho "$1" >> "$RESUME_LOG"\n')
  (module/'bootctl.sh').write_text('#!/bin/sh\nexit 0\n')
  (bins/'getprop').write_text('#!/bin/sh\necho 1\n')
  (bins/'am').write_text('#!/bin/sh\nn=0; [ ! -f "$TICKS" ] || n=$(cat "$TICKS"); if [ "$MODE" != never ] && [ "$n" -ge 14 ]; then echo RUNNING_UNLOCKED; else echo RUNNING_LOCKED; fi\n')
  (bins/'sleep').write_text('#!/bin/sh\nn=0; [ ! -f "$TICKS" ] || n=$(cat "$TICKS"); n=$((n+1)); echo "$n" > "$TICKS"; if [ "$MODE" = stopped ] && [ "$n" -ge 15 ]; then rm -f "$STATE/clock_id"; fi\n')
  for p in bins.iterdir():p.chmod(0o755)
  log=w/'calls';ticks=w/'ticks'
  env=dict(os.environ,PATH=f'{bins}:{os.environ["PATH"]}',RESUME_LOG=str(log),TICKS=str(ticks),STATE=str(state),MODE=mode)
  subprocess.run(['sh',str(module/'service.sh')],env=env,capture_output=True,check=True,timeout=30)
  calls=log.read_text().splitlines();nudges=calls.count('resume_app')
  assert nudges=={'late':3,'stopped':1,'absent':0,'disabled':0,'never':0}[mode],(mode,calls)
  if mode=='late':assert int(ticks.read_text())==16
  if mode=='never':assert int(ticks.read_text())==180
print('Guard resume service passed: unlock after old two-minute limit, three bounded nudges, revoked/absent/disabled authority gates')
