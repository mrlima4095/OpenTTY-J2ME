#!/usr/bin/env python
# -*- coding: utf-8 -*-
# 

import os
import sys
import time
import socket


class BackupApp:
    def __init__(self, host='192.168.1.33', port=31522): 

        self.s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.s.connect((host, int(port)))
        self.s.settimeout(10)

        self.host = host

        self.connect()
        
    def connect(self):
        files = self.get("dir v").split('\n')

        for file in files:
            content = self.get("cat " + file).strip()

            try: os.makedirs("backup")
            except: pass

            with open("backup/" + file, "wt+") as output:
                output.write(content)

            time.sleep(1)
            print("[Backup] Downloaded file '" + file + "'")

        self.get("execute clear; gc")


    def get(self, command):
        try:
            self.s.sendall((command + "\n").encode())
            return self.s.recv(4096).decode()
        except Exception as e:
            self.s.close()
            return e

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("> python backup.py [HOST] [PORT]")
        sys.exit(1)

    BackupApp(sys.argv[1], sys.argv[2])