#!/usr/bin/env python3
"""OpenTTY X11 proxy.

The proxy accepts the line protocol used by /bin/x11 and renders requested
windows using Tk, which creates native windows through the host display server.

Usage: python3 tools/x11_proxy.py [--host 0.0.0.0] [--port 31523]
"""

import argparse
import base64
import queue
import socket
import threading


def decode(value):
    return base64.b64decode(value.encode("ascii")).decode("utf-8")


class Client:
    def __init__(self, conn, address, commands):
        self.conn = conn
        self.address = address
        self.commands = commands
        self.lock = threading.Lock()

    def send(self, line):
        with self.lock:
            self.conn.sendall((line + "\n").encode("utf-8"))

    def run(self):
        try:
            print("OpenTTY client connected: %s:%d" % self.address, flush=True)
            with self.conn.makefile("r", encoding="utf-8", newline="\n") as stream:
                for line in stream:
                    fields = line.rstrip("\r\n").split("\t")
                    if not fields or not fields[0]:
                        continue
                    print("OpenTTY request: %s" % fields[0], flush=True)
                    reply = queue.Queue(1)
                    self.commands.put((self, fields, reply))
                    result = reply.get()
                    self.send(result)
        except (ConnectionError, OSError) as error:
            print("OpenTTY client error: %s" % error, flush=True)
        finally:
            print("OpenTTY client disconnected: %s:%d" % self.address, flush=True)
            self.conn.close()


class Proxy:
    def __init__(self, root):
        self.root = root
        self.windows = {}

    def event(self, client, kind, window, value=""):
        client.send("event\t%s\t%s\t%s" % (
            base64.b64encode(kind.encode("utf-8")).decode("ascii"),
            base64.b64encode(window.encode("utf-8")).decode("ascii"),
            base64.b64encode(value.encode("utf-8")).decode("ascii"),
        ))

    def handle(self, client, fields):
        try:
            operation = fields[0]
            values = [decode(value) for value in fields[1:]]
            if operation == "create":
                window_id, title, width, height = values
                if window_id in self.windows:
                    self.windows[window_id][0].destroy()
                window = tk.Toplevel(self.root)
                window.title(title)
                window.geometry("%sx%s" % (int(width), int(height)))
                canvas = tk.Canvas(window, background="white", highlightthickness=0)
                canvas.pack(fill="both", expand=True)
                window.protocol("WM_DELETE_WINDOW", lambda: self.close(client, window_id))
                self.windows[window_id] = (window, canvas, client)
            elif operation == "text":
                _, canvas, _ = self.windows[values[0]]
                canvas.create_text(int(values[1]), int(values[2]), text=values[3], fill=values[4], anchor="nw")
            elif operation == "rect":
                _, canvas, _ = self.windows[values[0]]
                x, y, width, height = map(int, values[1:5])
                canvas.create_rectangle(x, y, x + width, y + height, fill=values[5], outline=values[5])
            elif operation == "title":
                self.windows[values[0]][0].title(values[1])
            elif operation == "clear":
                self.windows[values[0]][1].delete("all")
            elif operation == "show":
                self.windows[values[0]][0].deiconify()
            elif operation == "close":
                self.close(client, values[0])
            else:
                return "error\tunknown operation"
            return "ok"
        except (IndexError, KeyError, ValueError, tk.TclError, UnicodeError) as error:
            return "error\t" + str(error)

    def close(self, client, window_id):
        record = self.windows.pop(window_id, None)
        if record:
            record[0].destroy()
            self.event(client, "close", window_id)


def main():
    parser = argparse.ArgumentParser(description="OpenTTY native X11 window proxy")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=31523)
    options = parser.parse_args()

    try:
        global tk
        import tkinter as tk
    except ImportError:
        parser.error("tkinter is required; install the python3-tk system package")

    root = tk.Tk()
    root.withdraw()
    commands = queue.Queue()
    proxy = Proxy(root)

    def accept_loop(server):
        while True:
            conn, address = server.accept()
            client = Client(conn, address, commands)
            threading.Thread(target=client.run, daemon=True).start()

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((options.host, options.port))
    server.listen()
    threading.Thread(target=accept_loop, args=(server,), daemon=True).start()
    print("OpenTTY X11 proxy listening on %s:%d" % (options.host, options.port))

    def process_commands():
        while True:
            try:
                client, fields, reply = commands.get_nowait()
            except queue.Empty:
                break
            reply.put(proxy.handle(client, fields))
        root.after(20, process_commands)

    root.after(20, process_commands)
    root.mainloop()


if __name__ == "__main__":
    main()
