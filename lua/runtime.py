"""
OpenTTY Lua Runtime — Python port of src/Lua.java
Tree-walking interpreter for OpenTTY's Lua dialect.
"""

import os as _os
import sys
import time
import math
import base64 as _base64
import uuid as _uuid
import hashlib
import re
import threading
import traceback as _traceback
from collections import OrderedDict

# ─── Lua constants ────────────────────────────────────────────────────────────

NIL = None  # Lua nil is Python None
TRUE = True
FALSE = False
LUA_NIL = None  # sentinel

# Token types
(EOF, NUMBER, STRING, BOOLEAN, NIL_T, IDENTIFIER,
 PLUS, MINUS, MULTIPLY, DIVIDE, MODULO,
 EQ, NE, LT, GT, LE, GE,
 AND, OR, NOT,
 ASSIGN, IF, THEN, ELSE, END, WHILE, DO, RETURN,
 FUNCTION, LPAREN, RPAREN, COMMA, LOCAL,
 LBRACE, RBRACE, LBRACKET, RBRACKET,
 CONCAT, DOT, ELSEIF, FOR, IN,
 POWER, BREAK, LENGTH, VARARG,
 REPEAT, UNTIL, COLON, LABEL, GOTO) = range(51)


def is_whitespace(c):
    return c in ' \t\n\r'


def is_digit(c):
    return '0' <= c <= '9'


def is_letter(c):
    return ('a' <= c <= 'z') or ('A' <= c <= 'Z') or c == '_'


def is_letter_or_digit(c):
    return is_letter(c) or is_digit(c)


# ─── Token ────────────────────────────────────────────────────────────────────

class Token:
    __slots__ = ('type', 'value', 'offset')

    def __init__(self, ttype, value=None, offset=-1):
        self.type = ttype
        self.value = value
        self.offset = offset

    def __repr__(self):
        return f"Token(type={self.type}, value={self.value!r})"


# ─── LuaFunction ──────────────────────────────────────────────────────────────

class LuaFunction:
    """Represents both user-defined Lua functions and native (C-like) functions."""

    def __init__(self, params=None, body_tokens=None, closure_scope=None, mod=-1, name=None):
        self.params = params or []
        self.body_tokens = body_tokens
        self.closure_scope = closure_scope
        self.mod = mod  # -1 = user function, else = native constant
        self.name = name

    def call(self, args, runtime):
        if self.mod != -1:
            return runtime.internals(self.mod, args)
        return runtime.call_user_function(self, args)


# ─── Process ──────────────────────────────────────────────────────────────────

class Process:
    def __init__(self, pid, name, uid=1000, owner="user"):
        self.pid = pid
        self.name = name
        self.uid = uid
        self.owner = owner
        self.cmd = ""
        self.handler = None
        self.sighandler = None
        self.scope = {}
        self.db = {}


# ─── Runtime ──────────────────────────────────────────────────────────────────

