"""Production loader in a temporary namespace. Mount/stat are emulated; stock sentinels are never changed."""
from pathlib import Path
import os,subprocess,tempfile,hashlib,json,re,shutil
ROOT=Path(__file__).resolve().parents[1]
catalog=json.loads((ROOT/'third_party/rising-bootanimations/catalog.json').read_text())
production=(ROOT/'recovery-module/bootctl.sh').read_text()
for e in catalog['animations']:
 assert f"bytes={e['bytes']}; hash={e['sha256']}" in production
 assert e['sha256'] in (ROOT/'app/src/main/java/com/elitedarkkaiser/nubialy/BootAnimations.java').read_text()
with tempfile.TemporaryDirectory(prefix='nubialy-boot-overlay-') as directory:
 w=Path(directory);module=w/'module';module.mkdir();state=w/'state';media=w/'media';media.mkdir();bin=w/'bin';bin.mkdir();ksu=w/'ksu';ksu.mkdir();player=w/'player';player.write_bytes(b'known-player');stock=media/'bootanimation.zip';stock.write_bytes(b'original-stock-untouched');staged=w/'app'/'boot-stage.zip';staged.parent.mkdir();boot_id=w/'boot-id';boot_id.write_text('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\n');mountinfo=w/'mountinfo';mountinfo.write_text('');marker=w/'active';calls=w/'mount-calls';payload=b'pinned-archive-content';digest=hashlib.sha256(payload).hexdigest();original=stock.read_bytes()
 text=production.replace('/data/adb/nubia_ly_guard',str(state)).replace('/data/adb/ksu',str(ksu)).replace('/system/lib64/libbootanimation.so',str(player)).replace('/system/media',str(media)).replace('/proc/sys/kernel/random/boot_id',str(boot_id)).replace('/proc/1/mountinfo',str(mountinfo)).replace('/proc/1/root','').replace('/data/user/0/com.elitedarkkaiser/nubialy', '/unused')
 for name in ['/data/user/0/com.elitedarkkaiser.nubialy/files/boot-stage.zip','/data/data/com.elitedarkkaiser.nubialy/files/boot-stage.zip']:text=text.replace(name,str(staged))
 text=text.replace('8a254699242729e1af694850415b60a8c98201fae8f3aec5c17ff188d2e12d55',hashlib.sha256(player.read_bytes()).hexdigest());text=re.sub(r'bytes=\d+; hash=[a-f0-9]{64}',f'bytes={len(payload)}; hash={digest}',text)
 for fallback in ['/apex/com.android.bootanimation/etc','/oem/media','/product/media']:text=text.replace(fallback,str(w/fallback.strip('/')))
 script=module/'bootctl.sh';script.write_text(text)
 (bin/'getprop').write_text('''#!/bin/sh
case "$1" in ro.product.model) echo "${MODEL:-NX809J}";;ro.build.version.sdk) echo "${SDK:-36}";;ro.boot.theme) echo "${THEME:-}";;ro.product.bootanim.file) echo "${PRODUCT_FILE:-}";;sys.boot_completed) echo "${COMPLETE:-0}";;persist.vendor.custom.variant.id) echo "${VARIANT:-}";;persist.sys.customanim.boot) echo "${CUSTOM:-}";;persist.vendor.sys.fansanim.boot) echo "${FANS:-0}";;esac
''')
 (bin/'id').write_text('#!/bin/sh\necho 0\n')
 realstat=shutil.which('stat')
 (bin/'stat').write_text(f'''#!/usr/bin/env python3
import os,sys,json,pathlib
p=pathlib.Path(os.environ['MARKER'])
if sys.argv[1:3]==['-c','%d:%i'] and p.exists() and os.environ.get('NS_MISS')!='1':
 m=json.loads(p.read_text())
 if sys.argv[-1]==m['target']:print(m['inode']);sys.exit()
os.execv({realstat!r},[{realstat!r}]+sys.argv[1:])
''')
 (bin/'mount').write_text('''#!/usr/bin/env python3
import sys,os,pathlib,json
args=sys.argv[1:]
assert args[:2]==['-o','bind,dev=KSU']
assert args[2]==os.environ['BOOT_PAYLOAD']
assert pathlib.Path(args[3]).name=='bootanimation.zip'
if os.environ.get('MOUNT_FAIL')=='1':sys.exit(1)
s=os.stat(args[2]);pathlib.Path(os.environ['MARKER']).write_text(json.dumps(dict(target=args[3],inode=f'{s.st_dev}:{s.st_ino}')))
with open(os.environ['CALLS'],'a') as f:f.write(args[3]+'\\n')
''')
 (bin/'ls').write_text('#!/bin/sh\nif [ "$1" = -Zd ]; then echo "u:object_r:system_file:s0 $2";else /bin/ls "$@";fi\n')
 (bin/'chcon').write_text('#!/bin/sh\nexit 0\n')
 for p in bin.iterdir():p.chmod(0o755)
 env=dict(os.environ,PATH=str(bin)+':'+os.environ['PATH'],MARKER=str(marker),CALLS=str(calls),BOOT_PAYLOAD=str(state/'boot/payload.zip'))
 def run(command,ok=True,extra=None,source=script):
  args=[command]
  if command.startswith('stage_'):args.append(str(int(float(Path('/proc/uptime').read_text().split()[0]))+15))
  r=subprocess.run(['sh',str(source),*args],env=dict(env,**(extra or {})),capture_output=True,text=True,timeout=5)
  assert (r.returncode==0)==ok,(command,r.returncode,r.stdout,r.stderr)
  assert stock.read_bytes()==original
  return r.stdout
 assert 'selection=stock' in run('status');assert not state.exists()
 run('stage_rising',False);assert not (state/'boot/selection').exists()
 staged.write_bytes(b'wrong');run('stage_rising',False);assert not (state/'boot/selection').exists()
 staged.write_bytes(payload)
 for entry in catalog['animations']:
  run('stage_'+entry['id']);assert (state/'boot/selection').read_text().strip()==entry['id'];assert (state/'boot/payload.zip').read_bytes()==payload
 assert not calls.exists()
 run('mount');assert len(calls.read_text().splitlines())==1;assert (state/'boot/attempt').exists();assert 'mounted=google_monet' in run('status')
 run('confirm',extra={'COMPLETE':'1'});assert not (state/'boot/attempt').exists()
 # Restage while a prior inode remains bound; original baseline must survive.
 old_baseline=(state/'boot/baseline').read_text();run('stage_rising');assert (state/'boot/baseline').read_text()==old_baseline
 run('restore');assert not (state/'boot/selection').exists();assert 'mounted=google_monet' in run('status')
 run('stage_rising');assert (state/'boot/baseline').read_text()==old_baseline
 marker.unlink();boot_id.write_text('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb\n');run('mount');assert len(calls.read_text().splitlines())==2
 # No completed-boot acknowledgement: following boot disarms without mounting.
 marker.unlink();boot_id.write_text('cccccccc-cccc-cccc-cccc-cccccccccccc\n');run('mount');assert len(calls.read_text().splitlines())==2;assert not (state/'boot/selection').exists()
 run('stage_rising');(module/'disable').touch();run('mount',False);assert 'profile=blocked' in run('status');run('restore');(module/'disable').unlink()
 run('stage_rising');run('mount',False,extra={'COMPLETE':'1'});run('restore')
 run('stage_rising');run('mount',False,extra={'MOUNT_FAIL':'1'});assert not (state/'boot/attempt').exists();assert 'result=mount_failed' in run('status');run('restore')
 run('stage_rising');run('mount',False,extra={'NS_MISS':'1'});assert not (state/'boot/selection').exists();assert 'result=unverified' in run('status');run('restore');marker.unlink()
 # Missing carrier archive uses generic only with no competing fallback.
 assert 'profile=ready' in run('status',extra={'VARIANT':'GEN_NON_EEA'})
 run('stage_rising',extra={'VARIANT':'GEN_NON_EEA'});assert str(stock) in (state/'boot/baseline').read_text();run('restore')
 assert 'profile=blocked' in run('status',extra={'VARIANT':'GEN_NON_EEA','THEME':'1'})
 assert 'profile=blocked' in run('status',extra={'VARIANT':'GEN_NON_EEA','PRODUCT_FILE':'custom.zip'})
 competitor=w/'product/media/bootanimation.zip';competitor.parent.mkdir(parents=True);competitor.write_bytes(b'competing')
 assert 'profile=blocked' in run('status',extra={'VARIANT':'GEN_NON_EEA'});competitor.unlink()
 dark=media/'bootanimation-dark.zip';dark.write_bytes(b'dark')
 assert 'profile=blocked' in run('status',extra={'VARIANT':'GEN_NON_EEA'});dark.unlink()
 # Native variant resolution is bounded; arbitrary paths/custom/fans are excluded.
 variant=media/'JD_ID';variant.mkdir();variant_stock=variant/'bootanimation.zip';variant_stock.write_bytes(b'original-variant')
 run('stage_rising',extra={'VARIANT':'JD_ID'});assert str(variant_stock) in (state/'boot/baseline').read_text()
 # At post-fs-data persist props may not yet exist; stage-time verified path is retained.
 run('mount');assert calls.read_text().splitlines()[-1]==str(variant_stock);assert variant_stock.read_bytes()==b'original-variant'
 run('restore');marker.unlink();run('stage_rising',False,extra={'VARIANT':'../other'});run('stage_rising',False,extra={'CUSTOM':str(stock)});run('stage_rising',False,extra={'FANS':'1'})
 player.write_bytes(b'changed-player');run('stage_rising',False);assert 'profile=blocked' in run('status');run('restore');player.write_bytes(b'known-player')
 # Exact boot-player allowlists are scoped by model and SDK; wildcard files never bypass NX809J.
 allowlist=module/'player_sha';player.write_bytes(b'new-player');new_hash=hashlib.sha256(player.read_bytes()).hexdigest()
 for unsafe in ['ANY\n','NX809J 36 ANY\n',new_hash+'\n','NX789J 35 '+new_hash+'\n','NX809J 35 '+new_hash+'\n']:
  allowlist.write_text(unsafe);allowlist.chmod(0o644);assert 'profile=blocked' in run('status')
 allowlist.write_text('NX809J 36 '+new_hash+'\n');assert 'profile=ready' in run('status')
 allowlist.chmod(0o666);assert 'profile=blocked' in run('status');allowlist.chmod(0o644)
 allowlist.write_text('NX789J 35 '+new_hash+'\n');assert 'profile=ready' in run('status',extra={'MODEL':'NX789J','SDK':'35'});assert 'profile=blocked' in run('status',extra={'MODEL':'NX789J','SDK':'36'})
 allowlist.write_text('NX789J 36 '+new_hash+'\n');assert 'profile=ready' in run('status',extra={'MODEL':'NX789J','SDK':'36'});assert 'profile=blocked' in run('status',extra={'MODEL':'NX789J','SDK':'35'})
 allowlist.unlink();player.write_bytes(b'known-player');assert 'profile=ready' in run('status');assert 'profile=blocked' in run('status',extra={'SDK':'35'})
 # Corrupted metadata must remain independently removable.
 (state/'boot/selection').write_text('garbage\n');os.chmod(state/'boot/selection',0o600);run('restore');assert not (state/'boot/selection').exists()
 # Busy restore never claims success or races a writer; old clock stop still works.
 run('stage_rising');lock=state/'boot/lock';lock.mkdir(mode=0o700);(lock/'pid').write_text(str(os.getpid()));os.chmod(lock/'pid',0o600);(lock/'boot').write_text(boot_id.read_text());os.chmod(lock/'boot',0o600);run('restore',False);assert (state/'boot/selection').exists()
 guard=w/'guard';guard.mkdir(mode=0o755);(state/'clock_id').write_text('enabled');os.chmod(state/'clock_id',0o600)
 guard_script=module/'guardctl.sh';guard_script.write_text((ROOT/'recovery-module/guardctl.sh').read_text().replace('/data/adb/nubia_ly_guard',str(state)).replace('/data/local/tmp/nubia-ly-guard',str(guard)))
 run('stop',False,source=guard_script);assert not (state/'clock_id').exists()
 (lock/'pid').unlink();(lock/'boot').unlink();lock.rmdir();run('restore')
 # Our state symlink cannot redirect writes/deletes into unrelated storage.
 old=state/'boot-old';(state/'boot').rename(old);sentinel=w/'sentinel';sentinel.mkdir();(sentinel/'selection').write_text('preserve');(state/'boot').symlink_to(sentinel,target_is_directory=True);run('restore',False);assert (sentinel/'selection').read_text()=='preserve'
print('Boot overlay passed: pinned staging, exact target, no live apply, unchanged stock, current-inode ownership, confirmation, incomplete-boot rollback, restore/disable/failure/variant/identity/symlink/busy fences')
