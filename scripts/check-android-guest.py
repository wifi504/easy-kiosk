"""Validate the Android guest's actual hardware, not just the emulator's requested settings."""
from pathlib import Path
import os, sys, json, re, subprocess
if os.environ.get('GITHUB_ACTIONS') != 'true': raise SystemExit('Guest checks must run in Actions')
def adb(*args): return subprocess.check_output(['adb', 'shell', *args], text=True).strip()
version, sdk = adb('getprop', 'ro.build.version.release'), adb('getprop', 'ro.build.version.sdk')
memory = adb('cat', '/proc/meminfo')
mem_kb = int(re.search(r'^MemTotal:\s+(\d+)', memory, re.M).group(1))
disk = adb('df', '-k', '/data'); disk_kb = int(disk.splitlines()[-1].split()[1])
size, density = adb('wm', 'size'), adb('wm', 'density')
report = {'android': version, 'api': sdk, 'mem_total_kb': mem_kb, 'data_total_kb': disk_kb, 'resolution': size, 'density': density}
Path(sys.argv[1]).write_text(json.dumps(report, indent=2) + '\n')
print(json.dumps(report, indent=2))
if version != '10' or sdk != '29': raise SystemExit('Wrong Android guest version')
if not 786432 <= mem_kb <= 1048576: raise SystemExit('Guest must actually have 1GiB RAM minus kernel reservations')
if not 14 * 1024 * 1024 <= disk_kb <= 17 * 1024 * 1024: raise SystemExit('Guest data partition must match the requested 16GiB configuration')
if not size.endswith('1080x1920'): raise SystemExit('Wrong guest display dimensions')