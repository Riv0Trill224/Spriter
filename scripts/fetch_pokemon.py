#!/usr/bin/env python3
"""Fetch the pinned, verified Pokemon pack before compiling; stdlib only."""
import concurrent.futures
import hashlib
import json
import pathlib
import time
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
LOCK = json.loads((ROOT / "scripts/pokemon.lock.json").read_text())
DEST = ROOT / "app/src/main/assets/pokemon"


def valid(data, expected):
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        return False
    blob = b"blob " + str(len(data)).encode("ascii") + b"\0" + data
    return hashlib.sha1(blob).hexdigest() == expected


def fetch(item):
    name, sha = item["file"], item["git_blob_sha"]
    target = DEST / name
    if target.exists() and valid(target.read_bytes(), sha):
        return name
    url = (f"https://raw.githubusercontent.com/{LOCK['repository']}/"
           f"{LOCK['commit']}/{LOCK['directory']}/{name}")
    for attempt in range(3):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": "Spriter-build/0.2.0"})
            with urllib.request.urlopen(request, timeout=30) as response:
                data = response.read(1_048_577)
            if len(data) > 1_048_576 or not valid(data, sha):
                raise ValueError(f"Sprite con contenido inesperado: {name}")
            temporary = target.with_suffix(".tmp")
            temporary.write_bytes(data)
            temporary.replace(target)
            return name
        except Exception:
            if attempt == 2:
                raise
            time.sleep(attempt + 1)


def main():
    expected = {f"{i}.png" for i in range(1, 152)}
    assert {item['file'] for item in LOCK['files']} == expected, "Catálogo incompleto"
    DEST.mkdir(parents=True, exist_ok=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        list(executor.map(fetch, LOCK['files']))
    actual = {p.name for p in DEST.iterdir() if p.is_file()}
    if actual != expected:
        raise RuntimeError("Hay archivos inesperados en assets/pokemon; revisa la carpeta.")
    notices = ROOT / "app/src/main/assets/notices"
    notices.mkdir(parents=True, exist_ok=True)
    (notices / "POKEAPI-LICENCE.txt").write_bytes((ROOT / "docs/POKEAPI-LICENCE.txt").read_bytes())
    (notices / "SOURCE.txt").write_text(
        f"Pokemon sprites: https://github.com/{LOCK['repository']}\n"
        f"Commit: {LOCK['commit']}\nPath: {LOCK['directory']}/1.png .. 151.png\n"
        "Image contents Copyright The Pokemon Company.\n", encoding="utf-8")
    print(f"Verificados {len(expected)} sprites Pokémon; colección lista para el APK.")


if __name__ == "__main__":
    main()
