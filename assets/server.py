#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
#   python server.py
#
#       OpenTTY Proxy Server (default) to provide Yang Packages, and it
#   connections to World Wide Web if device haven't support to HTTPS and 
#   TLS. You can use the HTTP Proxy (by nnproject, can charge this url at
#   attribute 'MIDlet-Proxy' of MANIFEST.MF). Recommended host your own
#   session if you wanna modify this software or don't like to leave logs
#   that you have connected there. (I'M NOT PARTNER OF nnproject).
#

import sys, os
import socket
import subprocess
import threading
import urllib.request
import urllib.parse
import http.client

from datetime import datetime

class Server:
    def __init__(self, host='0.0.0.0', port=31521):
        self.host = host
        self.port = port

    def start(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
            server_socket.bind((self.host, self.port))
            server_socket.listen(65535)
            print(f"[+] Listening on port {self.port}")

            while True:
                client_socket, addr = server_socket.accept()
                client_thread = threading.Thread(target=self.handle_client, args=(client_socket, addr))
                client_thread.start()

    def handle_client(self, client_socket, addr):
        try:
            command = client_socket.recv(4096).decode('utf-8').strip()
            if not command: return
                

            print(f"[+] {addr[0]} -> {command}")
            response = self.parse_command(command)
            client_socket.sendall(response.encode('utf-8'))
            client_socket.close()

        except Exception as e:
            print(f"[-] {addr[0]} -- {e}")
            client_socket.close()
            
    def parse_command(self, command):
        parts = command.split(maxsplit=1)
        cmd = parts[0]

        if cmd == "get": return self.get_file_content(parts[1] if len(parts) > 1 else "")
        elif cmd == "http": return self.fetch_url(parts[1] if len(parts) > 1 else "")
        elif cmd == "post": return self.post_request(parts[1] if len(parts) > 1 else "")
        elif cmd == "fetch": os.system("git pull"); return "200 OK"
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
            return "Missing URL\n"
        try:
            with urllib.request.urlopen(url if url.startswith("http://") or url.startswith("https://") else "http://" + url) as response:
                return response.read().decode('utf-8')
        except Exception as e:
            return f"{e}"

    def post_request(self, post_command):
        try:
            parts = post_command.split(maxsplit=1)
            if len(parts) < 2:
                return "URL or data is missing.\n"

            url, data = parts
            data_dict = dict(param.split('=', 1) for param in data.split('&') if '=' in param)
            encoded_data = urllib.parse.urlencode(data_dict).encode('utf-8')

            parsed_url = urllib.parse.urlparse(url)
            connection_class = http.client.HTTPSConnection if parsed_url.scheme == "https" else http.client.HTTPConnection
            connection = connection_class(parsed_url.netloc)
            connection.request("POST", parsed_url.path, body=encoded_data,
                               headers={"Content-Type": "application/x-www-form-urlencoded"})

            response = connection.getresponse()
            response_data = response.read().decode('utf-8')
            connection.close()

            return response_data

        except Exception as e:
            return f"{e}"

if __name__ == '__main__':
    Server().start()
