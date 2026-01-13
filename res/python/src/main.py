import os
import sys
import time
import random
import json
import base64
from pathlib import Path
from typing import Dict, List, Optional, Any, Union
import hashlib
# |
# OpenTTY Application
class OpenTTY:
    def __init__(self):
        self.uptime = time.time()
        self.useCache = True
        self.debug = False
        
        # System objects
        self.random = random.Random()
        
        # Storage structures
        self.attributes = {}
        self.fs = {}
        self.sys = {}
        self.graphics = {}
        self.network = {}
        self.globals = { "USER": "root", "PWD": "/home/", "ROOT": "/" }
        
        self.username = self.read("OpenRMS") or "root"
        self.build = "2026-py-1.18-001"
        self.init()

    def init(self):
        try:
            proc = {
                "name": "init",
                "owner": "root",
                "pid": "1"
            }
            
            args = {0: "/bin/init"}
            self.globals["arg"] = args
            
            # Store process
            self.sys["1"] = proc
            
            # Read and execute init script
            init_content = self.read("/bin/init")
            if init_content:

                
        except Exception as e:
            self.panic(e)

    # String utilities
    def get_command(self, text: str) -> str:
        space_index = text.find(' ')
        return text if space_index == -1 else text[:space_index]
    def get_argument(self, text: str) -> str:
        space_index = text.find(' ')
        return "" if space_index == -1 else text[space_index+1:].strip()
    def replace(self, source: str, target: str, replacement: str) -> str:
        return source.replace(target, replacement)
    def env(self, text: str, scope: Optional[Dict] = None) -> str:
        if scope:
            for key, value in scope.items():
                if isinstance(value, str):
                    text = self.replace(text, f"${key}", value)

        for key, value in self.attributes.items():
            if isinstance(value, str):
                text = self.replace(text, f"${key}", value)

        text = self.replace(text, "$USER", self.username)
        text = self.replace(text, "$.", "$")
        
        return self.escape(text)

    def escape(self, text: str) -> str:
        escapes = { "\\n": "\n", "\\r": "\r", "\\t": "\t", "\\b": "\b", "\\\\": "\\", "\\.": "\\" }
        
        for esc, char in escapes.items():
            text = text.replace(esc, char)
        
        return text

    def get_catch(self, error: Exception) -> str:
        message = str(error)
        if not message or message == "None":
            return error.__class__.__name__
        return f"{error.__class__.__name__}: {message}"

    def read(self, filename: str, scope: Optional[Dict] = None) -> str:
        """Read file content"""
        if scope is None:
            scope = self.globals
        
        # Resolve path
        filename = self.solvepath(filename, scope)
        
        # Check cache first
        if self.useCache and filename in self.cache:
            content = self.cache[filename]
            if isinstance(content, bytes):
                return content.decode('utf-8', errors='ignore')
            return content

        if filename.startswith("/dev/"):
            dev = filename[5:]
            if dev == "random":
                return str(random.randint(0, 256))
            elif dev == "stdin":
                return input()
            elif dev == "null":
                return ""
            elif dev == "zero":
                return "\0"

        if filename in self.fs:
            content = self.fs[filename]
            if isinstance(content, bytes):
                content = content.decode('utf-8', errors='ignore')
            
            # Apply environment expansion
            content = self.env(content, scope)
            
            if self.useCache:
                self.cache[filename] = content.encode('utf-8') if isinstance(content, str) else content
            
            return content if isinstance(content, str) else ""
        
        # Try to read from actual file system
        try:
            # Remove leading slash for relative path
            rel_path = filename[1:] if filename.startswith("/") else filename
            
            if os.path.exists(rel_path):
                with open(rel_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Apply environment expansion
                content = self.env(content, scope)
                
                if self.useCache:
                    self.cache[filename] = content.encode('utf-8')
                
                return content
        except Exception:
            pass
        
        return ""

    def write(self, filename: str, data: Union[str, bytes], 
              owner_id: int = 1000, scope: Optional[Dict] = None) -> int:
        """Write data to file"""
        if scope is None:
            scope = self.globals
        
        filename = self.solvepath(filename, scope)
        
        if not filename:
            return 2  # Invalid path
        
        # Convert data to bytes if needed
        if isinstance(data, str):
            data_bytes = data.encode('utf-8')
        else:
            data_bytes = data
        
        # Store in virtual file system
        self.fs[filename] = data_bytes
        
        # Update cache
        if self.useCache:
            self.cache[filename] = data_bytes
        
        return 0  # Success

    def delete_file(self, filename: str, owner_id: int = 1000, 
                   scope: Optional[Dict] = None) -> int:
        """Delete file"""
        if scope is None:
            scope = self.globals
        
        filename = self.solvepath(filename, scope)
        
        if not filename:
            return 2  # Invalid path
        
        if filename in self.fs:
            del self.fs[filename]
            
            # Remove from cache
            if filename in self.cache:
                del self.cache[filename]
            
            return 0  # Success
        
        # Check if file exists in actual file system
        rel_path = filename[1:] if filename.startswith("/") else filename
        if os.path.exists(rel_path):
            try:
                os.remove(rel_path)
                return 0
            except Exception:
                return 1  # General error
        
        return 127  # File not found

    # Path resolution
    def joinpath(self, path: str, scope: Dict) -> str:
        pwd = scope.get("PWD", "/")
        
        if path.startswith("/"): return path
        if pwd.endswith("/"): full_path = pwd + path
        else: full_path = pwd + "/" + path

        parts = []
        for part in full_path.split("/"):
            if part == "" or part == ".": continue
            elif part == "..": 
                if parts: parts.pop()
            else: parts.append(part)
        
        return "/" + "/".join(parts)
    def solvepath(self, path: str, scope: Dict) -> str:
        if not path: return "/"
        
        root = scope.get("ROOT", "/")
        
        if (path.startswith("/") and 
            not (path.startswith("/dev/") or 
                 path.startswith("/proc/") or 
                 path.startswith("/tmp/"))):
            if root != "/":
                if root.endswith("/"): return root + path[1:]
                else: return root + path
        
        return self.joinpath(path, scope)
    def read(self, filename: str, index: int) -> str:
        """Read from RMS-like storage"""
        try:
            storage_file = "opentty_storage.json"
            
            if os.path.exists(storage_file):
                with open(storage_file, 'r') as f:
                    storage = json.load(f)
                
                if filename in storage and str(index) in storage[filename]:
                    return storage[filename][str(index)]
            
            return ""
        except Exception:
            return ""

    # Base64 utilities
    def encode_base64(self, data: bytes) -> str: return base64.b64encode(data).decode('ascii')
    def decode_base64(self, data: str) -> bytes: return base64.b64decode(data)
    def is_pure_text(self, data: bytes) -> bool:
        text_chars = bytearray([7, 8, 9, 10, 12, 13, 27]) + bytearray(range(0x20, 0x7f))
        return all(c in text_chars for c in data[:100]) if data else True

    # Process management
    def genpid(self) -> str: return str(1000 + random.randint(0, 9000))
    def genprocess(self, name: str, pid: str, signals: Optional[Dict] = None) -> Dict:
        proc = { "name": name, "owner": "root" if pid == "0" else self.username, "pid": pid }
        if signals: proc["signals"] = signals
        
        return proc

    # Utility methods
    def print(self, message: str, stdout=sys.stdout):
        if stdout: print(message, file=stdout)

    def warn(self, title: str, message: str) -> int:
        print(f"\n[WARNING] {title}: {message}")
        return 0

    def get_jvm_info(self) -> str:
        info = [
            f"Python {sys.version}",
            f"Platform: {sys.platform}",
            f"Implementation: {sys.implementation.name}"
        ]
        return "\n".join(info)
