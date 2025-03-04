#!/bin/python3

import tkinter as tk
import http.client
import threading
import getpass
import socket
import time
import os

from time import sleep
from random import randint
from datetime import datetime
from tkinter import messagebox


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
    "microedition.configuration": "CLDC-1.0",
}

aliases = {}
shell = {}
attributes = {
    'PATCH': "Runtime Update",
    'VERSION': manifest['MIDlet-Version'],
    'RELEASE': "stable",
    'XVERSION': "0.6",
    'TYPE': system['microedition.platform'],
    'CONFIG': system['microedition.platform'],
    'PROFILE': system['microedition.profiles'],
}
paths = {}
trace = {}
objects = {}

history = []
username = ""
nanoContent = ""
logs = ""
path = "/"
build = "2025-1.13py-0.01"



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

        self.runScript(open(f"{os.getcwd()}/java/etc/initd.sh", "r").read())

        self.writeRMS("OpenRMS", getpass.getuser())
        nanoContent = self.loadRMS("nano")

        self.runScript(self.loadRMS("initd"))

    def processCommand(self, command, user=True):
        command = command if command.startswith("exec") else self.env(command)
        mainCommand = self.getCommand(command).lower()
        argument = self.getArgument(command)

        if mainCommand in aliases and user == True: return self.processCommand(f"{aliases[mainCommand]} {argument}")

        # Network Utilities
        elif mainCommand == "query": self.query(argument)
        elif mainCommand == "ping": self.pingCommand(argument)
        elif mainCommand == "bind":
        elif mainCommand == "gaddr":
        elif mainCommand == "http":
        elif mainCommand == "gobuster":
        elif mainCommand == "prscan":
        elif mainCommand == "nc":
        elif mainCommand == "server":
        elif mainCommand == "curl":
        elif mainCommand == "wget":
        elif mainCommand == "fw":
        elif mainCommand == "org":
        elif mainCommand == "genip":
        elif mainCommand == "netstat":
        elif mainCommand == "ifconfig":

        # File Utilities
        elif mainCommand == "pwd":
        elif mainCommand == "rm":
        elif mainCommand == "sed":
        elif mainCommand == "raw":
        elif mainCommand == "nano":
        elif mainCommand == "unmount":
        elif mainCommand == "rraw":
        elif mainCommand == "getty":
        elif mainCommand == "pjnc":
        elif mainCommand == "ls":
        elif mainCommand == "html":
        elif mainCommand == "install":
        elif mainCommand == "json":
        elif mainCommand == "add":
        elif mainCommand == "touch" or mainCommand == "rnano":
        elif mainCommand == "cat":
        elif mainCommand == "get":
        elif mainCommand == "cp":
        elif mainCommand == "fdisk":
        elif mainCommand == "du":
        elif mainCommand == "ph2s":
        elif mainCommand == "cd":
        elif mainCommand == "dir":
        elif mainCommand == "dd":
        elif mainCommand == "find":
        elif mainCommand == "grep":
        elif mainCommand == "mount": 
            if not argument: return 

            if argument.startswith("/"):
                argument = self.env(open(f"{os.getcwd()}{argument}"))
            elif argument == "nano":
                argument = nanoContent
            else:
                argument = self.loadRMS(argument)
            self.mount(argument)

        # General
        elif mainCommand == "alias": self.alias(argument) 
        elif mainCommand == "buff": self.echoCommand("not yet")
        elif mainCommand == "bruteforce": 
            self.start("bruteforce")
            while "bruteforce" in trace:
                self.processCommand(argument)
        elif mainCommand == "bg": threading.Thread(target=self.processCommand, args=(argument,)).start()
        elif mainCommand == "builtin" or mainCommand == "command": self.processCommand(argument, user=False)
        elif mainCommand == "basename": self.echoCommand(os.path.basename(argument))
        elif mainCommand == "build": self.echoCommand(build)
        elif mainCommand == "chmod": self.chmod(argument)
        elif mainCommand == "case": self.caseCommand(argument)
        elif mainCommand == "cron": return
        elif mainCommand == "cal": return
        elif mainCommand == "call": return
        elif mainCommand == "clear": self.stdout.config(state=tk.NORMAL), self.stdout.delete(1.0, tk.END), self.stdout.config(state=tk.DISABLED)
        elif mainCommand == "date": self.echoCommand(datetime.now().strftime(f"%a %b %d %H:%M:%S {time.strftime('%Z') or 'GMT'}%z %Y"))
        elif mainCommand == "debug": self.runScript(self.env(open(f"{os.getcwd()}/scripts/debug.sh").read()))
        elif mainCommand == "env": self.set(argument)
        elif mainCommand == "echo": self.echoCommand(argument)
        elif mainCommand == "exit": self.root.quit()
        elif mainCommand == "export": 
            if not argument: self.set()
            else: attributes[argument] = ""
        elif mainCommand == "execute": 
            for cmd in argument.split(';'):
                self.processCommand(cmd)
        elif mainCommand == "exec": 
            for cmd in argument.split('&'): 
                self.processCommand(cmd)
        elif mainCommand == "forget": history = []
        elif mainCommand == "for": return
        elif mainCommand == "gc": return
        elif mainCommand == "hostname": self.echoCommand(attributes['HOSTNAME'])
        elif mainCommand == "htop": return
        elif mainCommand == "help": self.viewer("OpenTTY Help", self.env(open(f"{os.getcwd()}/java/help.txt").read()))
        elif mainCommand == "hash": 
            if not argument: return 
            if argument.startswith("/"):
                argument = self.env(open(f"{os.getcwd()}{argument}"))
            elif argument == "nano":
                argument = nanoContent
            else:
                argument = self.loadRMS(argument)

            h = 0
            for char in argument:
                h = (31 * h + ord(char)) & 0xFFFFFFFF  # Limitar para 32 bits (igual ao int do Java)
            if h > 0x7FFFFFFF:
                h -= 0x100000000  # Converte para inteiro com sinal
            self.echoCommand(f"{h}")
        elif mainCommand == "hostid": return
        elif mainCommand == "history":
        elif mainCommand == "if":
        elif mainCommand == "java": return
        elif mainCommand == "kill": self.kill(argument)
        elif mainCommand == "log":
        elif mainCommand == "logcat":
        elif mainCommand == "logout":
        elif mainCommand == "locale":
        elif mainCommand == "lock":
        elif mainCommand == "mmspt":
        elif mainCommand == "mail":
        elif mainCommand == "open":
        elif mainCommand == "pkg":
        elif mainCommand == "ps":
        elif mainCommand == "proxy":
        elif mainCommand == "run": 
            if not argument: self.runScript(nanoContent)
            else: self.runScript(self.loadRMS(argument)) 
        elif mainCommand == "report":
        elif mainCommand == "reset": self.echoCommand("AutoRunError: null")
        elif mainCommand == "sleep": time.sleep(int(argument))
        elif mainCommand == "seed": self.echoCommand(randint(0, int(argument)))
        elif mainCommand == "set": self.set(argument)
        elif mainCommand == "start": self.start(argument)
        elif mainCommand == "stop": self.stop(argument)
        elif mainCommand == "sh" or mainCommand == "login": 
        elif mainCommand == "true" or mainCommand == "false" or mainCommand == "" or mainCommand.startswith("#"): return
        elif mainCommand == "tick": 
            if argument == "label": self.echoCommand("")
            else: return 
        elif mainCommand == "time": self.echoCommand(datetime.now().strftime(f"%H:%M:%S"))
        elif mainCommand == "tty": self.echoCommand(self.env("$TTY"))
        elif mainCommand == "ttysize":
        elif mainCommand == "trim": return
        elif mainCommand == "title":
            if not argument: self.root.title(self.env("OpenTTY $VERSION"))
            else: self.root.title(argument) 
        elif mainCommand == "todo":
        elif mainCommand == "trace":
        elif mainCommand == "top":
        elif mainCommand == "unalias":
        elif mainCommand == "uname":
        elif mainCommand == "unset": self.unset(argument)
        elif mainCommand == "vnt":
        elif mainCommand == "vendor":
        elif mainCommand == "version":
        elif mainCommand == "whoami": self.echoCommand(username)
        elif mainCommand == "warn":
        elif mainCommand == "xterm":
        elif mainCommand == "x11":

        elif mainCommand == "about":
        elif mainCommand == "import":

        elif mainCommand == "clone":
        elif mainCommand == "sign":

        elif mainCommand == "@stop":
        elif mainCommand == "@exec":
        elif mainCommand == "@login":
        elif mainCommand == "@screen":
        elif mainCommand == "@alert":
        elif mainCommand == "@reload":

        elif mainCommand == "!":
        elif mainCommand == ".":

        else: self.echoCommand(f"{mainCommand}: not found")

    def getCommand(self, command): 
        try: 
            if not command: return ""
            else: return command.split()[0]
        except IndexError: 
            return ""
    def getArgument(self, command):
        if not command or len(command.split()) < 1: return ""
        else: return ' '.join(command.split()[1:])
    def env(self, text): 
        text = text.replace("$PATH", path)
        text = text.replace("$USERNAME", username)
        text = text.replace("$TITLE", self.root.title())
        text = text.replace("$PROMPT", self.stdin.get())
        text = text.replace("\\n", "\n")
        text = text.replace("\\r", "\r")
        text = text.replace("\\t", "\t")

        for key in attributes:
            text = text.replace(f"${key}", attributes[key])

        return text

    def sendCommand(self, event=None):
        command = self.stdin.get()
        self.processCommand(command)
        self.stdin.delete(0, tk.END)

    

    # Registry API (aliases & env keys)
    def alias(self, argument):
        if not argument: 
            for alias in aliases:
                self.echoCommand(f"alias {alias}='{aliases[alias]}'")
        elif not "=" in argument or argument in aliases: self.echoCommand(f"{argument}={aliases[argument]}")
        else: aliases[argument.split('=')[0].strip()] = ' '.join(argument.split('=')[1:]).strip()
    def unalias(self, argument):
        if argument in aliases: del aliases[argument]
        else: self.echoCommand(f"unalias: {argument}: not found")
    def set(self, argument):
        if not argument: 
            for key in attributes:
                if attributes[key] == "": continue
                else: self.echoCommand(f"{key}='{attributes[key]}'")
        elif not "=" in argument or argument in attributes: self.echoCommand(f"{argument}={attributes[argument]}")
        else: attributes[argument.split('=')[0].strip()] = ' '.join(argument.split('=')[1:]).strip()
    def unalias(self, argument):
        if argument in attributes: del attributes[argument]

    # File API (RMS emulation)
    def writeRMS(self, name, data):
        if not name: return 

        try:
            with open(os.path.expanduser(f"~/.opentty/rms/{name}.txt"), "wt+") as reg:
                reg.write(data)
        except FileNotFoundError:
            os.makedirs(os.path.expanduser(f"~/.opentty/rms"))
            self.writeRMS(name, data)
    def loadRMS(self, name):
        if not name: return 

        try:
            with open(os.path.expanduser("~") + f"/.opentty/rms/{name}.txt", "r") as reg:
                return reg.read()
        except FileNotFoundError:
            self.writeRMS(name, "")
            return ""
    def deleteFile(self, name):
        if not name: return

        if os.path.exists(os.path.expanduser(f"~/.opentty/rms/{name}.txt")): os.remove(os.path.expanduser(f"~/.opentty/rms/{name}.txt"))
        else: self.echoCommand(f"rm: {name}: not found")

    def echoCommand(self, text):
        self.stdout.config(state=tk.NORMAL)
        self.stdout.insert(tk.END, "\n" + text)
        self.stdout.config(state=tk.DISABLED)

        attributes['OUTPUT'] = text
    def warnCommand(self, title, message):
        if not message: return

        messagebox.showinfo(title, message)

    def viewer(self, title, text, size="300x150"): 
        if not text or not text: return

        viewer = tk.Toplevel(root)
        viewer.title(title)
        viewer.geometry(size)
        label = tk.Label(viewer, text=text)
        label.pack(pady=20)

    def runScript(self, script): 
        for command in script.splitlines():
            self.processCommand(command)


    def mount(self, script):
        lines = script.split('\n')
        for line in lines:
            if line:
                line = line.strip()
                if line.startswith("#"):
                    continue
                if line.startswith("/"):
                    full_path = ""
                    start = 0
                    for j in range(1, len(line)):
                        if line[j] == '/':
                            dir = line[start + 1:j]
                            full_path += "/" + dir
                            self.addDirectory(full_path)
                            start = j
                    dir = line[start + 1:]
                    if dir:
                        full_path += "/" + dir
                        self.addDirectory(full_path)
    def addDirectory(self, full_path):
        if full_path not in self.paths:
            self.paths[full_path] = [".."]
            parent_path = full_path[:full_path.rfind('/')]
            if not parent_path:
                parent_path = "/"
            parent_contents = self.paths.get(parent_path, [])
            updated_contents = parent_contents[:]
            updated_contents.append(full_path[full_path.rfind('/') + 1:])
            self.paths[parent_path] = updated_contents
    def ifCommand(self, argument):
        argument = argument.strip()
        first_parenthesis = argument.find('(')
        last_parenthesis = argument.find(')')
        
        if first_parenthesis == -1 or last_parenthesis == -1 or first_parenthesis > last_parenthesis: return self.echoCommand("if (expr) [command]")
            
        expression = argument[first_parenthesis + 1:last_parenthesis].strip()
        command = argument[last_parenthesis + 1:].strip()
        parts = expression.split(' ')
        
        if len(parts) == 3:
            if parts[1] == "startswith":
                if parts[0].startswith(parts[2]):
                    self.processCommand(command)
            elif parts[1] == "endswith":
                if parts[0].endswith(parts[2]):
                    self.processCommand(command)
            elif parts[1] == "!=":
                if parts[0] != parts[2]:
                    self.processCommand(command)
            elif parts[1] == "==":
                if parts[0] == parts[2]:
                    self.processCommand(command)
        elif len(parts) == 2:
            if parts[0] == parts[1]:
                self.processCommand(command)
        elif len(parts) == 1:
            if parts[0] != "":
                self.processCommand(command)
    def caseCommand(self, argument):
        argument = argument.strip()

        first_parenthesis = argument.find('(')
        last_parenthesis = argument.find(')')

        if first_parenthesis == -1 or last_parenthesis == -1 or first_parenthesis > last_parenthesis: return

        method = self.getCommand(argument)
        expression = argument[first_parenthesis + 1:last_parenthesis].strip()
        command = argument[last_parenthesis + 1:].strip()

        if method == "file": 
            if os.path.exists(os.path.expanduser(f"~/.opentty/rms/{expression}.txt")):
                self.processCommand(command)
        elif method == "root": self.processCommand(command)
        elif method == "thread": return
        elif method == "trace": 
            if expression in trace: 
                self.processCommand(command)

        elif method == "!file": 
            if not os.path.exists(os.path.expanduser(f"~/.opentty/rms/{expression}.txt")):
                self.processCommand(command)
        elif method == "!root": self.processCommand(command)
        elif method == "!thread": return
        elif method == "!trace": 
            if not expression in trace: 
                self.processCommand(command)
    def forCommand(self, argument):
        argument = argument.strip()
        firstParenthesis = argument.find('(')
        lastParenthesis = argument.find(')')

        if firstParenthesis == -1 or lastParenthesis == -1 or firstParenthesis > lastParenthesis: return

        key = getCommand(argument)
        file = argument[firstParenthesis + 1:lastParenthesis].strip()
        command = argument[lastParenthesis + 1:].strip()

        if key.startswith("("):
            return

        if key.startswith("$"):
            key = key.replace("$", "")

        if file.startswith("/"):
            file = self.env(open(f"{os.getcwd()}{file}"))
        elif file == "nano":
            file = nanoContent
        else:
            file = self.loadRMS(file)

        lines = file.split("\n")

        for line in lines:
            if line:
                attributes[key] = line
                self.processCommand("set " + key + "=" + line)
                self.processCommand(command)
                del attributes[key]
    def StringEditor(self, command):
        command = env(command.strip())
        mainCommand = getCommand(command).lower()
        argument = getArgument(command)

        if mainCommand == "-2u": nanoContent = nanoContent.upper()
        elif mainCommand == "-2l": nanoContent = nanoContent.lower()
        elif mainCommand == "-d": nanoContent = nanoContent.replace(argument.split(" ")[0], "")
        elif mainCommand == "-a": nanoContent = argument if nanoContent == "" else nanoContent + "\n" + argument
        elif mainCommand == "-r":
            parts = argument.split(" ")
            if len(parts) >= 2: nanoContent = nanoContent.replace(parts[0], parts[1])
        elif mainCommand == "-l":
            try:
                i = int(argument)
                lines = nanoContent.split("\n")
                if i >= 0 and i < len(lines):
                    self.echoCommand(lines[i])
            except ValueError as e: return
        elif mainCommand == "-s":
            try:
                i = int(getCommand(argument))
                div = getArgument(argument)
                parts = nanoContent.split(div)
                if i >= 0 and i < len(parts):
                    self.echoCommand(parts[i])
                else:
                    self.echoCommand("null")
            except ValueError as e: self.echoCommand("null")
        elif mainCommand == "-p":
            lines = nanoContent.split("\n")
            updatedContent = "\n".join(argument + line for line in lines)
            nanoContent = updatedContent.strip()
        elif mainCommand == "-v":
            lines = nanoContent.split("\n")
            reversedLines = "\n".join(reversed(lines))
            nanoContent = reversedLines.strip()

    def chmod(self, node):
        if not node: return

        if node == "http": node = "javax.microedition.io.Connector.http"
        elif node == "socket": node = "javax.microedition.io.Connector.socket"
        elif node == "file": node = "javax.microedition.io.Connector.file"
        elif node == "prg": node = "javax.microedition.io.PushRegistry"
        else: return self.echoCommand(f"chmod: {node}: not found")
        
        self.MIDletLogs(f"add info Permission '{node}' granted");

    # Trace API
    def start(self, app):
        if not app: return
        else: trace[app] = f"{randint(1000, 9999)}" 
    def stop(self, app):
        if not app or app not in trace: return
        else: del trace[app] 

        if app == "sh": self.processCommand("exit")
    def kill(self, pid):
        if not pid: return

        for app in trace:
            if trace[app] == pid:
                if app == "sh": self.processCommand("exit") 

                del trace[app]
                return

    # MIDlet Services Command Processor
    def xserver(self, command): 
        mainCommand = command.split()[0]
        argument = ' '.join(command.split()[1:])

        if mainCommand == "": self.viewer("OpenTTY X.Org", self.env("OpenTTY X.Org - X Server $XVERSION\nRelease Date: 2024-11-27\nX Protocol Version 1, Revision 3\nBuild OS: $TYPE"))
        elif mainCommand == "title": self.root.title(argument)
        elif mainCommand == "term": return 
        elif mainCommand == "version": self.echoCommand(self.env("X Server $XVERSION"))
        elif mainCommand == "stop": return
        elif mainCommand == "tick": return
        elif mainCommand == "init":
        elif mainCommand == "cmd":
        elif mainCommand == "":
        elif mainCommand == "":
        elif mainCommand == "":
    def MIDletLogs(self, command):
        mainCommand = command.split()[0]
        argument = ' '.join(command.split()[1:])

        if mainCommand == "": return
        elif mainCommand == "clear": logs = ""
        elif mainCommand == "swap": self.writeRMS(argument if argument else "logs", logs)
        elif mainCommand == "view": self.viewer(self.root.title(), logs)
        elif mainCommand == "add": 
            if not argument: return
            
            if argument.split()[0] == "info": logs = logs + f"\n[INFO] {datetime.now().strftime('%H:%M:%S')} {' '.join(argument.split()[1:])}"
            elif argument.split()[0] == "warn": logs = logs + f"\n[INFO] {datetime.now().strftime('%H:%M:%S')} {' '.join(argument.split()[1:])}"
            elif argument.split()[0] == "debug": logs = logs + f"\n[INFO] {datetime.now().strftime('%H:%M:%S')} {' '.join(argument.split()[1:])}"
            elif argument.split()[0] == "error": logs = logs + f"\n[INFO] {datetime.now().strftime('%H:%M:%S')} {' '.join(argument.split()[1:])}"
            else: self.echoCommand(f"log: add: {argument.split()[0]}: level not found")  

        else: self.echoCommand(f"log: {mainCommand}: not found")


    

    
    # Lib API Service
    def importScript(self, script):


    # Network API Service
    def pingCommand(self, url):
        if not url: return
        if not url.startswith("http://") and not url.startswith("https://"): url = "http://" + url
        
        try:
            start_time = time.time()
            conn = http.client.HTTPConnection(host, timeout=5)
            conn.request("GET", "/")
            res = conn.getresponse()
            end_time = time.time()
            self.echoCommand(f"Ping to {url} successful, time={int((end_time - start_time) * 1000)}ms")
        except requests.exceptions.RequestException as e:
            self.echoCommand(f"Ping to {url} failed: {str(e)}")
    def query(self, command):
        command = self.env(command.strip())
        mainCommand = self.getCommand(command).lower()
        argument = self.getArgument(command)

        if not mainCommand: return self.echoCommand("query: missing [addr]")
        if not argument: return self.echoCommand("query: missing [data]")
            
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                host, port = mainCommand.split(':')
                sock.connect((host, int(port)))

                sock.sendall((argument + "\n").encode())
                data = sock.recv(4096).decode()

                if not env("$QUERY") or env("$QUERY") == "$QUERY":
                    echoCommand(data)
                    MIDletLogs("add warn Query storage setting not found")
                elif env("$QUERY").lower() == "show":
                    echoCommand(data)
                elif env("$QUERY").lower() == "nano":
                    global nanoContent
                    nanoContent = data
                    echoCommand("query: data retrieved")
                else:
                    writeRMS(env("$QUERY"), data)
        except Exception as e:
            echoCommand(str(e))


    # X Server

class QuestWindow:
    def __init__(self, parent, title, text=""):
        self.window = tk.Toplevel(parent)
        self.window.title(title)
        self.window.geometry("300x150")
        self.window.resizable(False, False)

        self.label = tk.Label(self.window, text="")
        self.label.pack(pady=10)

        self.entry = tk.Entry(self.window, width=30)
        self.entry.pack(pady=5)

        self.send_button = tk.Button(self.window, text="Send", command=self.send)
        self.send_button.pack(pady=10)

        self.result = None
        self.window.grab_set()
        self.window.wait_window()

    def send(self):
        self.result = self.entry.get()
        self.window.destroy()




if __name__ == "__main__":
    root = tk.Tk()
    app = OpenTTY(root)
    root.mainloop()
