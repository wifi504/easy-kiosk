"""Actions-only installed native/assets packaging. Host and engine DEX remain independent."""
from pathlib import Path
import os, sys, json, subprocess, tempfile, zipfile, re
if os.environ.get('GITHUB_ACTIONS') != 'true': raise SystemExit('APK assembly must run in Actions')
root = Path(__file__).resolve().parents[1]
apk = Path(sys.argv[1]).resolve()
assets = Path(sys.argv[2]) if len(sys.argv) > 2 else root / 'android/app/src/main/assets'
engine = assets / 'embedded-chromium.apk'
meta = json.loads((assets / 'embedded-chromium.json').read_text())
abi = meta['abi']
tools = Path(os.environ['ANDROID_HOME']) / 'build-tools/36.0.0'
badging = subprocess.check_output([str(tools / 'aapt'), 'dump', 'badging', str(apk)], text=True)
host_package = re.search(r"package: name='([^']+)'", badging).group(1)
with tempfile.TemporaryDirectory() as temp:
    temp = Path(temp); merged = temp / 'merged.apk'
    with zipfile.ZipFile(apk) as host, zipfile.ZipFile(engine) as browser, zipfile.ZipFile(merged, 'w', compression=zipfile.ZIP_DEFLATED) as out:
        for item in host.infolist():
            upper = item.filename.upper()
            signature = upper == 'META-INF/MANIFEST.MF' or (upper.startswith('META-INF/') and upper.endswith(('.SF', '.RSA', '.DSA', '.EC')))
            if not signature: out.writestr(item, host.read(item.filename))
        names = set(out.namelist())
        for item in browser.infolist():
            if (item.filename.startswith(f'lib/{abi}/') or item.filename.startswith('assets/')) and item.filename not in names:
                data = browser.read(item.filename)
                out.writestr(item, data)
                names.add(item.filename)
                suffix = '+com.android.webview+'
                if item.filename.startswith('assets/') and item.filename.endswith(suffix):
                    bare = item.filename[:-len(suffix)]
                    for alias in (bare, bare + '+' + host_package + '+'):
                        if alias not in names:
                            out.writestr(alias, data, compress_type=zipfile.ZIP_STORED); names.add(alias)
    subprocess.run([str(tools / 'zipalign'), '-P', '16', '-f', '4', str(merged), str(apk)], check=True)
# Only the public test key exists in build/test jobs. Formal signing runs on a fresh VM.
subprocess.run([str(tools / 'apksigner'), 'sign', '--v4-signing-enabled', 'false', '--ks', str(root / 'android/app/debug.keystore'),
    '--ks-pass', 'pass:android', '--key-pass', 'pass:android', '--ks-key-alias', 'androiddebugkey', str(apk)], check=True)
subprocess.run([str(tools / 'apksigner'), 'verify', '--verbose', str(apk)], check=True)
print(f'Packaged independent Chromium {meta["version"]} for {abi}; host DEX unchanged; public CI signature verified.')