"""CI-only experiment: keep renderer services isolated by merging engine DEX/native into host APK."""
from pathlib import Path
import zipfile, subprocess, os, tempfile
root = Path(__file__).resolve().parents[1]
loader = root / '.engine-loader/core'
manifest = loader / 'src/main/AndroidManifest.xml'
manifest.write_text(manifest.read_text().replace('android:exported="false"', 'android:exported="false" android:isolatedProcess="true"'))
for index in range(5):
    p = loader / f'src/main/java/com/norman/webviewup/lib/sandbox/stub/StubSandboxedProcessService{index}.java'
    p.write_text(f'''package com.norman.webviewup.lib.sandbox.stub;
public class StubSandboxedProcessService{index} extends org.chromium.content.app.SandboxedProcessService0 {{
    public StubSandboxedProcessService{index}() {{
        int uid = android.os.Process.myUid();
        int appId = uid % 100000;
        if (appId < 90000 || appId > 99999) throw new SecurityException("Renderer is not isolated: " + uid);
        android.util.Log.i("EasyKioskEngineProbe", "ISOLATED_RENDERER_UID " + uid);
    }}
}}
''')
with (loader / 'build.gradle').open('a') as out:
    out.write("\ndependencies { compileOnly files('../../tools/embedded-chromium-probe/renderer-stubs.jar') }\n")
build = root / 'tools/embedded-chromium-probe'
classes = build / 'renderer-stubs-classes'
classes.mkdir(exist_ok=True)
android_jar = Path(os.environ['ANDROID_HOME']) / 'platforms/android-36/android.jar'
subprocess.run(['javac', '-classpath', str(android_jar), '-d', str(classes), str(build / 'renderer-stubs/org/chromium/content/app/SandboxedProcessService0.java')], check=True)
subprocess.run(['jar', 'cf', str(build / 'renderer-stubs.jar'), '-C', str(classes), '.'], check=True)
print('Prepared direct renderer services with isolatedProcess=true; API stubs are compile-only.')
