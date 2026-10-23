from pathlib import Path
import hashlib,json,subprocess,tempfile,zipfile,io,struct
root=Path(__file__).resolve().parents[1];entries=json.loads((root/'ui-styles/catalog.json').read_text())
assert len(entries)==36 and len({e['id'] for e in entries})==9
assert {e['target'] for e in entries}=={'framework','vendor','settings','systemui'}
with zipfile.ZipFile(root/'ui-styles/overlays-unsigned.zip') as archive:
 assert set(archive.namelist())=={e['apk'] for e in entries}
 for e in entries:
  data=archive.read(e['apk']);assert hashlib.sha256(data).hexdigest()==e['sha256'];assert e['resources']>0
  assert e['package']=='com.elitedarkkaiser.nubialy.uistyle.'+e['id']+'.'+e['target']
  with zipfile.ZipFile(io.BytesIO(data)) as apk:
   assert 'resources.arsc' in apk.namelist() and 'AndroidManifest.xml' in apk.namelist()
   assert not any(n.endswith('.dex') for n in apk.namelist())
   table=apk.read('resources.arsc');offset=struct.unpack_from('<H',table,2)[0];found=False
   while offset<len(table):
    typ,header,size=struct.unpack_from('<HHI',table,offset)
    if typ==0x200:assert struct.unpack_from('<I',table,offset+8)[0]==0x80;found=True
    offset+=size
   assert found

with tempfile.TemporaryDirectory() as w:
 w=Path(w);p=w/'com/elitedarkkaiser/nubialy';p.mkdir(parents=True)
 (p/'Contract.java').write_text('''package com.elitedarkkaiser.nubialy;class Contract {public static void main(String[] x){for(var e:UiStyles.ENTRIES){if(!UiStyles.command("preview_"+e.id()))throw new AssertionError();}for(String bad:new String[]{null,"preview_../nights","preview_nights;id","boot","expire","preview_","preview_NIGHTS"})if(UiStyles.command(bad))throw new AssertionError();if(!UiStyles.command("status")||!UiStyles.command("confirm")||!UiStyles.command("restore"))throw new AssertionError();}}''')
 subprocess.run(['java','com.sun.tools.javac.Main','-d',str(w),str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/UiStyles.java'),str(root/'app/src/main/java/com/elitedarkkaiser/nubialy/UiStyleCommand.java'),str(p/'Contract.java')],check=True)
 subprocess.run(['java','-cp',str(w),'com.elitedarkkaiser.nubialy.Contract'],check=True)
print('UI styles: 36 fixed native-linked overlays, four targets, source hashes and command rejection passed')
