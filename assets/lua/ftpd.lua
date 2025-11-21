#!/bin/lua

--[[

[ Config ]

name=FTPd
version=1.0
description=J2ME FTP Server

api.version=1.17
api.require=lua
api.error=execute echo [ FTPd ] Required OpenTTY 1.17 or newer and Lua Runtime 
api.match=minimum

]]

local app = {}


function app.main()

end

os.setproc("name", "ftpd")
app.main()
