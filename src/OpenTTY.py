#!/usr/bin/python3
# -*- coding: utf-8 -*-
#    
#    python OpenTTY.py 
#    
#  Copyright (C) 2024 "Mr. Lima"
#  

import os
import tkinter as tk
from tkinter import simpledialog

class OpenTTY:
    def __init__(self, root):
        self.app = False
        self.logs = ""
        self.path = "/"
        self.version = "1.9"
        self.paths = {}
        self.aliases = {}
        self.commandHistory = []
        self.attributes = {
            "PATCH": "OpenTTY Revolution",
            "VERSION": self.version,
            "RELEASE": "stable",
            "XVERSION": "0.5",
            "TTY": "/java/optty1",
            "HOSTNAME": "localhost",
            "PORT": "4095",
            "RESPONSE": "com.opentty.server",
            "QUERY": "nano",
            "TYPE": "unix",
            "CONFIG": "CLDC-1.1",
            "PROFILE": "MIDP-2.1",
            "LOCALE": "en_US",
            "OUTPUT": "",
        }
        self.username = self.loadRMS("OpenRMS", 1)
        self.nanoContent = self.loadRMS("nano")
        
        # Configuração inicial do GUI
        self.root = root
        self.root.title(f"OpenTTY {self.version}")
        
        # Componentes da Interface
        self.output = tk.Text(root, height=20, state='disabled', wrap='word')
        self.output.pack(fill='both', expand=True)

        self.commandInput = tk.Entry(root)
        self.commandInput.pack(fill='x')
        
        # Botões de comando
        buttonFrame = tk.Frame(root)
        buttonFrame.pack(fill='x')
        tk.Button(buttonFrame, text="Send", command=self.commandAction).pack(side='right')
        tk.Button(buttonFrame, text="Help", command=self.processCommand("help")).pack(side='left')
        tk.Button(buttonFrame, text="Nano", command=self.nano).pack(side='left')
        tk.Button(buttonFrame, text="Clear", command=self.clearCommand).pack(side='left')
        tk.Button(buttonFrame, text="History", command=self.historyCommand).pack(side='left')

        if not self.app:
            self.commandInput.insert(0, f"{self.username} {self.path} $ ")
            self.app = True
            self.print(f"Welcome to OpenTTY {self.version}\nCopyright (C) 2024 - Mr. Lima\n")
            
            if self.username == "": self.username = self.login()
            self.processCommand("run initd")

    def commandAction(self):
        command = self.commandInput.get().strip()
        if command:
            self.commandHistory.append(command)
            self.commandInput.delete(0, 'end')
            self.processCommand(command)

    def processCommand(self, command):
        self.print(command)


    def print(self, text): self.output.configure(state='normal'), self.output.insert('end', text + "\n"), self.output.configure(state='disabled'), self.output.see('end')

    def clearCommand(self):
        self.output.configure(state='normal')
        self.output.delete('1.0', 'end')
        self.output.configure(state='disabled')

    def historyCommand(self):
        self.showHistory()

    def showHistory(self):
        self.print("\nCommand History:\n")
        for i, cmd in enumerate(self.commandHistory):
            self.printOutput(f"{i + 1}: {cmd}\n")

    def nano(self, filename=""):
        self.print("Nano editor opened...\n")
        # Exemplo básico de editor em um diálogo para edição
        content = simpledialog.askstring("Nano Editor", "Edit content:", initialvalue=self.nanoContent)
        if content:
            self.nanoContent = content
            self.print(f"Content saved to nano.\n")

    def login(self):
        while True:
            username = simpledialog.askstring("Login", "Enter your username:")

            if username: break

        return username

    def loadRMS(self, name):
        # Placeholder para simular a leitura de RMS (record management system)
        return "user" if name == "OpenRMS" else ""

# Inicializar a aplicação Tkinter
if __name__ == "__main__":
    root = tk.Tk()
    app = OpenTTY(root)
    root.mainloop()
