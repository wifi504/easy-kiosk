"""Verify every packaged native library really matches the requested Android ABI."""
import os, sys, zipfile
if os.environ.get('GITHUB_ACTIONS') != 'true':
    raise SystemExit('APK verification must run in Actions')
apk, abi = sys.argv[1:]
elf_class, machine = {'armeabi-v7a': (1, 40), 'arm64-v8a': (2, 183), 'x86': (1, 3), 'x86_64': (2, 62)}[abi]
count = 0
with zipfile.ZipFile(apk) as archive:
    for name in archive.namelist():
        if not name.startswith('lib/') or not name.endswith('.so'): continue
        if name.split('/')[1] != abi:
            raise SystemExit('Unexpected packaged architecture: ' + name)
        with archive.open(name) as stream: header = stream.read(20)
        if len(header) < 20 or header[:4] != b'\x7fELF' or header[4] != elf_class or header[5] != 1 or int.from_bytes(header[18:20], 'little') != machine:
            raise SystemExit('Native library ELF architecture mismatch: ' + name)
        count += 1
if not count: raise SystemExit('APK contains no native libraries')
print(f'APK_ABI_OK abi={abi} bits={elf_class * 32} libraries={count}')
