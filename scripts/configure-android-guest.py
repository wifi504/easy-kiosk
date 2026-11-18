"""Set the physical CI AVD display before boot so wm overrides are not size-clamped."""
from pathlib import Path
import os
if os.environ.get('GITHUB_ACTIONS') != 'true': raise SystemExit('AVD configuration must run in Actions')
avd_home = Path(os.environ.get('ANDROID_AVD_HOME', str(Path.home() / '.android/avd')))
config = avd_home / 'test.avd/config.ini'
values = {'hw.lcd.width': '1080', 'hw.lcd.height': '1920', 'hw.lcd.density': '144', 'hw.ramSize': '1024', 'vm.heapSize': '256'}
lines = config.read_text().splitlines()
lines = [line for line in lines if line.split('=', 1)[0].strip() not in values]
config.write_text('\n'.join(lines + [f'{key}={value}' for key, value in values.items()]) + '\n')
print('Configured physical AVD hardware:', values)