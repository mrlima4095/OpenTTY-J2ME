#!/usr/bin/python3
# -*- coding: utf-8 -*-
#    
#    python transfer.py 
#    
#  Copyright (C) 2024 "Mr. Lima"
#  


import socketserver
import http.server
import sys, random

class CustomHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        try: file_path = sys.argv[2]
        except IndexError: file_path = "file.txt"  
        
        try:
            with open(file_path, 'rb') as file:
                self.send_response(200)
                self.send_header("Content-type", "text/plain")
                self.end_headers()
                self.wfile.write(file.read())
        except FileNotFoundError:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"File not found")
        except Exception as e:
            self.send_response(500)
            self.end_headers()
            self.wfile.write(f"Server error: {str(e)}".encode('utf-8'))

def server(port):
    try:
        with socketserver.TCPServer(("", int(port)), CustomHTTPRequestHandler) as httpd:
            print(f"[+] Server hosted at http://localhost:{port}\n")
            httpd.serve_forever()
            
    except (KeyboardInterrupt, EOFError): print("\n[-] Server stopped.")
    except ValueError: print(f"server: invalid port '{port}'")

try: server(sys.argv[1])
except IndexError: server(random.randint(1024, 9000))   
