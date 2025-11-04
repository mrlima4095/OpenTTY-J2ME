return function (payload, source, pid, id)
    -- payload: requistion for the service (on Lua can be any value, from shell only strings)
    -- source: [lua, mod, shell] where this request was called?
    -- pid: process id of who make this request
    -- id: user id of who make this request

    return {} -- processed request returns anything, if it is returns to shell, the returned value will be displayed on stdout
end