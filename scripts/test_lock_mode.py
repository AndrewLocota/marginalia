"""Test lock-only mode after enabling it in the UI. Disposable emulator only.

Requires an installed build, card_lock_only + quote_card enabled, and notification
permission. Grants/revokes Notification Access on THIS TEST EMULATOR only.
"""
import os,subprocess,time,re,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SERIAL=os.environ['ANDROID_SERIAL']
assert SERIAL.startswith('emulator-'),'Never run on a personal phone'
ADB=str(Path.home()/'Library/Android/sdk/platform-tools/adb')
def adb(*args):return subprocess.check_output([ADB,'-s',SERIAL,*args],text=True).strip()
assert adb('shell','getprop','ro.kernel.qemu')=='1'
COMPONENT='app.marginalia/app.marginalia.LockScreenMonitor'
report={'sdk':adb('shell','getprop','ro.build.version.sdk'),'release':adb('shell','getprop','ro.build.version.release'),'checks':[]}
def visible():return '|app.marginalia|7493|' in adb('shell','cmd','notification','list')
def expect(state,label):
 start=time.monotonic()
 while time.monotonic()-start<12:
  if visible()==state:
   report['checks'].append({'check':label,'passed':True,'observed_after_seconds':round(time.monotonic()-start,3)});print('PASS',label,flush=True);return
  time.sleep(.15)
 raise AssertionError(label)
def wallpaper():return re.findall(r'User \d+: id=\d+|mWallpaperComponent=.*',adb('shell','dumpsys','wallpaper'))
def lock():
 adb('shell','input','keyevent','223');time.sleep(1);adb('shell','input','keyevent','224');time.sleep(2)
 assert 'showing=true' in adb('shell','dumpsys','window','policy'),'Keyguard did not appear'
def unlock():
 adb('shell','wm','dismiss-keyguard');time.sleep(1)
 assert 'showing=false' in adb('shell','dumpsys','window','policy'),'Keyguard did not dismiss'
before=wallpaper()
adb('shell','locksettings','set-disabled','false')
adb('shell','cmd','notification','allow_listener',COMPONENT)
unlock();expect(False,'No quote notification while unlocked')
for i in range(3):
 lock();expect(True,f'Quote appears on lock {i+1}');time.sleep(3);unlock();expect(False,f'Quote removed on unlock {i+1}')
lock();expect(True,'Quote present before access revocation')
adb('shell','cmd','notification','disallow_listener',COMPONENT);expect(False,'Revoking access removes quote')
unlock();lock();expect(False,'No quote without special access')
adb('shell','cmd','notification','allow_listener',COMPONENT);expect(True,'Granting access restores quote while locked')
unlock();expect(False,'No residual service notification from Marginalia')
assert not any('|app.marginalia|' in x for x in adb('shell','cmd','notification','list').splitlines())
assert before==wallpaper(),'Wallpaper changed'
report['wallpaper_ids_and_components_unchanged']=True
out=ROOT/'.tools'/('lock-mode-api'+report['sdk']+'.json');out.write_text(json.dumps(report,indent=2)+'\n');print('Saved',out)
