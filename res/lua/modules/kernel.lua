#!/bin/lua

return function (payload, args)
    local ok, response = os.request(1, payload, args)
    return response
end