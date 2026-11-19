"""Actions-only preparation of a pinned, verified, app-bundled Chromium engine."""
from pathlib import Path
import os, sys, json, subprocess, hashlib, re, shutil, zipfile
if os.environ.get('GITHUB_ACTIONS') != 'true':
    raise SystemExit('Kernel downloads and preparation must run in GitHub Actions.')
root = Path(__file__).resolve().parents[1]
lock = json.loads((root / 'config/chromium.json').read_text())
abi, directory = sys.argv[1], Path(sys.argv[2])
lock = dict(lock, **{k: v for k, v in lock['artifacts'][abi].items() if k != 'artifacts'})
cpu = {'arm64-v8a': 'arm64', 'x86_64': 'x64', 'armeabi-v7a': 'arm', 'x86': 'x86'}[abi]
name = f"Chromium-{lock['version']}-{cpu}.apk"
engine = directory / name
expected = (directory / (name + '.sha256')).read_text().split()[0].lower()
if not re.fullmatch('[0-9a-f]{64}', expected): raise SystemExit('Invalid kernel SHA-256 metadata')
sha = hashlib.file_digest(engine.open('rb'), 'sha256').hexdigest()
if sha != expected: raise SystemExit('Kernel SHA-256 mismatch')
if sha != lock['artifacts'][abi]['sha256']: raise SystemExit('Engine differs from pinned precompiled release')

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
assets = root / 'android/app/src/main/assets'; assets.mkdir(parents=True, exist_ok=True)
shutil.copyfile(engine, assets / 'embedded-chromium.apk')
subprocess.run([sys.executable, str(root / 'scripts/stage-renderer-assets.py'), str(engine), str(assets)], check=True)
meta = dict(lock, abi=abi, package=package, sha256=sha)
meta.pop('artifacts', None)
if 'provider_commit' in meta: meta.pop('provider_release_id', None)
(assets / 'embedded-chromium.json').write_text(json.dumps(meta, indent=2) + '\n')
notices = assets / 'third-party'; notices.mkdir(exist_ok=True)
shutil.copyfile(root / '.engine-loader/LICENSE', notices / 'WebViewUpgrade-LICENSE.txt')
shutil.copyfile(directory / 'Chromium-LICENSE.txt', notices / 'Chromium-LICENSE.txt')
if 'patches_commit' in lock:
    (notices / 'LineageOS-WebView-SOURCE.txt').write_text(
        f"Prebuilt: https://github.com/{lock['provider_repository']}/tree/{lock['provider_commit']}\n"
        f"Chromium: https://chromium.googlesource.com/chromium/src/+/{lock['source_commit']}\n"
        f"Build settings and patches: https://github.com/LineageOS/android_external_chromium-webview_patches/tree/{lock['patches_commit']}\n"
    )
subprocess.run([sys.executable, str(root / 'scripts/extract-chromium-notices.py'), str(engine), str(notices / 'Chromium-CREDITS.html')], check=True)
subprocess.run([sys.executable, str(root / 'scripts/prepare-embedded-loader.py'), version, sha, abi], check=True)
print(f'Verified bundled Chromium {version}, ABI={abi}, SHA256={sha}, minSdk={min_sdk} provider={lock["provider_repository"]} source={lock["source_commit"]}')
