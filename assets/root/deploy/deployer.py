import socket
import sys, time

host = "192.168.1.14"
port = 31522

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((host, port))

try: filename = sys.argv[1]
except IndexError: print("missing file"), sys.exit(0)

with open(filename, "rt") as file:
    file = file.readlines()

    for line in file:
        s.send(line.encode())
        time.sleep(1.5)

    print("End of File")