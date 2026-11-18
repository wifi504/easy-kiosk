"""Download a pinned precompiled WebView release in Actions, checking release and asset identities."""
from pathlib import Path
import os, sys, json, subprocess, hashlib
if os.environ.get('GITHUB_ACTIONS') != 'true':
    raise SystemExit('Engine downloads must run in GitHub Actions')
root = Path(__file__).resolve().parents[1]
lock = json.loads((root / 'config/chromium.json').read_text())
abi, folder = sys.argv[1], Path(sys.argv[2])
artifact = lock['artifacts'][abi]
repo = lock['provider_repository']
if repo != 'holtdroid/chromium-builds': raise SystemExit('Review new engine provider before use')
release = json.loads(subprocess.check_output(['gh', 'api', f"repos/{repo}/releases/{lock['provider_release_id']}"]))
if release['tag_name'] != lock['version'] or release['draft'] or release['prerelease']:
    raise SystemExit('Unexpected precompiled stable release')
asset = next(a for a in release['assets'] if a['id'] == artifact['asset_id'])
url = f"https://github.com/{repo}/releases/download/{lock['version']}/{artifact['name']}"
if asset['name'] != artifact['name'] or asset['browser_download_url'] != url or asset['digest'] != 'sha256:' + artifact['sha256']:
    raise SystemExit('Precompiled engine asset metadata changed')
cpu = {'arm64-v8a': 'arm64', 'x86_64': 'x64'}[abi]
folder.mkdir(parents=True, exist_ok=True)
apk = folder / f"Chromium-{lock['version']}-{cpu}.apk"
if not apk.exists():
    subprocess.run(['curl', '--fail', '--location', '--retry', '3', '--proto', '=https', '--proto-redir', '=https', url, '--output', str(apk)], check=True)
with apk.open('rb') as stream: actual = hashlib.file_digest(stream, 'sha256').hexdigest()
if actual != artifact['sha256']: raise SystemExit('Precompiled engine SHA-256 mismatch')
Path(str(apk) + '.sha256').write_text(actual + '  ' + apk.name + '\n')
print(f"Verified pinned {repo} release={release['id']} asset={asset['id']} version={lock['version']} ABI={abi} SHA256={actual}")