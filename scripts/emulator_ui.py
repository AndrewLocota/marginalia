"""Small adb UI helper. Use on the disposable emulator only, never a real phone."""
from pathlib import Path
import os, subprocess, re, xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ADB = os.environ.get('ADB', str(Path(os.environ.get('ANDROID_HOME', str(Path.home()/'Library/Android/sdk')))/'platform-tools/adb'))
SERIAL = os.environ.get('ANDROID_SERIAL', 'emulator-5554')

def adb(*args):
    return subprocess.check_output([ADB, '-s', SERIAL, *args])

def dump():
    output=adb('shell', 'uiautomator', 'dump', '/sdcard/marginalia-ui.xml')
    if b'dumped to' not in output:
        raise RuntimeError('UI dump failed; refusing to use a stale hierarchy: '+output.decode(errors='replace'))
    return ET.fromstring(adb('shell', 'cat', '/sdcard/marginalia-ui.xml'))

def click(value):
    root=dump()
    nodes=[n for n in root.iter('node') if value in (n.get('text'),n.get('content-desc'))]
    assert nodes, 'Visible control not found: '+value
    n=next((n for n in nodes if n.get('clickable')=='true'),nodes[0])
    a,b,c,d=map(int,re.findall(r'\d+',n.get('bounds')))
    adb('shell','input','tap',str((a+c)//2),str((b+d)//2))
    dump()

def screenshot(name):
    dump()
    (ROOT/'deliverables'/name).write_bytes(adb('exec-out','screencap','-p'))

def visible():
    for n in dump().iter('node'):
        if n.get('text') or n.get('content-desc'):
            print(n.get('text') or n.get('content-desc'), n.get('bounds'))
