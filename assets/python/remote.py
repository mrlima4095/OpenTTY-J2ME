#!/usr/bin/python3
# -*- coding: utf-8 -*-
#    
#    python remote.py 
#    
#  Copyright (C) 2024 "Mr. Lima"
#  

import socket
import subprocess

# Server settings
HOST = '0.0.0.0'  # Listen at current machine
PORT = 4095       # Connection port setting

def execute_command(command):
    try:
        output = subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT)
        return output.decode('utf-8')
    except subprocess.CalledProcessError as e:
        return str(e.output.decode('utf-8'))

def start_server():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
        server_socket.bind((HOST, PORT))
        server_socket.listen(5)
        print(f"[+] listening at port {PORT}")

        while True:
            client_socket, addr = server_socket.accept()
            with client_socket:
                print(f"[+] {addr[0]} connected")

                while True:
                    try:
                        command = client_socket.recv(1024).decode('utf-8')

                        if not command:
                            print("[-] Client disconnected")
                            break

                        print(f"[+] Running command: {command.strip()}")
                      
                        result = execute_command(command)
                        client_socket.sendall(result.strip().encode('utf-8'))

                    except Exception as e:
                        print(f"[-] Error: {e}")
                        break
                        
        server_socket.close()
                        
if __name__ == '__main__':
    start_server()
