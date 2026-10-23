from pathlib import Path
from lxml import etree as E
import json,base64,copy,subprocess,shutil,re,hashlib
import argparse
parser=argparse.ArgumentParser();parser.add_argument('--input-dir',type=Path,required=True);parser.add_argument('--aapt2',required=True);args=parser.parse_args()
r=Path(__file__).resolve().parents[1];inputs=args.input_dir;src=json.loads((r/'ui-styles/upstream-resources.json').read_text());up=r/'build/ui-styles/upstream';out=r/'build/ui-styles/ported';compiled=r/'build/ui-styles/compiled';shutil.rmtree(r/'build/ui-styles',ignore_errors=True);compiled.mkdir(parents=True,exist_ok=True)
profiles=[('espresso','AndroidEspressoTheme','Espresso'),('snow','AndroidSnowPaintDropTheme','Snow Paint Drop'),('vivid','AndroidVividTheme','Vivid'),('pacleggers','PacleggersTheme','Pacleggers'),('amalgamation','ShishuAmalgamationTheme','Shishu Amalgamation'),('illusions','ShishuIllusionsTheme','Shishu Illusions'),('immensity','ShishuImmensityTheme','Shishu Immensity'),('nights','ShishuNightsTheme','Shishu Nights'),('solarized','SolarizedDarkTheme','Solarized Dark')]
ns='http://schemas.android.com/apk/res/android';E.register_namespace('android',ns)
files={'framework':'framework-res.apk','vendor':'framework-zte-res.apk','settings':'Settings_MFV_abroad.apk','systemui':'SystemUI_MFV.apk'};pkgs={'framework':'android','vendor':'androidzte','settings':'com.android.settings','systemui':'com.android.systemui'}
from zipfile import ZipFile
from androguard.core.axml import ARSCParser
from loguru import logger
logger.remove();public={}
for k,v in files.items():
 with ZipFile(inputs/v) as archive:table=ARSCParser(archive.read('resources.arsc'))
 public[k]={(e.get('type'),e.get('name')) for e in E.fromstring(table.get_public_resources(table.get_packages_names()[0]))}
for f in src:
 if f['path'].endswith('AndroidManifest.xml'):continue
 rel=f['path'].split('themes/UiStyles/')[1];p=up/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(base64.b64decode(f['content']) if f.get('encoding')=='base64' else f['content'].encode())
