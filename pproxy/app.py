import socket
import threading
import sys
import os
import subprocess
import urllib.request
import urllib.parse
import http.client
import json
from datetime import datetime

HOST = os.environ.get("PPROXY_HOST", "0.0.0.0")
PORT = int(os.environ.get("PPROXY_PORT", "4096"))
HTTP_PORT = int(os.environ.get("PPROXY_HTTP_PORT", "8080"))

_lock = threading.Lock()

def log(msg=""):
    with _lock:
        print(f"[{datetime.now().strftime('%H:%M:%S')}] {msg}", flush=True)


class TCPProxy:
    def __init__(self):
        self.clients = {}
        self.next_id = 1

    def start(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            s.bind((HOST, PORT))
            s.listen(65535)
            log(f"TCP proxy listening on {HOST}:{PORT}")
            while True:
                conn, addr = s.accept()
                t = threading.Thread(target=self.handle_client, args=(conn, addr), daemon=True)
                t.start()

    def handle_client(self, conn, addr):
        client_id = f"client-{self.next_id}"
        self.next_id += 1
        log(f"[{client_id}] Connected from {addr[0]}")

        try:
            conn.settimeout(300)
            data = conn.recv(4096)
            if not data:
                conn.close()
                return

            password = data.decode("utf-8", errors="replace").strip()
            log(f"[{client_id}] Auth attempt")

            self.clients[client_id] = {
                "addr": addr[0],
                "connected_at": datetime.now().isoformat(),
                "password": password,
            }

            conn.sendall(f"WebProxy ID: {client_id}\n".encode("utf-8"))

            while True:
                try:
                    conn.settimeout(600)
                    cmd_data = conn.recv(4096)
                    if not cmd_data:
                        break
                    cmd = cmd_data.decode("utf-8", errors="replace").strip()
                    if not cmd:
                        continue

                    log(f"[{client_id}] -> {cmd[:80]}")
                    output = self.execute_command(cmd, client_id)
                    conn.sendall(output.encode("utf-8"))

                except socket.timeout:
                    log(f"[{client_id}] Timeout")
                    break
                except Exception as e:
                    log(f"[{client_id}] Error: {e}")
                    break

        except Exception as e:
            log(f"[{client_id}] Fatal: {e}")
        finally:
            self.clients.pop(client_id, None)
            conn.close()
            log(f"[{client_id}] Disconnected")

    def execute_command(self, cmd, client_id):
        parts = cmd.split(maxsplit=1)
        verb = parts[0].lower() if parts else ""

        if verb == "get":
            filename = parts[1] if len(parts) > 1 else ""
            return self.cmd_get(filename)
        elif verb == "http":
            url = parts[1] if len(parts) > 1 else ""
            return self.cmd_http(url)
        elif verb == "post":
            data = parts[1] if len(parts) > 1 else ""
            return self.cmd_post(data)
        elif verb == "fetch":
            return self.cmd_fetch()
        elif verb == "who":
            return self.cmd_who()
        elif verb == "help":
            return self.cmd_help()
        else:
            return self.cmd_shell(cmd)

    def cmd_get(self, filename):
        if not filename:
            return "Error: missing filename\n"
        if filename.startswith("/") or ".." in filename:
            return "Error: permission denied\n"
        try:
            with open(filename, "r") as f:
                return f.read()
        except FileNotFoundError:
            return f"Error: '{filename}' not found\n"
        except Exception as e:
            return f"Error: {e}\n"

    def cmd_http(self, url):
        if not url:
            return "Error: missing URL\n"
        if not url.startswith(("http://", "https://")):
            url = "http://" + url
        try:
            req = urllib.request.Request(url, headers={
                "User-Agent": "OpenTTY-WebProxy/1.0"
            })
            with urllib.request.urlopen(req, timeout=15) as resp:
                return resp.read().decode("utf-8", errors="replace")
        except Exception as e:
            return f"Error: {e}\n"

    def cmd_post(self, post_data):
        parts = post_data.split(maxsplit=1)
        if len(parts) < 2:
            return "Error: usage: post <url> <data>\n"
        url, data = parts
        if not url.startswith(("http://", "https://")):
            url = "http://" + url
        try:
            parsed = urllib.parse.urlparse(url)
            conn_class = http.client.HTTPSConnection if parsed.scheme == "https" else http.client.HTTPConnection
            c = conn_class(parsed.netloc)
            body = urllib.parse.urlencode(dict(p.split("=", 1) for p in data.split("&") if "=" in p))
            c.request("POST", parsed.path, body=body, headers={"Content-Type": "application/x-www-form-urlencoded"})
            resp = c.getresponse()
            result = resp.read().decode("utf-8", errors="replace")
            c.close()
            return result
        except Exception as e:
            return f"Error: {e}\n"

    def cmd_fetch(self):
        try:
            subprocess.run(["git", "pull"], capture_output=True, timeout=30)
            return "200 OK\n"
        except Exception as e:
            return f"Error: {e}\n"

    def cmd_who(self):
        with _lock:
            if not self.clients:
                return "No connected clients\n"
            lines = []
            for cid, info in self.clients.items():
                lines.append(f"{cid}  {info['addr']}  {info['connected_at']}")
            return "\n".join(lines) + "\n"

    def cmd_help(self):
        return (
            "OpenTTY WebProxy - Available commands:\n"
            "  get <file>       Read a file from the server\n"
            "  http <url>       Fetch a URL\n"
            "  post <url> <data> POST data to a URL\n"
            "  fetch            Run git pull\n"
            "  who              List connected clients\n"
            "  help             Show this help\n"
            "  <any command>    Execute shell command\n"
        )

    def cmd_shell(self, cmd):
        try:
            result = subprocess.run(
                cmd, shell=True, capture_output=True, text=True, timeout=30
            )
            output = result.stdout + result.stderr
            return output if output else "(no output)\n"
        except subprocess.TimeoutExpired:
            return "Error: command timed out\n"
        except Exception as e:
            return f"Error: {e}\n"


class HTTPHandler:
    def __init__(self, tcp_proxy):
        self.tcp_proxy = tcp_proxy

    def start(self):
        import http.server

        handler = self._make_handler()

        with http.server.HTTPServer((HOST, HTTP_PORT), handler) as httpd:
            log(f"HTTP proxy listening on {HOST}:{HTTP_PORT}")
            httpd.serve_forever()

    def _make_handler(self):
        tcp_proxy = self.tcp_proxy

        class Handler(http.server.BaseHTTPRequestHandler):
            def log_message(self, fmt, *args):
                log(f"[http] {fmt % args}")

            def do_GET(self):
                path = self.path.split("?")[0]

                if path == "/":
                    self.send_response(200)
                    self.send_header("Content-Type", "text/html; charset=utf-8")
                    self.end_headers()
                    self.wfile.write(HELLO_PAGE.encode())

                elif path == "/cli":
                    self.send_response(200)
                    self.send_header("Content-Type", "text/html; charset=utf-8")
                    self.end_headers()
                    self.wfile.write(CLI_PAGE.encode())

                elif path == "/api/clients":
                    clients = tcp_proxy.clients
                    data = json.dumps(clients, indent=2)
                    self.send_response(200)
                    self.send_header("Content-Type", "application/json")
                    self.end_headers()
                    self.wfile.write(data.encode())

                elif path == "/api/exec":
                    self.send_response(200)
                    self.send_header("Content-Type", "text/plain; charset=utf-8")
                    self.end_headers()
                    self.wfile.write(b"Use POST /api/exec with cmd= parameter\n")

                else:
                    self.send_response(404)
                    self.send_header("Content-Type", "text/plain")
                    self.end_headers()
                    self.wfile.write(b"404 Not Found\n")

            def do_POST(self):
                path = self.path.split("?")[0]

                if path == "/api/exec":
                    content_length = int(self.headers.get("Content-Length", 0))
                    body = self.rfile.read(content_length).decode("utf-8")
                    params = urllib.parse.parse_qs(body)
                    cmd = params.get("cmd", [""])[0]
                    if cmd:
                        output = tcp_proxy.execute_command(cmd, "http")
                        self.send_response(200)
                        self.send_header("Content-Type", "text/plain; charset=utf-8")
                        self.end_headers()
                        self.wfile.write(output.encode())
                    else:
                        self.send_response(400)
                        self.send_header("Content-Type", "text/plain")
                        self.end_headers()
                        self.wfile.write(b"Missing cmd parameter\n")
                else:
                    self.send_response(404)
                    self.end_headers()
                    self.wfile.write(b"404\n")

        return Handler


HELLO_PAGE = """<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><title>OpenTTY WebProxy</title>
<style>
body{background:#0b0e14;color:#eef3ff;font-family:'JetBrains Mono',monospace;padding:2rem;max-width:700px;margin:auto}
h1{color:#2dd4bf}
a{color:#2dd4bf}
pre{background:#111;padding:1rem;border-radius:8px;border-left:3px solid #2dd4bf}
</style></head>
<body>
<h1>OpenTTY WebProxy</h1>
<p>Proxy server running. Connected clients visible at <a href="/api/clients">/api/clients</a></p>
<h3>Commands (TCP)</h3>
<pre>get &lt;file&gt;     - Read file from server
http &lt;url&gt;     - Fetch URL
post &lt;url&gt; &lt;data&gt; - POST to URL
fetch          - git pull
who            - List connected clients</pre>
<h3>HTTP API</h3>
<pre>GET  /api/clients     - List connected clients
POST /api/exec        - Execute command (cmd= param)</pre>
<p><a href="/cli">Open Web Terminal</a></p>
</body></html>"""

CLI_PAGE = """<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><title>OpenTTY CLI</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{background:#0b0e14;color:#bbd4ff;font-family:'JetBrains Mono',monospace;height:100vh;display:flex;flex-direction:column}
#output{flex:1;overflow-y:auto;padding:1rem;white-space:pre-wrap;font-size:0.85rem}
#input-row{display:flex;border-top:1px solid #2a3342;padding:0.5rem 1rem;gap:0.5rem}
#prompt{color:#2dd4bf;white-space:nowrap}
#cmd{flex:1;background:transparent;border:none;color:#eef3ff;font-family:inherit;font-size:0.85rem;outline:none}
</style></head>
<body>
<div id="output"></div>
<div id="input-row"><span id="prompt">proxy&gt; </span><input id="cmd" autofocus autocomplete="off" spellcheck="false"></div>
<script>
const out=document.getElementById('output'),inp=document.getElementById('cmd');
let history=[],hidx=-1;
inp.addEventListener('keydown',async e=>{
  if(e.key==='Enter'){
    const c=inp.value.trim();inp.value='';
    if(!c)return;
    history.push(c);hidx=history.length;
    out.textContent+='proxy> '+c+'\\n';
    if(c==='clear'){out.textContent='';return}
    try{
      const r=await fetch('/api/exec',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'cmd='+encodeURIComponent(c)});
      const t=await r.text();
      if(t)out.textContent+=t;
    }catch(e){out.textContent+='Error: '+e+'\\n'}
    out.scrollTop=out.scrollHeight;
  }else if(e.key==='ArrowUp'){e.preventDefault();if(hidx>0){hidx--;inp.value=history[hidx]}}
  else if(e.key==='ArrowDown'){e.preventDefault();if(hidx<history.length-1){hidx++;inp.value=history[hidx]}else{hidx=history.length;inp.value=''}}
});
</script></body></html>"""


if __name__ == "__main__":
    log("Starting OpenTTY WebProxy")
    tcp = TCPProxy()
    http_server = HTTPHandler(tcp)

    t1 = threading.Thread(target=tcp.start, daemon=True)
    t2 = threading.Thread(target=http_server.start, daemon=True)
    t1.start()
    t2.start()

    log("All services started")
    try:
        while True:
            import time
            time.sleep(3600)
    except KeyboardInterrupt:
        log("Shutting down")
        sys.exit(0)
