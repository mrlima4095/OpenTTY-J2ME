#!/usr/bin/python3
# -*- coding: utf-8 -*-
#    
#    python localhost.py 
#    
#  Copyright (C) 2023 "Mr. Lima"
#  



import socketserver
import http.server
import sys, random

def server(port):
    try:
        with socketserver.TCPServer(("", int(port)), http.server.SimpleHTTPRequestHandler) as httpd:
            print(f"[+] Server hosted at http://localhost:{port}\n")
            httpd.serve_forever()
            
    except (KeyboardInterrupt, EOFError): print("\n[-] Server stopped.")
    except ValueError: print(f"server: invalid port '{port}'")

try: server(sys.argv[1])
except IndexError: server(random.randint(1024, 9000))   
