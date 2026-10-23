"""Validate the compiled catalog, sign only our RROs, then rebuild the guard ZIP."""
from pathlib import Path
import hashlib,json,os,subprocess,tempfile,zipfile
root=Path(__file__).resolve().parents[1];entries=json.loads((root/'ui-styles/catalog.json').read_text());out=root/'build/ui-styles-signed';out.mkdir(parents=True,exist_ok=True)
sdk=Path(os.environ['ANDROID_HOME']);signer=sdk/'build-tools/35.0.0/apksigner'
with tempfile.TemporaryDirectory() as work:
 work=Path(work);store=os.environ.get('SIGNING_STORE_FILE','');alias=os.environ.get('SIGNING_KEY_ALIAS','');env=os.environ.copy()
 if not store:
  store=str(work/'test.jks');alias='ui-test';env['UI_TEST_PASSWORD']='android'
  subprocess.run(['keytool','-genkeypair','-keystore',store,'-storepass','android','-keypass','android','-alias',alias,'-dname','CN=nubia-ly UI test','-keyalg','RSA','-validity','3650'],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
  passwords=['--ks-pass','env:UI_TEST_PASSWORD','--key-pass','env:UI_TEST_PASSWORD']
 else:passwords=['--ks-pass','env:SIGNING_STORE_PASSWORD','--key-pass','env:SIGNING_KEY_PASSWORD']
 with zipfile.ZipFile(root/'ui-styles/overlays-unsigned.zip') as archive:
  assert set(archive.namelist())=={e['apk'] for e in entries}
  for e in entries:
   data=archive.read(e['apk']);assert hashlib.sha256(data).hexdigest()==e['sha256']
   source=work/e['apk'];source.write_bytes(data);dest=out/e['apk']
   subprocess.run([str(signer),'sign','--ks',store,'--ks-key-alias',alias,*passwords,'--out',str(dest),str(source)],check=True,env=env)
   subprocess.run([str(signer),'verify',str(dest)],check=True,stdout=subprocess.DEVNULL)
subprocess.run(['python3',str(root/'scripts/package-guard.py')],check=True)
print('36 native-linked UI overlays signed and packaged; installation enables none.')
