"""Actions-only preparation of a pinned, verified, app-bundled Chromium engine."""
from pathlib import Path
import os, sys, json, subprocess, hashlib, re, shutil, zipfile
if os.environ.get('GITHUB_ACTIONS') != 'true':
    raise SystemExit('Kernel downloads and preparation must run in GitHub Actions.')
root = Path(__file__).resolve().parents[1]
lock = json.loads((root / 'config/chromium.json').read_text())
abi, directory = sys.argv[1], Path(sys.argv[2])
cpu = {'arm64-v8a': 'arm64', 'x86_64': 'x64'}[abi]
name = f"Chromium-{lock['version']}-{cpu}.apk"
engine = directory / name
expected = (directory / (name + '.sha256')).read_text().split()[0].lower()
if not re.fullmatch('[0-9a-f]{64}', expected): raise SystemExit('Invalid kernel SHA-256 metadata')
sha = hashlib.file_digest(engine.open('rb'), 'sha256').hexdigest()
if sha != expected: raise SystemExit('Kernel SHA-256 mismatch')
run = json.loads(subprocess.check_output(['gh', 'api', f"repos/wifi504/easy-kiosk/actions/runs/{lock['source_run']}"]))
if run['conclusion'] != 'success' or run['head_branch'] != 'dev' or run['head_sha'] != lock['source_workflow_commit'] or run['path'] != '.github/workflows/chromium-source-build.yml':
    raise SystemExit('Unexpected kernel source workflow provenance')
tools = Path(os.environ['ANDROID_HOME']) / 'build-tools/36.0.0'
subprocess.run([str(tools / 'apksigner'), 'verify', str(engine)], check=True)
badging = subprocess.check_output([str(tools / 'aapt'), 'dump', 'badging', str(engine)], text=True)
version = re.search(r"versionName='([^']+)'", badging).group(1)
package = re.search(r"package: name='([^']+)'", badging).group(1)
min_sdk = int(re.search(r"sdkVersion:'(\d+)'", badging).group(1))
if version != lock['version'] or package != 'com.android.webview' or min_sdk > 29:
    raise SystemExit(f'Unexpected kernel manifest: {package} {version} minSdk={min_sdk}')
xml = subprocess.check_output([str(tools / 'aapt'), 'dump', 'xmltree', str(engine), 'AndroidManifest.xml'], text=True)
service = 'org.chromium.content.app.SandboxedProcessService0'
if service not in xml: raise SystemExit('Kernel renderer service changed; review before integration')
with zipfile.ZipFile(engine) as archive:
    if not any(n.startswith(f'lib/{abi}/') and n.endswith('.so') for n in archive.namelist()):
        raise SystemExit('Kernel has no native library for ' + abi)
    if not any('webview_licenses' in n for n in archive.namelist()):
        raise SystemExit('Kernel third-party license asset is missing')
assets = root / 'android/app/src/main/assets'; assets.mkdir(parents=True, exist_ok=True)
shutil.copyfile(engine, assets / 'embedded-chromium.apk')
meta = dict(lock, abi=abi, package=package, sha256=sha)
(assets / 'embedded-chromium.json').write_text(json.dumps(meta, indent=2) + '\n')
notices = assets / 'third-party'; notices.mkdir(exist_ok=True)
shutil.copyfile(root / '.engine-loader/LICENSE', notices / 'WebViewUpgrade-LICENSE.txt')
loader = root / '.engine-loader/core'
manifest = loader / 'src/main/AndroidManifest.xml'
manifest.write_text(manifest.read_text().replace('android:exported="false"', 'android:exported="false" android:isolatedProcess="true"'))
for index in range(5):
    source = loader / f'src/main/java/com/norman/webviewup/lib/sandbox/stub/StubSandboxedProcessService{index}.java'
    source.write_text(f'''package com.norman.webviewup.lib.sandbox.stub;
public class StubSandboxedProcessService{index} extends {service} {{
    public StubSandboxedProcessService{index}() {{
        int uid = android.os.Process.myUid();
        int appId = uid % 100000;
        if (appId < 90000 || appId > 99999) throw new SecurityException("Renderer is not isolated: " + uid);
        android.util.Log.i("EasyKioskEngine", "ISOLATED_RENDERER_UID " + uid);
    }}
}}
''')
build = root / 'android/build/embedded-chromium'; build.mkdir(parents=True, exist_ok=True)
(build / 'config.properties').write_text(f"version={version}\nsha256={sha}\nabi={abi}\n")
stub = build / 'stubs/org/chromium/content/app/SandboxedProcessService0.java'; stub.parent.mkdir(parents=True, exist_ok=True)
stub.write_text('package org.chromium.content.app; public class SandboxedProcessService0 extends android.app.Service { public android.os.IBinder onBind(android.content.Intent intent) { throw new UnsupportedOperationException(); } }')
classes = build / 'stub-classes'; classes.mkdir(exist_ok=True)
subprocess.run(['javac', '-classpath', str(Path(os.environ['ANDROID_HOME']) / 'platforms/android-36/android.jar'), '-d', str(classes), str(stub)], check=True)
subprocess.run(['jar', 'cf', str(build / 'renderer-stubs.jar'), '-C', str(classes), '.'], check=True)
print(f'Verified bundled Chromium {version}, ABI={abi}, SHA256={sha}, source run={lock["source_run"]}')
