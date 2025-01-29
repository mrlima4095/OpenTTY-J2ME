#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
# 

import os
import sys
import time
import socket


class RemoteApp:
    def __init__(self, host='192.168.1.33', port=31522): 

        self.s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.s.connect((host, int(port)))
        self.s.settimeout(2)

        self.host = host

        self.connect()
        
    def connect(self):
        self.username = self.get("logname").strip()
        self.path = self.get("pwd").strip()

        while True:
            try:
                command = input(f"{self.username} {self.path} $ ").strip()
                command = self.get(command).strip()
                
                if (command != ""): print(command)
            except KeyboardInterrupt:
                self.path = self.get("pwd").strip()
                self.username = self.get("logname").strip()

                self.clear()
            except Exception as e:
                print("\nClosed Connection")
                self.s.close()
                break

    def get(self, command):
        try:
            self.s.sendall((command + "\n").encode())
            return self.s.recv(4096).decode()
        except Exception as e:
            self.s.close()
            return e
    def clear(self):
        if (os.name == 'windows'): os.system("cls")
        elif (os.name == 'posix'): os.system("clear") 

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("> python remote.py [HOST] [PORT]")
        sys.exit(1)

    RemoteApp(sys.argv[1], sys.argv[2])