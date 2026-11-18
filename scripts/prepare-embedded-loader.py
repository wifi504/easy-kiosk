"""Prepare loader sources and compile-only renderer stubs in Actions; no engine binary needed."""
from pathlib import Path
import os, sys, subprocess
if os.environ.get('GITHUB_ACTIONS') != 'true':
    raise SystemExit('Loader preparation must run in Actions')
root = Path(__file__).resolve().parents[1]
version, sha, abi = sys.argv[1:4]
service = 'org.chromium.content.app.SandboxedProcessService0'
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
