"""graphics.* LCDUI backend, rendered with Python tkinter windows.

Port of the J2ME graphics package (src/Lua.java, mods 600-614) so that
graphics.new / display / append / addCommand / handler / SetTitle / clear ...
open and drive real windows via tkinter instead of MIDP displayables.

Machines without a display (or without tkinter) run headless: the object
model, graphics.db and the handler dispatch all still work exactly the same;
screens just never get an actual window. Set OPEN_TTY_NOGUI=1 to force
headless mode even on a machine with a display.

Threading: every tkinter call must happen on the thread that created the
root. The Lua interpreter lives on that (main) thread, so graphics.* calls
run directly; calls arriving from java.run daemon threads are marshalled onto
the main thread through a queue drained by pump().
"""

import os
import threading

try:
    import tkinter as tk
    _TK = tk
except Exception:  # pragma: no cover - no tkinter installed
    _TK = None


class LuaError(Exception):
    pass


def _headless_env():
    return os.environ.get("OPEN_TTY_NOGUI", "").strip().lower() not in ("", "0", "off", "false", "no")


def headless_forced():
    """True when OPEN_TTY_NOGUI was set explicitly (tests / servers)."""
    return _headless_env()


_MISSING = object()  # sentinel for "no live widget state recorded"


# ── Objects the Lua scripts manipulate ────────────────────────────────────────


class Command:
    """An LCDUI Command (graphics.new("command", ...)); used as a table key."""

    __slots__ = ("label", "ctype", "priority")

    def __init__(self, label="Command", ctype="screen", priority=1):
        self.label = label
        self.ctype = ctype
        self.priority = priority

    def __repr__(self):
        return 'graphics command "%s"' % self.label


class Image:
    """graphics.render(path) result; resolved to a tk.PhotoImage when shown."""

    __slots__ = ("path",)

    def __init__(self, path):
        self.path = path

    def __repr__(self):
        return 'image "%s"' % self.path


class Item:
    """The stream item behind io.stdout / io.stdin (StringItem / TextField)."""

    __slots__ = ("kind", "label", "text")

    def __init__(self, kind="display", label=""):
        self.kind = kind  # "display" (StringItem) | "input" (TextField)
        self.label = label
        self.text = ""

    def append_text(self, chunk):
        chunk = chunk or ""
        if self.kind == "input":
            self.text = (self.text or "") + chunk
        else:
            self.text = (self.text or "") + (("\n" if self.text else "") + chunk)

    def __repr__(self):
        return 'console %s item' % self.kind


class Screen:
    """A Form / List / TextBox / Alert as returned by graphics.new().

    kind:    "form" | "list" | "edit" | "alert"
    entries: form rows (tuples below) for forms, option strings for lists.
        ("text", label, value)              StringItem
        ("item", <Item>)                    io.stdout / io.stdin
        ("image", <Image|path>)             image item
        ("field", label, value, mode)       TextField
        ("spacer", width, height)           Spacer
        ("gauge", label, value, max)        Gauge
        ("choice", label, mode, options)    ChoiceGroup
    """

    def __init__(self, kind, title, backend):
        self.kind = kind
        self.title = title or ""
        self.ticker = ""
        self.entries = []
        self.commands = []
        self.handler = {}
        self.backend = backend
        self.text = ""  # "edit" (TextBox) buffer
        self.window = None  # tk.Toplevel once rendered
        self._widgets = []
        self._values = {}  # id(row) -> live field value
        self.dirty = False
        self.alive = False

    def append(self, item):
        self.entries.append(item)
        self.mark_dirty()

    def clear(self):
        self.entries = []
        self._values = {}
        self.mark_dirty()

    def mark_dirty(self):
        self.dirty = True

    def show(self):
        win = self.backend.ensure_window(self)
        self.mark_dirty()
        if win is not None:
            try:
                win.deiconify()
                win.lift()
            except Exception:
                pass

    def close(self):
        if self.window is not None:
            try:
                self.window.destroy()
            except Exception:
                pass
        self.window = None
        self.alive = False
        self.dirty = False


