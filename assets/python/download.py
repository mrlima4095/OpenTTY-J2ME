#!/usr/bin/python3
# -*- coding: utf-8 -*-
#    
#    python download.py 
#    
#  Copyright (C) 2023 "Mr. Lima"
#  

import os
import socket

server_ip = "192.168.1.28"
port = 4095

# Name of file that response will be save
filename = "file.txt"

# Packet that will be send
message = "com.assets.python.download"


s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((server_ip, port))

s.sendall(message.encode('utf-8'))

with open(filename, 'wt+') as file:
    file.write(s.recv(4096).decode('utf-8'))
    
s.close()
