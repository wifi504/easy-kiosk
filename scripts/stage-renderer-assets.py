"""Stage engine DEX as signed read-only assets for an independent isolated-process classloader."""
from pathlib import Path
import os, sys, json, hashlib, zipfile
if os.environ.get('GITHUB_ACTIONS') != 'true': raise SystemExit('Renderer assets must be staged in Actions')
engine, assets = Path(sys.argv[1]), Path(sys.argv[2])
folder = assets / 'embedded-renderer'; folder.mkdir(parents=True, exist_ok=True)
index = []
with zipfile.ZipFile(engine) as archive:
    for name in sorted(archive.namelist()):
        if name.startswith('classes') and name.endswith('.dex') and '/' not in name:
            data = archive.read(name)
            (folder / name).write_bytes(data)
            index.append({'name': name, 'sha256': hashlib.sha256(data).hexdigest()})
if not index: raise SystemExit('No engine DEX in pinned APK')
(folder / 'index.json').write_text(json.dumps(index) + '\n')
print('Staged independent installed engine DEX assets:', index)