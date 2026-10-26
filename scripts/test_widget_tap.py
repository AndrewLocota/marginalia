"""Exercise pinned widget taps on a disposable emulator; records actual launcher motion."""
import sys,time,subprocess,re,json,os
import emulator_ui as ui
root=ui.ROOT
ui.adb('shell','input','keyevent','224')
ui.adb('shell','wm','dismiss-keyguard')
ui.adb('shell','input','keyevent','3')
nodes=[n for n in ui.dump().iter('node') if 'Tap for an author emoji burst.' in n.get('content-desc','') and n.get('clickable')=='true']
if not nodes:
    ui.adb('shell','input','swipe','950','1200','100','1200','300')
    nodes=[n for n in ui.dump().iter('node') if 'Tap for an author emoji burst.' in n.get('content-desc','') and n.get('clickable')=='true']
assert nodes,'No pinned burst widget found'
bounds=list(map(int,re.findall(r'\d+',nodes[0].get('bounds'))));x=(bounds[0]+bounds[2])//2;y=(bounds[1]+bounds[3])//2
version=os.environ.get('TEST_VERSION','1.7')
out=root/'deliverables'/('v'+version+'-tap-'+ui.SERIAL);out.mkdir(exist_ok=True)
(out/'before.png').write_bytes(ui.adb('exec-out','screencap','-p'))
ui.adb('logcat','-c')
record=subprocess.Popen([ui.ADB,'-s',ui.SERIAL,'shell','screenrecord','--time-limit','6','/sdcard/marginalia-v17-tap.mp4'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
time.sleep(1)
ui.adb('shell','input','tap',str(x),str(y))
time.sleep(.50)
(out/'burst.png').write_bytes(ui.adb('exec-out','screencap','-p'))
time.sleep(1.6)
top=ui.adb('shell','dumpsys','activity','activities').decode()
assert not re.search(r'mResumedActivity.*BurstActivity',top),'Burst stayed open'
ui.adb('shell','input','tap',str(x),str(y))
time.sleep(1.9)
record.wait(timeout=10)
ui.adb('pull','/sdcard/marginalia-v17-tap.mp4',str(out/'tap.mp4'))
logs=ui.adb('logcat','-d').decode(errors='replace');(out/'logcat.txt').write_text(logs)
assert 'FATAL EXCEPTION' not in logs,'Crash during widget tap'
assert 'BurstActivity' in logs,'Widget did not launch burst'
assert not re.search(r'mResumedActivity.*BurstActivity',ui.adb('shell','dumpsys','activity','activities').decode()),'Second burst stayed open'
(out/'after.png').write_bytes(ui.adb('exec-out','screencap','-p'))
(out/'result.json').write_text(json.dumps({'serial':ui.SERIAL,'bounds':bounds,'two_taps_return_to_launcher':True,'no_crash':True},indent=2)+'\n')
print(out)
