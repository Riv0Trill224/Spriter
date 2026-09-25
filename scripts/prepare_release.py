#!/usr/bin/env python3
"""Verify the RGDS signing identity and create updater metadata from the built APK."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
EXPECTED = '1c09515ee923d5610dc41e85d75a49900c10158b18c4703463896e4fd1b10264'
apk = ROOT / 'app/build/outputs/apk/release/app-release.apk'
tools = Path(os.environ['ANDROID_HOME']) / 'build-tools/35.0.0'
verified = subprocess.check_output([str(tools / 'apksigner'), 'verify', '--verbose', '--print-certs', str(apk)], text=True)
certs = re.findall(r'Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]+)', verified)
if len(certs) != 1 or certs[0].lower() != EXPECTED:
    raise SystemExit('La firma no coincide con la clave estable de RGDS Dashboard. No se publica.')
badging = subprocess.check_output([str(tools / 'aapt'), 'dump', 'badging', str(apk)], text=True)
package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
if not package or package[1] != 'com.riv0trill.spriter':
    raise SystemExit('Package de release no válido')
if not re.fullmatch(r'\d+\.\d+\.\d+', package[3]):
    raise SystemExit('Versión de release no válida')
dist = ROOT / 'dist'
dist.mkdir(exist_ok=True)
shutil.copyfile(apk, dist / 'Spriter.apk')
digest = hashlib.sha256(apk.read_bytes()).hexdigest()
metadata = dict(package=package[1], versionCode=int(package[2]), versionName=package[3], sizeBytes=apk.stat().st_size, sha256=digest)
(dist / 'update.json').write_text(json.dumps(metadata, indent=2) + '\n')
(dist / 'SHA256SUMS.txt').write_text(digest + '  Spriter.apk\n')
if os.environ.get('GITHUB_OUTPUT'):
    with open(os.environ['GITHUB_OUTPUT'], 'a') as out:
        out.write('tag=v' + package[3] + '\n')
print('Verificados package, versión y certificado RGDS. Release v' + package[3])
