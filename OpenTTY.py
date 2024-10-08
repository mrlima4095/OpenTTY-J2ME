import tkinter as tk
from tkinter import Menu, filedialog
import subprocess
import sys

class OpenTTY:
    def __init__(self, root):
        self.package = {
            "version": "1.9",
            "vendor": "Mr. Lima",

        }

        self.root = root
        self.root.title("OpenTTY" + self.package['version'])

        # Bloqueia o redimensionamento da janela
        self.root.resizable(False, False)

        # Menu superior
        self.menu_bar = Menu(self.root)
        self.root.config(menu=self.menu_bar)

        # Menu "Files" com botão "Exit"
        file_menu = Menu(self.menu_bar, tearoff=0)
        file_menu.add_command(label="Clear", command=self.clear_output)
        file_menu.add_command(label="Open File", command=self.load_output)
        file_menu.add_command(label="Save as", command=self.save_output)
        file_menu.add_command(label="Exit", command=self.root.quit)
        self.menu_bar.add_cascade(label="Files", menu=file_menu)

        # Caixa de saída de texto para exibir o resultado dos comandos
        self.output_text = tk.Text(self.root, height=20, width=100, wrap=tk.WORD)
        self.output_text.pack(padx=10, pady=10, expand=True)

        # Frame para alinhar a caixa de entrada e o botão na parte inferior
        bottom_frame = tk.Frame(self.root)
        bottom_frame.pack(side=tk.BOTTOM, fill=tk.X, padx=10, pady=10)

        # Caixa de entrada de comandos (no bottom_frame)
        self.command_input = tk.Entry(bottom_frame, width=91)
        self.command_input.pack(side=tk.LEFT, expand=True, padx=5)

        # Botão "Enviar" para processar o comando (no bottom_frame)
        self.send_button = tk.Button(bottom_frame, text="Send", command=self.process_command)
        self.send_button.pack(side=tk.RIGHT, padx=5)

        # Atalho para pressionar "Enter" e enviar o comando
        self.command_input.bind("<Return>", self.process_command)

    def process_command(self, event=None):
        """Executa o comando digitado e exibe a saída na caixa de texto."""
        command = self.command_input.get().strip()

        if not command: return

        if command.split()[0] == "exit": sys.exit(' '.join(command.split()[1:]))
        elif command.split()[0] == "clear": return self.clear_output(), self.command_input.delete(0, tk.END)


        self.command_input.delete(0, tk.END)

        try:
            result = subprocess.run(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            output = result.stdout if result.returncode == 0 else result.stderr
        except Exception as e:
            output = e

        self.print(output)

    def print(self, text): self.output_text.insert(tk.END, f"{text}"), self.output_text.see(tk.END)
    def clear_output(self): self.output_text.delete('1.0', tk.END)
    def save_output(self):
        file_path = filedialog.asksaveasfilename(defaultextension=".txt", filetypes=[("Text files", "*.txt"), ("All files", "*.*")])
        if file_path:
            try:
                with open(file_path, 'wt+') as file: file.write(self.output_text.get('1.0', tk.END))
            except Exception as e: self.print(e)
    def load_output(self):
        file_path = filedialog.askopenfilename(defaultextension=".txt", filetypes=[("Text files", "*.txt"), ("All files", "*.*")])
        if file_path:
            try:
                with open(file_path, 'rt') as file: self.clear_output(), self.print(file.read())
            except Exception as e: self.print(e)


if __name__ == "__main__":
    # Cria a janela principal
    root = tk.Tk()
    OpenTTY(root)
    root.mainloop()
