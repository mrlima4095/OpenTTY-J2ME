import tkinter as tk
from tkinter import filedialog, messagebox, Menu

class OpenTTYSDK:
    def __init__(self, root):
        self.root = root
        self.root.title("OpenTTY SDK")
        self.root.geometry("800x600")
        
        self.filename = None
        self.create_menu()
        self.create_text_editor()

    def create_menu(self):
        menubar = Menu(self.root)
        file_menu = Menu(menubar, tearoff=0)
        file_menu.add_command(label="Open", command=self.open_file)
        file_menu.add_command(label="Save", command=self.save_file)
        file_menu.add_command(label="Save as...", command=self.save_file_as)
        file_menu.add_separator()
        file_menu.add_command(label="Exit", command=self.root.quit)
        menubar.add_cascade(label="File", menu=file_menu)

        project_menu = Menu(menubar, tearoff=0)
        project_menu.add_command(label="New Deamon", command=self.create_deamon)
        project_menu.add_command(label="New Server", command=self.create_server)
        project_menu.add_command(label="New Canvas", command=self.create_canvas)
        project_menu.add_command(label="New Enhanced", command=self.create_advanced)
        project_menu.add_separator()
        project_menu.add_command(label="Compile")
        project_menu.add_command(label="Debug")
        project_menu.add_command(label="Run")
        menubar.add_cascade(label="Project", menu=project_menu)

        self.root.config(menu=menubar)

    def create_text_editor(self):
        frame = tk.Frame(self.root)
        frame.pack(expand=1, fill="both")

        # Create the line number display area
        self.line_numbers = tk.Text(frame, width=4, padx=3, takefocus=0, bd=0, bg='lightgray', fg='black', font=("Arial", 12))
        self.line_numbers.pack(side="left", fill="y")

        # Create the main text editor area
        self.text_editor = tk.Text(frame, wrap="word", font=("Arial", 12))
        self.text_editor.pack(side="right", expand=1, fill="both")

        # Sync the line numbers with the text editor
        self.text_editor.bind("<KeyRelease>", self.update_line_numbers)
        self.text_editor.bind("<ButtonRelease-1>", self.update_line_numbers)
        self.update_line_numbers()

    def update_line_numbers(self, event=None):
        # Get the number of lines in the text editor
        lines = int(self.text_editor.index('end-1c').split('.')[0])

        # Update the line numbers
        self.line_numbers.delete(1.0, tk.END)
        for i in range(1, lines + 1):
            self.line_numbers.insert(tk.END, f"{i}\n")

    def open_file(self):
        file_path = filedialog.askopenfilename(filetypes=[("INI files", "*.ini"), ("All files", "*.*")])
        if file_path:
            self.filename = file_path
            with open(file_path, "r") as file:
                content = file.read()
                self.text_editor.delete(1.0, tk.END)
                self.text_editor.insert(tk.END, content)
            self.update_line_numbers()

    def save_file(self):
        if self.filename:
            content = self.text_editor.get(1.0, tk.END)
            with open(self.filename, "w") as file:
                file.write(content)
            messagebox.showinfo("", "App saved")
        else:
            self.save_file_as()

    def save_file_as(self):
        file_path = filedialog.asksaveasfilename(defaultextension=".ini", filetypes=[("INI files", "*.ini")])
        if file_path:
            self.filename = file_path
            self.save_file()

    def create_deamon(self):
        self.text_editor.delete(1.0, tk.END)
        template = "[ Config ]\n\nname=\nversion=\ndescription=\n\napi.version=\napi.error=\n\nprocess.name=\nprocess.type=\nprocess.port=\nprocess.host=\nprocess.db=\n\ninclude=\n\nconfig=\ncommand=\n\n"
        self.text_editor.insert(tk.END, template)
        self.update_line_numbers()

    def create_server(self):
        self.text_editor.delete(1.0, tk.END)
        template = "[ Config ]\n\nname=\nversion=\ndescription=\n\napi.version=\napi.error=\n\nprocess.port=\nprocess.host=\n\nconfig=\n\n"
        self.text_editor.insert(tk.END, template)
        self.update_line_numbers()

    def create_canvas(self):
        self.text_editor.delete(1.0, tk.END)
        template = "[ Config ]\n\nname=\nversion=\ndescription=\n\napi.version=\napi.error=\n\ninclude=\n\nconfig=\ncommand=\n\n[ DISPLAY ]\n\nscreen.title=\nscreen.content=\nscreen.content.style=\nscreen.back.label=\nscreen.back=\nscreen.button=\nscreen.button.cmd=\n\ncanvas.title=\ncanvas.content=\ncanvas.content.type=\ncanvas.content.link=\ncanvas.content.style=\ncanvas.button=\ncanvas.button.cmd=\ncanvas.back.label=\ncanvas.back=\ncanvas.mouse=\ncanvas.background=\ncanvas.background.type=\n\nquest.title=\nquest.label=\nquest.key=\nquest.cmd=\nquest.back=\n\nlist.title=\nlist.content=\nlist.back.label=\nlist.back=\nlist.button=\n\n"
        self.text_editor.insert(tk.END, template)
        self.update_line_numbers()

    def create_advanced(self):
        self.text_editor.delete(1.0, tk.END)
        template = "[ Config ]\n\nname=\nversion=\ndescription=\n\napi.version=\napi.error=\nprocess.name=\nprocess.type=\nprocess.port=\nprocess.host=\nprocess.db=\n\ninclude=\n\nconfig=\ncommand=\n\nshell.name=\nshell.args=\n\n[ COMMAND ]\n\nitem.label=\nitem.cmd=\n\n[ DISPLAY ]\n\nscreen.title=\nscreen.content=\nscreen.content.style=\nscreen.back.label=\nscreen.back=\nscreen.button=\nscreen.button.cmd=\n\ncanvas.title=\ncanvas.content=\ncanvas.content.type=\ncanvas.content.link=\ncanvas.content.style=\ncanvas.button=\ncanvas.button.cmd=\ncanvas.back.label=\ncanvas.back=\ncanvas.mouse=\ncanvas.background=\ncanvas.background.type=\n\nquest.title=\nquest.label=\nquest.key=\nquest.cmd=\nquest.back=\n\nlist.title=\nlist.content=\nlist.back.label=\nlist.back=\nlist.button=\n\n"
        self.text_editor.insert(tk.END, template)
        self.update_line_numbers()

if __name__ == "__main__":
    root = tk.Tk()
    app = OpenTTYSDK(root)
    root.mainloop()
