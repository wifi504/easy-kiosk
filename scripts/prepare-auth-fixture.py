"""Create a public-key TOTP APK fixture in Actions; never uses repository secrets."""
from pathlib import Path
import json
import os
import subprocess
import sys
import tempfile
import zipfile

if os.environ.get("GITHUB_ACTIONS") != "true":
    raise SystemExit("Fixture creation must run in Actions")
source, target = map(Path, sys.argv[1:])
policy = {"schema": 1, "mode": "totp", "secret": "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"}
with tempfile.TemporaryDirectory() as folder:
    merged = Path(folder) / "fixture.apk"
    with zipfile.ZipFile(source) as incoming, zipfile.ZipFile(merged, "w") as outgoing:
        for info in incoming.infolist():
            upper = info.filename.upper()
            if info.filename == "assets/easykiosk-auth.json" or upper == "META-INF/MANIFEST.MF" or (upper.startswith("META-INF/") and upper.endswith((".SF", ".RSA", ".DSA", ".EC"))):
                continue
            outgoing.writestr(info, incoming.read(info.filename))
        outgoing.writestr("assets/easykiosk-auth.json", json.dumps(policy), compress_type=zipfile.ZIP_STORED)
    tools = Path(os.environ["ANDROID_HOME"]) / "build-tools/36.0.0"
    subprocess.run([str(tools / "zipalign"), "-P", "16", "-f", "4", str(merged), str(target)], check=True)
    subprocess.run([str(tools / "apksigner"), "sign", "--v4-signing-enabled", "false", "--ks", "android/app/debug.keystore", "--ks-pass", "pass:android", "--key-pass", "pass:android", "--ks-key-alias", "androiddebugkey", str(target)], check=True)
