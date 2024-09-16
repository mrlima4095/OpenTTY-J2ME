#!/usr/bin/python3
# -*- coding: utf-8 -*-
#    
#    python opentty.py
#    
#  Copyright (C) 2024 "Mr. Lima"
#  

import os
import sys
import time
import socket
import getpass


class OpenTTY:
    def __init__(self):
        self.aliases = {"true": "", "false": "", "cls": "clear"}
        self.attributes = {
            "VERSION": "1.9", "PACTH": "Netman Update", "RELEASE": "mod", "XVERSION": "0.4",
            "TTY": "/java/optty1", "HOSTNAME": "localhost", "PORT": "4095", "RESPONSE": "com.opentty.server",
            "TYPE": "linux", "CONFIG": "CLDC-1.0", "PROFILE": "MIDP-2.1", "LOCALE": "en-US", 
            "OUTPUT": "", "l": "\n"
        }
        self.history = []
        
        
        print(f"Welcome to OpenTTY {self.attributes['VERSION']}")
        print(f"Copyright (C) 2024 - Mr. Lima\n")
        
        while True:
            try:
                command = self.env(input(f"{getpass.getuser()} / $ ").strip())
                
                if command: self.processCommand(command), self.history.append(command)
            except KeyboardInterrupt: 
                break    
                
    def processCommand(self, command):
        try:
            cmd = command.split()[0].lower() 
            argument = ' '.join(command.split()[1:])   
        except IndexError: pass
            
        if cmd == "": return
        elif cmd in self.aliases: self.processCommand(f"{self.aliases[cmd]} {argument}")   
            
        elif cmd == "alias": self.alias(argument)    
        elif cmd == "basename": print(os.path.basename(argument))    
        elif cmd == "clear": self.clear()
        elif cmd == "call": print("Unavaliable API in Python Edition")   
        elif cmd == "date": print(time.ctime())
        elif cmd == "echo": print(argument)   
        elif cmd == "exit": sys.exit() 
        elif cmd == "execute": self.processCommand(argument)    
        elif cmd == "forget": self.history = []
        elif cmd == "history": print('\n'.join(self.history))
                
        else:
            print(f"{cmd}: not found")        
            
    def alias(self, argument):
        try:
            alias = argument.split()[0].lower() 
            command = ' '.join(argument.split()[1:])   
        except IndexError: return print("Usage: alias <alias> <command>")
            
        self.aliases[alias] = command  
    def clear(self):
        if os.name == "posix": os.system("clear") 
        else: os.system("cls")
    def env(self, text):
        for key in self.attributes:
            text = text.replace(f"${key}", self.attributes[key])
        
        text = text.replace("$PATH", os.getcwd())
        text = text.replace("$USERNAME", getpass.getuser())
        
        return text    
    def ifCommand(self, argument):
        print()
    
if __name__ == "__main__":
    OpenTTY()
