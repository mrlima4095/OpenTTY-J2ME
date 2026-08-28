"""
OpenTTY Python 1.18 — kernel.

A desktop re-implementation of the OpenTTY kernel found in src/OpenTTY.java and
src/Lua.java (ELF emulation left out, per request). It boots /bin/init as PID 1,
runs the process table, serves daemons, manages users, and exposes a real
filesystem mounted on a directory of the host machine (no RMS).

The Lua interpreter itself is the existing Python port in lua/runtime.py; this
module wires it to a kernel that owns:

    * the filesystem  (/bin, /etc, /lib, /home, /tmp, /mnt backed by real
      files;  /dev and /proc are generated pseudo-files),
    * the process table (processes, pids, signals, priorities, sockets),
    * services       (os.request(1, "serve", ...) and the deamon convention),
    * users          (credentials persisted in ~/.opentty-keys).
"""

import os as _os
import sys
import time
import math
import random
import socket
import shutil
import secrets
import getpass
import re
import urllib.request
import urllib.error

from lua.runtime import LuaRuntime, LuaFunction, Process, LuaError, LuaExit

# ─── constants from src/ (error codes match libcore.errormsg) ────────────────

OPEN_VERSION = "1.18.1"
OPEN_BUILD = "2026-1.18.1-python"

# exit/return codes (shared with apps/sys/.../libcore.so)
E_OK = 0
E_BADPARAMS = 2
E_NOTDIR = 20
E_SERVICE = 68
E_UNSERVICE = 69
E_NOTFOUND = 127
E_PERM = 13
E_ROSTORAGE = 5

# known virtual mounts (mirrors src/etc/fstab)
ROOT_MOUNTS = ["bin/", "dev/", "etc/", "home/", "lib/", "mnt/", "proc/", "tmp/"]
PSEUDO_DEV = ["null", "random", "stdin", "stdout", "zero"]
PSEUDO_PROC = ["uptime", "version", "meminfo"]


def java_hash(s):
    """Java String.hashCode() (signed 32-bit), as used by the MIDlet login."""
    if s is None:
        s = ""
    h = 0
    for ch in s:
        h = (31 * h + ord(ch)) & 0xFFFFFFFF
    if h >= 0x80000000:
        h -= 0x100000000
    return h


def pid_str(v):
    """Normalise a Lua number/string pid into its string form ("1" not "1.0")."""
    if v is None:
        return "None"
    if isinstance(v, bool):
        return "1" if v else "0"
    if isinstance(v, float) and v == int(v) and not math.isinf(v):
        return str(int(v))
    if isinstance(v, int):
        return str(v)
    return str(v)


# ─── I/O value objects (surrogate for J2ME InputStream/OutputStream) ──────────

class LuaStream:
    """An in-memory readable file stream (io.open result)."""

    __slots__ = ("path", "data", "pos")

    def __init__(self, path, content=""):
        self.path = path
        self.data = content if isinstance(content, str) else content.decode("utf-8", "replace")
        self.pos = 0

    def read(self, n=None):
        self.data = self.data
        if n is None or n < 0:
            out = self.data[self.pos:]
            self.pos = len(self.data)
            return out
        out = self.data[self.pos:self.pos + n]
        self.pos += len(out)
        return out

    def write_bytes(self, data):
        return 0.0

    def close(self):
        pass

    def __str__(self):
        return "stream:" + self.path


class LuaSocket:
    """A connected TCP socket, used for both the input and output side."""

    def __init__(self, sock):
        self.sock = sock

    def read(self, n=None):
        try:
            if n is None or n < 0:
                self.sock.settimeout(0.8)
                try:
                    raw = self.sock.recv(65536)
                except (socket.timeout, TimeoutError):
                    return ""
                finally:
                    self.sock.settimeout(None)
                return raw.decode("utf-8", "replace")
            return self.sock.recv(max(int(n), 1)).decode("utf-8", "replace")
        except OSError:
            return ""

    def write_bytes(self, data):
        if isinstance(data, str):
            data = data.encode("utf-8")
        try:
            self.sock.sendall(data)
        except OSError:
            return 1
        return 0

    def close(self):
        try:
            self.sock.close()
        except OSError:
            pass

    def __str__(self):
        return "connection"


class LuaServer:
    """A listening TCP server (socket.server result)."""

    def __init__(self, srv, port):
        self.srv = srv
        self.port = int(port)

    def accept(self):
        conn, _ = self.srv.accept()
        return LuaSocket(conn)

    def close(self):
        try:
            self.srv.close()
        except OSError:
            pass

    def __str__(self):
        return "server:" + str(self.port)


# ─── Process (kernel flavour of lua.runtime.Process) ─────────────────────────

class KernelProcess(Process):
    """Process table entry: matches the schema used by src/OpenTTY.java."""

    def __init__(self, pid, name, uid=1000, owner="user"):
        super().__init__(pid, name, uid, owner)
        self.cmd = ""
        self.priority = 10
        self.kill = False
        self.net = {}
        self.startTime = int(time.time() * 1000)

    def __str__(self):
        return "{ name=%s, owner=%s, uid=%d, pid=%s, cmd=%s, handler=%s, priority=%d, db=%s, net=%s }" % (
            self.name, self.owner, self.uid, self.pid, self.cmd,
            "kernel" if self.handler is not None and not isinstance(self.handler, LuaFunction)
            else ("fn" if self.handler is not None else "none"),
            self.priority, self.db, self.net)


# ─── Runtime subclass: hooks the Python Lua port into the kernel ──────────────

