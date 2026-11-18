"""Preserve Chromium's supplied Credits HTML from its original DataPack assets in Actions."""
from pathlib import Path
import os, sys, zipfile, struct, gzip, brotli
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
            elif payload.startswith(b'\x1e\x9b'):
                expected_size = int.from_bytes(payload[2:8], 'little')
                payload = brotli.decompress(payload[8:])
                if len(payload) != expected_size: raise SystemExit('Chromium Brotli resource size mismatch')
            text = payload.lower()
            if b'<title>credits</title>' in text:
                found.append((name, resource_id, payload))
        print('Inspected Chromium DataPack', name, 'resources=', count)
if len(found) != 1: raise SystemExit('Expected exactly one upstream Chromium Credits HTML, found ' + str(len(found)))
name, resource_id, html = found[0]
output.parent.mkdir(parents=True, exist_ok=True)
output.write_bytes(html)
print('Preserved upstream Chromium Credits HTML:', name, 'resource=', resource_id, 'bytes=', len(html), 'sample=', b'This is sample credits page.' in html)