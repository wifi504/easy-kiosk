"""Actions-only engine merge and public CI test signature; deployment signing is isolated."""
from pathlib import Path
import os, sys, json, subprocess, tempfile, zipfile
if os.environ.get('GITHUB_ACTIONS') != 'true': raise SystemExit('APK assembly must run in Actions')
root = Path(__file__).resolve().parents[1]
apk = Path(sys.argv[1]).resolve()
assets = root / 'android/app/src/main/assets'
engine = assets / 'embedded-chromium.apk'
meta = json.loads((assets / 'embedded-chromium.json').read_text())
abi = meta['abi']
tools = Path(os.environ['ANDROID_HOME']) / 'build-tools/36.0.0'
subprocess.run([str(root / 'android/gradlew'), '-p', str(root / 'tools/engine-dex-tools'), '--no-daemon', 'installDist'], check=True)
with tempfile.TemporaryDirectory() as temp:
    temp = Path(temp)
    inputs = []
    for label, source in [('host', apk), ('engine', engine)]:
        with zipfile.ZipFile(source) as archive:
            for name in archive.namelist():
                if name.startswith('classes') and name.endswith('.dex') and '/' not in name:
                    p = temp / (label + '-' + name); p.write_bytes(archive.read(name)); inputs.append(str(p))
    filtered = temp / 'filtered'
    report = apk.with_suffix('.shared-runtime.txt')
    subprocess.run(['java', '-cp', str(root / 'tools/engine-dex-tools/build/install/EngineDexTools/lib/*'),
                    'com.wifi504.easykiosk.tools.FilterEngineDex', str(temp), str(filtered), str(report)], check=True)
    inputs = [p for p in inputs if Path(p).name.startswith('host-')] + [str(p) for p in sorted(filtered.glob('*.dex'))]
    dex = temp / 'dex'; dex.mkdir()
    subprocess.run([str(tools / 'd8'), '--release', '--min-api', '29', '--output', str(dex), *inputs], check=True)
    merged = temp / 'merged.apk'
    with zipfile.ZipFile(apk) as host, zipfile.ZipFile(engine) as browser, zipfile.ZipFile(merged, 'w', compression=zipfile.ZIP_DEFLATED) as out:
        for item in host.infolist():
            upper = item.filename.upper()
            is_signature = upper == 'META-INF/MANIFEST.MF' or (upper.startswith('META-INF/') and upper.endswith(('.SF', '.RSA', '.DSA', '.EC')))
            if is_signature or (item.filename.startswith('classes') and item.filename.endswith('.dex') and '/' not in item.filename): continue
            out.writestr(item, host.read(item.filename))
        names = set(out.namelist())
        for item in browser.infolist():
            if (item.filename.startswith(f'lib/{abi}/') or item.filename.startswith('assets/')) and item.filename not in names:
                out.writestr(item, browser.read(item.filename))
        for p in sorted(dex.glob('*.dex')): out.writestr(p.name, p.read_bytes())
    subprocess.run([str(tools / 'zipalign'), '-P', '16', '-f', '4', str(merged), str(apk)], check=True)
# Public repository test key only. No production credentials are available to this job.
subprocess.run([str(tools / 'apksigner'), 'sign', '--ks', str(root / 'android/app/debug.keystore'),
                '--ks-pass', 'pass:android', '--key-pass', 'pass:android', '--ks-key-alias', 'androiddebugkey', str(apk)], check=True)
subprocess.run([str(tools / 'apksigner'), 'verify', '--verbose', '--print-certs', str(apk)], check=True)
print(f'Embedded Chromium {meta["version"]} for {abi}; CI test signature verified; deployment signing occurs in a separate isolated job.')
