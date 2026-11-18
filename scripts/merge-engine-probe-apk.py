"""CI-only DEX/native merge; re-signs only the disposable engine probe APK."""
from pathlib import Path
import zipfile, subprocess, os, tempfile
root = Path(__file__).resolve().parents[1]
build = root / 'tools/embedded-chromium-probe'
apk = build / 'app/build/outputs/apk/debug/app-debug.apk'
kernel = build / 'app/src/main/assets/chromium-probe.apk'
tools = Path(os.environ['ANDROID_HOME']) / 'build-tools/36.0.0'
with tempfile.TemporaryDirectory() as temp:
    temp = Path(temp)
    inputs = []
    for label, source in [('host', apk), ('engine', kernel)]:
        with zipfile.ZipFile(source) as z:
            for name in z.namelist():
                if name.startswith('classes') and name.endswith('.dex') and '/' not in name:
                    p = temp / (label + '-' + name)
                    p.write_bytes(z.read(name)); inputs.append(str(p))
    dex = temp / 'dex'; dex.mkdir()
    subprocess.run([str(tools / 'd8'), '--min-api', '29', '--output', str(dex), *inputs], check=True)
    merged = temp / 'merged.apk'
    with zipfile.ZipFile(apk) as host, zipfile.ZipFile(kernel) as engine, zipfile.ZipFile(merged, 'w', compression=zipfile.ZIP_DEFLATED) as out:
        for item in host.infolist():
            if item.filename.startswith('META-INF/') or (item.filename.startswith('classes') and item.filename.endswith('.dex')): continue
            out.writestr(item, host.read(item.filename))
        names = set(out.namelist())
        for item in engine.infolist():
            if (item.filename.startswith('lib/') or item.filename.startswith('assets/')) and item.filename not in names:
                out.writestr(item, engine.read(item.filename))
        for p in sorted(dex.glob('*.dex')): out.writestr(p.name, p.read_bytes())
    subprocess.run([str(tools / 'zipalign'), '-f', '4', str(merged), str(apk)], check=True)
subprocess.run([str(tools / 'apksigner'), 'sign', '--ks', str(Path.home() / '.android/debug.keystore'), '--ks-pass', 'pass:android', '--key-pass', 'pass:android', str(apk)], check=True)
subprocess.run([str(tools / 'apksigner'), 'verify', str(apk)], check=True)
print('Merged real engine classes/native/assets into disposable host APK for isolated renderer access.')
