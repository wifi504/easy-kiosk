"""Use the same production loader and renderer classloader in a small Actions-only fixture app."""
from pathlib import Path
import os, shutil
if os.environ.get('GITHUB_ACTIONS') != 'true': raise SystemExit('Probe preparation must run in Actions')
root = Path(__file__).resolve().parents[1]
probe = root / 'tools/embedded-chromium-probe/app/src/main'
shutil.copytree(root / 'android/app/src/main/assets', probe / 'assets', dirs_exist_ok=True)
java = probe / 'java/com/freekiosk'; java.mkdir(parents=True, exist_ok=True)
shutil.copyfile(root / 'android/app/src/main/java/com/freekiosk/EmbeddedChromiumComponentFactory.java', java / 'EmbeddedChromiumComponentFactory.java')
loader = root / '.engine-loader/core'
(loader / 'build.gradle').write_text('''plugins { id 'com.android.library' }
android {
 namespace 'com.norman.webviewup.lib'
 compileSdk 36
 defaultConfig { minSdk 29; consumerProguardFiles 'consumer-rules.pro' }
 compileOptions { sourceCompatibility JavaVersion.VERSION_1_8; targetCompatibility JavaVersion.VERSION_1_8 }
}
dependencies { compileOnly 'androidx.annotation:annotation:1.7.1' }
''')