import os, time, random
# |
# OpenTTY Application
class OpenTTY:
    def __init__(self):
        self.uptime = time.time()
        self.useCache = True
        self.debug = False

        self.attributes = {}
        self.sys = {}
        self.graphics = {}
        self.network = {}
        self.globals = { USER = "root", PWD = "/home/", ROOT = "/" }

        self.username = self.read("/home/OpenRMS")
        self.build = "2026-py-1.18-001"
        
