# Usage: python portscanner.py
#
# Verify opened ports in a host


import socket

def port_scanner(ip, port):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    socket.setdefaulttimeout(1)
    result = s.connect_ex((ip, port))
    if result == 0:
        print(f"Port {port} is open")
    s.close()

ip_address = "192.168.1.1"
for port in range(1, 99999):
    port_scanner(ip_address, port)