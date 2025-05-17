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
    def __init__(self):
        self.config = {}

        with open("server.properties", "r") as file:
            for line in file.readlines():
                line = line.strip()
                if "=" in line:
                    key, value = line.split("=", 1)
                    self.config[key.strip()] = value.strip()

        self.host = self.config['host']
        self.port = self.config['port']

    def start(self):

        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
            server_socket.bind((self.host, self.port))
            server_socket.listen(31522)
            print(f"[+] Listening on port {self.port}")

            while True:
                client_socket, addr = server_socket.accept()
                client_thread = threading.Thread(target=self.handle_client, args=(client_socket, addr))
                client_thread.start()

    def handle_client(self, client_socket, addr):
        print(f"[+] {addr[0]} connected")

        passwd = client_socket.recv(256).decode('utf-8').strip()
        if passwd != self.config['passwd']: 
            client_socket.sendall("Wrong password".encode('utf-8'))
            client_socket.close()

            return

        try:
            while True:
                command = client_socket.recv(4096).decode('utf-8').strip()
                if not command:
                    print(f"[-] {addr[0]} disconnected")
                    break

                client_socket.sendall(self.execute_command(command).encode('utf-8'))

        except Exception as e:
            print(f"[-] {addr[0]} -- {e}")

        finally:
            client_socket.close()

    def execute_command(self, command):
        try: return subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8')
        except subprocess.CalledProcessError as e: return e.output.decode('utf-8')

if __name__ == '__main__':
    Server().start()
