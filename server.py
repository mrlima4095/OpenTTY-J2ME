#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
#   python server.py [port]
#
# OpenTTY mirror service. One request per connection:
#
#   get <file>                  -> raw file content
#   http <url>                  -> "<status>\n<body>" (-1 on transport error)
#   post <url> <length>\n<body> -> "<status>\n<body>" (body is forwarded as-is)
#   post <url> <data>           -> raw response body (legacy 1.18 form)
#
# The length form keeps the body framing explicit, so the device can upload a
# paste larger than a single TCP segment without the mirror having to guess
# where the form body ends. The legacy form still sends everything in one
# write with no terminator, so the header is also considered complete after a
# short idle gap (see HEADER_IDLE) and its answer keeps the old body-only
# shape.

import sys, os
import socket
import threading
import urllib.request
import urllib.parse
import urllib.error
import http.client


class Server:
    MAX_REQUEST = 512 * 1024
    # How long the client may stay silent before the header counts as done.
    # Only matters for the legacy one-write form, which has no terminator.
    HEADER_IDLE = 0.3

    def __init__(self, host='0.0.0.0', port=31522):
        self.host = host
        self.port = port

    def start(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
            server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            server_socket.bind((self.host, self.port))
            server_socket.listen(65535)
            print(f"[+] Listening on port {self.port}")

            while True:
                client_socket, addr = server_socket.accept()
                client_thread = threading.Thread(target=self.handle_client, args=(client_socket, addr))
                client_thread.start()

    def handle_client(self, client_socket, addr):
        try:
            client_socket.settimeout(self.HEADER_IDLE)
            header, extra = self.read_header(client_socket)
            if not header: return

            command = header.decode('utf-8', 'replace').strip()
            if not command: return

            print(f"[+] {addr[0]} -> {command}")
            response = self.parse_command(command, client_socket, extra)
            client_socket.sendall(response.encode('utf-8'))
            client_socket.close()

        except Exception as e:
            print(f"[-] {addr[0]} -- {e}")
            try: client_socket.close()
            except Exception: pass

    def read_header(self, client_socket):
        data = b''
        while b'\n' not in data and len(data) <= 8192:
            try:
                chunk = client_socket.recv(1024)
            except socket.timeout:
                # Legacy clients never send '\n'; their single write is the
                # whole command, so the silence marks the end of the header.
                break
            if not chunk: break
            data += chunk
        client_socket.settimeout(30)
        line, _, extra = data.partition(b'\n')
        return line, extra

    @staticmethod
    def read_exact(client_socket, extra, length):
        body = bytearray(extra[:length])
        while len(body) < length:
            chunk = client_socket.recv(min(65536, length - len(body)))
            if not chunk: break
            body.extend(chunk)
        return bytes(body)

    @staticmethod
    def reply(status, body):
        return f"{status}\n{body}"

    def parse_command(self, command, client_socket=None, extra=b''):
        parts = command.split(maxsplit=1)
        cmd = parts[0]
        args = parts[1] if len(parts) > 1 else ""

        if cmd == "get": return self.get_file_content(args.strip())
        elif cmd == "http": return self.fetch_url(args.strip())
        elif cmd == "post": return self.post_request(args, client_socket, extra)
        else: return "Invalid API request\n"

    def get_file_content(self, filename):
        if not filename:
            return "Missing filename\n"
        elif not os.path.isfile(filename):
            return f"File '{filename}' not found.\n"
        elif filename.startswith("/") or ".." in filename :
            return "Permission Error\n"
        else:
            try:
                with open(filename, "rt") as f: return f.read()
            except Exception as e: return f"{e}"

    def fetch_url(self, url):
        if not url:
            return self.reply(-1, "Missing URL")
        try:
            target = url if url.startswith("http://") or url.startswith("https://") else "http://" + url
            request = urllib.request.Request(target, headers={"User-Agent": "OpenTTY-Mirror/1.19"})
            with urllib.request.urlopen(request, timeout=30) as response:
                return self.reply(response.status, response.read().decode('utf-8', 'replace'))
        except urllib.error.HTTPError as e:
            try: body = e.read().decode('utf-8', 'replace')
            except Exception: body = str(e)
            return self.reply(e.code, body)
        except Exception as e:
            return self.reply(-1, f"{e}")

    def post_request(self, args, client_socket=None, extra=b''):
        parts = args.split(maxsplit=1)
        if len(parts) < 2:
            return self.reply(-1, "URL or data is missing.")

        url, tail = parts[0], parts[1].strip()
        if tail.isdigit():
            length = int(tail)
            if length > self.MAX_REQUEST:
                return self.reply(-1, f"request body too large (max {self.MAX_REQUEST} bytes)")
            if client_socket is None:
                return self.reply(-1, "request body missing")

            body = self.read_exact(client_socket, extra, length)
            if len(body) < length:
                return self.reply(-1, "incomplete request body")
            return self.post_forward(url, body.decode('utf-8', 'replace'))

        # Legacy form: the form body travels on the command line and the answer
        # keeps its pre-1.19 shape (raw body, no status line).
        return self.post_forward(url, tail, legacy=True)

    def post_forward(self, url, body, legacy=False):
        try:
            target = url if url.startswith("http://") or url.startswith("https://") else "http://" + url
            parsed = urllib.parse.urlparse(target)
            path = parsed.path or "/"
            if parsed.query: path += "?" + parsed.query

            connection_class = http.client.HTTPSConnection if parsed.scheme == "https" else http.client.HTTPConnection
            connection = connection_class(parsed.netloc, timeout=30)
            connection.request("POST", path, body=body.encode('utf-8'), headers={
                "Content-Type": "application/x-www-form-urlencoded",
                "User-Agent": "OpenTTY-Mirror/1.19",
            })

            response = connection.getresponse()
            data = response.read().decode('utf-8', 'replace')
            status = response.status
            connection.close()

            if legacy: return data
            return self.reply(status, data)

        except Exception as e:
            if legacy: return f"{e}"
            return self.reply(-1, f"{e}")


if __name__ == '__main__':
    port = 31522
    if len(sys.argv) > 1:
        try: port = int(sys.argv[1])
        except ValueError:
            print(f"Usage: python server.py [port]")
            sys.exit(2)
    Server(port=port).start()
