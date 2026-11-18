"""Extract Chromium's complete Credits HTML from its original DataPack assets in Actions."""
from pathlib import Path
import os, sys, zipfile, struct, gzip
if os.environ.get('GITHUB_ACTIONS') != 'true': raise SystemExit('Notice extraction must run in Actions')
apk, output = Path(sys.argv[1]), Path(sys.argv[2])
found = []
with zipfile.ZipFile(apk) as archive:
    for name in archive.namelist():
        if not name.startswith('assets/') or 'resources.pak' not in name: continue
        data = archive.read(name)
        version = struct.unpack_from('<I', data)[0]
        if version == 5:
            encoding, count, aliases = struct.unpack_from('<BxxxHH', data, 4); header = 12
        elif version == 4:
            count, encoding = struct.unpack_from('<IB', data, 4); header = 9
        else: raise SystemExit('Unsupported Chromium DataPack version: ' + str(version))
        entries = [struct.unpack_from('<HI', data, header + i * 6) for i in range(count + 1)]
        for index in range(count):
            resource_id, begin = entries[index]; end = entries[index + 1][1]
            if begin > end or end > len(data): raise SystemExit('Invalid Chromium DataPack offsets')
            payload = data[begin:end]
            if payload.startswith(b'\x1f\x8b'): payload = gzip.decompress(payload)
            text = payload.lower()
            if b'<title>credits</title>' in text and b'copyright' in text and b'license' in text and len(text) > 10000:
                found.append((name, resource_id, payload))
        print('Inspected Chromium DataPack', name, 'resources=', count)
if len(found) != 1: raise SystemExit('Expected exactly one complete Chromium Credits HTML, found ' + str(len(found)))
name, resource_id, html = found[0]
output.parent.mkdir(parents=True, exist_ok=True)
output.write_bytes(html)
print('Preserved complete Chromium Credits HTML:', name, 'resource=', resource_id, 'bytes=', len(html))