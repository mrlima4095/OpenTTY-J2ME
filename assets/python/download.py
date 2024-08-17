#!/usr/bin/python3
# -*- coding: utf-8 -*-
#    
#    python download.py 
#    
#  Copyright (C) 2023 "Mr. Lima"
#  

import os
import sys
import socket

def download():
    try:
        server_ip = sys.argv[1]
        port = sys.argv[2]
    except IndexError:
        print("python download.py <ip> <port> [filename]")
        
        return
        
    try: filename = sys.argv[3]
    except IndexError: filename = "file.txt"
      
    try: message = sys.argv[4]
    except IndexError: message = "com.assets.download"
      
    
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((server_ip, port))
    
    s.sendall(message.encode('utf-8'))
    
    with open(filename, 'wt+') as file:
        file.write(s.recv(4096).decode('utf-8'))
        
    s.close()
      
      
download()      