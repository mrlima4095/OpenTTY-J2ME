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

import sys
import socket
import subprocess
import threading
import urllib.request
import urllib.parse
import http.client
import os

class Server:
    def __init__(self, host='0.0.0.0', port=31522, blacklist_file=None):
        self.host = host
        self.port = port
        self.blacklist_file = blacklist_file

    def blacklist(self, file):
        if file and os.path.isfile(file):
            with open(file, "rt") as f:
                return set(f.read().splitlines())
        return set()

    def start(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
            server_socket.bind((self.host, self.port))
            server_socket.listen(50)
            print(f"Listening on port {self.port}")

            while True:
                client_socket, addr = server_socket.accept()
                if addr[0] in self.blacklist(self.blacklist_file):
                    print(f"[-] {addr[0]} is blocked")
                    client_socket.sendall("You are on the server blacklist!".encode('utf-8'))
                    client_socket.close()
                else:
                    client_thread = threading.Thread(target=self.handle_client, args=(client_socket, addr))
                    client_thread.start()

    def handle_client(self, client_socket, addr):
        print(f"[+] {addr[0]} connected")

        try:
            while True:
                command = client_socket.recv(4096).decode('utf-8').strip()
                if not command:
                    print(f"[-] {addr[0]} disconnected")
                    break

                print(f"[+] {addr[0]} -> {command}")
                response = self.parse_command(command)
                client_socket.sendall(response.encode('utf-8'))

        except Exception as e:
            print(f"[-] {addr[0]} -- {e}")

        finally:
            client_socket.close()

    def parse_command(self, command):
        parts = command.split(maxsplit=1)
        cmd = parts[0]
        
        if cmd == "get": return self.get_file_content(parts[1] if len(parts) > 1 else "")
        elif cmd == "http": return self.fetch_url(parts[1] if len(parts) > 1 else "")
        elif cmd == "post": return self.post_request(parts[1] if len(parts) > 1 else "")
        #else: return self.execute_command(command)

    def get_file_content(self, filename):
        if not filename:
            return "Filename is missing."
        elif not os.path.isfile(filename):
            return f"File '{filename}' not found."
        else:
            try:
                with open(filename, "rt") as f:
                    return f.read()
            except Exception as e:
                return f"Error opening file: {e}"

    def fetch_url(self, url):
        if not url:
            return "URL is missing."
        try:
            with urllib.request.urlopen(url if url.startswith("http://") or url.startswith("https://") else "http://" + url) as response:
                return response.read().decode('utf-8')
        except Exception as e:
            return f"Error accessing URL: {e}"

    def post_request(self, post_command):
        try:
            parts = post_command.split(maxsplit=1)
            if len(parts) < 2:
                return "URL or data is missing."
                
            url, data = parts
            data_dict = dict(param.split('=') for param in data.split('&'))
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
            return f"[-] {e}"

    def execute_command(self, command):
        try: return subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8')
        except subprocess.CalledProcessError as e: return e.output.decode('utf-8')

if __name__ == '__main__':
    blacklist_file = sys.argv[1] if len(sys.argv) > 1 else None
    server = Server(blacklist_file=blacklist_file)
    server.start()