class LuaRuntime:
    """The main Lua runtime. One instance per execution.

    Tokenizer state (tokens / token_index) is thread-local: java.run background
    threads and the foreground interpreter share one runtime but must not
    clobber each other's token stream mid-statement.
    """

    def __init__(self, base_dir=None, username="user"):
        self.base_dir = base_dir or _os.path.dirname(_os.path.dirname(_os.path.abspath(__file__)))
        self.globals = {}
        self.require_cache = {}
        self.labels = {}
        self._tstate = threading.local()

        # Tokenizer state (thread-local; see properties below)
        self.tokens = []
        self.token_index = 0
        self.last_code = ""
        self.current_source = ""

        # Interpreter state
        self.doreturn = False
        self.break_loop = False
        self.loop_depth = 0

        # Process
        self.pid = "1"
        self.uid = 1000
        self.username = username
        self.status = 0
        self.processes = {}  # pid -> Process
        self.next_pid = 100
        self.uptime = int(time.time() * 1000)
        self.stdout_buf = None  # StringBuffer-like for print capture
        self.stdout_target = None  # Where print goes

        # Virtual filesystem
        self.vfs = {}  # path -> content (for /bin/, /etc/, /lib/)
        self.tmp = {}  # /tmp entries
        self.mounts = {}  # Virtual mount table
        self.attributes = {}  # Environment variables
        self.fs = {}  # Mount points from fstab

        # Frame stack for tracebacks
        self.frame_stack = []
        self.thrown_frames = []
        self.thrown_tokens = None
        self.thrown_token_index = -1

        # Load base files into VFS
        self._load_base_files()

        # Register standard libraries
        self._register_globals()

    def _load_base_files(self):
        """Load src/bin, src/etc, src/lib into the virtual filesystem."""
        src_dir = _os.path.join(self.base_dir, "src")
        for subdir in ("bin", "etc", "lib"):
            full = _os.path.join(src_dir, subdir)
            if _os.path.isdir(full):
                for fname in _os.listdir(full):
                    fpath = _os.path.join(full, fname)
                    if _os.path.isfile(fpath):
                        try:
                            with open(fpath, "r", errors="replace") as f:
                                content = f.read()
                            self.vfs[f"/{subdir}/{fname}"] = content
                        except Exception:
                            pass

    # ─── Path resolution ──────────────────────────────────────────────────

    @property
    def tokens(self):
        st = self._tstate
        if not hasattr(st, "tokens"):
            st.tokens = []
        return st.tokens

    @tokens.setter
    def tokens(self, value):
        self._tstate.tokens = value

    @property
    def token_index(self):
        st = self._tstate
        if not hasattr(st, "token_index"):
            st.token_index = 0
        return st.token_index

    @token_index.setter
    def token_index(self, value):
        self._tstate.token_index = value

    def solve_path(self, path, scope=None):
        if scope is None:
            scope = self.scope
        if not path:
            return path
        if path.startswith("/"):
            return path
        pwd = scope.get("PWD", "/home/") if scope else "/home/"
        return pwd.rstrip("/") + "/" + path

    def join_path(self, path, scope=None):
        if scope is None:
            scope = self.scope
        if not path:
            return path
        if path.startswith("/"):
            return path
        root = scope.get("ROOT", "") if scope else ""
        pwd = scope.get("PWD", "/home/") if scope else "/home/"
        if root:
            return root + pwd.rstrip("/") + "/" + path
        return pwd.rstrip("/") + "/" + path

    # ─── Scope / environment ──────────────────────────────────────────────

    @property
    def scope(self):
        return self._scope if hasattr(self, '_scope') else {}

    @scope.setter
    def scope(self, val):
        self._scope = val

    # ─── Globals registration ─────────────────────────────────────────────

    def _register_globals(self):
        g = self.globals

        # Global functions
        g["print"] = LuaFunction(mod=0, name="print")
        g["error"] = LuaFunction(mod=1, name="error")
        g["pcall"] = LuaFunction(mod=2, name="pcall")
        g["require"] = LuaFunction(mod=3, name="require")
        g["load"] = LuaFunction(mod=4, name="load")
        g["pairs"] = LuaFunction(mod=5, name="pairs")
        g["ipairs"] = LuaFunction(mod=14, name="ipairs")
        g["collectgarbage"] = LuaFunction(mod=6, name="collectgarbage")
        g["tostring"] = LuaFunction(mod=7, name="tostring")
        g["tonumber"] = LuaFunction(mod=8, name="tonumber")
        g["select"] = LuaFunction(mod=9, name="select")
        g["type"] = LuaFunction(mod=10, name="type")
        g["getAppProperty"] = LuaFunction(mod=11, name="getAppProperty")
        g["setmetatable"] = LuaFunction(mod=12, name="setmetatable")
        g["getmetatable"] = LuaFunction(mod=13, name="getmetatable")

        g["_VERSION"] = "Lua J2ME"
        g["_G"] = g

        # os library
        os_t = {}
        for name, const in [
            ("getenv", 301), ("setenv", 302), ("clock", 303), ("setlocale", 304),
            ("exit", 305), ("date", 306), ("getpid", 307), ("setproc", 308),
            ("getproc", 309), ("getcwd", 310), ("request", 313),
            ("getuid", 311), ("chdir", 312), ("su", 318), ("remove", 319),
            ("scope", 320), ("join", 321), ("mkdir", 322),
        ]:
            os_t[name] = LuaFunction(mod=const, name=f"os.{name}")
        os_t["execute"] = LuaFunction(mod=300, name="os.execute")
        g["os"] = os_t

        # io library
        io_t = {}
        for name, const in [
            ("read", 400), ("write", 401), ("close", 402), ("open", 403),
            ("popen", 404), ("dirs", 405), ("setstdout", 406), ("mount", 407),
            ("new", 408), ("copy", 409),
        ]:
            io_t[name] = LuaFunction(mod=const, name=f"io.{name}")
        io_t["stdout"] = ""  # stdout buffer
        io_t["stdin"] = ""   # stdin buffer
        g["io"] = io_t

        # string library
        string_t = {}
        for name, const in [
            ("upper", 100), ("lower", 101), ("len", 102), ("find", 103),
            ("match", 104), ("reverse", 105), ("sub", 106), ("hash", 107),
            ("byte", 108), ("char", 109), ("trim", 110), ("split", 111),
            ("uuid", 112), ("getCommand", 113), ("getArgument", 114),
            ("env", 115), ("getpattern", 118), ("startswith", 119), ("endswith", 120),
        ]:
            string_t[name] = LuaFunction(mod=const, name=f"string.{name}")
        g["string"] = string_t

        # table library
        table_t = {}
        for name, const in [
            ("insert", 200), ("concat", 201), ("remove", 202), ("sort", 203),
            ("move", 204), ("unpack", 205), ("pack", 206), ("decode", 207),
        ]:
            table_t[name] = LuaFunction(mod=const, name=f"table.{name}")
        g["table"] = table_t

        # math library
        math_t = {"random": LuaFunction(mod=15, name="math.random")}
        g["math"] = math_t

        # package library
        pkg_t = {"loaded": self.require_cache, "loadlib": g["require"]}
        g["package"] = pkg_t

        # base64 library
        b64_t = {
            "encode": LuaFunction(mod=116, name="base64.encode"),
            "decode": LuaFunction(mod=117, name="base64.decode"),
        }
        g["base64"] = b64_t

        # socket library
        socket_t = {}
        for name, const in [
            ("connect", 502), ("peer", 503), ("device", 504),
            ("server", 505), ("accept", 506),
        ]:
            socket_t[name] = LuaFunction(mod=const, name=f"socket.{name}")
        http_t = {}
        for name, const in [
            ("get", 500), ("post", 501), ("rget", 507), ("rpost", 508),
        ]:
            http_t[name] = LuaFunction(mod=const, name=f"socket.http.{name}")
        socket_t["http"] = http_t
        g["socket"] = socket_t

        # java library
        java_t = {}
        for name, const in [
            ("class", 700), ("getName", 701), ("delete", 702),
            ("run", 704), ("thread", 705), ("sleep", 706),
        ]:
            java_t[name] = LuaFunction(mod=const, name=f"java.{name}")
        jdb = {"username": self.username, "cache": {}, "build": "PC-1.0"}
        jdb["uptime"] = LuaFunction(mod=703, name="java.midlet.uptime")
        java_t["midlet"] = jdb
        g["java"] = java_t

        # graphics library (stub for PC)
        graphics_t = {}
        for name, const in [
            ("display", 600), ("new", 601), ("render", 602),
            ("append", 603), ("addCommand", 604), ("handler", 605),
            ("getCurrent", 606), ("SetTitle", 607), ("SetTicker", 608),
            ("vibrate", 609), ("SetLabel", 610), ("SetText", 611),
            ("GetLabel", 612), ("GetText", 613), ("clear", 614),
        ]:
            graphics_t[name] = LuaFunction(mod=const, name=f"graphics.{name}")
        graphics_t["db"] = {}
        graphics_t["fire"] = None
        g["graphics"] = graphics_t

        # audio library (stub)
        audio_t = {}
        for name, const in [
            ("load", 800), ("play", 801), ("pause", 802),
            ("volume", 803), ("duration", 804), ("time", 805),
        ]:
            audio_t[name] = LuaFunction(mod=const, name=f"audio.{name}")
        g["audio"] = audio_t

        # push library (stub)
        push_t = {}
        for name, const in [
            ("register", 900), ("unregister", 901), ("list", 902),
            ("pending", 903), ("setAlarm", 904),
        ]:
            push_t[name] = LuaFunction(mod=const, name=f"push.{name}")
        g["push"] = push_t

        # os.execute shell handler
        self.shell_handler = g["os"]["execute"]

    # ─── Type helpers ──────────────────────────────────────────────────────

    @staticmethod
    def lua_type(v):
        if v is None:
            return "nil"
        if isinstance(v, bool):
            return "boolean"
        if isinstance(v, (int, float)):
            return "number"
        if isinstance(v, str):
            return "string"
        if isinstance(v, LuaFunction):
            return "function"
        if isinstance(v, dict):
            return "table"
        if isinstance(v, list):
            return "table"
        if isinstance(v, (bytes, bytearray)):
            return "stream"
        return "userdata"

    @staticmethod
    def to_lua_string(v):
        if v is None:
            return "nil"
        if isinstance(v, bool):
            return "true" if v else "false"
        if isinstance(v, (int, float)):
            if isinstance(v, float) and v == int(v) and not _math_is_inf(v):
                return str(int(v))
            return str(v)
        return str(v)

    @staticmethod
    def is_truthy(v):
        if v is None:
            return False
        if isinstance(v, bool):
            return v
        return True

    @staticmethod
    def unwrap(v):
        return None if v is LUAL_NIL_SENTINEL else v

    @staticmethod
    def _collapse(v):
        """Standard Lua multi-return collapse: single-elem list -> scalar."""
        if isinstance(v, list) and len(v) == 1:
            return v[0]
        if isinstance(v, list) and len(v) == 0:
            return None
        return v

    @staticmethod
    def wrap(v):
        return v if v is not None else LUAL_NIL_SENTINEL

    def _find_var(self, name, scope):
        """Look up a variable in scope chain, then globals."""
        val = scope.get(name, _MISSING)
        if val is not _MISSING:
            return val
        val = self.globals.get(name, _MISSING)
        if val is not _MISSING:
            return val
        return None

    # ─── Tokenizer ────────────────────────────────────────────────────────

    def tokenize(self, code):
        if not code:
            return [Token(EOF)]
        tokens = []
        i = 0
        n = len(code)

        # Skip shebang
        if code.startswith("#!"):
            while i < n and code[i] != '\n':
                i += 1
            if i < n and code[i] == '\n':
                i += 1

        while i < n:
            start = i
            c = code[i]

            if is_whitespace(c) or c == ';':
                i += 1
                continue

            # Comments
            if c == '-' and i + 1 < n and code[i + 1] == '-':
                i += 2
                if i + 1 < n and code[i] == '[' and code[i + 1] == '[':
                    i += 2
                    while i + 1 < n and not (code[i] == ']' and code[i + 1] == ']'):
                        i += 1
                    if i + 1 < n:
                        i += 2
                else:
                    while i < n and code[i] != '\n':
                        i += 1
                continue

            # Dots / concat / vararg
            if c == '.':
                if i + 2 < n and code[i + 1] == '.' and code[i + 2] == '.':
                    tokens.append(Token(VARARG, "...", start))
                    i += 3
                elif i + 1 < n and code[i + 1] == '.':
                    tokens.append(Token(CONCAT, "..", start))
                    i += 2
                else:
                    tokens.append(Token(DOT, ".", start))
                    i += 1
                continue

            # Colon / label
            if c == ':':
                if i + 1 < n and code[i + 1] == ':':
                    i += 2
                    name_start = i
                    while i < n and (is_letter_or_digit(code[i]) or code[i] == '_'):
                        i += 1
                    if i + 1 < n and code[i] == ':' and code[i + 1] == ':':
                        i += 2
                        tokens.append(Token(LABEL, code[name_start:i - 2], start))
                    else:
                        i -= 2
                        tokens.append(Token(COLON, ":", start))
                        i += 1
                else:
                    tokens.append(Token(COLON, ":", start))
                    i += 1
                continue

            # Numbers
            if is_digit(c) or (c == '.' and i + 1 < n and is_digit(code[i + 1])):
                has_dot = False
                while i < n and (is_digit(code[i]) or code[i] == '.'):
                    if code[i] == '.':
                        if has_dot:
                            break
                        if i + 1 < n and code[i + 1] == '.':
                            break
                        has_dot = True
                    i += 1
                tokens.append(Token(NUMBER, float(code[start:i]), start))
                continue

            # Negative numbers
            if c == '-' and i + 1 < n and (is_digit(code[i + 1]) or (code[i + 1] == '.' and i + 2 < n and is_digit(code[i + 2]))):
                i += 1
                has_dot = False
                while i < n and (is_digit(code[i]) or code[i] == '.'):
                    if code[i] == '.':
                        if has_dot:
                            break
                        if i + 1 < n and code[i + 1] == '.':
                            break
                        has_dot = True
                    i += 1
                tokens.append(Token(NUMBER, float(code[start:i]), start))
                continue

            # Strings
            if c in ('"', "'"):
                quote = c
                i += 1
                str_start = i
                while i < n and code[i] != quote:
                    i += 1
                tokens.append(Token(STRING, code[str_start:i], start))
                if i < n:
                    i += 1
                continue

            if c == '[' and i + 1 < n and code[i + 1] == '[':
                i += 2
                str_start = i
                while i + 1 < n and not (code[i] == ']' and code[i + 1] == ']'):
                    i += 1
                tokens.append(Token(STRING, code[str_start:i], start))
                if i + 1 < n:
                    i += 2
                continue

            # Identifiers / keywords
            if is_letter(c):
                word_start = i
                while i < n and is_letter_or_digit(code[i]):
                    i += 1
                word = code[word_start:i]
                keywords = {
                    "true": BOOLEAN, "false": BOOLEAN, "nil": NIL_T,
                    "and": AND, "or": OR, "not": NOT,
                    "if": IF, "then": THEN, "else": ELSE, "elseif": ELSEIF, "end": END,
                    "while": WHILE, "do": DO, "return": RETURN,
                    "function": FUNCTION, "local": LOCAL,
                    "for": FOR, "in": IN, "break": BREAK,
                    "repeat": REPEAT, "until": UNTIL,
                    "goto": GOTO,
                }
                ttype = keywords.get(word, IDENTIFIER)
                tokens.append(Token(ttype, word, start))
                continue

            # Operators
            ops = {
                '+': PLUS, '-': MINUS, '*': MULTIPLY, '/': DIVIDE,
                '%': MODULO, '(': LPAREN, ')': RPAREN, ',': COMMA,
                '^': POWER, '#': LENGTH, '{': LBRACE, '}': RBRACE,
                '[': LBRACKET, ']': RBRACKET,
            }
            if c in ops:
                tokens.append(Token(ops[c], c, start))
                i += 1
                continue
            if c == '=':
                if i + 1 < n and code[i + 1] == '=':
                    tokens.append(Token(EQ, "==", start))
                    i += 2
                else:
                    tokens.append(Token(ASSIGN, "=", start))
                    i += 1
                continue
            if c == '~':
                if i + 1 < n and code[i + 1] == '=':
                    tokens.append(Token(NE, "~=", start))
                    i += 2
                else:
                    raise LuaError(f"Unexpected character '~'")
                continue
            if c == '<':
                if i + 1 < n and code[i + 1] == '=':
                    tokens.append(Token(LE, "<=", start))
                    i += 2
                else:
                    tokens.append(Token(LT, "<", start))
                    i += 1
                continue
            if c == '>':
                if i + 1 < n and code[i + 1] == '=':
                    tokens.append(Token(GE, ">=", start))
                    i += 2
                else:
                    tokens.append(Token(GT, ">", start))
                    i += 1
                continue

            raise LuaError(f"Unexpected character '{c}'")

        tokens.append(Token(EOF, "EOF", n))
        return tokens

    def peek(self):
        if self.token_index < len(self.tokens):
            return self.tokens[self.token_index]
        return Token(EOF)

    def peek_next(self):
        if self.token_index + 1 < len(self.tokens):
            return self.tokens[self.token_index + 1]
        return Token(EOF)

    def consume(self, expected=None):
        tok = self.peek()
        if expected is not None and tok.type != expected:
            raise LuaError(f"Expected token type {expected} but got {tok.type} with value {tok.value}")
        self.token_index += 1
        return tok

    # ─── Statement evaluator ──────────────────────────────────────────────

    def run(self, source, code, arg_table=None):
        """Execute Lua source code. Returns status code."""
        self.current_source = source or ""
        self.last_code = code or ""
        self.frame_stack = []
        self.thrown_frames = []
        self.thrown_tokens = None
        self.thrown_token_index = -1
        self.status = 0
        self.doreturn = False
        self.break_loop = False

        scope = dict(self.globals)
        if arg_table is not None:
            scope["arg"] = arg_table
        self.scope = scope

        try:
            self.tokens = self.tokenize(code)
            self.token_index = 0
            self._collect_labels()

            while self.peek().type != EOF:
                result = self.statement(scope)
                if self.doreturn:
                    self.doreturn = False
                    break
        except LuaExit as e:
            self.status = e.status
        except LuaError as e:
            self._record_throw()
            tb = self._get_traceback(e)
            print(tb, file=sys.stderr)
            self.status = 1
        except Exception as e:
            self._record_throw()
            tb = self._get_traceback(e)
            print(tb, file=sys.stderr)
            self.status = 1

        return self.status

    def exec_code(self, code, scope=None):
        """Execute code in a new scope (for load/require)."""
        if not code:
            return None
        saved_tokens = self.tokens
        saved_index = self.token_index
        saved_source = self.current_source
        saved_code = self.last_code
        try:
            self.tokens = self.tokenize(code)
            self.token_index = 0
            self.last_code = code
            mod_scope = dict(scope) if scope else {}
            for k, v in self.globals.items():
                if k not in mod_scope:
                    mod_scope[k] = v
            ret = None
            while self.peek().type != EOF:
                result = self.statement(mod_scope)
                if self.doreturn:
                    ret = result
                    self.doreturn = False
                    break
            return ret
        finally:
            self.tokens = saved_tokens
            self.token_index = saved_index
            self.current_source = saved_source
            self.last_code = saved_code

    def _collect_labels(self):
        saved = self.token_index
        self.labels = {}
        self.token_index = 0
        while self.peek().type != EOF:
            tok = self.peek()
            if tok.type == LABEL:
                self.consume(LABEL)
                self.labels[tok.value] = self.token_index
            else:
                self.consume()
        self.token_index = saved

    # ─── Statements ───────────────────────────────────────────────────────

    def statement(self, scope):
        current = self.peek()

        if current.type == IDENTIFIER:
            return self._stmt_identifier(scope)
        elif current.type == LABEL:
            self.consume(LABEL)
            return None
        elif current.type == GOTO:
            self.consume(GOTO)
            name = self.consume(IDENTIFIER).value
            if name not in self.labels:
                raise LuaError(f"undefined label '{name}'")
            self.token_index = self.labels[name]
            return None
        elif current.type == IF:
            return self._stmt_if(scope)
        elif current.type == FOR:
            return self._stmt_for(scope)
        elif current.type == WHILE:
            return self._stmt_while(scope)
        elif current.type == REPEAT:
            return self._stmt_repeat(scope)
        elif current.type == RETURN:
            return self._stmt_return(scope)
        elif current.type == FUNCTION:
            return self._stmt_function(scope)
        elif current.type == LOCAL:
            return self._stmt_local(scope)
        elif current.type == BREAK:
            if self.loop_depth == 0:
                raise LuaError("break outside loop")
            self.consume(BREAK)
            self.break_loop = True
            return None
        elif current.type == DO:
            return self._stmt_do(scope)
        elif current.type == END:
            self.consume(END)
            return None
        elif current.type in (LPAREN, NUMBER, STRING, BOOLEAN, NIL_T, NOT):
            return self.expression(scope)
        raise LuaError(f"Unexpected token at statement: {current}")

    def _stmt_identifier(self, scope):
        # Check for multi-assign pattern: a, b, c = ...
        la = 0
        is_multi = False
        if self.token_index + la < len(self.tokens) and self.tokens[self.token_index + la].type == IDENTIFIER:
            la += 1
            while (self.token_index + la < len(self.tokens) and
                   self.tokens[self.token_index + la].type == COMMA):
                if not (self.token_index + la + 1 < len(self.tokens) and
                        self.tokens[self.token_index + la + 1].type == IDENTIFIER):
                    is_multi = False
                    break
                la += 2
            if (self.token_index + la < len(self.tokens) and
                    self.tokens[self.token_index + la].type == ASSIGN):
                is_multi = True

        nxt = self.peek_next()
        if not is_multi and nxt.type == LPAREN:
            func_name = self.consume(IDENTIFIER).value
            return self._call_function(func_name, scope)

        if is_multi:
            var_names = [self.consume(IDENTIFIER).value]
            while self.peek().type == COMMA:
                self.consume(COMMA)
                var_names.append(self.consume(IDENTIFIER).value)
            self.consume(ASSIGN)
            values = [self.expression(scope)]
            while self.peek().type == COMMA:
                self.consume(COMMA)
                values.append(self.expression(scope))
            # Expand last value if it's a list
            assign_vals = []
            for i, v in enumerate(values):
                if i == len(values) - 1 and isinstance(v, list):
                    assign_vals.extend(v)
                else:
                    assign_vals.append(v)
            for i, vn in enumerate(var_names):
                val = assign_vals[i] if i < len(assign_vals) else None
                scope[vn] = val if val is not None else NIL
            return None

        var_name = self.consume(IDENTIFIER).value
        if self.peek().type in (DOT, LBRACKET):
            table_obj, key = self._resolve_table_key(var_name, scope)
            if not isinstance(table_obj, dict):
                raise LuaError("Attempt to index non-table value")
            if self.peek().type == ASSIGN:
                self.consume(ASSIGN)
                val = self.expression(scope)
                table_obj[key] = val if val is not None else NIL
                return None
            elif self.peek().type == LPAREN:
                fn = table_obj.get(key)
                return self._call_function_object(fn, scope)
            else:
                return table_obj.get(key)
        elif self.peek().type == COLON:
            self_obj = self._find_var(var_name, scope)
            if self_obj is None:
                raise LuaError(f"attempt to call method on nil value: {var_name}")
            self.consume(COLON)
            method_name = self.consume(IDENTIFIER).value
            method_obj = self._resolve_method(self_obj)
            if isinstance(method_obj, dict):
                fn = method_obj.get(method_name)
                if fn is None:
                    raise LuaError(f"method '{method_name}' not found")
                return self._call_method(self_obj, var_name, fn, method_name, scope)
            raise LuaError(f"attempt to call method on unsupported type: {self.lua_type(self_obj)}")
        else:
            if self.peek().type == ASSIGN:
                self.consume(ASSIGN)
                val = self.expression(scope)
                scope[var_name] = val if val is not None else NIL
                return None
            elif self.peek().type == LPAREN:
                return self._call_function(var_name, scope)
            else:
                return self._find_var(var_name, scope)

    def _stmt_if(self, scope):
        self.consume(IF)
        cond = self.expression(scope)
        self.consume(THEN)
        result = None
        taken = False
        if self.is_truthy(cond):
            taken = True
            while self.peek().type not in (ELSEIF, ELSE, END):
                result = self.statement(scope)
                if self.doreturn:
                    return result
        else:
            self._skip_if_body()
        while self.peek().type == ELSEIF:
            self.consume(ELSEIF)
            cond = self.expression(scope)
            self.consume(THEN)
            if not taken and self.is_truthy(cond):
                taken = True
                while self.peek().type not in (ELSEIF, ELSE, END):
                    result = self.statement(scope)
                    if self.doreturn:
                        return result
            else:
                self._skip_if_body()
        if self.peek().type == ELSE:
            self.consume(ELSE)
            if not taken:
                while self.peek().type != END:
                    result = self.statement(scope)
                    if self.doreturn:
                        return result
            else:
                self._skip_until_matching_end()
        self.consume(END)
        return result

    def _stmt_for(self, scope):
        self.consume(FOR)
        self.loop_depth += 1
        try:
            if self.peek().type == IDENTIFIER:
                save = self.token_index
                name = self.consume(IDENTIFIER).value
                if self.peek().type == ASSIGN:
                    return self._for_numeric(name, scope)
                else:
                    self.token_index = save
                    return self._for_generic(scope)
            raise LuaError("Malformed 'for' statement")
        finally:
            self.loop_depth -= 1

    def _for_numeric(self, name, scope):
        self.consume(ASSIGN)
        a = self.expression(scope)
        self.consume(COMMA)
        b = self.expression(scope)
        start = float(a) if isinstance(a, (int, float)) else float(self.to_lua_string(a))
        stop = float(b) if isinstance(b, (int, float)) else float(self.to_lua_string(b))
        step = 1.0
        if self.peek().type == COMMA:
            self.consume(COMMA)
            c = self.expression(scope)
            step = float(c) if isinstance(c, (int, float)) else float(self.to_lua_string(c))
            if step == 0.0:
                raise LuaError("for step must not be zero")
        self.consume(DO)
        body_tokens = self._capture_block_tokens()
        while (step > 0 and start <= stop) or (step < 0 and start >= stop):
            if self.break_loop:
                self.break_loop = False
                break
            scope[name] = start
            result = self._exec_token_list(body_tokens, scope)
            if result is not None:
                return result
            if self.doreturn:
                return result
            start += step
        return None

    def _for_generic(self, scope):
        names = [self.consume(IDENTIFIER).value]
        while self.peek().type == COMMA:
            self.consume(COMMA)
            names.append(self.consume(IDENTIFIER).value)
        self.consume(IN)
        iter_src = self.expression(scope)
        self.consume(DO)
        body_tokens = self._capture_block_tokens()

        # ipairs iterator
        if isinstance(iter_src, dict) and "__table" in iter_src:
            table_obj = iter_src["__table"]
            idx = int(iter_src.get("__index", 0))
            if isinstance(table_obj, dict):
                lst = self._table_to_list(table_obj)
                for i in range(idx, len(lst)):
                    item = lst[i]
                    if len(names) >= 1:
                        scope[names[0]] = float(i + 1)
                    if len(names) >= 2:
                        scope[names[1]] = item if item is not None else NIL
                    iter_src["__index"] = float(i + 1)
                    result = self._exec_token_list(body_tokens, scope)
                    if self.doreturn or result is not None:
                        return result
                    if self.break_loop:
                        self.break_loop = False
                        break
        elif isinstance(iter_src, dict):
            for k, v in iter_src.items():
                if len(names) >= 1:
                    scope[names[0]] = k
                if len(names) >= 2:
                    scope[names[1]] = v if v is not None else NIL
                result = self._exec_token_list(body_tokens, scope)
                if self.doreturn or result is not None:
                    return result
                if self.break_loop:
                    self.break_loop = False
                    break
        elif isinstance(iter_src, list):
            for idx, item in enumerate(iter_src):
                if isinstance(item, list) and len(item) >= 2:
                    k, v = item[0], item[1]
                else:
                    k, v = float(idx + 1), item
                if len(names) >= 1:
                    scope[names[0]] = k
                if len(names) >= 2:
                    scope[names[1]] = v if v is not None else NIL
                result = self._exec_token_list(body_tokens, scope)
                if self.doreturn or result is not None:
                    return result
                if self.break_loop:
                    self.break_loop = False
                    break
        return None

    def _stmt_while(self, scope):
        self.consume(WHILE)
        cond_start = self.token_index
        self.loop_depth += 1
        result = None
        end_consumed = False
        try:
            while True:
                self.token_index = cond_start
                cond = self.expression(scope)
                if not self.is_truthy(cond) or self.break_loop or self.doreturn:
                    self._skip_to_matching_end()
                    end_consumed = True
                    break
                self.consume(DO)
                while self.peek().type != END:
                    result = self.statement(scope)
                    if self.doreturn or self.break_loop:
                        break
                self.token_index = cond_start
        finally:
            self.loop_depth -= 1
        if not end_consumed:
            self.consume(END)
        return result

    def _stmt_repeat(self, scope):
        self.consume(REPEAT)
        body_start = self.token_index
        result = None
        self.loop_depth += 1
        try:
            while True:
                self.token_index = body_start
                while self.peek().type != UNTIL:
                    result = self.statement(scope)
                    if self.doreturn or self.break_loop:
                        while self.peek().type not in (UNTIL, EOF):
                            self.consume()
                        break
                self.consume(UNTIL)
                cond = self.expression(scope)
                if self.is_truthy(cond) or self.doreturn:
                    break
                elif self.break_loop:
                    self.break_loop = False
                    break
        finally:
            self.loop_depth -= 1
        return result

    def _stmt_return(self, scope):
        self.consume(RETURN)
        self.doreturn = True
        if self.peek().type in (EOF, END):
            return []
        results = [self.expression(scope)]
        while self.peek().type == COMMA:
            self.consume(COMMA)
            results.append(self.expression(scope))
        return results

    def _stmt_function(self, scope):
        self.consume(FUNCTION)
        func_name = self.consume(IDENTIFIER).value
        is_table_assign = self.peek().type in (DOT, LBRACKET)
        target_table = None
        key = None
        if is_table_assign:
            target_table, key = self._resolve_table_key(func_name, scope)
            if not isinstance(target_table, dict):
                raise LuaError("Attempt to index non-table value in function definition")
        self.consume(LPAREN)
        params = self._parse_params()
        self.consume(RPAREN)
        body_tokens = self._capture_block_tokens(depth_start=1)
        func = LuaFunction(params, body_tokens, scope, name=func_name)
        if is_table_assign:
            target_table[key] = func
        else:
            scope[func_name] = func
        return None

    def _stmt_local(self, scope):
        self.consume(LOCAL)
        if self.peek().type == FUNCTION:
            self.consume(FUNCTION)
            func_name = self.consume(IDENTIFIER).value
            self.consume(LPAREN)
            params = self._parse_params()
            self.consume(RPAREN)
            body_tokens = self._capture_block_tokens(depth_start=1)
            func = LuaFunction(params, body_tokens, scope, name=func_name)
            scope[func_name] = func
            return None
        else:
            var_names = [self.consume(IDENTIFIER).value]
            while self.peek().type == COMMA:
                self.consume(COMMA)
                var_names.append(self.consume(IDENTIFIER).value)
            if self.peek().type == ASSIGN:
                self.consume(ASSIGN)
                values = [self.expression(scope)]
                while self.peek().type == COMMA:
                    self.consume(COMMA)
                    values.append(self.expression(scope))
                assign_vals = []
                for i, v in enumerate(values):
                    if i == len(values) - 1 and isinstance(v, list):
                        assign_vals.extend(v)
                    else:
                        assign_vals.append(v)
                for i, vn in enumerate(var_names):
                    val = assign_vals[i] if i < len(assign_vals) else None
                    scope[vn] = val if val is not None else NIL
            else:
                for vn in var_names:
                    scope[vn] = NIL
            return None

    def _stmt_do(self, scope):
        self.consume(DO)
        body_tokens = self._capture_block_tokens(depth_start=1)
        return self._exec_token_list(body_tokens, scope)

    def _capture_block_tokens(self, depth_start=1):
        depth = depth_start
        body = []
        while depth > 0:
            tok = self.consume()
            if tok.type in (FUNCTION, IF, DO):
                depth += 1
            elif tok.type == END:
                depth -= 1
            elif tok.type == EOF:
                raise LuaError("Unmatched block: Expected 'end'")
            if depth > 0:
                body.append(tok)
        return body

    def _skip_if_body(self):
        depth = 1
        while True:
            t = self.consume()
            if t.type in (IF, WHILE, FUNCTION, FOR):
                depth += 1
            elif t.type == END:
                depth -= 1
                if depth == 0:
                    self.token_index -= 1
                    return
            elif t.type in (ELSEIF, ELSE) and depth == 1:
                self.token_index -= 1
                return
            elif t.type == EOF:
                raise LuaError("Unmatched 'if' statement: Expected 'end'")

    def _skip_until_matching_end(self):
        depth = 1
        while depth > 0:
            t = self.consume()
            if t.type in (IF, WHILE, FUNCTION, FOR):
                depth += 1
            elif t.type == END:
                depth -= 1
            elif t.type == EOF:
                raise LuaError("Unmatched block: Expected 'end'")
        self.token_index -= 1

    def _skip_to_matching_end(self):
        depth = 1
        while depth > 0:
            t = self.consume()
            if t.type in (IF, WHILE, FUNCTION, FOR):
                depth += 1
            elif t.type == END:
                depth -= 1
            elif t.type == EOF:
                raise LuaError("Unmatched block: Expected 'end'")

    def _exec_token_list(self, body_tokens, scope):
        saved_tokens = self.tokens
        saved_index = self.token_index
        try:
            self.tokens = body_tokens
            self.token_index = 0
            result = None
            while self.peek().type != EOF:
                result = self.statement(scope)
                if self.doreturn or self.break_loop:
                    break
            return result
        finally:
            self.tokens = saved_tokens
            self.token_index = saved_index

    def _parse_params(self):
        params = []
        while True:
            t = self.peek().type
            if t == IDENTIFIER:
                params.append(self.consume(IDENTIFIER).value)
            elif t == VARARG:
                self.consume(VARARG)
                params.append("...")
                break
            else:
                break
            if self.peek().type == COMMA:
                self.consume(COMMA)
            else:
                break
        return params

    # ─── Expressions ──────────────────────────────────────────────────────

    def expression(self, scope):
        return self._logical_or(scope)

    def _logical_or(self, scope):
        left = self._logical_and(scope)
        while self.peek().type == OR:
            self.consume(OR)
            right = self._logical_and(scope)
            left = left if self.is_truthy(left) else right
        return left

    def _logical_and(self, scope):
        left = self._comparison(scope)
        while self.peek().type == AND:
            self.consume(AND)
            right = self._comparison(scope)
            left = right if self.is_truthy(left) else left
        return left

    def _comparison(self, scope):
        left = self._concatenation(scope)
        while self.peek().type in (EQ, NE, LT, GT, LE, GE):
            op = self.consume()
            right = self._concatenation(scope)
            if op.type == EQ:
                left = left == right
            elif op.type == NE:
                left = left != right
            elif op.type == LT:
                left = float(left) < float(right)
            elif op.type == GT:
                left = float(left) > float(right)
            elif op.type == LE:
                left = float(left) <= float(right)
            elif op.type == GE:
                left = float(left) >= float(right)
        return left

    def _concatenation(self, scope):
        left = self._arithmetic(scope)
        if self.peek().type != CONCAT:
            return left
        parts = [self.to_lua_string(left)]
        while self.peek().type == CONCAT:
            self.consume(CONCAT)
            parts.append(self.to_lua_string(self._arithmetic(scope)))
        return ''.join(parts)

    def _arithmetic(self, scope):
        left = self._term(scope)
        while self.peek().type in (PLUS, MINUS):
            op = self.consume()
            right = self._term(scope)
            if not (isinstance(left, (int, float)) and isinstance(right, (int, float))):
                raise LuaError("Arithmetic operation on non-number types.")
            if op.type == PLUS:
                left = float(left) + float(right)
            elif op.type == MINUS:
                left = float(left) - float(right)
        return left

    def _term(self, scope):
        left = self._exponentiation(scope)
        while self.peek().type in (MULTIPLY, DIVIDE, MODULO):
            op = self.consume()
            right = self._exponentiation(scope)
            if not (isinstance(left, (int, float)) and isinstance(right, (int, float))):
                raise LuaError("Arithmetic operation on non-number types.")
            l, r = float(left), float(right)
            if op.type == MULTIPLY:
                left = l * r
            elif op.type == DIVIDE:
                if r == 0:
                    raise LuaError("Division by zero.")
                left = l / r
            elif op.type == MODULO:
                if r == 0:
                    raise LuaError("Modulo by zero.")
                left = l % r
        return left

    def _exponentiation(self, scope):
        left = self._factor(scope)
        while self.peek().type == POWER:
            self.consume(POWER)
            right = self._factor(scope)
            if not (isinstance(left, (int, float)) and isinstance(right, (int, float))):
                raise LuaError("Arithmetic operation on non-number types.")
            base, exp = float(left), float(right)
            if exp == 0:
                result = 1.0
            elif exp == 0.5:
                if base < 0:
                    raise LuaError("Square root of negative number.")
                result = math.sqrt(base)
            elif exp < 0 and exp == int(exp):
                base = 1 / base
                exp = -exp
                result = 1.0
                for _ in range(int(exp)):
                    result *= base
            elif exp == int(exp):
                result = 1.0
                for _ in range(int(exp)):
                    result *= base
            else:
                raise LuaError(f"Fractional exponent not supported: {exp}")
            left = result
        return left

    def _factor(self, scope):
        current = self.peek()

        if current.type == STRING:
            self.consume(STRING)
            return current.value
        elif current.type == NUMBER:
            self.consume(NUMBER)
            return current.value
        elif current.type == BOOLEAN:
            self.consume(BOOLEAN)
            return current.value == "true"
        elif current.type == NIL_T:
            self.consume(NIL_T)
            return None
        elif current.type == NOT:
            self.consume(NOT)
            return not self.is_truthy(self._factor(scope))
        elif current.type == LPAREN:
            self.consume(LPAREN)
            val = self.expression(scope)
            self.consume(RPAREN)
            return val
        elif current.type == LENGTH:
            self.consume(LENGTH)
            val = self._factor(scope)
            if val is None or isinstance(val, bool):
                raise LuaError(f"attempt to get length of a {'nil' if val is None else 'boolean'} value")
            elif isinstance(val, str):
                return float(len(val))
            elif isinstance(val, (dict, list)):
                return float(len(val))
            elif isinstance(val, bytes):
                return float(len(val))
            return 0.0
        elif current.type == FUNCTION:
            self.consume(FUNCTION)
            self.consume(LPAREN)
            params = self._parse_params()
            self.consume(RPAREN)
            body_tokens = self._capture_block_tokens(depth_start=1)
            return LuaFunction(params, body_tokens, scope)
        elif current.type == VARARG:
            self.consume(VARARG)
            varargs = scope.get("...")
            return varargs if varargs is not None else {}
        elif current.type == LBRACE:
            return self._table_constructor(scope)
        elif current.type == IDENTIFIER:
            name = self.consume(IDENTIFIER).value
            value = self._find_var(name, scope)
            # Table access
            while self.peek().type in (LBRACKET, DOT):
                if self.peek().type == LBRACKET:
                    self.consume(LBRACKET)
                    key = self.expression(scope)
                    self.consume(RBRACKET)
                else:
                    self.consume(DOT)
                    key = self.consume(IDENTIFIER).value
                if value is None:
                    return None
                if not isinstance(value, dict):
                    raise LuaError("attempt to index a non-table value")
                value = value.get(key)
            # Method call on expression
            if self.peek().type == COLON:
                object_name = self.tokens[self.token_index - 1].value
                self_obj = self._find_var(object_name, scope)
                if self_obj is None:
                    raise LuaError(f"attempt to call method on nil value: {object_name}")
                self.consume(COLON)
                method_name = self.consume(IDENTIFIER).value
                module = self._resolve_method(self_obj)
                fn = None
                if module is self_obj and isinstance(self_obj, dict):
                    fn = self_obj.get(method_name)
                elif isinstance(module, dict):
                    fn = module.get(method_name)
                if fn is None:
                    raise LuaError(f"method '{method_name}' not found for type: {self.lua_type(self_obj)}")
                return self._call_method(self_obj, object_name, fn, method_name, scope)
            elif self.peek().type == LPAREN:
                return self._call_function_object(value, scope)
            return value

        raise LuaError(f"Unexpected token at factor: {current}")

    def _table_constructor(self, scope):
        self.consume(LBRACE)
        table = {}
        index = 1.0
        while self.peek().type != RBRACE:
            if self.peek().type == IDENTIFIER and self.peek_next().type == ASSIGN:
                key = self.consume(IDENTIFIER).value
                self.consume(ASSIGN)
                val = self.expression(scope)
            elif self.peek().type == LBRACKET:
                self.consume(LBRACKET)
                key = self.expression(scope)
                self.consume(RBRACKET)
                self.consume(ASSIGN)
                val = self.expression(scope)
            else:
                val = self.expression(scope)
                key = index
                index += 1.0
            table[key] = val if val is not None else NIL
            if self.peek().type == COMMA:
                self.consume(COMMA)
            elif self.peek().type == RBRACE:
                break
            else:
                raise LuaError("Malformed table syntax.")
        self.consume(RBRACE)
        return table

    # ─── Function calls ───────────────────────────────────────────────────

    def _call_function(self, func_name, scope):
        self.consume(LPAREN)
        args = []
        if self.peek().type != RPAREN:
            args.append(self.expression(scope))
            while self.peek().type == COMMA:
                self.consume(COMMA)
                args.append(self.expression(scope))
        self.consume(RPAREN)
        func_obj = self._find_var(func_name, scope)
        if isinstance(func_obj, LuaFunction):
            return self._collapse(func_obj.call(args, self))
        raise LuaError(f"Attempt to call a non-function value: {func_name}")

    def _call_function_object(self, func_obj, scope):
        self.consume(LPAREN)
        args = []
        if self.peek().type != RPAREN:
            args.append(self.expression(scope))
            while self.peek().type == COMMA:
                self.consume(COMMA)
                args.append(self.expression(scope))
        self.consume(RPAREN)
        if isinstance(func_obj, LuaFunction):
            return self._collapse(func_obj.call(args, self))
        raise LuaError("Attempt to call a non-function value (by object).")

    def _call_method(self, self_obj, var_name, method_obj, method_name, scope):
        self.consume(LPAREN)
        args = [self_obj]
        if self.peek().type != RPAREN:
            args.append(self.expression(scope))
            while self.peek().type == COMMA:
                self.consume(COMMA)
                args.append(self.expression(scope))
        self.consume(RPAREN)
        if isinstance(method_obj, LuaFunction):
            return self._collapse(method_obj.call(args, self))
        raise LuaError(f"attempt to call non-function as method: {method_name}")

    def _resolve_table_key(self, var_name, scope):
        table = self._find_var(var_name, scope)
        key = None
        while self.peek().type in (DOT, LBRACKET):
            if self.peek().type == DOT:
                self.consume(DOT)
                key = self.consume(IDENTIFIER).value
            else:
                self.consume(LBRACKET)
                key = self.expression(scope)
                self.consume(RBRACKET)
            if table is None:
                raise LuaError("attempt to index a nil value")
            if not isinstance(table, dict):
                raise LuaError("attempt to index a nil value")
            if self.peek().type in (DOT, LBRACKET):
                table = table.get(key)
        return table, key

    def _resolve_method(self, obj):
        if isinstance(obj, dict):
            mt = obj.get("__metatable")
            if isinstance(mt, dict):
                idx = mt.get("__index")
                if isinstance(idx, (dict, LuaFunction)):
                    return idx
        t = self.lua_type(obj)
        type_map = {
            "string": "string", "table": "table",
            "connection": "socket", "server": "socket",
        }
        if t in type_map:
            return self.globals.get(type_map[t])
        return obj

    # ─── Virtual filesystem ───────────────────────────────────────────────

    def get_content(self, path, scope=None):
        if scope is None:
            scope = self.scope
        if not path:
            return ""

        # Try VFS first
        if path in self.vfs:
            return self.vfs[path]

        # Try real filesystem (for /mnt/)
        if path.startswith("/mnt/"):
            real = _os.path.join("/mnt", path[5:])
            if _os.path.isfile(real):
                try:
                    with open(real, "r", errors="replace") as f:
                        return f.read()
                except Exception:
                    return ""

        # Try /tmp/
        if path.startswith("/tmp/"):
            return self.tmp.get(path, "")

        # Try relative to pwd
        if not path.startswith("/"):
            pwd = scope.get("PWD", "/home/") if scope else "/home/"
            full = pwd.rstrip("/") + "/" + path
            if full in self.vfs:
                return self.vfs[full]
            return ""

        return ""

    def get_input_stream(self, path, scope=None):
        content = self.get_content(path, scope)
        if content:
            return content.encode("utf-8")
        return None

    def write_file(self, path, content, uid=1000, scope=None):
        if scope is None:
            scope = self.scope
        if isinstance(content, str):
            self.vfs[path] = content
            return 0
        elif isinstance(content, bytes):
            self.vfs[path] = content.decode("utf-8", errors="replace")
            return 0
        return 1

    def delete_file(self, path, uid=1000, scope=None):
        if path in self.vfs:
            del self.vfs[path]
            return 0
        if path.startswith("/tmp/") and path in self.tmp:
            del self.tmp[path]
            return 0
        return 127

    def get_dirs(self, path, scope=None):
        if scope is None:
            scope = self.scope
        if not path:
            path = scope.get("PWD", "/home/")
        if not path.startswith("/"):
            pwd = scope.get("PWD", "/home/")
            path = pwd.rstrip("/") + "/" + path
        if not path.endswith("/"):
            path += "/"
        # Normalize
        path = self.solve_path(path, scope)

        result = {}
        idx = 1

        if path == "/tmp/":
            for k in self.tmp:
                result[float(idx)] = k
                idx += 1
        elif path == "/mnt/":
            try:
                import subprocess
                roots = subprocess.check_output(["lsblk", "-dnpo", "NAME"], text=True).strip().split("\n")
                for r in roots:
                    result[float(idx)] = _os.path.basename(r)
                    idx += 1
            except Exception:
                pass
        elif path.startswith("/mnt/"):
            real = _os.path.join("/mnt", path[5:])
            if _os.path.isdir(real):
                for entry in _os.listdir(real):
                    result[float(idx)] = entry
                    idx += 1
        elif path in ("/bin/", "/etc/", "/lib/"):
            for vpath in self.vfs:
                prefix = path
                if vpath.startswith(prefix) and vpath != prefix:
                    name = vpath[len(prefix):]
                    if "/" not in name:
                        result[float(idx)] = name
                        idx += 1
        elif path == "/home/":
            for vpath in self.vfs:
                if vpath.startswith("/home/"):
                    parts = vpath[6:].strip("/").split("/")
                    if parts and parts[0]:
                        name = parts[0] + "/"
                        if name not in result.values():
                            result[float(idx)] = name
                            idx += 1

        # Virtual mounts
        if path in self.fs:
            entries = self.fs[path]
            for e in entries:
                result[float(idx)] = e
                idx += 1

        return result

    # ─── Process management ───────────────────────────────────────────────

    def genpid(self):
        self.next_pid += 1
        return str(self.next_pid)

    def register_process(self, pid, proc):
        self.processes[pid] = proc

    def remove_process(self, pid):
        self.processes.pop(pid, None)

    def get_process(self, pid):
        return self.processes.get(pid)

    def get_process_by_name(self, name):
        for p in self.processes.values():
            if p.name == name:
                return p
        return None

    # ─── Shell builtins ───────────────────────────────────────────────────

    def exec_shell(self, command_str):
        """Execute a shell command string. Returns exit status."""
        import shlex
        command_str = self._env_expand(command_str)
        parts = command_str.strip().split(None, 1)
        if not parts:
            return 0
        cmd = parts[0]
        args_str = parts[1] if len(parts) > 1 else ""
        args = args_str.split() if args_str else []

        # Check aliases
        scope = self.scope
        aliases = scope.get("ALIAS", {}) if isinstance(scope.get("ALIAS"), dict) else {}
        if cmd in aliases:
            return self.exec_shell(aliases[cmd] + " " + args_str)

        if cmd == "" or cmd == "true" or cmd.startswith("#"):
            return 0
        elif cmd == "gc":
            import gc
            gc.collect()
            return 0
        elif cmd == "clear":
            # erase the console output item (J2ME: midlet.stdout.setText(""))
            out = self.globals.get("io", {}).get("stdout")
            if out is not None and hasattr(out, "text"):
                try:
                    out.text = ""
                except Exception:
                    pass
            import sys as _sys
            try:
                _sys.stdout.write("\x1b[2J\x1b[H")
                _sys.stdout.flush()
            except Exception:
                pass
            return 0
        elif cmd == "cat":
            for a in args:
                path = self.join_path(a)
                content = self.get_content(path)
                if content:
                    print(content, end="")
                else:
                    return 2
            return 0
        elif cmd == "ls":
            target = self.join_path(args[0]) if args else scope.get("PWD", "/home/")
            dirs = self.get_dirs(target)
            items = []
            for k in sorted(dirs.keys()):
                v = dirs[k]
                if not v.startswith("."):
                    items.append(str(v))
            print("\t".join(items))
            return 0
        elif cmd == "ps":
            print("PID\tPROCESS")
            for pid, proc in self.processes.items():
                print(f"{pid}\t{proc.name}")
            return 0
        elif cmd == "whoami":
            print(scope.get("USER", self.username))
            return 0
        elif cmd == "id":
            print(f"uid={self.uid}({scope.get('USER', self.username)})")
            return 0
        elif cmd == "pwd":
            print(scope.get("PWD", "/home/"))
            return 0
        elif cmd == "cd":
            target = args[0] if args else "/home/"
            status = self._chdir(target)
            return status
        elif cmd == "echo":
            print(args_str)
            return 0
        elif cmd == "env" or cmd == "export" or cmd == "set":
            if not args:
                for k, v in self.attributes.items():
                    print(f"{k}={v}")
            else:
                for a in args:
                    eq = a.find("=")
                    if eq > 0:
                        self.attributes[a[:eq]] = a[eq + 1:]
                    else:
                        v = self.attributes.get(a)
                        if v is not None:
                            print(f"{a}={v}")
                        else:
                            print(f"{cmd}: {a}: not found")
                            return 127
            return 0
        elif cmd == "exit":
            code = int(args[0]) if args else 0
            raise LuaExit(code)
        elif cmd == "time":
            before = int(time.time() * 1000)
            remaining = " ".join(args) if args else ""
            status = self.exec_shell(remaining) if remaining else 0
            elapsed = int(time.time() * 1000) - before
            print(f"at {elapsed}")
            return status
        elif cmd == "uptime":
            ms = int(time.time() * 1000) - self.uptime
            print(f"{ms} ms")
            return 0
        elif cmd == "alias":
            if not args:
                for k, v in aliases.items():
                    print(f"alias {k}='{v}'")
            else:
                for a in args:
                    eq = a.find("=")
                    if eq > 0:
                        key = a[:eq]
                        val = a[eq + 1:]
                        aliases[key] = val
                        scope["ALIAS"] = aliases
                    elif a in aliases:
                        print(f"alias {a}='{aliases[a]}'")
                    else:
                        print(f"alias: {a}: not found")
                        return 127
            return 0
        elif cmd == "unalias":
            if not args:
                print("unalias: usage: unalias [-a] name [name ...]")
                return 1
            elif args[0] == "-a":
                aliases.clear()
                scope["ALIAS"] = aliases
            else:
                for a in args:
                    if a in aliases:
                        del aliases[a]
                    else:
                        print(f"unalias: {a}: not found")
                        return 127
            return 0
        elif cmd == "unset":
            for a in args:
                self.attributes.pop(a, None)
            return 0
        elif cmd == "builtin" or cmd == "command":
            return self.exec_shell(args_str)
        elif cmd == "false":
            return 255
        elif cmd == ".":
            if args:
                path = self.join_path(args[0])
                content = self.get_content(path)
                if content:
                    if content.startswith("#!/bin/sh"):
                        return self._exec_shell_script(content)
                    pid = self.genpid()
                    proc = Process(pid, f"lua {path}", self.uid)
                    self.register_process(pid, proc)
                    result = self.run(path, content, self._make_arg_table(path, args_str))
                    self._gui_foreground_wait()
                    self.remove_process(pid)
                    return result
                else:
                    print(f". {args[0]}: not found")
                    return 127
            return 0
        else:
            # Try to run as /bin/<cmd>
            path = f"/bin/{cmd}"
            content = self.get_content(path)
            if content:
                pid = self.genpid()
                proc = Process(pid, f"lua {path}", self.uid, scope.get("USER", self.username))
                proc.scope = dict(scope)
                self.register_process(pid, proc)
                saved_pid = self.pid
                self.pid = pid
                arg_table = self._make_arg_table(path, args_str)
                result = self.run(path, content, arg_table)
                self.pid = saved_pid
                self._gui_foreground_wait()
                self.remove_process(pid)
                return result
            else:
                print(f"{cmd}: not found")
                return 127

    def _chdir(self, target):
        scope = self.scope
        pwd = scope.get("PWD", "/home/")
        if not target:
            scope["PWD"] = "/home/"
            return 0
        if target == "..":
            if pwd == "/":
                return 1
            last = pwd.rstrip("/").rfind("/")
            scope["PWD"] = pwd[:last + 1] if last > 0 else "/"
            return 0
        if target == "/":
            scope["PWD"] = "/"
            return 0
        # Resolve path
        if target.startswith("/"):
            full = target
        else:
            full = pwd.rstrip("/") + "/" + target
        if not full.endswith("/"):
            full += "/"
        # Check if dir exists in VFS or real FS
        if full in self.fs or full in self.vfs:
            scope["PWD"] = full
            return 0
        if full.startswith("/mnt/"):
            real = _os.path.join("/mnt", full[5:])
            if _os.path.isdir(real):
                scope["PWD"] = full
                return 0
        # Check without trailing slash
        full2 = full.rstrip("/")
        if full2 in self.vfs:
            scope["PWD"] = full
            return 0
        return 127

    def _make_arg_table(self, path, args_str):
        """Build the arg table for a script."""
        parts = args_str.split() if args_str else []
        arg_table = {}
        arg_table[0.0] = path
        for i, p in enumerate(parts):
            arg_table[float(i + 1)] = p
        return arg_table

    def _env_expand(self, s):
        """Expand $VAR and ${VAR} in a string."""
        if not s:
            return s
        result = []
        i = 0
        while i < len(s):
            if s[i] == '$' and i + 1 < len(s):
                i += 1
                if s[i] == '{':
                    i += 1
                    end = s.find('}', i)
                    if end == -1:
                        result.append('$')
                        continue
                    var = s[i:end]
                    val = self.attributes.get(var, self.scope.get(var, ""))
                    result.append(str(val) if val is not None else "")
                    i = end + 1
                elif is_letter(s[i]):
                    var = []
                    while i < len(s) and (is_letter_or_digit(s[i]) or s[i] == '_'):
                        var.append(s[i])
                        i += 1
                    var = ''.join(var)
                    val = self.attributes.get(var, self.scope.get(var, ""))
                    result.append(str(val) if val is not None else "")
                else:
                    result.append('$')
            else:
                result.append(s[i])
                i += 1
        return ''.join(result)

    def _get_command(self, s):
        """Extract command name from a command string."""
        s = self._env_expand(s).strip()
        parts = s.split(None, 1)
        return parts[0] if parts else ""

    def _get_argument(self, s):
        """Extract arguments from a command string."""
        s = self._env_expand(s).strip()
        parts = s.split(None, 1)
        return parts[1] if len(parts) > 1 else ""

    def _split_args(self, s):
        """Split a command string into args (shell-style)."""
        if not s:
            return []
        return s.split()

    # ─── Traceback ────────────────────────────────────────────────────────

    def _get_traceback(self, e):
        msg = str(e) if str(e) else repr(e)
        sb = msg

        line = -1
        line_text = None
        col = -1
        near = ""

        if self.thrown_tokens and self.thrown_token_index >= 0:
            t = min(self.thrown_token_index, len(self.thrown_tokens) - 1)
            tok = self.thrown_tokens[t]
            if tok.offset >= 0 and self.last_code and tok.offset < len(self.last_code):
                line = 1
                for k in range(tok.offset):
                    if self.last_code[k] == '\n':
                        line += 1
                ls = tok.offset
                while ls > 0 and self.last_code[ls - 1] != '\n':
                    ls -= 1
                le = tok.offset
                while le < len(self.last_code) and self.last_code[le] != '\n':
                    le += 1
                line_text = self.last_code[ls:le]
                col = tok.offset - ls
                near = self._token_lexeme(tok)

        if self.current_source:
            sb += f"\nLua {self.current_source}"
            if line > 0:
                sb += f":{line}"
        sb += self._pointer_block(line_text, col, near)

        frames = self.thrown_frames if self.thrown_frames else self.frame_stack
        if frames:
            sb += "\nstack traceback:"
            for i in range(len(frames) - 1, -1, -1):
                sb += f"\n\tin function '{frames[i]}'"

        return sb

    @staticmethod
    def _pointer_block(line_text, col, near):
        if not line_text or not line_text.strip():
            return ""
        sb = "\n\t" + line_text
        sb += "\n\t" + " " * min(col, len(line_text))
        sb += "^"
        sb += "-" * max(0, len(line_text) - col - 1)
        if near:
            sb += f" (near '{near}')"
        return sb

    @staticmethod
    def _token_lexeme(tok):
        if tok is None:
            return ""
        v = tok.value
        if isinstance(v, (int, float)):
            if isinstance(v, float) and v == int(v) and not _math_is_inf(v):
                return str(int(v))
            return str(v)
        return str(v) if v is not None else ""

    def _record_throw(self):
        if self.thrown_token_index != -1:
            return
        self.thrown_token_index = self.token_index
        self.thrown_tokens = self.tokens

    # ─── Call user function ───────────────────────────────────────────────

    def call_user_function(self, func, args):
        func_scope = ChainScope(func.closure_scope, self.globals)

        # Set params
        params = func.params
        has_vararg = params and params[-1] == "..."
        fixed = len(params) - 1 if has_vararg else len(params)
        for i in range(fixed):
            val = args[i] if i < len(args) else None
            func_scope[params[i]] = val if val is not None else NIL
        if has_vararg:
            vararg = {}
            for i in range(fixed, len(args)):
                vararg[float(i - fixed + 1)] = args[i] if args[i] is not None else NIL
            func_scope["..."] = vararg

        saved_tokens = self.tokens
        saved_index = self.token_index
        try:
            self.tokens = func.body_tokens
            self.token_index = 0
            self.frame_stack.append(func.name or "[anonymous]")
            result = None
            try:
                while self.peek().type != EOF:
                    result = self.statement(func_scope)
                    if self.doreturn:
                        self.doreturn = False
                        break
            except Exception:
                snapshot = list(self.frame_stack)
                if not self.thrown_frames:
                    self.thrown_frames = snapshot
                self._record_throw()
                raise
            finally:
                self.frame_stack.pop()
            return result
        finally:
            self.tokens = saved_tokens
            self.token_index = saved_index

    # ─── Native function implementations ──────────────────────────────────

    def internals(self, mod, args):
        """Dispatch native function calls. Matches Java's internals() switch."""

        # PRINT (0)
        if mod == 0:
            parts = []
            for a in args:
                if isinstance(a, list):
                    for j, v in enumerate(a):
                        parts.append(self.to_lua_string(v))
                        if j < len(a) - 1:
                            parts.append("\t")
                else:
                    parts.append(self.to_lua_string(a))
            out = "\t".join(parts) if not parts else "".join(parts) if len(parts) == 1 else "\t".join(parts)
            # Simple print: just print
            # Actually match the Java behavior: space-separated args, tab-separated list elements
            buf = []
            for i, a in enumerate(args):
                if isinstance(a, list):
                    for j, v in enumerate(a):
                        buf.append(self.to_lua_string(v))
                        if j < len(a) - 1:
                            buf.append("\t")
                else:
                    buf.append(self.to_lua_string(a))
                if i < len(args) - 1:
                    buf.append("\t")
            text = "".join(buf)
            print(text)
            try:
                self.onprint(text)
            except Exception:
                pass
            return None

        # ERROR (1)
        elif mod == 1:
            msg = self.to_lua_string(args[0] if args else None)
            raise LuaError(msg if msg != "nil" else "error")

        # PCALL (2)
        elif mod == 2:
            if not args:
                raise LuaError("bad argument #1 to 'pcall' (function expected)")
            fn = args[0]
            if isinstance(fn, LuaFunction):
                fn_args = args[1:]
                try:
                    value = fn.call(fn_args, self)
                    result = [True]
                    if isinstance(value, list):
                        result.extend(value)
                    else:
                        result.append(value)
                    return result
                except LuaError as e:
                    tb = self._get_traceback(e)
                    self.thrown_frames = []
                    self.thrown_tokens = None
                    self.thrown_token_index = -1
                    return [False, tb]
                except LuaExit:
                    raise
                except Exception as e:
                    tb = self._get_traceback(e)
                    self.thrown_frames = []
                    self.thrown_tokens = None
                    self.thrown_token_index = -1
                    return [False, tb]
            else:
                return [False, f"attempt to call a {self.lua_type(fn)} value"]

        # REQUIRE (3)
        elif mod == 3:
            if not args:
                raise LuaError("bad argument #1 to 'require' (string expected, got no value)")
            name = str(args[0])
            if name in self.require_cache:
                cached = self.require_cache[name]
                return None if cached is NIL else cached
            code = self.get_content(name)
            if not code:
                code = self.get_content(f"/lib/{name}.lua")
                if not code:
                    code = self.get_content(f"/lib/{name}.so")
                    if not code:
                        raise LuaError(f"module '{name}' not found")
            obj = self.exec_code(code)
            self.require_cache[name] = obj if obj is not None else NIL
            return obj

        # LOAD (4)
        elif mod == 4:
            if not args or args[0] is None:
                return None
            code = str(args[0])
            env = args[1] if len(args) > 1 and isinstance(args[1], dict) else None
            return self.exec_code(code, env)

        # PAIRS (5)
        elif mod == 5:
            if not args:
                raise LuaError("bad argument #1 to 'pairs' (table expected, got no value)")
            t = args[0]
            if t is None or isinstance(t, (dict, list)):
                return t
            raise LuaError(f"bad argument #1 to 'pairs' (table expected, got {self.lua_type(t)})")

        # GC (6)
        elif mod == 6:
            if not args:
                import gc
                gc.collect()
                return 0.0
            opt = str(args[0])
            if opt == "collect" or opt == "restart":
                import gc
                gc.collect()
            elif opt == "count":
                import gc
                return float(gc.get_count()[0])
            return 0.0

        # TOSTRING (7)
        elif mod == 7:
            return self.to_lua_string(args[0] if args else "nil")

        # TONUMBER (8)
        elif mod == 8:
            if not args:
                raise LuaError("bad argument #1 to 'tonumber' (value expected)")
            try:
                return float(str(args[0]))
            except (ValueError, TypeError):
                raise LuaError(f"bad argument #1 to 'tonumber' (number expected, got {self.lua_type(args[0])})")

        # SELECT (9)
        elif mod == 9:
            if not args or args[0] is None:
                raise LuaError("bad argument #1 to 'select' (number expected, got no value)")
            idx_str = str(args[0])
            if idx_str == "#":
                if len(args) > 1 and isinstance(args[1], dict):
                    return float(len(args[1]))
                return float(len(args) - 1)
            index = int(float(idx_str))
            rest = args[1:]
            if index < 0:
                index = len(rest) + index + 1
            if index < 1 or index > len(rest):
                return {}
            result = {}
            ri = 1
            for i in range(index - 1, len(rest)):
                result[float(ri)] = rest[i] if rest[i] is not None else NIL
                ri += 1
            return result

        # TYPE (10)
        elif mod == 10:
            if not args:
                raise LuaError("bad argument #1 to 'type' (value expected)")
            return self.lua_type(args[0])

        # GETPROPERTY (11)
        elif mod == 11:
            if not args:
                return None
            query = str(args[0])
            if query.startswith("/"):
                return _os.environ.get(query[1:], "")
            return self.attributes.get(query, "")

        # SETMETATABLE (12)
        elif mod == 12:
            if len(args) < 2:
                raise LuaError("bad argument #1 to 'setmetatable' (table expected, got no value)")
            table, mt = args[0], args[1]
            if not isinstance(table, dict):
                raise LuaError(f"bad argument #1 to 'setmetatable' (table expected, got {self.lua_type(table)})")
            if mt is not None and not isinstance(mt, dict):
                raise LuaError(f"bad argument #2 to 'setmetatable' (nil or table expected, got {self.lua_type(mt)})")
            table["__metatable"] = mt if mt is not None else NIL
            return table

        # GETMETATABLE (13)
        elif mod == 13:
            if not args:
                raise LuaError("bad argument #1 to 'getmetatable' (table expected, got no value)")
            table = args[0]
            if not isinstance(table, dict):
                raise LuaError(f"bad argument #1 to 'getmetatable' (table expected, got {self.lua_type(table)})")
            mt = table.get("__metatable")
            return None if mt is None or mt is NIL else mt

        # IPAIRS (14)
        elif mod == 14:
            if not args:
                raise LuaError("bad argument #1 to 'ipairs' (table expected, got no value)")
            t = args[0]
            if t is None or isinstance(t, (dict, list)):
                return {"__table": t, "__index": 0.0}
            raise LuaError(f"bad argument #1 to 'ipairs' (table expected, got {self.lua_type(t)})")

        # MATH.RANDOM (15)
        elif mod == 15:
            import random
            if not args:
                return random.random()
            n = int(float(str(args[0])))
            if n <= 0:
                n = 100
            return float(random.randint(1, n))

        # STRING functions (100-120)
        elif 100 <= mod <= 120:
            return self._string_internals(mod, args)

        # TABLE functions (200-207)
        elif 200 <= mod <= 207:
            return self._table_internals(mod, args)

        # OS functions (300-322)
        elif 300 <= mod <= 322:
            return self._os_internals(mod, args)

        # IO functions (400-409)
        elif 400 <= mod <= 409:
            return self._io_internals(mod, args)

        # SOCKET / HTTP functions (500-508)
        elif 500 <= mod <= 508:
            return self._socket_internals(mod, args)

        # GRAPHICS stubs (600-614)
        elif 600 <= mod <= 614:
            return self._graphics_internals(mod, args)

        # JAVA stubs (700-706)
        elif 700 <= mod <= 706:
            return self._java_internals(mod, args)

        # BASE64 (116-117)
        elif mod == 116:
            if not args:
                raise LuaError("bad argument #1 to 'encode' (string or table expected)")
            data = args[0]
            if isinstance(data, str):
                return _base64.b64encode(data.encode("utf-8")).decode("ascii")
            elif isinstance(data, dict):
                lst = self._table_to_list(data)
                b = bytes([int(x) if isinstance(x, (int, float)) else 0 for x in lst])
                return _base64.b64encode(b).decode("ascii")
            return _base64.b64encode(str(data).encode("utf-8")).decode("ascii")

        elif mod == 117:
            if not args:
                raise LuaError("bad argument #1 to 'decode' (string expected)")
            s = str(args[0])
            raw = _base64.b64decode(s)
            if len(args) > 1 and args[1]:
                return raw
            result = {}
            for i, b in enumerate(raw):
                result[float(i + 1)] = float(b)
            return result

        # AUDIO stubs (800-805)
        elif 800 <= mod <= 805:
            return None

        # PUSH stubs (900-904)
        elif 900 <= mod <= 904:
            return None

        return None

    def _string_internals(self, mod, args):
        if mod == 100:  # upper
            return str(args[0]).upper() if args else ""
        elif mod == 101:  # lower
            return str(args[0]).lower() if args else ""
        elif mod == 102:  # len
            return float(len(str(args[0]))) if args else 0.0
        elif mod == 103:  # find (literal, not regex)
            if len(args) < 2:
                return None
            s, pattern = str(args[0]), str(args[1])
            init = int(float(str(args[2]))) if len(args) > 2 else 1
            if init < 1:
                init = 1
            idx = s.find(pattern, init - 1)
            if idx == -1:
                return None
            return [float(idx + 1), float(idx + len(pattern))]
        elif mod == 104:  # match (literal substring)
            if len(args) < 2:
                return None
            s, pattern = str(args[0]), str(args[1])
            init = int(float(str(args[2]))) if len(args) > 2 else 0
            idx = s.find(pattern, init)
            if idx == -1:
                return None
            return s[idx:idx + len(pattern)]
        elif mod == 105:  # reverse
            return str(args[0])[::-1] if args else ""
        elif mod == 106:  # sub
            if len(args) < 2:
                return ""
            s = str(args[0])
            i = int(float(str(args[1])))
            j = int(float(str(args[2]))) if len(args) > 2 else len(s)
            if i < 0:
                i = max(len(s) + i + 1, 1)
            if j < 0:
                j = max(len(s) + j + 1, 1)
            if i < 1:
                i = 1
            if j > len(s):
                j = len(s)
            if i > j:
                return ""
            return s[i - 1:j]
        elif mod == 107:  # hash
            return float(hash(str(args[0]))) if args else 0.0
        elif mod == 108:  # byte
            if not args:
                return 0.0
            s = str(args[0])
            i = int(float(str(args[1]))) if len(args) > 1 else 1
            j = int(float(str(args[2]))) if len(args) > 2 else i
            if i < 1:
                i = 1
            if j > len(s):
                j = len(s)
            if i == j:
                if 1 <= i <= len(s):
                    return float(ord(s[i - 1]))
                return 0.0
            result = {}
            ri = 1
            for k in range(i - 1, j):
                if 0 <= k < len(s):
                    result[float(ri)] = float(ord(s[k]))
                    ri += 1
            return result
        elif mod == 109:  # char
            if not args:
                return ""
            if isinstance(args[0], dict):
                lst = self._table_to_list(args[0])
                return ''.join(chr(int(x)) for x in lst if isinstance(x, (int, float)))
            return ''.join(chr(int(float(str(a)))) for a in args)
        elif mod == 110:  # trim
            return str(args[0]).strip() if args else ""
        elif mod == 111:  # split
            if not args:
                return {}
            s = str(args[0])
            sep = str(args[1]) if len(args) > 1 and args[1] is not None else None
            if sep is None:
                # Shell-style split
                parts = s.split()
            elif sep == "":
                # Individual chars
                parts = list(s)
            else:
                parts = s.split(sep)
            result = {}
            for i, p in enumerate(parts):
                result[float(i + 1)] = p
            return result
        elif mod == 112:  # uuid
            return str(_uuid.uuid4())
        elif mod == 113:  # getCommand
            s = str(args[0]) if args else ""
            return self._get_command(s)
        elif mod == 114:  # getArgument
            s = str(args[0]) if args else ""
            return self._get_argument(s)
        elif mod == 115:  # env
            s = str(args[0]) if args else ""
            return self._env_expand(s)
        elif mod == 118:  # getpattern
            return str(args[0]) if args else ""
        elif mod == 119:  # startswith
            if len(args) < 2:
                return False
            return str(args[0]).startswith(str(args[1]))
        elif mod == 120:  # endswith
            if len(args) < 2:
                return False
            return str(args[0]).endswith(str(args[1]))
        return None

    def _table_internals(self, mod, args):
        if mod == 200:  # insert
            if len(args) < 2:
                raise LuaError("bad argument #1 to 'insert' (wrong number of arguments)")
            t = args[0]
            if not isinstance(t, dict):
                raise LuaError(f"bad argument #1 to 'insert' (table expected, got {self.lua_type(t)})")
            if len(args) >= 3:
                pos = int(float(str(args[1])))
                val = args[2]
            else:
                pos = len(t) + 1
                val = args[1]
            # Shift right
            for i in range(len(t), pos - 1, -1):
                v = t.get(float(i))
                if v is not None:
                    t[float(i + 1)] = v
                elif float(i) in t:
                    del t[float(i)]
            t[float(pos)] = val if val is not None else NIL
            return None
        elif mod == 201:  # concat
            if not args:
                raise LuaError("bad argument #1 to 'concat' (table expected)")
            t = args[0]
            if not isinstance(t, dict):
                raise LuaError(f"bad argument #1 to 'concat' (table expected)")
            sep = str(args[1]) if len(args) > 1 and args[1] is not None else ""
            i = int(float(str(args[2]))) if len(args) > 2 else 1
            j = int(float(str(args[3]))) if len(args) > 3 else len(t)
            lst = self._table_to_list(t)
            if i < 1 or j > len(lst) or i > j:
                return ""
            return sep.join(self.to_lua_string(lst[k]) for k in range(i - 1, j))
        elif mod == 202:  # remove
            if not args:
                raise LuaError("bad argument #1 to 'remove' (table expected)")
            t = args[0]
            if not isinstance(t, dict):
                raise LuaError(f"bad argument #1 to 'remove' (table expected)")
            pos = int(float(str(args[1]))) if len(args) > 1 else len(t)
            removed = t.get(float(pos))
            if removed is not None:
                del t[float(pos)]
                for i in range(pos, len(t) + 1):
                    nxt = t.get(float(i + 1))
                    if nxt is not None:
                        t[float(i)] = nxt
                    elif float(i + 1) in t:
                        del t[float(i)]
                last = float(len(t) + 1)
                if last in t:
                    del t[last]
            return removed if removed is not None else NIL
        elif mod == 203:  # sort (no comparator)
            if not args:
                raise LuaError("bad argument #1 to 'sort' (table expected)")
            t = args[0]
            if not isinstance(t, dict):
                raise LuaError(f"bad argument #1 to 'sort' (table expected)")
            lst = self._table_to_list(t)
            lst.sort(key=lambda x: (x is None, x if isinstance(x, (int, float)) else str(x)))
            t.clear()
            for i, v in enumerate(lst):
                t[float(i + 1)] = v
            return None
        elif mod == 205:  # unpack
            if not args:
                raise LuaError("bad argument #1 to 'unpack' (table expected)")
            t = args[0]
            if not isinstance(t, dict):
                raise LuaError(f"bad argument #1 to 'unpack' (table expected)")
            lst = self._table_to_list(t)
            i = int(float(str(args[1]))) if len(args) > 1 else 1
            j = int(float(str(args[2]))) if len(args) > 2 else len(lst)
            return lst[i - 1:j] if 1 <= i <= j <= len(lst) else []
        elif mod == 206:  # pack
            result = {}
            for i, a in enumerate(args):
                result[float(i + 1)] = a if a is not None else NIL
            result["n"] = float(len(args))
            return result
        elif mod == 207:  # decode
            if not args:
                raise LuaError("bad argument #1 to 'decode' (string expected)")
            text = str(args[0])
            if not text:
                return {}
            result = {}
            for line in text.split("\n"):
                if line.startswith("#") or not line.strip():
                    continue
                eq = line.find("=")
                if eq > 0 and eq < len(line) - 1:
                    key = line[:eq].strip()
                    val = line[eq + 1:].strip()
                    result[key] = val
            return result
        return None

    def _os_internals(self, mod, args):
        if mod == 300:  # execute
            if not args:
                raise LuaError("bad argument #1 to 'execute' (string expected)")
            command = self._env_expand(str(args[0]))
            return float(self.exec_shell(command))
        elif mod == 301:  # getenv
            if not args:
                return dict(self.attributes)
            return self.attributes.get(str(args[0]))
        elif mod == 302:  # setenv
            if args:
                val = str(args[1]) if len(args) > 1 else None
                key = str(args[0])
                if val is None:
                    self.attributes.pop(key, None)
                else:
                    self.attributes[key] = val
            return None
        elif mod == 303:  # clock
            return float(int(time.time() * 1000) - self.uptime)
        elif mod == 304:  # setlocale
            if args:
                self.attributes["LOCALE"] = str(args[0])
            return None
        elif mod == 305:  # exit
            status = 0
            if args:
                try:
                    status = int(float(str(args[0])))
                except (ValueError, TypeError):
                    status = 1
            self.status = status
            raise LuaExit(status)
        elif mod == 306:  # date
            return time.strftime("%a %b %d %H:%M:%S %Y")
        elif mod == 307:  # getpid
            if not args or args[0] is None:
                return self.pid
            name = str(args[0])
            proc = self.get_process_by_name(name)
            return proc.pid if proc else None
        elif mod == 308:  # setproc
            if not args:
                return None
            if isinstance(args[0], bool):
                return None
            attr = str(args[0]).lower().strip()
            val = args[1] if len(args) > 1 else None
            proc = self.processes.get(self.pid)
            if proc:
                if attr == "name":
                    proc.name = str(val) if val else proc.name
                elif attr == "scope":
                    if isinstance(val, dict):
                        proc.scope = val
                        self._scope = val
                elif attr == "handler":
                    proc.handler = val
                elif attr == "cmd":
                    proc.cmd = str(val) if val else ""
                elif attr == "sighandler":
                    proc.sighandler = val
                else:
                    if val is None:
                        proc.db.pop(attr, None)
                    else:
                        proc.db[attr] = val
            return None
        elif mod == 309:  # getproc
            if not args:
                result = {}
                for pid, p in self.processes.items():
                    result[pid] = p.name
                return result
            pid = str(args[0]).strip()
            proc = self.get_process(pid)
            if proc:
                if len(args) > 1:
                    return proc.db.get(str(args[1]).strip())
                return proc.name
            return None
        elif mod == 310:  # getcwd
            return self.scope.get("PWD", "/home/")
        elif mod == 311:  # getuid
            if not args or args[0] is None:
                return float(self.uid)
            user = str(args[0])
            if user == self.username:
                return 1000.0
            return 1000.0  # Default for unknown users
        elif mod == 312:  # chdir
            return float(self._chdir(str(args[0]) if args else ""))
        elif mod == 313:  # request
            if not args:
                raise LuaError("bad argument #1 to 'request' (string expected)")
            if len(args) < 2:
                raise LuaError("bad argument #2 to 'request' (value expected)")
            pid = str(args[0])
            proc = self.get_process(pid)
            if proc and proc.handler:
                payload = str(args[1]) if len(args) > 1 else ""
                arg = args[2] if len(args) > 2 else None
                try:
                    return proc.handler.call([payload, arg, self.scope, self.pid, float(self.uid)], self)
                except Exception as e:
                    return self._get_traceback(e)
            elif not proc:
                return "process not found"
            return "not a service"
        elif mod == 318:  # su
            if not args:
                raise LuaError("bad argument #1 to 'su' (username expected)")
            user = str(args[0])
            passwd = str(args[1]) if len(args) > 1 else None
            if user == self.username:
                self.uid = 1000
                self.scope["USER"] = user
                proc = self.processes.get(self.pid)
                if proc:
                    proc.uid = 1000
                return 0.0
            elif user == "root":
                self.uid = 0
                self.scope["USER"] = "root"
                proc = self.processes.get(self.pid)
                if proc:
                    proc.uid = 0
                return 0.0
            elif passwd is None:
                return 13.0
            else:
                return 13.0
        elif mod == 319:  # remove
            if not args:
                raise LuaError("bad argument #1 to 'remove' (string expected)")
            return float(self.delete_file(str(args[0]), self.uid))
        elif mod == 320:  # scope
            if not args:
                return self.scope
            elif isinstance(args[0], dict):
                self._scope = args[0]
                self.scope = args[0]
                return None
            return None
        elif mod == 321:  # join
            if not args:
                raise LuaError("bad argument #1 to 'join' (string expected)")
            return self.join_path(str(args[0]))
        elif mod == 322:  # mkdir
            if not args:
                return 0.0
            path = str(args[0])
            if path.startswith("/mnt/"):
                real = _os.path.join("/mnt", path[5:])
                try:
                    if _os.path.exists(real):
                        return 128.0
                    _os.makedirs(real, exist_ok=True)
                    return 0.0
                except PermissionError:
                    return 13.0
                except Exception:
                    return 1.0
            return 5.0
        return None

    def _io_internals(self, mod, args):
        if mod == 400:  # read
            if not args:
                return ""
            arg = args[0]
            if isinstance(arg, str):
                return self.get_content(arg)
            elif isinstance(arg, bytes):
                length = int(float(str(args[1]))) if len(args) > 1 else 1024
                return arg[:length].decode("utf-8", errors="replace")
            return ""
        elif mod == 401:  # write
            if not args:
                return None
            buf = args[0]
            target = args[1] if len(args) > 1 else None
            how = args[2] if len(args) > 2 else None
            mode = str(how) == "a" if how else False

            if isinstance(buf, str) and target is None:
                # print to stdout
                print(buf, end="")
                return 0.0
            if isinstance(buf, str) and isinstance(target, str):
                path = target
                if mode:
                    existing = self.get_content(path)
                    self.write_file(path, existing + buf, self.uid)
                else:
                    self.write_file(path, buf, self.uid)
                return 0.0
            if isinstance(buf, bytes):
                path = str(target) if target else "/dev/stdout"
                content = buf.decode("utf-8", errors="replace")
                if mode:
                    existing = self.get_content(path)
                    self.write_file(path, existing + content, self.uid)
                else:
                    self.write_file(path, content, self.uid)
                return 0.0
            return 0.0
        elif mod == 402:  # close
            return None
        elif mod == 403:  # open
            if not args:
                return b""
            path = str(args[0])
            data = self.get_input_stream(path)
            return data if data else None
        elif mod == 404:  # popen
            return self._popen(args)
        elif mod == 405:  # dirs
            return self.get_dirs(str(args[0]) if args else None)
        elif mod == 406:  # setstdout
            return None
        elif mod == 407:  # mount
            if not args:
                return None
            struct = str(args[0])
            if not struct:
                self.fs.clear()
                return None
            for line in struct.split("\n"):
                line = line.strip()
                eq = line.find("=")
                if line.startswith("#") or not line or eq == -1:
                    continue
                base = line[:eq].strip()
                files = line[eq + 1:].strip().split(",")
                content = [".."]
                for f in files:
                    f = f.strip()
                    if f and f not in content:
                        if f.endswith("/"):
                            self.fs[base + f] = [".."]
                        content.append(f)
                self.fs[base] = content
            return None
        elif mod == 408:  # new (StringBuffer)
            return ""  # On PC, just use strings
        elif mod == 409:  # copy
            if len(args) < 2:
                raise LuaError("bad argument #1 to 'copy' (wrong number of arguments)")
            source, target = args[0], args[1]
            if isinstance(source, str):
                content = self.get_content(source)
                if not content:
                    return 127.0
                if isinstance(target, str):
                    self.write_file(target, content, self.uid)
                elif isinstance(target, dict):
                    # StringBuffer-like: append
                    target["__value"] = target.get("__value", "") + content
            elif isinstance(source, bytes):
                content = source.decode("utf-8", errors="replace")
                if isinstance(target, str):
                    self.write_file(target, content, self.uid)
            elif isinstance(source, str) and isinstance(target, str):
                self.write_file(target, self.get_content(source), self.uid)
            return 0.0
        return None

    def _exec_shell_script(self, content):
        status = 0
        for line in content.split("\n"):
            if not line.strip():
                continue
            status = self.exec_shell(line)
            if status != 0:
                break
        return status

    def _popen(self, args):
        if not args:
            return [0]
        program = str(args[0])
        arguments = str(args[1]) if len(args) > 1 else ""
        owner = self.uid
        if len(args) > 2:
            if isinstance(args[2], bool):
                owner = self.uid if args[2] else 1000
        output = args[3] if len(args) > 3 else ""
        scope = args[4] if len(args) > 4 and isinstance(args[4], dict) else self.scope
        is_stream = args[5] if len(args) > 5 else None

        path = self.join_path(program)
        content = self.get_content(path, scope) if not is_stream else None
        if is_stream and isinstance(is_stream, bytes):
            content = is_stream.decode("utf-8", errors="replace")

        if content is None:
            return [127]

        if content.startswith("#!/bin/sh"):
            return [self._exec_shell_script(content), output if isinstance(output, str) else ""]

        arg_table = {}
        arg_table[0.0] = program
        if arguments:
            for i, a in enumerate(arguments.split()):
                arg_table[float(i + 1)] = a

        pid = self.genpid()
        proc = Process(pid, f"lua {path}", owner)
        proc.scope = dict(scope) if isinstance(scope, dict) else {}
        self.register_process(pid, proc)

        saved_pid = self.pid
        self.pid = pid
        result_status = self.run(path, content, arg_table)
        self.pid = saved_pid
        self.remove_process(pid)

        return [result_status, output if isinstance(output, str) else ""]

    def _socket_internals(self, mod, args):
        # Stubs — networking not available on PC runtime
        if mod == 502:  # connect
            raise LuaError("socket.connect: not available in PC runtime")
        elif mod == 500:  # http.get
            return ["", 0.0]
        elif mod == 501:  # http.post
            return ["", 0.0]
        return None

    def _graphics_internals(self, mod, args):
        # Stubs — no graphics on PC (the kernel runtime overrides this)
        if mod == 601:  # new
            return {}  # Return empty screen object
        elif mod == 606:  # GetCurrent
            return {}
        elif mod == 611:  # SetText
            return None
        elif mod == 613:  # GetText
            return ""
        return None

    def onprint(self, text):
        """Hook called after print(); subclasses stream to a console item."""
        return None

    def _gui_foreground_wait(self):
        """Foreground GUI processes stay alive while their windows are open
        (J2ME kill=false after graphics.display). The kernel runtime overrides
        this to block and pump tk events until the windows close."""
        return False

    def _java_internals(self, mod, args):
        if mod == 704:  # run (threading)
            if args and isinstance(args[0], LuaFunction):
                fn = args[0]
                name = str(args[1]) if len(args) > 1 else "thread"

                def _bg():
                    try:
                        fn.call([], self)
                    except Exception as e:
                        print(self._get_traceback(e), file=sys.stderr)

                t = threading.Thread(target=_bg, name=name, daemon=True)
                t.start()
            return None
        elif mod == 705:  # thread
            return threading.current_thread().name
        elif mod == 706:  # sleep
            if args:
                time.sleep(float(str(args[0])) / 1000.0)
            return None
        elif mod == 700:  # class
            return False  # No Java classes on PC
        elif mod == 701:  # getName
            return "OpenTTY PC"
        elif mod == 703:  # uptime
            return float(int(time.time() * 1000) - self.uptime)
        return None

    # ─── Helpers ──────────────────────────────────────────────────────────

    @staticmethod
    def _table_to_list(t):
        if not isinstance(t, dict):
            return []
        if not t:
            return []
        size = len(t)
        result = []
        for i in range(1, size + 1):
            result.append(t.get(float(i)))
        return result


# ─── Exceptions ───────────────────────────────────────────────────────────────

class LuaError(Exception):
    pass


class LuaExit(Exception):
    def __init__(self, status=0):
        self.status = status
        super().__init__(f"exit({status})")


# ─── Sentinel ─────────────────────────────────────────────────────────────────

class _MissingSentinel:
    pass

_MISSING = _MissingSentinel()
LUAL_NIL_SENTINEL = _MissingSentinel()  # distinct from None


class ChainScope(dict):
    """dict whose get() falls back to a parent chain then globals.

    Mirrors Lua.java's ScopeTable: writes stay local, reads chain up.
    """

    def __init__(self, parent=None, globals_scope=None):
        super().__init__()
        self.parent = parent
        self.globals_scope = globals_scope

    def get(self, key, default=None):
        val = dict.get(self, key, _MISSING)
        if val is not _MISSING:
            return val
        if self.parent is not None:
            val = self.parent.get(key, _MISSING)
            if val is not _MISSING:
                return val
        if self.globals_scope is not None:
            val = self.globals_scope.get(key, _MISSING)
            if val is not _MISSING:
                return val
        return default


def _math_is_inf(v):
    return v == float('inf') or v == float('-inf')