class OpenTTYRuntime(LuaRuntime):
    """LuaRuntime wired to an OpenTTYKernel for FS/process/user/socket services."""

    def __init__(self, kernel, base_dir=None, username="user"):
        self.kernel = kernel
        super().__init__(base_dir=base_dir, username=username)
        self.next_pid = 1000
        self.uptime = int(time.time() * 1000)

        # java.midlet mirrors the J2ME runtime info the scripts rely on
        midlet = self.globals.get("java", {}).get("midlet", {})
        midlet["build"] = OPEN_BUILD
        midlet["username"] = username
        midlet["VERSION"] = self.attributes.get("VERSION", OPEN_VERSION)
        self.globals["_VERSION"] = "Lua J2ME"

    # ── process table helpers ────────────────────────────────────────────

    def register_process(self, pid, proc):
        if not hasattr(proc, "net"):
            proc.net = {}
        if not hasattr(proc, "startTime"):
            proc.startTime = int(time.time() * 1000)
        if not hasattr(proc, "priority"):
            proc.priority = 10
        super().register_process(pid, proc)

    # ── filesystem hooks ─────────────────────────────────────────────────

    def get_content(self, path, scope=None):
        if scope is None:
            scope = self.scope
        return self.kernel.read(path, scope)

    def get_input_stream(self, path, scope=None):
        if scope is None:
            scope = self.scope
        content = self.kernel.read(path, scope)
        return content.encode("utf-8") if content else None

    def write_file(self, path, content, uid=1000, scope=None):
        if scope is None:
            scope = self.scope
        return self.kernel.write(path, content, uid, scope)

    def delete_file(self, path, uid=1000, scope=None):
        if scope is None:
            scope = self.scope
        return self.kernel.delete(path, uid, scope)

    def get_dirs(self, path, scope=None):
        if scope is None:
            scope = self.scope
        return self.kernel.list_dir(path, scope)

    def _chdir(self, target):
        return self.kernel.chdir(self.scope, target)

    def mkdir(self, path, uid):
        return self.kernel.mkdir(path, uid)

    # ── os.* hooks that depend on kernel state ───────────────────────────

    def _os_internals(self, mod, args):
        if mod == 311:  # getuid
            if not args or args[0] is None:
                return float(self.uid)
            return float(self.kernel.get_user_id(str(args[0])))
        if mod == 313 and args:  # request — normalise pid
            args = list(args)
            args[0] = pid_str(args[0])
            return super()._os_internals(313, args)
        if mod == 318:  # su
            return self.kernel.os_su(self, args)
        if mod == 322:  # mkdir
            path = str(args[0]) if args else ""
            return float(self.kernel.mkdir(path, self.uid))
        return super()._os_internals(mod, args)

    # ── io.* hooks (streams/sockets/files) ───────────────────────────────

    def _io_internals(self, mod, args):
        if mod == 400:
            return self._io_read(args)
        if mod == 401:
            return self._io_write(args)
        if mod == 402:
            return self._io_close(args)
        if mod == 403:
            return self._io_open(args)
        return super()._io_internals(mod, args)

    def _io_read(self, args):
        if not args or args[0] is None:
            return ""
        arg = args[0]
        n = args[1] if len(args) > 1 and args[1] is not None else None
        if isinstance(arg, (LuaSocket, LuaStream)):
            if n is None:
                return arg.read()
            try:
                cnt = int(float(str(n)))
            except (ValueError, TypeError):
                cnt = -1
            return arg.read(cnt)
        if isinstance(arg, str):
            return self.kernel.read(arg, self.scope)
        if isinstance(arg, bytes):
            if n is None:
                return arg.decode("utf-8", "replace")
            try:
                cnt = int(float(str(n)))
            except (ValueError, TypeError):
                cnt = -1
            if cnt < 0:
                return arg.decode("utf-8", "replace")
            return arg[:cnt].decode("utf-8", "replace")
        return ""

    def _io_write(self, args):
        if not args:
            return None
        buf = args[0]
        target = args[1] if len(args) > 1 else None
        how = args[2] if len(args) > 2 else None
        mode = str(how) == "a" if how else False
        append = mode or (str(how) == "a+")

        if isinstance(target, (LuaSocket, LuaStream)):
            return float(target.write_bytes(buf))
        if isinstance(target, str):
            data = buf if isinstance(buf, str) else (buf.decode("utf-8", "replace") if isinstance(buf, bytes) else str(buf))
            return float(self.kernel.write(target, data, self.uid, self.scope, append=append))
        if isinstance(buf, str) and target is None:
            print(buf, end="")
            return 0.0
        if isinstance(buf, bytes) and target is None:
            sys.stdout.write(buf.decode("utf-8", "replace"))
            sys.stdout.flush()
            return 0.0
        return 0.0

    def _io_close(self, args):
        for a in args:
            if isinstance(a, (LuaSocket, LuaServer, LuaStream)):
                a.close()
            elif isinstance(a, (list, tuple)):
                for b in a:
                    if isinstance(b, (LuaSocket, LuaServer, LuaStream)):
                        b.close()
        return None

    def _io_open(self, args):
        if not args:
            return None
        path = str(args[0])
        content = self.kernel.read(path, self.scope)
        if content is None or content == "":
            return None
        return LuaStream(path, content)

    # ── shell / process execution, preserving the live shell scope ───────

    def _sync_globals_from_scope(self, scope):
        if not isinstance(scope, dict):
            return
        for k in ("PWD", "USER", "ROOT", "ALIAS"):
            if k in scope:
                self.globals[k] = scope[k]

    def exec_shell(self, command_str):
        saved = self.scope
        try:
            return super().exec_shell(command_str)
        finally:
            self.scope = saved
            if isinstance(saved, dict):
                self.kernel.live = saved
            self._sync_globals_from_scope(saved)

    def _popen(self, args):
        saved = self.scope
        try:
            return super()._popen(args)
        finally:
            self.scope = saved
            if isinstance(saved, dict):
                self.kernel.live = saved
            self._sync_globals_from_scope(saved)

    # ── sockets ──────────────────────────────────────────────────────────

    def _socket_internals(self, mod, args):
        return self.kernel.socket_internals(self, mod, args)


# ─── Kernel handler (the "kernel core" payload dispatcher) ───────────────────

class KernelHandler:
    """os.request(1, payload, arg, scope, pid, uid) dispatcher (src/Lua.java KERNEL)."""

    def __init__(self, kernel):
        self.kernel = kernel

    def call(self, args, runtime):
        if not args:
            return None
        payload = args[0]
        arg = args[1] if len(args) > 1 else None
        scope = args[2] if len(args) > 2 and isinstance(args[2], dict) else {}
        pid = args[3] if len(args) > 3 and args[3] is not None else runtime.pid
        uid = runtime.uid
        try:
            uid = int(float(str(args[4]))) if len(args) > 4 else runtime.uid
        except (ValueError, TypeError):
            uid = runtime.uid
        return self.kernel.handle(payload, arg, scope, pid_str(pid), uid)


