"""Receives the compendium dumps export_foundry.js POSTs and writes them to ./export.

POST /save?name=<file.json> with the JSON text as the body. Only plain file names are accepted.
"""
import os
import re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'export')


class Handler(BaseHTTPRequestHandler):
    def _cors(self):
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')

    def do_OPTIONS(self):
        self.send_response(204)
        self._cors()
        self.end_headers()

    def do_POST(self):
        query = parse_qs(urlparse(self.path).query)
        name = (query.get('name') or [''])[0]
        if not re.fullmatch(r'[A-Za-z0-9_.-]+\.json', name):
            self.send_response(400)
            self._cors()
            self.end_headers()
            return
        length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(length)
        with open(os.path.join(OUT, name), 'wb') as f:
            f.write(body)
        self.send_response(200)
        self._cors()
        self.send_header('Content-Type', 'text/plain')
        self.end_headers()
        self.wfile.write(f'saved {name} {len(body)}'.encode())


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    print('listening on http://127.0.0.1:8766, saving to', OUT)
    ThreadingHTTPServer(('127.0.0.1', 8766), Handler).serve_forever()
