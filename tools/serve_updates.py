#!/usr/bin/env python3
"""Publishes the debug APK on the local network so the keyboard can update itself.

    python tools/serve_updates.py            # build, publish and serve (Ctrl+C to stop)
    python tools/serve_updates.py --no-build # publish the APK that is already built
    python tools/serve_updates.py --no-build --no-serve --dir dist   # only write APK + update.json
                                             # (what .github/workflows/release.yml attaches)

The phone finds this PC by itself: it sends a UDP broadcast question (port 47800) and this script
answers with the HTTP port, so it keeps working when the router gives the PC another IP.
While the script is not running the keyboard simply finds no updates.

Only the Python standard library is used.
"""
import argparse
import functools
import hashlib
import http.server
import json
import os
import re
import shutil
import socket
import subprocess
import sys
import threading

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APK = os.path.join(ROOT, "app", "build", "outputs", "apk", "debug", "app-ime-debug.apk")
OUT = os.path.join(ROOT, "build", "updates")
DISCOVERY_PORT = 47800
QUESTION = b"OHK-UPDATES?"
KEEP_APKS = 3


def git(*args):
    return subprocess.run(["git", *args], cwd=ROOT, check=True, capture_output=True, text=True).stdout.strip()


def build():
    gradlew = os.path.join(ROOT, "gradlew.bat" if os.name == "nt" else "gradlew")
    subprocess.run([gradlew, "--console=plain", "-q", "assembleDebug"], cwd=ROOT, check=True)


def version_name():
    gradle = open(os.path.join(ROOT, "app", "build.gradle.kts"), encoding="utf-8").read()
    match = re.search(r'versionName = "([^"]+)"', gradle)
    return (match.group(1) if match else "?") + "-debug"


def sha256(path):
    digest = hashlib.sha256()
    with open(path, "rb") as file:
        for block in iter(lambda: file.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def publish(out):
    """Copies the APK next to an update.json describing it (the commit the APK was built from)."""
    if not os.path.isfile(APK):
        sys.exit(f"No APK at {APK}: build it first (run without --no-build).")
    commit = git("rev-parse", "--short", "HEAD")
    os.makedirs(out, exist_ok=True)
    name = f"OneHandKeyboard-{commit}.apk"
    shutil.copyfile(APK, os.path.join(out, name))
    manifest = {
        "commit": commit,
        "commitTime": int(git("log", "-1", "--format=%ct")),
        "versionName": version_name(),
        "apk": name,
        "size": os.path.getsize(APK),
        "sha256": sha256(APK),
    }
    temp = os.path.join(out, "update.json.tmp")
    with open(temp, "w", encoding="utf-8") as file:
        json.dump(manifest, file, indent=2)
    os.replace(temp, os.path.join(out, "update.json"))
    old = sorted(
        (f for f in os.listdir(out) if f.startswith("OneHandKeyboard-") and f.endswith(".apk") and f != name),
        key=lambda f: os.path.getmtime(os.path.join(out, f)),
    )
    for stale in old[: max(0, len(old) - (KEEP_APKS - 1))]:
        os.remove(os.path.join(out, stale))
    return manifest


def lan_ip():
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as probe:
        try:
            probe.connect(("10.255.255.255", 1))  # no packet is sent; it just picks the LAN interface
            return probe.getsockname()[0]
        except OSError:
            return "127.0.0.1"


def answer_discovery(http_port):
    """Replies to the phone's broadcast question with the HTTP port."""
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind(("", DISCOVERY_PORT))
        while True:
            data, sender = sock.recvfrom(256)
            if data.strip() == QUESTION:
                sock.sendto(f"OHK-UPDATES {http_port}\n".encode(), sender)
                print(f"  ↪ el teléfono {sender[0]} preguntó por actualizaciones")


class QuietHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, fmt, *args):
        if self.path.endswith(".apk"):
            print(f"  ⬇ {self.client_address[0]} descarga {self.path.lstrip('/')}")


def main():
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--no-build", action="store_true", help="publish the APK already built")
    parser.add_argument("--no-serve", action="store_true", help="only write the APK and update.json")
    parser.add_argument("--port", type=int, default=8000, help="HTTP port (default 8000)")
    parser.add_argument("--dir", default=OUT, help="folder to publish to (default build/updates)")
    args = parser.parse_args()

    if git("status", "--porcelain", "--untracked-files=no"):
        print("⚠ Hay cambios sin commit: el teclado solo ve versiones nuevas por commit.")
    if not args.no_build:
        print("Compilando el APK de debug…")
        build()
    manifest = publish(args.dir)
    print(f"Publicado {manifest['apk']} ({manifest['size'] / 1024 / 1024:.1f} MB)")
    if args.no_serve:
        return 0

    threading.Thread(target=answer_discovery, args=(args.port,), daemon=True).start()
    handler = functools.partial(QuietHandler, directory=args.dir)
    server = http.server.ThreadingHTTPServer(("0.0.0.0", args.port), handler)
    print(f"Sirviendo en http://{lan_ip()}:{args.port}  (Ctrl+C para parar)")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nServidor detenido.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
