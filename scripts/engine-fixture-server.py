"""Fixed loopback-only fixtures used exclusively by Actions browser tests."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
HTML = b"""<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body><h1>EasyKiosk Chromium test</h1><script>
window.__easyKioskResult = null;
(async () => {
 try {
  const ping = await fetch('/ping').then(r => r.json());
  window.__easyKioskResult = {
   ua: navigator.userAgent, js: ({x:7})?.x === 7 && typeof Promise.withResolvers === 'function',
   css: CSS.supports('grid-template-columns','subgrid'), net: ping.ok === true,
   bridge: typeof window.ReactNativeWebView?.postMessage === 'function'
  };
 } catch(e) { window.__easyKioskResult = {error:String(e)}; }
})();
</script></body></html>"""
class Handler(BaseHTTPRequestHandler):
 def do_GET(self):
  body = b'{"ok":true}' if self.path == '/ping' else HTML
  self.send_response(200)
  self.send_header('Content-Type','application/json' if self.path == '/ping' else 'text/html; charset=utf-8')
  self.send_header('Content-Length',str(len(body)))
  self.end_headers()
  self.wfile.write(body)
ThreadingHTTPServer(('127.0.0.1',8765),Handler).serve_forever()
