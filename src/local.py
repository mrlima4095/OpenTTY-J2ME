#!/bin/python3

import tkinter as tk
import os

from time import sleep
from random import randint


manifest = {
	"MIDlet-Name": "OpenTTY",
	"MIDlet-Version": "1.13",
	"MIDlet-Description": "Terminal Emulator for J2ME",
	"MIDlet-Info-URL": "https://github.com/mrlima4095/OpenTTY-J2ME",
	"MicroEdition-Configuration": "CLDC-1.0",
	"MicroEdition-Profile": "MIDP-2.0",
	"MIDlet-Icon": "/java/bin/icon.png",
	"MIDlet-1": "OpenTTY,/java/bin/icon.png,OpenTTY",
	"MIDlet-Vendor": "Mr Lima",
}

system = {
    "microedition.platform": os.name,
    "microedition.profiles": "MIDP-2.1",
    "microedition.configuration": "CLDC-1.0"
}

aliases = {}
shell = {}
attributes = {}
paths = {}
trace = {}
objects = {}

history = []
logs = ""
path = "/",
build = "2025-1.13py-0.01",



class OpenTTY:
    def __init__(self, root):
        self.root = root
        self.root.title("OpenTTY " + manifest['MIDlet-Version'])

        self.stdout = tk.Text(root, height=15, width=50, wrap=tk.WORD)
        self.stdout.grid(row=0, column=0, padx=10, pady=10)
        self.stdout.config(state=tk.DISABLED)

        self.stdin = tk.Entry(root, width=40)
        self.stdin.grid(row=1, column=0, padx=10, pady=10)

        self.send = tk.Button(root, text="Send", command=self.sendCommand)
        self.send.grid(row=1, column=1, padx=10, pady=10)

        self.root.bind('<Return>', self.sendCommand)

        attributes['PATCH'] = "Runtime Update"
        attributes['VERSION'] = manifest['MIDlet-Version']
        attributes['RELEASE'] = "stable"
        attributes['XVERSION'] = "0.6"
        attributes['TYPE'] = system['microedition.platform']
        attributes['CONFIG'] = system['microedition.platform']
        attributes['PROFILE'] = system['microedition.profiles']

    def processCommand(self, command):
        mainCommand = command.split()[0]
        argument = ' '.join(command.split()[1:])

        if mainCommand in aliases: return self.processCommand(f"{aliases[mainCommand]} {argument}")

        if mainCommand == "echo": self.echoCommand(argument)
        elif mainCommand == "exit": self.root.quit()

        elif mainCommand == "alias": self.alias(argument) 
        elif mainCommand == "buff": self.buff(argument)

        else:
            self.echoCommand(f"{mainCommand}: not found")

    def sendCommand(self, event=None):
        command = self.stdin.get()
        self.processCommand(command)
        self.stdin.delete(0, tk.END)

    def echoCommand(self, text):
        self.stdout.config(state=tk.NORMAL)
        self.stdout.insert(tk.END, text + "\n")
        self.stdout.config(state=tk.DISABLED)

    def buff(self, argument):
        self.stdin.insert(0, argument)

    def alias(self, argument):
        if not argument: 
            for alias in aliases:
                self.echoCommand(f"alias {alias}='{aliases[alias]}'")
        elif not "=" in argument or argument in aliases: self.echoCommand(f"{argument}={aliases[argument]}")
        else: aliases[argument.split('=')[0].strip()] = ' '.join(argument.split('=')[1:].strip())    

    def unalias(self, argument):
        if argument in aliases: del aliases[argument]
        else: self.echoCommand(f"unalias: {argument}: not found")

    def runScript(self, script): 
        for command in script:
            self.processCommand(command)

    # Trace API
    def start(self, app):
        if not app: return
        else: trace[app] = randint(1000, 9999) 
    def stop(self, app):
        if not app or app not in trace: return
        else: del trace[app] 

    def x_server(self, command): 
        mainCommand = command.split()[0]
        argument = ' '.join(command.split()[1:])



if __name__ == "__main__":
    root = tk.Tk()
    app = OpenTTY(root)
    root.mainloop()
