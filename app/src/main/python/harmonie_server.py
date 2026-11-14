"""Serveur local sécurisé pour l'interface originale de Harmonie.

Écoute uniquement 127.0.0.1 : les fichiers musicaux ne sont jamais envoyés
vers Internet. Les données Radio Browser sont récupérées sans CORS par Python.
"""
from __future__ import annotations
import ctypes
import json
import mimetypes
import os
from pathlib import Path
import threading
import urllib.error
import urllib.parse
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HOST, PORT = "127.0.0.1", 8767
SOURCES = (
    "https://de1.api.radio-browser.info",
    "https://nl1.api.radio-browser.info",
    "https://at1.api.radio-browser.info",
    "https://fr1.api.radio-browser.info",
)
_server = None
_thread = None
_root = None
_native = None


def attach_rust(native_lib_directory: str) -> dict:
    """Charge la bibliothèque Rust compilée pour l'ABI Android cible."""
    global _native
    if not native_lib_directory:
        return {"rust": False, "reason": "Répertoire des bibliothèques absent"}
    path = Path(native_lib_directory) / "libharmonie_core.so"
    try:
        _native = ctypes.CDLL(str(path))
        _native.harmonie_version.restype = ctypes.c_char_p
        version = _native.harmonie_version().decode("ascii")
        return {"rust": True, "version": version}
    except (OSError, AttributeError) as exc:
        return {"rust": False, "reason": str(exc)}


def media_safe_url(url: str) -> bool:
    """Refuse les protocoles dangereux et les adresses locales privées."""
    p = urllib.parse.urlsplit(url)
    return p.scheme == "https" and p.hostname in {
        "de1.api.radio-browser.info", "nl1.api.radio-browser.info",
        "at1.api.radio-browser.info", "fr1.api.radio-browser.info"
    }


def remote_radio_data(endpoint: str) -> bytes:
    """Retourne une liste JSON Radio Browser, avec repli entre serveurs."""
    if not endpoint.startswith("/json/") or len(endpoint) > 2048:
        raise ValueError("Endpoint radio interdit")
    last_error = None
    for base in SOURCES:
        url = base + endpoint
        if not media_safe_url(url):
            continue
        try:
            request = urllib.request.Request(url, headers={
                "User-Agent": "HarmonieAndroid/3.0", "Accept": "application/json"
            })
            with urllib.request.urlopen(request, timeout=8) as response:
                data = response.read(4 * 1024 * 1024 + 1)
            if len(data) > 4 * 1024 * 1024:
                raise ValueError("Catalogue trop volumineux")
            parsed = json.loads(data)
            if not isinstance(parsed, list):
                raise ValueError("Format du catalogue inattendu")
            return json.dumps(parsed, ensure_ascii=False).encode("utf-8")
        except (OSError, ValueError, urllib.error.URLError) as exc:
            last_error = exc
    raise OSError("Radio Browser inaccessible : " + str(last_error))


class Handler(BaseHTTPRequestHandler):
    server_version = "Harmonie/3"

    def log_message(self, fmt, *args):
        pass

    def _response(self, code: int, body: bytes, content_type: str, **headers):
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store" if code != 200 else "private, max-age=0")
        self.send_header("X-Content-Type-Options", "nosniff")
        for k, v in headers.items():
            self.send_header(k.replace("_", "-"), str(v))
        self.end_headers()
        if self.command != "HEAD":
            try:
                self.wfile.write(body)
            except (BrokenPipeError, ConnectionResetError):
                pass

    def do_HEAD(self):
        self.do_GET()

    def do_GET(self):
        path = urllib.parse.urlsplit(self.path).path
        if path in ("/", "/index.html"):
            try:
                data = (_root / "harmonie.html").read_bytes()
            except OSError as exc:
                self._response(500, str(exc).encode("utf-8"), "text/plain; charset=utf-8")
                return
            self._response(200, data, "text/html; charset=utf-8")
        elif path == "/healthz":
            self._response(200, b'{"ready":true}', "application/json")
        elif path == "/api/core":
            self._response(200, json.dumps({"python": True, "rust": _native is not None}).encode(), "application/json")
        elif path.startswith("/json/"):
            try:
                data = remote_radio_data(self.path)
                self._response(200, data, "application/json; charset=utf-8")
            except (OSError, ValueError) as exc:
                self._response(502, json.dumps({"error": str(exc)}).encode(), "application/json")
        else:
            self._response(404, b"Not found", "text/plain; charset=utf-8")


def start(root: str, native_lib_directory: str = "") -> str:
    """Démarre un seul serveur local par processus et retourne son URL."""
    global _server, _thread, _root
    _root = Path(root).resolve()
    if not (_root / "harmonie.html").is_file():
        raise FileNotFoundError("Interface Harmonie absente : " + str(_root))
    attach_rust(native_lib_directory)
    if _server is None:
        class Server(ThreadingHTTPServer):
            daemon_threads = True
            allow_reuse_address = True
        _server = Server((HOST, PORT), Handler)
        _thread = threading.Thread(target=_server.serve_forever, daemon=True, name="harmonie-http")
        _thread.start()
    return "http://127.0.0.1:%d/" % PORT


def stop():
    global _server, _thread
    if _server:
        _server.shutdown()
        _server.server_close()
        _server = None
        _thread = None
