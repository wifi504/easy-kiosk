"""Download a pinned precompiled WebView release in Actions, checking release and asset identities."""
from pathlib import Path
import os, sys, json, subprocess, hashlib, base64
if os.environ.get('GITHUB_ACTIONS') != 'true':
    raise SystemExit('Engine downloads must run in GitHub Actions')
root = Path(__file__).resolve().parents[1]
lock = json.loads((root / 'config/chromium.json').read_text())
abi, folder = sys.argv[1], Path(sys.argv[2])
artifact = lock['artifacts'][abi]
selected = dict(lock, **{k: v for k, v in artifact.items() if k != 'artifacts'})
repo = selected['provider_repository']
if repo == 'holtdroid/chromium-builds':
    release = json.loads(subprocess.check_output(['gh', 'api', f"repos/{repo}/releases/{selected['provider_release_id']}"]))
    if release['tag_name'] != selected['version'] or release['draft'] or release['prerelease']:
        raise SystemExit('Unexpected precompiled stable release')
    asset = next(a for a in release['assets'] if a['id'] == artifact['asset_id'])
    url = f"https://github.com/{repo}/releases/download/{selected['version']}/{artifact['name']}"
    if asset['name'] != artifact['name'] or asset['browser_download_url'] != url or asset['digest'] != 'sha256:' + artifact['sha256']:
        raise SystemExit('Precompiled engine asset metadata changed')
elif repo in ('LineageOS/android_external_chromium-webview_prebuilt_arm', 'LineageOS/android_external_chromium-webview_prebuilt_x86'):
    commit = artifact['provider_commit']
    # GitHub media serves the real Git-LFS object, rather than its small pointer.
    # The immutable source commit, byte length and LFS SHA-256 are pinned together.
    url = f"https://media.githubusercontent.com/media/{repo}/{commit}/webview.apk"
else:
    raise SystemExit('Review new engine provider before use')
cpu = {'arm64-v8a': 'arm64', 'x86_64': 'x64', 'armeabi-v7a': 'arm', 'x86': 'x86'}[abi]
folder.mkdir(parents=True, exist_ok=True)
apk = folder / f"Chromium-{selected['version']}-{cpu}.apk"
if not apk.exists():
    subprocess.run(['curl', '--fail', '--location', '--retry', '3', '--proto', '=https', '--proto-redir', '=https', url, '--output', str(apk)], check=True)
with apk.open('rb') as stream: actual = hashlib.file_digest(stream, 'sha256').hexdigest()
if 'size' in artifact and apk.stat().st_size != artifact['size']: raise SystemExit('Kernel size mismatch')
if actual != artifact['sha256']: raise SystemExit('Precompiled engine SHA-256 mismatch')
Path(str(apk) + '.sha256').write_text(actual + '  ' + apk.name + '\n')
print(f"Verified pinned {repo} version={selected['version']} ABI={abi} SHA256={actual}")
license_file = folder / 'Chromium-LICENSE.txt'
if not license_file.exists():
    license_url = f"https://chromium.googlesource.com/chromium/src/+/{selected['source_commit']}/LICENSE?format=TEXT"
    # Gitiles occasionally returns 503. Retry transient download errors, and never
    # cache an error page or a partially written/unverified license.
    encoded = subprocess.check_output([
        'curl', '--fail', '--silent', '--show-error', '--location',
        '--retry', '5', '--retry-all-errors', '--retry-delay', '3',
        '--connect-timeout', '20', '--max-time', '120',
        '--proto', '=https', '--proto-redir', '=https', license_url,
    ])
    license_bytes = base64.b64decode(encoded.strip(), validate=True)
    if hashlib.sha256(license_bytes).hexdigest() != lock['license_sha256']:
        raise SystemExit('Downloaded Chromium upstream LICENSE checksum mismatch')
    temporary_license = license_file.with_suffix('.txt.tmp')
    temporary_license.write_bytes(license_bytes)
    temporary_license.replace(license_file)
if hashlib.sha256(license_file.read_bytes()).hexdigest() != lock['license_sha256']:
    raise SystemExit('Chromium upstream LICENSE checksum mismatch')