meta=[]
for slug,theme,title in profiles:
 td=up/theme;android=td/'android/res' if (td/'android').exists() else td/'res';palette={}
 for mode in ['values','values-night']:
  p=android/mode/'colors.xml'
  if p.exists():palette[mode]={e.get('name'):e.text for e in E.parse(str(p)).getroot() if isinstance(e.tag,str) and e.tag=='color'}
 for target in files:
  dest=out/slug/target;res=dest/'res';res.mkdir(parents=True,exist_ok=True)
  original=android if target=='framework' else td/target/'res'
  if target!='vendor' and original.exists():shutil.copytree(original,res,dirs_exist_ok=True)
  def values(mode):
   p=res/mode/'nubia.xml';p.parent.mkdir(exist_ok=True);return p,E.Element('resources')
  def add(root,typ,name,val):E.SubElement(root,typ,name=name).text=val
  for mode in ['values','values-night']:
   colors=palette.get(mode,palette.get('values',{}));night=mode.endswith('night');suffix='dark' if night else 'light';surface=colors.get('background_device_default_'+suffix,'@*android:color/system_neutral1_'+('900' if night else '50'));card='@*android:color/system_neutral1_'+('800' if night else '100');text='@*android:color/system_neutral1_'+('50' if night else '900')
   p,v=values(mode)
   if target=='framework':
    for typ,name in sorted(public[target]):
     if typ=='color' and (name.startswith(('system_surface','system_background')) and name.endswith('_'+suffix)):
      add(v,'color',name,text if '_on_' in name else surface)
   if target=='vendor':
    for typ,name in sorted(public[target]):
     if typ=='color' and name.startswith('zte_materialColorSurface'):add(v,'color',name,card if 'Container' in name else surface)
     elif typ=='color' and name.startswith('zte_materialColorOnSurface'):add(v,'color',name,text)
   if target=='settings':
    for name,val in {'mfvc_bg_color':surface,'mfvc_bg_color_dark':surface,'mfvc_card_bg_color':surface,'mfvc_card_bg_color_dark':surface,'mfv_common_list_primary_txt':text}.items():
     if ('color',name) in public[target]:add(v,'color',name,val)
    sourcecolors={}
    for x in [td/'settings/res/values/colors.xml',td/('settings/res/'+mode+'/colors.xml')]:
     if x.exists():sourcecolors.update({e.get('name'):e.text for e in E.parse(str(x)).getroot() if isinstance(e.tag,str) and e.tag=='color'})
    for typ,name in sorted(public[target]):
     if typ=='color' and name.startswith('mfv_common_card_') and 'pressed' not in name:add(v,'color',name,sourcecolors.get('mtx_pref_color',card))
    # Preserve each native parent instead of transplanting an AOSP Settings hierarchy.
    if theme in ['ShishuNightsTheme','ShishuImmensityTheme','ShishuIllusionsTheme']:
     for name,parent in [('Theme.Settings','Theme.SettingsBaseZte'),('Theme.SettingsSinkHome','Theme.SettingsSink')]:
      st=E.SubElement(v,'style',name=name,parent='@*com.android.settings:style/'+parent)
      for key,val in [('windowShowWallpaper','true'),('windowIsTranslucent','true'),('windowBackground',surface),('colorBackground',surface)]:E.SubElement(st,'item',name='android:'+key).text=val
   if target=='systemui':
    # Upstream resources named volume_bg and its variants do not drive Nubia volume surfaces.
    sourcecolors={}
    for x in [td/'systemui/res/values/colors.xml',td/('systemui/res/'+mode+'/colors.xml')]:
     if x.exists():sourcecolors.update({e.get('name'):e.text for e in E.parse(str(x)).getroot() if isinstance(e.tag,str) and e.tag=='color'})
    for name in ['volume_dialog_background','volume_dialog_background_color','volume_dialog_background_color_above_blur','volume_dialog_background_color_no_gauss','zte_notification_material_background_color','zte_notification_material_background_dimmed_color']:
     if ('color',name) in public[target]:add(v,'color',name,sourcecolors.get('volume_bg',surface) if name.startswith('volume') else card)
   if len(v):p.write_bytes(E.tostring(v,pretty_print=True,xml_declaration=True,encoding='utf-8'))
  for xml in res.rglob('*.xml'):
   xml.write_text(xml.read_text().replace('@*android:color/bootleg_accent_gradient_end_color','@*android:color/system_accent1_400'))
  # Overlays keep private helper resources, but expose only resource names present on this target.
  targets=[]
  for f in res.rglob('*.xml'):
   tree=E.parse(str(f));root=tree.getroot()
   if root.tag=='resources':
    for e in root:
     if isinstance(e.tag,str) and e.get('name') and (e.tag,e.get('name')) in public[target]:targets.append((e.tag,e.get('name')))
   else:
    if f.parent.name.startswith(('drawable','color')) and (f.parent.name.split('-')[0],f.stem) in public[target]:targets.append((f.parent.name.split('-')[0],f.stem))
  # Every mapped public target uses resource-map, so missing upstream helpers are not silently treated as ports.
  maps=dest/'res/xml/overlays.xml';maps.parent.mkdir(exist_ok=True);m=E.Element('overlay')
  for typ,name in sorted(set(targets)):E.SubElement(m,'item',target=typ+'/'+name,value='@'+typ+'/'+name)
  maps.write_bytes(E.tostring(m,pretty_print=True))
  package='com.elitedarkkaiser.nubialy.uistyle.'+slug+'.'+target
  manifest=E.Element('manifest',nsmap={'android':ns},package=package);manifest.set('{'+ns+'}versionCode','1');manifest.set('{'+ns+'}versionName','0.1-test')
  E.SubElement(manifest,'uses-sdk',{'{'+ns+'}minSdkVersion':'35','{'+ns+'}targetSdkVersion':'28'})
  E.SubElement(manifest,'overlay',{'{'+ns+'}targetPackage':pkgs[target],'{'+ns+'}isStatic':'false','{'+ns+'}priority':'999','{'+ns+'}resourcesMap':'@xml/overlays'})
  E.SubElement(manifest,'application',{'{'+ns+'}hasCode':'false','{'+ns+'}label':'nubia-ly '+title+' '+target})
  (dest/'AndroidManifest.xml').write_bytes(E.tostring(manifest,pretty_print=True,xml_declaration=True,encoding='utf-8'))
  resourcezip=dest/'resources.zip';apk=compiled/(slug+'-'+target+'.apk')
  cmd=[args.aapt2,'compile','--dir',str(res),'-o',str(resourcezip)];subprocess.run(cmd,check=True,capture_output=True)
  cmd=[args.aapt2,'link','-o',str(apk),'--manifest',str(dest/'AndroidManifest.xml')]
  for lib in ['framework-res.apk','framework-zte-res.apk','framework-res-zpub.apk']+([files[target]] if target in ['settings','systemui'] else []):cmd+=['-I',str(inputs/lib)]
  cmd+=['--package-id','0x80','--no-resource-deduping','--no-resource-removal','--auto-add-overlay',str(resourcezip)]
  run=subprocess.run(cmd,text=True,capture_output=True)
  if run.returncode:print(slug,target,run.stderr);raise SystemExit(1)
  resourcezip.unlink();meta.append({'id':slug,'name':title,'target':target,'package':package,'apk':apk.name,'resources':len(set(targets)),'sha256':hashlib.sha256(apk.read_bytes()).hexdigest()});print(slug,target,len(set(targets)),flush=True)
(r/'build/ui-styles/catalog.json').write_text(json.dumps(meta,indent=2))
