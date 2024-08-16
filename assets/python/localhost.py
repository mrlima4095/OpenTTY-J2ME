#!/usr/bin/python3
# -*- coding: utf-8 -*-
#    
#    python localhost.py 
#    
#  Copyright (C) 2023 "Mr. Lima"
#  


import socketserver
import http.server

def server(port):
    try:
        with socketserver.TCPServer(("", int(port)), http.server.SimpleHTTPRequestHandler) as httpd:
            print(f"Server openned at http://localhost:{port}...\n")
            httpd.serve_forever()
            
    except (KeyboardInterrupt, EOFError): print("\nServer stopped.")
    except ValueError: print(f"server: invalid port '{port}'")

server(4095)
