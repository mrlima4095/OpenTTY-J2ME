import socket
import threading
import sys
import tkinter as tk
from tkinter import ttk, filedialog, messagebox


class OpenTTYClient:
    def __init__(self, master, ip, port):
        self.master = master
        self.master.title("Connecting...")

        self.ip = ip
        self.port = int(port)
        self.socket = None
        self.connected = False

        # Configure dark mode
        self.master.configure(bg="#2E2E2E")
        self.style = ttk.Style()
        self.style.theme_use("clam")
        self.style.configure("TFrame", background="#2E2E2E")
        self.style.configure("TLabel", background="#2E2E2E", foreground="#FFFFFF")
        self.style.configure("TButton", background="#424242", foreground="#FFFFFF")
        self.style.configure("TEntry", fieldbackground="#424242", foreground="#FFFFFF", insertcolor="#FFFFFF") 
        self.style.configure("Text", background="#424242", foreground="#FFFFFF")

        # Menubar
        self.menubar = tk.Menu(self.master, bg="#2E2E2E", fg="#FFFFFF", tearoff=0)
        self.file_menu = tk.Menu(self.menubar, tearoff=0, bg="#424242", fg="#FFFFFF")
        self.file_menu.add_command(label="Open nano", command=self.open_nano_editor) 
        self.file_menu.add_command(label="Clear Output", command=self.clear_output) 
        self.file_menu.add_separator()
        self.file_menu.add_command(label="Exit", command=self.close_connection)
        self.menubar.add_cascade(label="File", menu=self.file_menu)

        self.commands_menu = tk.Menu(self.menubar, tearoff=0, bg="#424242", fg="#FFFFFF")
        self.commands_menu.add_command(label="Process", command=lambda: self.send_predefined_command("ps"))
        self.commands_menu.add_command(label="View Files", command=lambda: self.send_predefined_command("dir v"))
        self.commands_menu.add_command(label="Run Debug Script", command=lambda: self.send_predefined_command("debug"))
        self.commands_menu.add_command(label="Import (default)", command=lambda: self.send_predefined_command("execute import /java/lib/yang; import /java/lib/netkit; import /java/lib/settings;"))
        self.menubar.add_cascade(label="Commands", menu=self.commands_menu)

        self.master.config(menu=self.menubar)

        # Main frame
        self.frame = ttk.Frame(master)
        self.frame.pack(fill=tk.BOTH, expand=True)

        # Output text area
        self.output_text = tk.Text(self.frame, state=tk.DISABLED, wrap=tk.WORD, bg="#424242", fg="#FFFFFF")
        self.output_text.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)

        # Input frame
        self.input_frame = ttk.Frame(self.frame)
        self.input_frame.pack(fill=tk.X, padx=5, pady=5)

        self.input_text = ttk.Entry(self.input_frame)
        self.input_text.pack(side=tk.LEFT, fill=tk.X, expand=True)
        self.input_text.bind("<Return>", lambda event: self.send_message())

        self.send_button = ttk.Button(self.input_frame, text="Send", command=self.send_message)
        self.send_button.pack(side=tk.LEFT, padx=5)

        self.connect()

    def connect(self):
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.connect((self.ip, self.port))
            self.connected = True

            self.socket.sendall(b'version\n')
            version = self.socket.recv(1024).decode().strip()

            self.socket.sendall(b'logname\n')
            username = self.socket.recv(1024).decode().strip()

            self.master.title(f"{version} ({username}@{self.ip})")

            threading.Thread(target=self.receive_messages, daemon=True).start()
        except Exception as e:
            messagebox.showerror("OpenTTY", e)
            self.master.destroy()

    def send_message(self):
        message = self.input_text.get()
        if message.strip() and self.connected:
            try:
                if message.split()[0] == "/exit": self.master.destroy(),
                elif message.split()[0] == "/clear": self.clear_output()
                elif message.split()[0] == "/nano": self.open_nano_editor()
                else: self.socket.sendall((message + "\n").encode())
                
                self.input_text.delete(0, tk.END)
            except Exception as e:
                self.show_message(f"[-] {str(e)}")
                self.close_connection()

    def send_predefined_command(self, command):
        if self.connected:
            try:
                self.socket.sendall((command + "\n").encode())
            except Exception as e:
                self.show_message(f"[-] {str(e)}")
                self.close_connection()


    def receive_messages(self):
        try:
            while self.connected:
                data = self.socket.recv(4096)
                if not data:
                    break
                self.show_message(data.decode().strip())
        except Exception as e:
            self.show_message(f"[-] {str(e)}")
        finally:
            self.close_connection()

    def clear_output(self):
        self.output_text.configure(state=tk.NORMAL)
        self.output_text.delete("1.0", tk.END)
        self.output_text.configure(state=tk.DISABLED)

    def show_message(self, message):
        self.output_text.configure(state=tk.NORMAL)
        self.output_text.insert(tk.END, message + "\n")
        self.output_text.see(tk.END)
        self.output_text.configure(state=tk.DISABLED)

    def close_connection(self):
        self.connected = False
        if self.socket:
            try:
                self.socket.close()
            except:
                pass
        self.master.destroy()

    def open_nano_editor(self):
        if not self.connected:
            messagebox.showwarning("OpenTTY", "Not connected to the server.")
            return

        try:
            self.socket.sendall(b"execute raw\n")
            raw_data = self.socket.recv(4096).decode()
        except Exception as e:
            messagebox.showerror("OpenTTY", f"Failed to fetch data: {str(e)}")
            return

        editor_window = tk.Toplevel(self.master)
        editor_window.title("Nano Editor")
        editor_window.configure(bg="#2E2E2E")

        editor_text = tk.Text(editor_window, wrap=tk.WORD, bg="#424242", fg="#FFFFFF")
        editor_text.insert(tk.END, raw_data)
        editor_text.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)

        def save_and_close():
            edited_content = editor_text.get("1.0", tk.END).strip()
            lines = edited_content.split("\n")
            try:
                self.socket.sendall(b"execute touch\n")
                for line in lines:
                    self.socket.sendall(f"add {line}\n".encode())
            except Exception as e:
                messagebox.showerror("OpenTTY", f"Failed to send data: {str(e)}")
            editor_window.destroy()

        save_button = ttk.Button(editor_window, text="Back", command=save_and_close)
        save_button.pack(pady=5)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python OpenTTY.py [ip] [port]")
        sys.exit(1)

    ip = sys.argv[1]
    port = sys.argv[2]

    root = tk.Tk()
    client = OpenTTYClient(root, ip, port)
    root.mainloop()
