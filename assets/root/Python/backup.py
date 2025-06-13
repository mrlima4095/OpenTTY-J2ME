#!/usr/bin/env python
# -*- coding: utf-8 -*-
# 
#   python backup.py [HOST] [PORT]
#
#       Connect to OpenTTY and backup your RMS files in 
#   local Device. Required to host a Bind Server in OpenTTY
#   and prompt the HOST IP Address and PORT of Server to 
#   make the connection.

import os
import sys
import time
import socket


class BackupApp:
    def __init__(self, host='192.168.1.17', port=31522): 

        self.s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.s.connect((host, int(port)))
        self.s.settimeout(10)

        self.host = host

        self.connect()
        
    def connect(self):
        files = self.get("execute cd /home; dir").split('\n')

        try: os.makedirs("backup")
        except: pass
        
        for file in files:
            content = self.get("cat " + file).strip()


            with open("backup/" + file, "wt+") as output:
                output.write(content)

            time.sleep(1)
            print("[Backup] Downloaded file '" + file + "'")

        self.get("execute clear; gc")


    def get(self, command):
        try:
            self.s.sendall((command + "\n").encode())
            return self.s.recv(1000000).decode()
        except Exception as e:
            self.s.close()
            return e

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("> python backup.py [HOST] [PORT]")
        sys.exit(1)

    BackupApp(sys.argv[1], sys.argv[2])