from pathlib import Path
import tempfile,subprocess,json,os,hashlib,shutil
root=Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory() as w:
 w=Path(w);module=w/'module';module.mkdir();state=w/'state';bin=w/'bin';bin.mkdir();active=w/'active.json';active.write_text(json.dumps(['oem.keep']))
 script=(root/'recovery-module/uictl.sh').read_text().replace('STATE=/data/adb/nubia_ly_guard/ui_style','STATE='+str(state));(module/'uictl.sh').write_text(script)
 for name in ['ui-fingerprint']:shutil.copy(root/'recovery-module'/name,module/name)
 native=w/'native';native.write_bytes(b'native');(module/'ui-native-sha256').write_text(hashlib.sha256(b'native').hexdigest()+' '+str(native)+'\n')
 (bin/'getprop').write_text('#!/bin/sh\ncase "$1" in ro.product.model) echo "${MODEL:-NX809J}";; ro.build.version.sdk) echo 36;; ro.build.fingerprint) cat "'+str(module/'ui-fingerprint')+'";; esac\n')
 (bin/'nohup').write_text('#!/bin/sh\nexit 0\n')
 (bin/'cmd').write_text('''#!/usr/bin/env python3
import json,os,sys
from pathlib import Path
p=Path(os.environ['ACTIVE']);active=json.loads(p.read_text());args=sys.argv[1:];op=args[1];themes='espresso snow vivid pacleggers amalgamation illusions immensity nights solarized'.split();targets='framework vendor settings systemui'.split();prefix='com.elitedarkkaiser.nubialy.uistyle.'
if op=='list':
 print('[x] oem.keep')
 for theme in themes:
  for target in targets:
   name=prefix+theme+'.'+target;print(('[x] ' if name in active else '[ ] ')+name)
elif op=='set-priority':pass
elif op=='lookup':print('#ff101014')
elif op in ['enable','disable']:
 name=args[-1]
 if not name.startswith(prefix):raise SystemExit('foreign mutation')
 if op=='enable' and os.environ.get('FAIL') and '.vivid.' in name and name.endswith('.'+os.environ['FAIL']):raise SystemExit(1)
 if op=='enable' and name not in active:active.append(name)
 if op=='disable' and name in active:active.remove(name)
 p.write_text(json.dumps(active))
else:raise SystemExit(2)
''')
 for p in bin.iterdir():p.chmod(0o755)
 env={**os.environ,'PATH':str(bin)+':'+os.environ['PATH'],'ACTIVE':str(active)}
 def run(op,*args,ok=True,extra=None):
  r=subprocess.run(['sh',str(module/'uictl.sh'),op,*args],env={**env,**(extra or {})},capture_output=True,text=True)
  assert (r.returncode==0)==ok,(op,r.returncode,r.stdout,r.stderr);return r
 def own():return [n for n in json.loads(active.read_text()) if n!='oem.keep']
 run('status');run('preview_nights');assert len(own())==4
 token=(state/'pending').read_text().strip();run('expire','stale');assert len(own())==4
 run('confirm');run('expire',token);assert len(own())==4
 run('preview_vivid',ok=False,extra={'FAIL':'settings'});assert set(own())=={'com.elitedarkkaiser.nubialy.uistyle.nights.'+t for t in ['framework','vendor','settings','systemui']}
 run('preview_espresso');token=(state/'pending').read_text().strip();run('expire',token);assert not own()
 run('preview_nights',ok=False,extra={'MODEL':'NX789J'});assert not own()
 native.write_bytes(b'updated');run('preview_nights',ok=False);assert not own();native.write_bytes(b'native')
 run('preview_nights');run('boot');assert not own()
 run('preview_nights');run('restore');assert not own()
 run('preview_nights;id',ok=False);run('preview_../nights',ok=False);assert json.loads(active.read_text())==['oem.keep']
 state.joinpath('pending').symlink_to(w/'foreign');run('restore',ok=False);assert not (w/'foreign').exists()
print('UI guard: scoped apply, partial failure rollback, stale/confirmed expiry, boot reset, profile/hash rejection and symlink refusal passed')