# ── Backend ──────────────────────────────────────────────────────────────────


class _Backend:
    def __init__(self):
        self.root = None
        self.current = None  # last displayed screen
        self.dispatch = None  # kernel cb: (screen, command, args, handler_fn)
        self.image_resolver = None  # kernel cb: (dev path) -> host path
        self._screens = []
        self._lock = threading.Lock()
        self._ops = []  # cross-thread work queue
        self._probed = False

    # -- environment --------------------------------------------------------

    def rendering_enabled(self):
        return _TK is not None and not _headless_env()

    def ensure_display(self):
        """Return the hidden tk root, or None when running headless."""
        if not self.rendering_enabled():
            return None
        if self.root is None and not self._probed:
            self._probed = True
            try:
                self.root = _TK.Tk()
                self.root.withdraw()
            except Exception:
                self.root = None
        if self.root is None:
            raise LuaError("tkinter display unavailable (headless? use a real X/Wayland session)")
        return self.root

    # -- cross-thread marshalling ------------------------------------------

    @staticmethod
    def _on_main_thread():
        return threading.current_thread() is threading.main_thread()

    def call_main(self, fn, *args):
        if self._on_main_thread():
            return fn(*args)
        box, done = [], threading.Event()

        def wrap():
            try:
                box.append(("ok", fn(*args)))
            except Exception as e:
                box.append(("err", e))
            done.set()

        with self._lock:
            self._ops.append(wrap)
        done.wait(timeout=30)
        if box:
            kind, payload = box[0]
            if kind == "err":
                raise payload
            return payload
        return None

    def _drain(self):
        if not self._on_main_thread():
            return
        with self._lock:
            ops, self._ops = self._ops, []
        for op in ops:
            try:
                op()
            except Exception:
                pass

    def pump(self):
        """Process queued ops and tk events, then rebuild dirty windows.

        Only ever touches tk from the main thread; calls from background
        threads defer the work to the next main-thread pump."""
        if not self._on_main_thread():
            with self._lock:
                self._ops.append(self._pump_main)
            return
        self._pump_main()

    def _pump_main(self):
        self._drain()
        if self.root is not None:
            try:
                self.root.update()
            except Exception:
                pass
        for scr in self._visible():
            if scr.dirty:
                try:
                    self.build_window(scr)
                    scr.dirty = False
                except Exception:
                    scr.dirty = False

    # -- screen registry ----------------------------------------------------

    def new_screen(self, kind, title):
        scr = Screen(kind, title, self)
        self._screens.append(scr)
        return scr

    def _visible(self):
        for scr in self._screens:
            if scr.window is not None and scr.alive:
                yield scr

    def any_window(self):
        return any(True for _ in self._visible())

    def close_all(self):
        for scr in list(self._screens):
            scr.close()
        if self.root is not None:
            try:
                self.root.update()
            except Exception:
                pass

    # -- windows --------------------------------------------------------------

    def ensure_window(self, scr):
        root = self.ensure_display()
        if root is None:
            return None
        if scr.window is None or not scr.alive:
            scr.window = _TK.Toplevel(root)
            scr.window.title(scr.title or "OpenTTY")
            scr.window.protocol("WM_DELETE_WINDOW", lambda s=scr: self._on_close(s))
            scr.alive = True
        self.current = scr
        return scr.window

    def _on_close(self, scr):
        scr.alive = False
        try:
            if scr.window is not None:
                scr.window.destroy()
        except Exception:
            pass
        scr.window = None

    def build_window(self, scr):
        win = scr.window
        if win is None:
            return
        for w in scr._widgets:
            try:
                w.destroy()
            except Exception:
                pass
        scr._widgets = []
        scr._widgets.append(_TK.Frame(win))

        if scr.ticker:
            t = _TK.Label(win, text=scr.ticker, relief="sunken", anchor="w")
            t.pack(fill="x", side="top")
            scr._widgets.append(t)

        body = _TK.Frame(win)
        body.pack(fill="both", expand=True, padx=4, pady=4)
        scr._widgets.append(body)

        try:
            if scr.kind in ("form", "edit"):
                self._build_form(scr, body)
            elif scr.kind == "list":
                self._build_list(scr, body)
            elif scr.kind == "alert":
                self._build_alert(scr, body)
        except Exception as e:
            err = _TK.Label(win, text="(graphics error: %s)" % e, fg="red")
            err.pack(fill="x")
            scr._widgets.append(err)
            return

        bar = _TK.Frame(win)
        bar.pack(fill="x", side="bottom", pady=(0, 4))
        scr._widgets.append(bar)
        for cmd in scr.commands:
            b = _TK.Button(bar, text=cmd.label or "Command",
                           command=lambda c=cmd, s=scr: self._fire(s, c))
            b.pack(side="left", padx=2, expand=True, fill="x")
            scr._widgets.append(b)

    def _add(self, parent, kind, **kw):
        w = _TK.Label(parent, **kw) if kind == "label" else None
        if w is None:
            raise ValueError(kind)
        return w

    def _build_form(self, scr, body):
        for row in scr.entries:
            if isinstance(row, Item):
                if row.kind == "display":
                    lab = _TK.Label(body, text=row.text or " ", justify="left", anchor="w")
                    lab.pack(fill="x")
                    scr._widgets.append(lab)
                else:
                    frm = _TK.Frame(body)
                    frm.pack(fill="x")
                    if row.label:
                        _TK.Label(frm, text=row.label, anchor="w").pack(side="left")
                    var = _TK.StringVar()
                    var.set(row.text or "")
                    ent = _TK.Entry(frm, textvariable=var)
                    ent.pack(side="left", fill="x", expand=True)
                    ent.bind("<KeyRelease>", lambda e, i=row, v=var: self._sync_item(i, v))
                    scr._widgets.append(frm)
                continue
            kind = row[0]
            if kind == "text":
                label, value = row[1], row[2]
                payload = (label if label else "") + ((": " + value) if value else "")
                lab = _TK.Label(body, text=payload or " ", justify="left", anchor="w")
                lab.pack(fill="x")
                scr._widgets.append(lab)
            elif kind == "itemrow":
                root, label = row[1], row[2]
                btn = _TK.Button(body, text=label or "Item",
                                 command=lambda f=root: self.invoke(f, []))
                btn.pack(fill="x", pady=1)
                scr._widgets.append(btn)
            elif kind == "item":
                obj = row[1]
                if obj.kind == "display":
                    lab = _TK.Label(body, text=obj.text or " ", justify="left", anchor="w")
                    lab.pack(fill="x")
                    scr._widgets.append(lab)
                else:
                    frm = _TK.Frame(body)
                    frm.pack(fill="x")
                    if obj.label:
                        _TK.Label(frm, text=obj.label, anchor="w").pack(side="left")
                    var = _TK.StringVar()
                    var.set(obj.text or "")
                    ent = _TK.Entry(frm, textvariable=var)
                    ent.pack(side="left", fill="x", expand=True)
                    ent.bind("<KeyRelease>", lambda e, i=obj, v=var: self._sync_item(i, v))
                    scr._widgets.append(frm)
            elif kind == "image":
                img = self._load_image(row[1])
                if img is not None:
                    lab = _TK.Label(body, image=img)
                    lab.pack(fill="both", expand=True)
                    scr._widgets.append(lab)
                else:
                    lab = _TK.Label(body, text="(image: %s)" % (row[1] if isinstance(row[1], str) else getattr(row[1], "path", "?")))
                    lab.pack(fill="x")
                    scr._widgets.append(lab)
            elif kind == "field":
                label, value, mode = row[1], row[2], row[3] if len(row) > 3 else ""
                frm = _TK.Frame(body)
                frm.pack(fill="x")
                if label:
                    _TK.Label(frm, text=label, anchor="w").pack(side="left")
                var = _TK.StringVar()
                var.set(value or "")
                ent = _TK.Entry(frm, textvariable=var, show="*" if mode == "password" else None)
                ent.pack(side="left", fill="x", expand=True)
                scr._values[id(row)] = value or ""
                ent.bind("<KeyRelease>", lambda e, rid=id(row), v=var: self._sync_field(scr, rid, v))
                scr._widgets.append(frm)
            elif kind == "spacer":
                sp = _TK.Frame(body, height=int(row[2] or 10))
                sp.pack(fill="x")
                scr._widgets.append(sp)
            elif kind == "gauge":
                label, mx, val = row[1], row[2], row[3]
                root = row[4] if len(row) > 4 else None
                frm = _TK.Frame(body)
                frm.pack(fill="x")
                var = _TK.DoubleVar(value=float(val or 0))
                sc = _TK.Scale(frm, from_=0, to=float(mx or 100), orient="horizontal", variable=var)
                sc.pack(side="left", fill="x", expand=True)
                scr._values[id(row)] = float(val or 0)
                if root is not None:
                    sc.bind("<ButtonRelease-1>",
                            lambda e, rid=id(row), v=var: self._state_gauge(scr, rid, v, root))
                else:
                    sc.bind("<ButtonRelease-1>", lambda e, rid=id(row), v=var: self._sync_gauge(scr, rid, v))
                if label:
                    _TK.Label(frm, text=label, anchor="w").pack(side="left")
                scr._widgets.append(frm)
            elif kind == "choice":
                label, mode, options = row[1], row[2], row[3]
                root = row[4] if len(row) > 4 else None
                frm = _TK.Frame(body)
                frm.pack(fill="x", anchor="w")
                if label:
                    _TK.Label(frm, text=label, anchor="w").pack(anchor="w")
                if mode == "multiple":
                    selected = {}
                    for opt in options:
                        var = _TK.BooleanVar()
                        ck = _TK.Checkbutton(frm, text=opt, variable=var)
                        ck.pack(anchor="w")
                        selected[opt] = var
                        if root is not None:
                            ck.configure(command=lambda o=opt, v=var: self._state_choice(scr, id(row), root))
                        scr._widgets.append(ck)
                    scr._values[id(row)] = selected
                else:
                    var = _TK.StringVar()
                    if options:
                        var.set(options[0])
                    for opt in options:
                        rb = _TK.Radiobutton(frm, text=opt, value=opt, variable=var)
                        rb.pack(anchor="w")
                        if root is not None:
                            rb.configure(command=lambda o=opt, v=var: self._state_choice(scr, id(row), root))
                        scr._widgets.append(rb)
                    scr._values[id(row)] = var
                scr._widgets.append(frm)
            else:
                lab = _TK.Label(body, text=str(row), justify="left")
                lab.pack(fill="x")
                scr._widgets.append(lab)

    def _build_list(self, scr, body):
        lb = _TK.Listbox(body, selectmode="extended")
        lb.pack(fill="both", expand=True)
        for opt in scr.entries:
            lb.insert("end", opt)
        # implicit list: tap / Enter fires graphics.fire
        scr._listbox = lb
        lb.bind("<Double-Button-1>", lambda e: self._fire(scr, SELECT))
        lb.bind("<Return>", lambda e: self._fire(scr, SELECT))
        scr._widgets.append(lb)

    def _build_alert(self, scr, body):
        msg = scr.text or scr.title or ""
        lab = _TK.Label(body, text=msg, justify="left", wraplength=420, anchor="nw")
        lab.pack(fill="both", expand=True, padx=10, pady=10)
        scr._widgets.append(lab)

    def _load_image(self, spec):
        path = getattr(spec, "path", None) or spec
        if path is None:
            return None
        if self.image_resolver is not None:
            try:
                path = self.image_resolver(path)
            except Exception:
                pass
        if isinstance(path, str) and os.path.isfile(path) and _TK is not None:
            try:
                return _TK.PhotoImage(file=path)
            except Exception:
                pass
        return None

    def _sync_item(self, item, var):
        try:
            item.text = var.get()
        except Exception:
            pass

    def _sync_field(self, scr, rid, var):
        scr._values[rid] = var.get()

    def _sync_gauge(self, scr, rid, var):
        try:
            scr._values[rid] = float(var.get())
        except Exception:
            pass

    def _state_gauge(self, scr, rid, var, root):
        try:
            value = float(var.get())
        except Exception:
            value = 0.0
        scr._values[rid] = value
        self.invoke(root, [value])

    def _state_choice(self, scr, rid, root):
        # mirrors itemStateChanged(): one boolean per option, in order
        row = None
        for r in scr.entries:
            if isinstance(r, tuple) and r and r[0] == "choice" and id(r) == rid:
                row = r
                break
        if row is None:
            return
        state = scr._values.get(rid, {})
        self.invoke(root, [self._is_selected(row, opt, state) for opt in row[3]])

    @staticmethod
    def _is_selected(row, opt, state):
        var = state.get(opt)
        if var is not None and hasattr(var, "get"):
            try:
                return bool(var.get())
            except Exception:
                return False
        return bool(var)

    def invoke(self, fn, args):
        """Invoke a LuaFunction safely (marshalled to the main thread)."""
        if self._on_main_thread():
            self._invoke_here(fn, args)
        else:
            self.call_main(self._invoke_here, fn, args)

    def _invoke_here(self, fn, args):
        if fn is None or not hasattr(fn, "call"):
            return
        if self.dispatch is not None:
            self.dispatch(None, None, args, fn)
        else:
            try:
                fn.call(args, None)
            except Exception:
                pass

    # -- command dispatch (mirrors Lua.java commandAction) -------------------

    @staticmethod
    def _command_args(scr):
        # mirrors src/Lua.java commandAction()
        if scr.kind == "list":
            lb = getattr(scr, "_listbox", None)
            if lb is not None:
                try:
                    return [scr.entries[int(i)] for i in lb.curselection()]
                except Exception:
                    pass
            return [opt for opt in scr.entries]
        if scr.kind == "edit":
            return [scr.text or ""]
        args = []
        for row in scr.entries:
            if isinstance(row, Item):
                continue  # StringItem contributes nothing
            kind = row[0]
            if kind == "item":
                obj = row[1]
                if obj.kind == "input":
                    args.append(obj.text or "")
            elif kind == "field":
                args.append(scr._values.get(id(row), row[2] or ""))
            elif kind == "gauge":
                args.append(scr._values.get(id(row), float(row[3] or 0)))
            elif kind == "choice":
                mode, options = row[2], row[3]
                selected = {}
                state = scr._values.get(id(row), {})
                for i, opt in enumerate(options, 1):
                    var = state.get(opt, _MISSING)
                    if var is _MISSING:
                        # tk default: exclusive/popup selects the first option
                        selected[float(i)] = mode != "multiple" and i == 1
                    elif hasattr(var, "get"):
                        try:
                            selected[float(i)] = bool(var.get())
                        except Exception:
                            selected[float(i)] = False
                    else:
                        selected[float(i)] = bool(var)
                args.append(selected)
        return args

    def _fire(self, scr, cmd):
        if self._on_main_thread():
            self._dispatch(scr, cmd)
        else:
            self.call_main(self._dispatch, scr, cmd)

    def _dispatch(self, scr, cmd):
        fn = (scr.handler or {}).get(cmd, None)
        if fn is None:
            fn = (scr.handler or {}).get(None, None)
        if fn is None or not hasattr(fn, "call"):
            return
        args = self._command_args(scr)
        if self.dispatch is not None:
            self.dispatch(scr, cmd, args, fn)
        else:
            try:
                fn.call(args, None)
            except Exception:
                pass


SELECT = Command("Select", "item", 0)

_B = None


def backend():
    global _B
    if _B is None:
        _B = _Backend()
    return _B