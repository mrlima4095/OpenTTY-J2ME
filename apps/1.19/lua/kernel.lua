-- rock: name=kernel
-- rock: version=1.1.0
-- rock: description=Typed wrappers around the /bin/init kernel payloads
-- rock: here=/lib/kernel.lua

-- `/bin/init` is PID 1 and owns the kernel payloads: signals, process lookup,
-- password checks, the shell hook, the VFS and user management. The raw call
-- is `os.request(1, payload, args)`; this module gives each payload a name so
-- scripts do not have to remember the argument shapes.
local K = {}

K.SIGHUP = "1"
K.SIGINT = "2"
K.SIGKILL = "9"
K.SIGTERM = "15"

local function request(payload, args)
    return os.request(1, payload, args)
end

function K.request(payload, args) return request(payload, args) end

-- Signal a process. A custom sighandler runs for every signal except SIGKILL.
function K.signal(pid, signal)
    return request("sendsig", { pid = pid, signal = signal or K.SIGTERM })
end

function K.kill(pid) return K.signal(pid, K.SIGKILL) end

-- Return the Process object for a pid (nil when missing or not permitted).
function K.proc(pid) return request("proc", pid) end

function K.nice(pid, priority)
    return request("nice", { pid = pid, priority = priority })
end

-- passwd(old, new) changes the password; passwd(password) only checks it.
function K.passwd(old, new)
    if new == nil then return request("passwd", old) end
    return request("passwd", { old = old, new = new })
end

function K.checkpasswd(password) return request("passwd", password) end

-- Install a Lua function as the shell used by os.execute, or reset it with nil.
function K.setsh(shell) return request("setsh", shell) end

-- cache() reads the token cache flag; cache(true/false) changes it.
function K.cache(value)
    if value == nil then return request("cache") end
    return request("cache", value and true or false)
end

-- memory() returns the ELF heap size in KB; memory(kb) sets it.
function K.memory(value)
    if value == nil then return request("memory") end
    return request("memory", value)
end

function K.debug(value)
    if value == nil then return request("debug") end
    return request("debug", value and true or false)
end

-- Table of every process that owns a socket: { [pid] = { [1] = "socket://..." } }.
function K.netsh() return request("netsh") end

-- Start a service script (it must return a handler function).
function K.serve(program) return request("serve", program) end

-- Clear a VFS directory. Root only; the global mounts are protected.
function K.rms(path) return request("rms", path) end

function K.useradd(name) return request("useradd", name) end
function K.userdel(name) return request("userdel", name) end
function K.user(uid) return request("user", uid) end

function K.is_root() return os.getuid() == 0 end

-- Keep the old callable form: `local k = require("kernel"); k("proc", pid)`.
return setmetatable(K, {
    __call = function(_, payload, args) return request(payload, args) end
})
