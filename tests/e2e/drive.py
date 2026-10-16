import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

USAGE = 'usage: drive.py join <room> | drive.py wait <text-pattern> [seconds]'
ADB = os.environ.get('ADB', os.path.expanduser('~/Android/sdk/platform-tools/adb'))


def adb(*args):
    return subprocess.run([ADB, *args], capture_output=True, text=True, check=True).stdout


def nodes():
    try:
        adb('shell', 'uiautomator', 'dump', '/sdcard/call.xml')
        return list(ET.fromstring(adb('shell', 'cat', '/sdcard/call.xml')).iter('node'))
    except (subprocess.CalledProcessError, ET.ParseError):
        return []


def center(node):
    left, top, right, bottom = map(int, re.findall(r'\d+', node.get('bounds')))
    return str((left + right) // 2), str((top + bottom) // 2)


def find(predicate, timeout):
    for _ in range(timeout):
        match = next((n for n in nodes() if predicate(n)), None)
        if match is not None:
            return match
        time.sleep(1)
    return None


def wait(pattern, timeout=60):
    match = find(lambda n: re.fullmatch(pattern, n.get('text') or ''), timeout)
    if match is None:
        texts = [n.get('text') for n in nodes() if n.get('class') == 'android.widget.TextView' and n.get('text')]
        sys.exit(f'FAIL waiting for /{pattern}/, screen shows {texts}')
    print(f'ok   {match.get("text")}')


def join(room):
    box = find(lambda n: n.get('class') == 'android.widget.EditText', 30)
    if box is None:
        sys.exit('FAIL the room box never appeared')
    adb('shell', 'input', 'tap', *center(box))
    adb('shell', 'input', 'text', room)
    adb('shell', 'input', 'tap', *center(find(lambda n: n.get('text') == 'Join', 10)))
    wait(r'Waiting for someone to join room .*')


if __name__ == '__main__':
    command, args = sys.argv[1] if len(sys.argv) > 1 else '', sys.argv[2:]
    if command == 'join' and len(args) == 1:
        join(args[0])
    elif command == 'wait' and len(args) in (1, 2):
        wait(args[0], *map(int, args[1:]))
    else:
        sys.exit(USAGE)