# ─── The kernel ───────────────────────────────────────────────────────────────

class OpenTTYKernel:
    def __init__(self, devroot=None, keys=None, username=None, hostname="opentty", base_dir=None):
        self.base_dir = _os.path.abspath(base_dir) if base_dir else _os.path.dirname(_os.path.dirname(_os.path.abspath(__file__)))
        self.devroot = _os.path.abspath(devroot) if devroot else _os.path.abspath(_os.getcwd())
        self.keys_path = keys or _os.path.expanduser("~/.opentty-keys")
        self.hostname = hostname or "opentty"
        self.uptime = int(time.time() * 1000)

        # users: main user is uid 1000, root is uid 0
        self.main_user = (username or _os.environ.get("USER") or getpass.getuser() or "user").strip()
        if self.main_user == "root":
            self.main_user = "user"
        self.users = {}          # name -> uid
        self.passwords = {}      # name -> java hash (int)
        self.next_uid = 1001
        self.created_keys = False

        # env / daemon
        self.attributes = {}
        self.cache_flag = True
        self.debug_flag = False
        self.shell = None
        self.servers = {}        # port -> LuaServer
        self.sockets = {}        # pid -> {addr -> LuaSocket/LuaServer}
        self.live = {}

        self.runtime = OpenTTYRuntime(kernel=self, base_dir=self.base_dir, username=self.main_user)
        self.attributes = self.runtime.attributes
        self.handler = KernelHandler(self)

        # index available apps (apps/<cat>/<name>/main.lua or apps/<cat>/<name>.lua)
        self.apps_index = {}
        self._index_apps()

        self.runtime.globals["PWD"] = "/home/"
        self.runtime.globals["USER"] = "root"
        self.runtime.globals["ROOT"] = "/"
        self.runtime.globals["ALIAS"] = {}
        self.attributes["VERSION"] = OPEN_VERSION
        self.attributes["REPO"] = "socket://opentty.fun:31522"
        self.attributes["SHELL"] = "/bin/sh"
        self.attributes["PATCH"] = "Deep Dream"
        self.attributes["RELEASE"] = "mod"
        self.attributes["TYPE"] = "platform"
        self.attributes["CONFIG"] = "configuration"
        self.attributes["PROFILE"] = "profiles"
        self.attributes["LOCALE"] = "locale"
        self.attributes["MIDlet-Version"] = OPEN_VERSION
        self.attributes["MIDlet-Name"] = "OpenTTY"
        self.attributes["MIDlet-Build"] = OPEN_BUILD
        self.attributes["MIDlet-Proxy"] = "http://nnp.nnchan.ru/proxy.php?"
        self.attributes["HOSTNAME"] = self.hostname

    # ── app overlay (run the on-device catalog without installing) ───────

    def _index_apps(self):
        """Expose apps/ as /bin commands.

        The authoritative mapping is the pkg mirror (src/bin/pkg): it gives the
        on-device install path (here=) for each app (remote=). We mirror that so
        commands like useradd/userdel/svchost work without installing. Flat
        apps/<cat>/<name>.lua and <cat>/<name>/main.lua are also indexed as a
        fallback for anything not yet in the catalog.
        """
        self.apps_index = {}  # device path ("/bin/docker") -> host file under apps/
        apps_root = _os.path.join(self.base_dir, "apps")

        mirror = _os.path.join(self.base_dir, "src", "bin", "pkg")
        try:
            with open(mirror, "r") as f:
                lines = f.readlines()
        except OSError:
            lines = []
        entry = re.compile(r'^\s*\["([^"]+)"\]\s*=\s*\{ remote = "([^"]+)", here = "([^"]+)"')
        for line in lines:
            if re.match(r"^\s*--", line):
                continue
            m = entry.match(line)
            if not m:
                continue
            _, remote, here = m.groups()
            host = _os.path.join(apps_root, remote)
            if _os.path.isdir(host):
                host = _os.path.join(host, "main.lua")
            if _os.path.isfile(host):
                self.apps_index[here] = host

        if _os.path.isdir(apps_root):
            try:
                for cat in sorted(_os.listdir(apps_root)):
                    cfg = _os.path.join(apps_root, cat)
                    if not _os.path.isdir(cfg):
                        continue
                    for name in sorted(_os.listdir(cfg)):
                        direct = _os.path.join(cfg, name)
                        main = _os.path.join(direct, "main.lua")
                        if _os.path.isdir(direct) and _os.path.isfile(main):
                            self.apps_index.setdefault("/bin/" + name, main)
                        elif _os.path.isfile(direct) and direct.endswith(".lua"):
                            self.apps_index.setdefault("/bin/" + name[:-4], direct)
            except OSError:
                pass

    def _find_app_script(self, dev_path):
        host = self.apps_index.get(dev_path)
        if host:
            try:
                with open(host, "r", errors="replace") as f:
                    return f.read()
            except OSError:
                return None
        return None

    # ── device directory / seeding ───────────────────────────────────────

    def seed_device(self):
        """Create the device-root directory tree and mirror src/{bin,etc,lib}."""
        for sub in ("bin", "etc", "lib", "home", "tmp", "mnt", "dev", "proc"):
            _os.makedirs(_os.path.join(self.devroot, sub), exist_ok=True)
        for sub in ("bin", "etc", "lib"):
            src = _os.path.join(self.base_dir, "src", sub)
            dst = _os.path.join(self.devroot, sub)
            if not _os.path.isdir(src):
                continue
            if not _os.path.isdir(dst):
                _os.makedirs(dst)
            for name in _os.listdir(src):
                sp, dp = _os.path.join(src, name), _os.path.join(dst, name)
                if _os.path.isfile(sp) and not _os.path.exists(dp):
                    try:
                        shutil.copy2(sp, dp)
                    except OSError:
                        pass
        try:
            with open(_os.path.join(self.devroot, "etc", "hostname"), "r") as f:
                hn = f.read().strip()
            if hn:
                self.hostname = hn
                self.attributes["HOSTNAME"] = hn
        except OSError:
            pass

    # ── credentials (~/.opentty-keys) ────────────────────────────────────

    def _load_keys(self):
        self.users = {}
        self.passwords = {}
        if _os.path.isfile(self.keys_path):
            try:
                with open(self.keys_path, "r") as f:
                    for raw in f:
                        line = raw.strip()
                        if not line or line.startswith("#"):
                            continue
                        if line.startswith("username:"):
                            self.main_user = line.split(":", 1)[1].strip()
                        elif line.startswith("user:"):
                            parts = line.split(None, 2)
                            if len(parts) >= 3:
                                try:
                                    self.users[parts[1]] = int(parts[2])
                                except ValueError:
                                    pass
                        elif line.startswith("password:"):
                            parts = line.split(None, 2)
                            if len(parts) >= 3:
                                try:
                                    self.passwords[parts[1]] = int(parts[2])
                                except ValueError:
                                    pass
            except OSError:
                pass
        if self.main_user == "root":
            self.main_user = "user"
        self.users.setdefault(self.main_user, 1000)
        self.passwords.setdefault(self.main_user, 0)
        self.next_uid = max([self.users.get(n, 1000) for n in self.users] + [1001]) + 1

    def _save_keys(self):
        try:
            with open(self.keys_path, "w") as f:
                f.write("# OpenTTY Python %s keyring\n" % OPEN_VERSION)
                f.write("username: %s\n" % self.main_user)
                f.write("user: %s %d\n" % (self.main_user, 1000))
                f.write("password: %s %d\n" % (self.main_user, self.passwords.get(self.main_user, 0)))
                for name in sorted(self.users):
                    if name == self.main_user:
                        continue
                    f.write("user: %s %d\n" % (name, self.users[name]))
                for name in sorted(self.passwords):
                    if name == self.main_user:
                        continue
                    f.write("password: %s %d\n" % (name, self.passwords[name]))
            _os.chmod(self.keys_path, 0o600)
        except OSError:
            pass

    def ensure_login(self):
        """Load/create credentials. Mirrors the MIDlet sign-up flow."""
        self._load_keys()
        if self.passwords.get(self.main_user, ""):
            return self.main_user
        interactive = sys.stdin.isatty() and sys.stdout.isatty() and not self._keys_exist()
        if interactive:
            print("OpenTTY Python %s — First run" % OPEN_VERSION)
            print("Create your account (stored in %s)" % self.keys_path)
            try:
                name = input("Username (default %s): " % self.main_user).strip() or self.main_user
                if name == "root":
                    name = self.main_user
                pw = getpass.getpass("Password: ")
                pw2 = getpass.getpass("Confirm password: ")
            except (EOFError, KeyboardInterrupt):
                return None
            if pw != pw2:
                print("Passwords do not match.", file=sys.stderr)
                return None
            self.main_user = name
            self.users[name] = 1000
            self.passwords[name] = java_hash(pw)
            self.created_keys = True
            print("Account created. Welcome, %s!" % name)
        else:
            if not self._keys_exist():
                pw = secrets.token_urlsafe(9)
                self.passwords[self.main_user] = java_hash(pw)
                self.created_keys = True
                print("Created OpenTTY user '%s'. Keys at %s" % (self.main_user, self.keys_path), file=sys.stderr)
                print("Initial password: %s" % pw, file=sys.stderr)
            else:
                self.passwords.setdefault(self.main_user, 0)
        self._save_keys()
        self.runtime.username = self.main_user

        # give the main account its home directory (like the MIDlet's first run)
        try:
            _os.makedirs(_os.path.join(self.devroot, "home", self.main_user), exist_ok=True)
        except OSError:
            pass
        return self.main_user

    def _keys_exist(self):
        return _os.path.isfile(self.keys_path)

    # ── user manager ─────────────────────────────────────────────────────

    def get_user_id(self, name):
        if name == "root":
            return 0
        if name == self.main_user:
            return 1000
        return self.users.get(name, -1)

    def get_user(self, uid):
        if uid == 0:
            return "root"
        for name, u in self.users.items():
            if u == uid:
                return name
        return None

    def useradd(self, name):
        name = name.strip()
        if not name or name == "root":
            return E_BADPARAMS
        if self.get_user_id(name) != -1:
            return 128
        self.users[name] = self.next_uid
        self.passwords[name] = 0
        self.next_uid += 1
        self._save_keys()
        return E_OK

    def userdel(self, name, uid):
        name = name.strip()
        if not name or name == "root" or name == self.main_user:
            return E_PERM
        if self.get_user_id(name) == -1:
            return E_NOTFOUND
        if uid != 0:
            return E_PERM
        self.users.pop(name, None)
        self.passwords.pop(name, None)
        self._save_keys()
        return E_OK

    def password_ok(self, name, pw):
        stored = self.passwords.get(name)
        if stored is None or stored == 0:
            return True
        return stored == java_hash(pw or "")

    def set_password(self, name, newpw):
        self.passwords[name] = java_hash(newpw or "")
        self._save_keys()
        return E_OK

    def os_su(self, rt, args):
        name = str(args[0]) if args and args[0] is not None else None
        if not name:
            return 13.0
        password = str(args[1]) if len(args) > 1 and args[1] is not None else None
        if name == "root":
            stored = self.passwords.get(self.main_user)
            ok = (rt.uid == 0) or (stored in (None, 0)) or (password is not None and java_hash(password) == stored)
            if not ok:
                return 13.0
            new_uid = 0
        elif name == "guest":
            new_uid = 1001
        else:
            new_uid = self.get_user_id(name)
            if new_uid == -1:
                return 13.0
        rt.uid = new_uid
        rt.scope["USER"] = name
        proc = rt.processes.get(rt.pid)
        if proc:
            proc.uid = new_uid
            proc.owner = name
        rt._sync_globals_from_scope(rt.scope)
        return 0.0

    # ── path resolution / filesystem ─────────────────────────────────────

    def _norm(self, path):
        if not path:
            return "/"
        absolute = path.startswith("/")
        trailing = path.endswith("/")
        parts = []
        for seg in path.split("/"):
            if seg in ("", "."):
                continue
            if seg == "..":
                if parts and parts[-1] != "..":
                    parts.pop()
                elif not absolute:
                    parts.append("..")
            else:
                parts.append(seg)
        out = "/".join(parts)
        if absolute:
            out = "/" + out
        elif not out:
            out = "."
        if trailing and out != "/" and not out.endswith("/"):
            out += "/"
        return out

    def _resolve(self, path, scope):
        if scope is None:
            scope = self.runtime.scope
        if path is None:
            return "/"
        path = str(path)
        abs_path = path if path.startswith("/") else (((scope.get("PWD") or "/home/").rstrip("/")) + "/" + path)
        abs_path = self._norm(abs_path)
        root = (scope.get("ROOT") or "") if isinstance(scope, dict) else ""
        if root and root != "/":
            plain = abs_path.lstrip("/")
            if not (abs_path.startswith(("/dev/", "/proc/", "/tmp/"))):
                abs_path = self._norm(root.rstrip("/") + "/" + plain)
        return abs_path

    def _special(self, abs_path):
        if abs_path == "/dev" or abs_path.startswith("/dev/"):
            name = abs_path[5:].rstrip("/")
            if name == "null":
                return ""
            if name == "zero":
                return "\0"
            if name == "random":
                return str(random.randint(0, 255))
            return ""
        if abs_path == "/proc" or abs_path.startswith("/proc/"):
            name = abs_path[6:].rstrip("/")
            if name == "uptime":
                return str(int((time.time() * 1000 - self.uptime) / 1000))
            if name == "version":
                return "OpenTTY %s build %s kernel-python" % (OPEN_VERSION, OPEN_BUILD)
            if name == "meminfo":
                return "MemTotal: %d kB" % (_os.sysconf("SC_PHYS_PAGES") * _os.sysconf("SC_PAGE_SIZE") // 1024)
            return ""
        return None

    def _real(self, abs_path):
        return _os.path.join(self.devroot, abs_path.lstrip("/"))

    def _is_dir(self, abs_path):
        base = abs_path.rstrip("/")
        if base in ("", "/dev", "/proc", "/bin", "/etc", "/lib", "/home", "/tmp", "/mnt"):
            return True
        if base.startswith(("/dev/", "/proc/")):
            return False
        return _os.path.isdir(self._real(base))

    def _writable(self, abs_path, uid):
        if abs_path.startswith(("/bin/", "/etc/", "/lib/")):
            return uid == 0
        if abs_path.startswith(("/tmp/", "/mnt/")):
            return True
        if abs_path.startswith("/dev/"):
            return True
        if abs_path.startswith("/home/"):
            if uid == 0:
                return True
            rel = abs_path[len("/home/"):]
            top = rel.split("/", 1)[0]
            if not top:
                return False
            return self.get_user_id(top) == uid
        return False

    def read(self, path, scope=None):
        abs_path = self._resolve(path, scope)
        special = self._special(abs_path)
        if special is not None:
            return special
        real = self._real(abs_path)
        if _os.path.isfile(real):
            try:
                with open(real, "r", errors="replace") as f:
                    return f.read()
            except OSError:
                return ""
        if abs_path.startswith("/bin/") or abs_path.startswith("/lib/"):
            script = self._find_app_script(abs_path)
            if script is not None:
                return script
        if abs_path in self.runtime.vfs:
            return self.runtime.vfs[abs_path]
        return ""

    def write(self, path, data, uid=1000, scope=None, append=False):
        abs_path = self._resolve(path, scope)
        if not abs_path or abs_path in ("/", "/dev", "/proc"):
            return E_BADPARAMS
        if abs_path.startswith("/dev/"):
            name = abs_path[5:].rstrip("/")
            if name == "null" or name == "zero":
                return E_OK
            return E_ROSTORAGE
        if abs_path.startswith("/proc/"):
            return E_ROSTORAGE
        if not self._writable(abs_path, uid):
            return E_PERM if abs_path.startswith(("/bin/", "/etc/", "/lib/")) else E_ROSTORAGE
        if not isinstance(data, str):
            data = data.decode("utf-8", "replace") if isinstance(data, (bytes, bytearray)) else str(data)
        real = self._real(abs_path)
        try:
            _os.makedirs(_os.path.dirname(real), exist_ok=True)
            with open(real, "a" if append else "w", encoding="utf-8", errors="replace") as f:
                f.write(data)
            return E_OK
        except OSError:
            return 1

    def delete(self, path, uid=1000, scope=None):
        abs_path = self._resolve(path, scope)
        if abs_path.startswith(("/dev/", "/proc/")):
            return E_ROSTORAGE
        if not self._writable(abs_path, uid):
            return E_PERM if abs_path.startswith(("/bin/", "/etc/", "/lib/")) else E_ROSTORAGE
        real = self._real(abs_path)
        if _os.path.isfile(real):
            try:
                _os.remove(real)
                return E_OK
            except OSError:
                return 1
        if abs_path.startswith("/bin/"):
            if abs_path in self.apps_index:
                return E_ROSTORAGE
        return E_NOTFOUND

    def list_dir(self, path, scope=None):
        result = {}
        idx = 1

        def put(name):
            nonlocal idx
            result[float(idx)] = name
            idx += 1

        abs_path = self._resolve(path, scope)
        if not abs_path.endswith("/"):
            abs_path += "/"

        if abs_path == "/":
            for name in ROOT_MOUNTS:
                put(name)
            for ent in sorted(_os.listdir(self.devroot)):
                if ent in ("bin", "dev", "etc", "home", "lib", "mnt", "proc", "tmp"):
                    continue
                put(ent + "/" if _os.path.isdir(_os.path.join(self.devroot, ent)) else ent)
            return result

        if abs_path == "/dev/":
            for name in PSEUDO_DEV:
                put(name)
            return result
        if abs_path == "/proc/":
            for name in PSEUDO_PROC:
                put(name)
            return result

        if abs_path == "/bin/":
            seen = set()
            real = self._real("/bin/")
            if _os.path.isdir(real):
                for ent in sorted(_os.listdir(real)):
                    seen.add(ent)
                    put(ent + "/" if _os.path.isdir(_os.path.join(real, ent)) else ent)
            for key in sorted(self.apps_index):
                if key.startswith("/bin/"):
                    name = key[len("/bin/"):]
                    if name and name not in seen:
                        put(name)
            return result

        if abs_path == "/lib/":
            seen = set()
            real = self._real("/lib/")
            if _os.path.isdir(real):
                for ent in sorted(_os.listdir(real)):
                    seen.add(ent)
                    put(ent + "/" if _os.path.isdir(_os.path.join(real, ent)) else ent)
            for key in sorted(self.apps_index):
                if key.startswith("/lib/"):
                    name = key[len("/lib/"):]
                    if name and name not in seen:
                        put(name)
            return result

        real = self._real(abs_path)
        if _os.path.isdir(real):
            for ent in sorted(_os.listdir(real)):
                full = _os.path.join(real, ent)
                put(ent + "/" if _os.path.isdir(full) else ent)
        return result

    def chdir(self, scope, target):
        if not target:
            scope["PWD"] = "/home/"
            return E_OK
        if target == "..":
            pwd = scope.get("PWD", "/home/")
            if pwd == "/":
                return 1
            last = pwd.rstrip("/").rfind("/")
            scope["PWD"] = pwd[:last + 1] if last > 0 else "/"
            return E_OK
        abs_path = self._resolve(target, scope)
        if not abs_path.endswith("/"):
            abs_path += "/"
        if abs_path in ("" , "/"):
            scope["PWD"] = "/"
            return E_OK
        if self._is_dir(abs_path):
            scope["PWD"] = abs_path
            return E_OK
        return E_NOTFOUND

    def mkdir(self, path, uid=1000):
        if not path:
            return E_BADPARAMS
        abs_path = self._resolve(path, self.runtime.scope)
        if abs_path.startswith(("/dev/", "/proc/")):
            return E_ROSTORAGE
        if abs_path.startswith(("/bin/", "/etc/", "/lib/")) and uid != 0:
            return E_PERM
        if abs_path.startswith("/home/"):
            rel = abs_path[len("/home/"):]
            top = rel.split("/", 1)[0]
            if top and uid != 0 and self.get_user_id(top) != uid:
                return E_PERM
        real = self._real(abs_path)
        try:
            _os.makedirs(real, exist_ok=True)
            return E_OK
        except OSError:
            return 1

    # ── kernel core payload dispatcher ───────────────────────────────────

    def handle(self, payload, arg, scope, pid, uid):
        if payload is None or payload == "":
            return None
        if not isinstance(payload, str):
            payload = self.runtime.to_lua_string(payload)

        if payload == "sendsig":
            if not isinstance(arg, dict) or arg is None:
                return float(E_BADPARAMS)
            target = pid_str(arg.get("pid"))
            signal = self.runtime.to_lua_string(arg.get("signal"))
            proc = self.runtime.get_process(target)
            if proc is None:
                return float(E_NOTFOUND)
            if proc.uid != uid and uid != 0:
                return float(E_PERM)
            if signal != "9" and getattr(proc, "sighandler", None) is not None:
                try:
                    proc.sighandler.call([signal], self.runtime)
                except Exception:
                    pass
            self.runtime.remove_process(target)
            return float(E_OK)

        if payload == "proc":
            if not arg or arg == "":
                return float(E_BADPARAMS)
            proc = self.runtime.get_process(pid_str(arg))
            if proc is None:
                return float(E_NOTFOUND)
            if getattr(proc, "uid", 1000) != uid and uid != 0:
                return float(E_PERM)
            return {
                "name": proc.name, "owner": getattr(proc, "owner", ""), "pid": proc.pid,
                "cmd": getattr(proc, "cmd", ""), "uid": float(getattr(proc, "uid", 1000)),
                "priority": float(getattr(proc, "priority", 10)),
                "startTime": float(getattr(proc, "startTime", 0)),
            }

        if payload == "nice":
            if not isinstance(arg, dict):
                return float(E_BADPARAMS)
            target = pid_str(arg.get("pid"))
            try:
                priority = int(float(self.runtime.to_lua_string(arg.get("priority"))))
            except (ValueError, TypeError):
                return float(E_BADPARAMS)
            proc = self.runtime.get_process(target)
            if proc is None:
                return float(E_NOTFOUND)
            if getattr(proc, "uid", 1000) != uid and uid != 0:
                return float(E_PERM)
            proc.priority = max(0, min(20, priority))
            return float(E_OK)

        if payload == "passwd":
            if isinstance(arg, str):
                return self.password_ok(self.main_user, arg)
            if isinstance(arg, dict):
                old = arg.get("old")
                new = arg.get("new")
                if old is None or new is None or old == "" or new == "":
                    return float(E_BADPARAMS)
                if uid == 0 or self.password_ok(self.main_user, old):
                    return float(self.set_password(self.main_user, new))
                return float(E_PERM)
            return None

        if payload == "setsh":
            if arg is None or arg == "":
                self.shell = None
            elif isinstance(arg, LuaFunction):
                self.shell = arg
            else:
                return float(E_BADPARAMS)
            return None

        if payload == "cache":
            if arg is None or arg == "":
                return self.cache_flag
            if arg is True or self.runtime.to_lua_string(arg) == "true":
                self.cache_flag = True
            elif arg is False or self.runtime.to_lua_string(arg) == "false":
                self.cache_flag = False
                self.runtime.require_cache.clear()
            else:
                return float(E_BADPARAMS)
            return None

        if payload == "debug":
            if arg is None or arg == "":
                return self.debug_flag
            if arg is True or self.runtime.to_lua_string(arg) == "true":
                self.debug_flag = True
            elif arg is False or self.runtime.to_lua_string(arg) == "false":
                self.debug_flag = False
            else:
                return float(E_BADPARAMS)
            return None

        if payload == "netsh":
            if arg is None or arg == "":
                result = {}
                for p, table in self.sockets.items():
                    if not table:
                        continue
                    entry = {1.0: next(iter(table))}
                    result[p] = entry
                return result
            return None

        if payload == "serve":
            if arg is None or arg == "":
                return float(E_BADPARAMS)
            result = self.serve(self.runtime.to_lua_string(arg), scope)
            # the Java kernel returns nil on success (callers use os.getpid / sockets)
            return None if isinstance(result, KernelProcess) else result

        if payload == "rms":
            if uid != 0:
                return float(E_PERM)
            if arg is None or arg == "":
                return float(E_BADPARAMS)
            target = self.runtime.to_lua_string(arg)
            if target in ("/bin/", "/etc/", "/lib/"):
                real = self._real(target)
                if _os.path.isdir(real):
                    for name in _os.listdir(real):
                        try:
                            _os.remove(_os.path.join(real, name))
                        except OSError:
                            pass
                return float(E_OK)
            return float(E_BADPARAMS)

        if payload == "useradd":
            if arg is None or arg == "" or self.runtime.to_lua_string(arg) == "root":
                return float(E_BADPARAMS)
            name = self.runtime.to_lua_string(arg)
            return float(self.useradd(name))

        if payload == "userdel":
            if arg is None or arg == "":
                return float(E_PERM)
            name = self.runtime.to_lua_string(arg)
            return float(self.userdel(name, uid))

        if payload == "user":
            if arg is None:
                return float(E_BADPARAMS)
            try:
                u = int(float(self.runtime.to_lua_string(arg)))
            except (ValueError, TypeError):
                return float(E_BADPARAMS)
            user = self.get_user(u)
            if user is None:
                return float(E_NOTFOUND)
            return user

        return None

    # ── serve: spawn a deamon (services) ─────────────────────────────────

    def _run_capture(self, source, code, arg_table):
        """Run Lua code like LuaRuntime.run() but capture the top-level return.

        Runs the daemon configuration in its own token stream and restores the
        interpreter state afterwards, so a serve() call from running Lua code
        does not clobber the caller's tokens.
        """
        rt = self.runtime
        saved = (rt.tokens, rt.token_index, rt.current_source, rt.last_code,
                 rt.frame_stack, rt.thrown_frames, rt.thrown_tokens, rt.thrown_token_index)
        rt.current_source = source or ""
        rt.last_code = code or ""
        rt.frame_stack = []
        rt.thrown_frames = []
        rt.thrown_tokens = None
        rt.thrown_token_index = -1
        rt.status = 0
        rt.doreturn = False
        rt.break_loop = False
        scope = dict(rt.globals)
        if arg_table is not None:
            scope["arg"] = arg_table
        rt.scope = scope
        value = None
        try:
            rt.tokens = rt.tokenize(code)
            rt.token_index = 0
            rt._collect_labels()
            while rt.peek().type != 0:
                result = rt.statement(scope)
                if rt.doreturn:
                    value = result
                    rt.doreturn = False
                    break
        except LuaExit as e:
            rt.status = e.status
        except LuaError as e:
            print(rt._get_traceback(e), file=sys.stderr)
            rt.status = 1
        except Exception as e:
            print(rt._get_traceback(e), file=sys.stderr)
            rt.status = 1
        finally:
            (rt.tokens, rt.token_index, rt.current_source, rt.last_code,
             rt.frame_stack, rt.thrown_frames, rt.thrown_tokens, rt.thrown_token_index) = saved
        return value

    def serve(self, program, scope):
        rt = self.runtime
        code = self.read(program, scope)
        if not code:
            return "service '%s' not found" % program

        uid = rt.uid if scope is None else self.get_user_id(scope.get("USER", "root") or "root")
        if uid == -1:
            uid = rt.uid
        owner = self.get_user(uid) or "root"

        pid = rt.genpid()
        proc = KernelProcess(pid, program, uid, owner)
        proc.cmd = "/bin/init --serve=" + program
        proc.kill = False
        proc.scope = dict(scope) if isinstance(scope, dict) else dict(rt.scope)
        rt.register_process(pid, proc)

        old_pid, old_uid, old_scope = rt.pid, rt.uid, rt.scope
        rt.pid = pid
        rt.uid = uid
        try:
            handler = self._run_capture(program, code, {0.0: program, 1.0: "--deamon"})
        finally:
            rt.pid, rt.uid, rt.scope = old_pid, old_uid, old_scope

        if isinstance(handler, list) and handler:
            handler = handler[0]
        if isinstance(handler, LuaFunction):
            proc.handler = handler
            handler.name = program
        proc.scope = dict(rt.scope)
        return proc

    # ── sockets ──────────────────────────────────────────────────────────

    def _parse_socket_uri(self, uri):
        if not uri.startswith("socket://"):
            uri = "socket://" + uri
        rest = uri[len("socket://"):]
        if ":" not in rest:
            raise LuaError("bad argument #1 to 'connect' (invalid socket URI '%s')" % uri)
        host, _, port = rest.rpartition(":")
        if not host or not port:
            raise LuaError("bad argument #1 to 'connect' (invalid socket URI '%s')" % uri)
        return host, int(port)

    def socket_internals(self, rt, mod, args):
        pid = rt.pid

        if mod == 500:  # http.get
            return self._http("GET", args)
        if mod == 501:  # http.post
            return self._http("POST", args)
        if mod == 507:  # http.rget
            return self._http("GET", args, raw=True)
        if mod == 508:  # http.rpost
            return self._http("POST", args, raw=True)

        if mod == 502:  # connect
            if not args or args[0] is None:
                raise LuaError("bad argument #1 to 'connect' (string expected, got no value)")
            uri = rt.to_lua_string(args[0])
            host, port = self._parse_socket_uri(uri)
            try:
                sock = socket.create_connection((host, port), timeout=30)
            except OSError as e:
                raise LuaError(str(e))
            conn = LuaSocket(sock)
            self.sockets.setdefault(pid, {})[uri] = conn
            return [conn, conn, conn, uri, float(rt.uid)]

        if mod in (503, 504):  # peer / device
            if not args or not isinstance(args[0], LuaSocket):
                raise LuaError("bad argument #1 to '%s' (connection expected)" % ("peer" if mod == 503 else "device"))
            conn = args[0]
            try:
                if mod == 503:
                    return [conn.sock.getpeername()[0], float(conn.sock.getpeername()[1])]
                return [conn.sock.getsockname()[0], float(conn.sock.getsockname()[1])]
            except OSError:
                return ["0.0.0.0", 0.0]

        if mod == 505:  # server
            if not args or not isinstance(args[0], (int, float)):
                raise LuaError("bad argument #1 to 'server' (number expected, got %s)" % rt.lua_type(args[0]))
            port = int(float(str(args[0])))
            try:
                srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                srv.bind(("0.0.0.0", port))
                srv.listen(8)
            except OSError as e:
                raise LuaError(str(e))
            server = LuaServer(srv, port)
            self.servers[str(port)] = server
            self.sockets.setdefault(pid, {})[str(port)] = server
            return server

        if mod == 506:  # accept
            if not args or not isinstance(args[0], LuaServer):
                raise LuaError("bad argument #1 to 'accept' (server expected, got %s)" % rt.lua_type(args[0]))
            conn = args[0].accept()
            self.sockets.setdefault(pid, {})["socket://:" + str(args[0].port)] = conn
            return [conn, conn, conn]

        return None

    def _http(self, method, args, raw=False):
        if not args or args[0] is None:
            raise LuaError("bad argument #1 to '%s' (string expected, got no value)" % ("rget" if raw else method.lower()))
        url = self.runtime.to_lua_string(args[0])
        if not url.startswith("http://") and not url.startswith("https://"):
            url = "http://" + url
        data = self.runtime.to_lua_string(args[1]) if len(args) > 1 and args[1] is not None else None
        headers = args[2] if len(args) > 2 and isinstance(args[2], dict) else {}
        hdrs = {}
        for k, v in headers.items():
            hdrs[self.runtime.to_lua_string(k)] = self.runtime.to_lua_string(v)
        body = None
        if method == "POST":
            body = (data or "").encode("utf-8")
            hdrs.setdefault("Content-Type", "application/x-www-form-urlencoded")
        try:
            req = urllib.request.Request(url, data=body, headers=hdrs, method=method)
            with urllib.request.urlopen(req, timeout=30) as resp:
                payload = resp.read()
                code = resp.getcode() or 200
        except urllib.error.HTTPError as e:
            payload = e.read()
            code = e.code
        except OSError as e:
            raise LuaError(str(e))
        content = payload.decode("utf-8", "replace")
        return [content, float(code)]

    # ── boot / execution entry points ────────────────────────────────────

    def boot(self):
        """Seed the device tree, register PID 1 (init) and run /bin/init."""
        self.seed_device()
        code = self.read("/bin/init", self.runtime.scope)
        rt = self.runtime

        proc = KernelProcess("1", "init", 0, "root")
        proc.cmd = "/bin/init"
        proc.handler = self.handler
        proc.scope = dict(rt.globals)
        rt.register_process("1", proc)

        old_pid = rt.pid
        rt.pid = "1"
        rt.uid = 0
        try:
            rt.run("/bin/init", code, {0.0: "/bin/init"})
        finally:
            rt.pid = old_pid

        proc.scope = rt.scope
        self.live = rt.scope
        rt._sync_globals_from_scope(rt.scope)
        # shell runs as the main (logged in) user, like the MIDlet after su
        if rt.uid == 0:
            rt.uid = 1000
            if isinstance(rt.scope, dict):
                rt.scope["USER"] = self.main_user
            rt._sync_globals_from_scope(rt.scope)
        return rt.status

    def os_execute(self, command):
        # shell builtins that the ported runtime's exec_shell lacks (su) run
        # through the same code path as the interactive REPL parser
        parts = command.strip().split(None, 1)
        if parts and parts[0] == "su":
            return self._su(parts[1] if len(parts) > 1 else "")
        try:
            return self.runtime.shell_handler.call([command], self.runtime)
        except LuaExit as e:
            return e.status

    def _su(self, body):
        """Match the terminal su (src/Lua.java): root needs the account
        password; named users just switch; bare "su" returns to the user."""
        rt = self.runtime
        argsv = body.split()
        if not argsv:
            if rt.uid != 1000:
                rt.uid = 1000
                rt.scope["USER"] = self.main_user
                rt._sync_globals_from_scope(rt.scope)
                return 0
            print("su: usage: su [username] [passwd]")
            return 2
        if argsv[0] == "root":
            status = self.os_su(rt, ["root", argsv[1] if len(argsv) > 1 else None])
        elif rt.uid == 1000 and argsv[0] == self.main_user:
            return 0
        else:
            status = self.os_su(rt, [argsv[0]])
        if status in (13, 13.0):
            print("Permission denied!")
        return status

    def run_script(self, path, argv=()):
        """Run a Lua/script as a foreground process (like io.popen)."""
        code = self.read(path, self.runtime.scope)
        if not code:
            print("%s: not found" % path)
            return E_NOTFOUND
        rt = self.runtime
        pid = rt.genpid()
        proc = KernelProcess(pid, path, rt.uid, self.get_user(rt.uid) or self.main_user)
        proc.scope = dict(rt.scope)
        rt.register_process(pid, proc)
        arg_table = {0.0: path}
        for i, a in enumerate(argv):
            arg_table[float(i + 1)] = a
        old_pid, old_scope = rt.pid, rt.scope
        rt.pid = pid
        try:
            rt.run(path, code, arg_table)
        finally:
            rt.pid, rt.scope = old_pid, old_scope
        rt.remove_process(pid)
        return rt.status

    def repl(self):
        """Interactive terminal: the replacement for the wiring of the MIDlet UI."""
        rt = self.runtime
        host = self.hostname
        parser = _ShellParser(self)
        print("OpenTTY Python %s — type 'exit' to quit" % OPEN_VERSION)
        while True:
            try:
                scope = rt.scope
                prompt = "[%s@%s %s]%s " % (
                    scope.get("USER", self.main_user), host, scope.get("PWD", "/home/"),
                    "#" if rt.uid == 0 else "$")
                line = input(prompt)
            except (EOFError, KeyboardInterrupt):
                print()
                return
            if not line.strip():
                continue
            try:
                status = parser.run(line)
                if status is not None and isinstance(status, (int, float)) and status and status in (1, 2, 13, 127, 255):
                    if rt.status and not str(status).startswith("0"):
                        pass
            except LuaExit as e:
                return


class _ShellParser:
    """Front-line prompt: expands env, splits args and dispatches shell builtins
    that the Python-port runtime's exec_shell does not implement (su/source/etc)."""

    def __init__(self, kernel):
        self.kernel = kernel

    def run(self, line):
        rt = self.kernel.runtime
        line = line.strip()
        if not line:
            return None
        # quick builtin pass for shell-only commands the base runtime lacks
        parts = line.split(None, 1)
        cmd = parts[0]
        body = parts[1] if len(parts) > 1 else ""
        if cmd == "su":
            return self.kernel._su(body)
        if cmd in ("exit", "cd", "pwd", "whoami"):
            return rt.shell_handler.call([line], rt)
        try:
            return rt.shell_handler.call([line], rt)
        except LuaExit as e:
            raise
        except LuaError as e:
            print("Error: %s" % e, file=sys.stderr)
            return 1


# EOF