import os
import sys
import time
import random
import json
import base64
import hashlib
from pathlib import Path
from typing import Dict, List, Optional, Any, Union
import urllib.request
import urllib.parse
# |
import lupa
from lupa import LuaRuntime
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
                lua = Lua(self)
                lua.run(init_content)

        except Exception as e:
            return

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

    def read(self, path, scope) -> str:
        return
    def write(self, path, content, id, scope) -> int:
        return
    def remove(self, path, id, scope) -> int:
        return

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
# |
class Lua:
    def __init__(self, opentty):
        self.opentty = opentty
        self.lua = LuaRuntime()
        self._setup_globals()
    
    def _setup_globals(self):
        g = self.lua.globals()
        
        # Basic Lua functions
        g.print = self._lua_print
        g.error = self._lua_error
        g.pcall = self._lua_pcall
        g.require = self._lua_require
        g.type = self._lua_type
        g.tostring = self._lua_tostring
        g.tonumber = self._lua_tonumber
        g.select = self._lua_select
        
        # String module
        string = self.lua.table()
        string.upper = self._string_upper
        string.lower = self._string_lower
        string.len = self._string_len
        string.find = self._string_find
        string.match = self._string_match
        string.reverse = self._string_reverse
        string.sub = self._string_sub
        string.gsub = self._string_gsub
        string.byte = self._string_byte
        string.char = self._string_char
        string.trim = self._string_trim
        string.split = self._string_split
        g.string = string
        
        # Table module
        table = self.lua.table()
        table.insert = self._table_insert
        table.remove = self._table_remove
        table.concat = self._table_concat
        table.sort = self._table_sort
        table.unpack = self._table_unpack
        table.pack = self._table_pack
        g.table = table
        
        # OS module
        os = self.lua.table()
        os.execute = self._os_execute
        os.getenv = self._os_getenv
        os.setenv = self._os_setenv
        os.time = self._os_time
        os.date = self._os_date
        os.exit = self._os_exit
        g.os = os
        
        # IO module
        io = self.lua.table()
        io.open = self._io_open
        io.read = self._io_read
        io.write = self._io_write
        io.close = self._io_close
        io.lines = self._io_lines
        g.io = io
        
        # Math module
        math = self.lua.table()
        math.random = self._math_random
        math.floor = lambda x: int(x)
        math.ceil = lambda x: int(math.ceil(x))
        g.math = math
        
        # OpenTTY specific modules
        self._setup_opentty_modules(g)
    
    def _setup_opentty_modules(self, g):
        # Graphics module
        graphics = self.lua.table()
        graphics.display = self._graphics_display
        graphics.new = self._graphics_new
        graphics.append = self._graphics_append
        g.graphics = graphics
        
        # Audio module
        audio = self.lua.table()
        audio.load = self._audio_load
        audio.play = self._audio_play
        audio.pause = self._audio_pause
        g.audio = audio
        
        # Network module
        socket = self.lua.table()
        socket.http = self.lua.table()
        socket.http.get = self._http_get
        socket.http.post = self._http_post
        g.socket = socket
        
        # Java module (emulating J2ME)
        java = self.lua.table()
        java.midlet = self.lua.table()
        java.midlet.username = self.opentty.username
        java.midlet.build = self.opentty.build
        g.java = java
        
        # Package module
        package = self.lua.table()
        package.loaded = self.lua.table()
        g.package = package
    
    # ========== BASIC LUA FUNCTIONS ==========
    
    def _lua_print(self, *args):
        parts = []
        for arg in args:
            parts.append(self._tostring(arg))
        message = "\t".join(parts)
        self.opentty.print(message, sys.stdout)
        return None
    def _lua_error(self, message): raise Exception(message or "error")
    def _lua_pcall(self, func, *args):
        try:
            result = func(*args)
            return True, result
        except Exception as e:
            return False, str(e)
    def _lua_require(self, module_name):
        # Check if already loaded
        loaded = self.lua.globals().package.loaded
        if module_name in loaded:
            return loaded[module_name]
        
        # Try to load module
        module_code = self.opentty.read(f"/lib/{module_name}.lua", self.opentty.globals)
        if not module_code:
            module_code = self.opentty.read(f"/lib/{module_name}.so", self.opentty.globals)
        
        if module_code:
            # Execute module
            module_env = self.lua.table()
            module_env._M = module_env
            module_env._NAME = module_name
            module_env._PACKAGE = self.lua.globals().package
            
            # Run module in its environment
            chunk = self.lua.execute(module_code, module_env)
            result = chunk()
            
            # Store in loaded modules
            loaded[module_name] = result or module_env
            return loaded[module_name]
        
        raise Exception(f"module '{module_name}' not found")
    def _lua_type(self, obj):
        if obj is None:
            return "nil"
        elif isinstance(obj, bool):
            return "boolean"
        elif isinstance(obj, (int, float)):
            return "number"
        elif isinstance(obj, str):
            return "string"
        elif isinstance(obj, (list, dict)):
            return "table"
        elif callable(obj):
            return "function"
        else:
            return "userdata"
    def _lua_tostring(self, obj): return self._tostring(obj)
    def _lua_tonumber(self, obj, base=10):
        try:
            if isinstance(obj, (int, float)):
                return float(obj)
            elif isinstance(obj, str):
                if base == 10:
                    return float(obj)
                else:
                    return float(int(obj, base))
            return None
        except:
            return None
    def _lua_select(self, index, *args):
        if index == "#":
            return len(args)
        
        if index < 0:
            index = len(args) + index + 1
        
        if index < 1 or index > len(args):
            return None
        
        result = []
        for i in range(index - 1, len(args)):
            result.append(args[i])
        
        if len(result) == 1:
            return result[0]
        return tuple(result)
    
    # ========== STRING FUNCTIONS ==========
    def _string_upper(self, s): return s.upper() if isinstance(s, str) else ""
    def _string_lower(self, s): return s.lower() if isinstance(s, str) else ""
    def _string_len(self, s): return len(s) if isinstance(s, str) else 0
    def _string_find(self, s, pattern, init=1, plain=False):
        if not isinstance(s, str):
            return None
        
        if init < 0:
            init = len(s) + init + 1
        if init < 1:
            init = 1
        
        pos = s.find(pattern, init - 1)
        if pos == -1:
            return None
        return pos + 1
    def _string_match(self, s, pattern, init=1):
        if not isinstance(s, str):
            return None
        
        if init < 0:
            init = len(s) + init + 1
        if init < 1:
            init = 1
        
        pos = s.find(pattern, init - 1)
        if pos == -1:
            return None
        return s[pos:pos + len(pattern)]
    def _string_reverse(self, s): return s[::-1] if isinstance(s, str) else ""
    def _string_sub(self, s, i, j=None):
        if not isinstance(s, str):
            return ""
        
        if i < 0:
            i = len(s) + i + 1
        if j is None:
            j = len(s)
        elif j < 0:
            j = len(s) + j + 1
        
        if i < 1:
            i = 1
        if j > len(s):
            j = len(s)
        
        if i > j:
            return ""
        
        return s[i-1:j]
    def _string_gsub(self, s, pattern, repl, n=-1):
        if not isinstance(s, str):
            return "", 0
        
        if n < 0:
            n = 0
        
        # Simple replacement (no regex)
        count = 0
        result = []
        i = 0
        while i < len(s):
            if s[i:i+len(pattern)] == pattern and (n == 0 or count < n):
                result.append(repl)
                i += len(pattern)
                count += 1
            else:
                result.append(s[i])
                i += 1
        
        return "".join(result), count
    def _string_byte(self, s, i=1, j=1):
        if not isinstance(s, str):
            return None
        
        if i < 0:
            i = len(s) + i + 1
        if j < 0:
            j = len(s) + j + 1
        
        if i < 1:
            i = 1
        if j > len(s):
            j = len(s)
        
        if i > j:
            return None
        
        if i == j:
            return ord(s[i-1])
        
        result = []
        for idx in range(i-1, j):
            result.append(ord(s[idx]))
        return tuple(result)
    def _string_char(self, *args):
        result = []
        for arg in args:
            if isinstance(arg, (int, float)):
                result.append(chr(int(arg)))
        return "".join(result)
    def _string_trim(self, s): return s.strip() if isinstance(s, str) else ""
    def _string_split(self, s, sep=None, maxsplit=-1):
        if not isinstance(s, str):
            return []
        
        if sep is None:
            sep = " "
        
        parts = s.split(sep) if maxsplit == -1 else s.split(sep, maxsplit)
        return tuple(parts)
    
    # ========== TABLE FUNCTIONS ==========
    def _table_insert(self, t, pos, value=None):
        if value is None:
            # table.insert(t, value) - append
            value = pos
            pos = len(t) + 1
        
        if not isinstance(t, list):
            return None
        
        if pos < 1:
            pos = 1
        if pos > len(t) + 1:
            pos = len(t) + 1
        
        t.insert(pos - 1, value)
        return None
    def _table_remove(self, t, pos=None):
        """Lua table.remove"""
        if not isinstance(t, list):
            return None
        
        if pos is None:
            pos = len(t)
        
        if pos < 1 or pos > len(t):
            return None
        
        return t.pop(pos - 1)
    def _table_concat(self, t, sep="", i=1, j=None):
        if not isinstance(t, list):
            return ""
        
        if j is None:
            j = len(t)
        
        if i < 1:
            i = 1
        if j > len(t):
            j = len(t)
        
        if i > j:
            return ""
        
        parts = []
        for idx in range(i-1, j):
            parts.append(self._tostring(t[idx]))
        
        return sep.join(parts)
    def _table_sort(self, t, comp=None):
        if not isinstance(t, list):
            return None
        
        if comp:
            import functools
            t.sort(key=functools.cmp_to_key(comp))
        else:
            t.sort()
        
        return None
    def _table_unpack(self, t, i=1, j=None):
        if not isinstance(t, list):
            return tuple()
        
        if j is None:
            j = len(t)
        
        if i < 1:
            i = 1
        if j > len(t):
            j = len(t)
        
        if i > j:
            return tuple()
        
        return tuple(t[i-1:j])
    def _table_pack(self, *args):
        result = list(args)
        result.n = len(args)  # Lua-style
        return result
    
    # ========== OS FUNCTIONS ==========
    def _os_execute(self, command):
        try:
            result = subprocess.run(command, shell=True, capture_output=True, text=True)
            if result.returncode == 0:
                return True, result.stdout
            else:
                return False, result.stderr
        except Exception as e:
            return False, str(e)
    def _os_getenv(self, name):
        if name in self.opentty.attributes:
            return self.opentty.attributes[name]
        return os.environ.get(name)
    def _os_setenv(self, name, value):
        """Lua os.setenv"""
        self.opentty.attributes[name] = value
        return None
    def _os_time(self, table=None):
        if table:
            # Convert Lua table to datetime
            year = table.get("year", 1970)
            month = table.get("month", 1)
            day = table.get("day", 1)
            hour = table.get("hour", 0)
            min = table.get("min", 0)
            sec = table.get("sec", 0)
            
            dt = datetime.datetime(year, month, day, hour, min, sec)
            return time.mktime(dt.timetuple())
        else:
            return time.time()
    def _os_date(self, format_str="%c", time_val=None):
        if time_val is None:
            time_val = time.time()
        
        dt = datetime.datetime.fromtimestamp(time_val)
        
        if format_str == "*t":
            return {
                "year": dt.year,
                "month": dt.month,
                "day": dt.day,
                "hour": dt.hour,
                "min": dt.minute,
                "sec": dt.second,
                "wday": dt.weekday() + 1,
                "yday": dt.timetuple().tm_yday,
                "isdst": -1
            }
        else:
            return dt.strftime(format_str)
    def _os_exit(self, code=0): sys.exit(code)
    
    # ========== IO FUNCTIONS ==========
    def _io_open(self, filename, mode="r"):
        try:
            full_path = self.opentty.solvepath(filename, self.opentty.globals)
            
            if mode.startswith("r"):
                content = self.opentty.read(full_path, self.opentty.globals)
                return io.StringIO(content)
            elif mode.startswith("w"):
                return io.StringIO()
            elif mode.startswith("a"):
                content = self.opentty.read(full_path, self.opentty.globals)
                stream = io.StringIO(content)
                stream.seek(0, io.SEEK_END)
                return stream
        except:
            return None
    def _io_read(self, file_obj, *formats):
        if file_obj is None:
            # Read from stdin
            return input()
        
        if not hasattr(file_obj, "read"):
            return None
        
        if not formats:
            # Read entire file
            file_obj.seek(0)
            return file_obj.read()
        
        # Handle different formats
        results = []
        for fmt in formats:
            if fmt == "*n":  # Number
                line = file_obj.readline()
                try:
                    results.append(float(line.strip()))
                except:
                    results.append(None)
            elif fmt == "*l":  # Line
                line = file_obj.readline()
                if line:
                    results.append(line.rstrip("\n"))
                else:
                    results.append(None)
            elif fmt == "*a":  # All
                file_obj.seek(0)
                results.append(file_obj.read())
            else:  # Read specific number of chars
                try:
                    n = int(fmt)
                    results.append(file_obj.read(n))
                except:
                    results.append(None)
        
        if len(results) == 1:
            return results[0]
        return tuple(results)
    def _io_write(self, file_obj, *args):
        output = []
        for arg in args:
            output.append(self._tostring(arg))
        
        result = "".join(output)
        
        if file_obj is None:
            # Write to stdout
            sys.stdout.write(result)
            sys.stdout.flush()
        elif hasattr(file_obj, "write"):
            file_obj.write(result)
        
        return file_obj
    def _io_close(self, file_obj):
        if file_obj and hasattr(file_obj, "close"):
            file_obj.close()
        return None
    
    # ========== MATH FUNCTIONS ==========
    def _math_random(self, m=None, n=None):
        if m is None and n is None:
            return random.random()
        elif n is None:
            return random.randint(1, int(m))
        else:
            return random.randint(int(m), int(n))
    
    # ========== GRAPHICS FUNCTIONS ==========
    def _graphics_display(self, screen):
        """OpenTTY graphics.display"""
        # In console mode, just print info
        print(f"[DISPLAY] Showing: {type(screen).__name__}")
        return None
    def _graphics_new(self, type_name, *args):
        """OpenTTY graphics.new"""
        if type_name == "alert":
            return {"type": "alert", "title": args[0] if args else "", "text": args[1] if len(args) > 1 else ""}
        elif type_name == "list":
            return {"type": "list", "title": args[0] if args else "", "items": []}
        elif type_name == "form":
            return {"type": "form", "title": args[0] if args else "", "items": []}
        elif type_name == "command":
            return {"type": "command", "label": args[0] if args else "Command"}
        else:
            return {"type": type_name}
    def _graphics_append(self, container, item):
        """OpenTTY graphics.append"""
        if isinstance(container, dict) and container.get("type") in ["list", "form"]:
            if "items" not in container:
                container["items"] = []
            container["items"].append(item)
        return None
    
    # ========== AUDIO FUNCTIONS ==========
    def _audio_load(self, filename): return {"type": "audio", "filename": filename, "loaded": True}
    def _audio_play(self, audio_obj):
        if isinstance(audio_obj, dict) and audio_obj.get("type") == "audio":
            print(f"[AUDIO] Playing: {audio_obj.get('filename')}")
        return None
    def _audio_pause(self, audio_obj):
        if isinstance(audio_obj, dict) and audio_obj.get("type") == "audio":
            print(f"[AUDIO] Paused: {audio_obj.get('filename')}")
        return None
    
    # ========== HTTP FUNCTIONS ==========
    def _http_get(self, url, headers=None): return self._http_request("GET", url, None, headers)
    def _http_post(self, url, data, headers=None): return self._http_request("POST", url, data, headers)
    def _http_request(self, method, url, data=None, headers=None):
        try:
            if not url.startswith("http"):
                url = "http://" + url
            
            req_headers = {}
            if headers:
                for k, v in headers.items():
                    req_headers[k] = self._tostring(v)
            
            if method == "GET":
                req = urllib.request.Request(url, headers=req_headers)
            else:  # POST
                data_bytes = data.encode() if isinstance(data, str) else data
                req = urllib.request.Request(url, data=data_bytes, headers=req_headers)
                req.method = method
            
            with urllib.request.urlopen(req) as response:
                content = response.read().decode()
                return content, response.status
                
        except Exception as e:
            return str(e), 0
    
    # ========== UTILITY FUNCTIONS ==========
    def _tostring(self, obj):
        if obj is None:
            return "nil"
        elif isinstance(obj, bool):
            return "true" if obj else "false"
        elif isinstance(obj, (int, float)):
            if obj == int(obj):
                return str(int(obj))
            return str(obj)
        elif isinstance(obj, str):
            return obj
        elif isinstance(obj, (list, tuple, dict)):
            return str(obj)
        elif callable(obj):
            return "function"
        else:
            return str(obj)

    def run(self, code, env=None):
        try:
            if env: chunk = self.lua.eval(code, env)
            else: chunk = self.lua.eval(code)
            
            return chunk
        except Exception as e:
            print(f"Lua Error: {e}")
            return None
    
    def execute_file(self, filename):
        content = self.opentty.read(filename, self.opentty.globals)
        if content:
            return self.run(content)
        return None

OpenTTY()