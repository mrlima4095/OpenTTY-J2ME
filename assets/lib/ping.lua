#!/bin/lua

local function ping_http(url)

end
local function ping_socket(uri)

end

if arg[1] then
    if string.sub(arg[1], 1, #"http://") == "http://" or string.sub(arg[1], 1, #"https://") == "https://" then
        ping_http(arg[1])
    end
else print("ping [address]")
end

--[[

else if (mainCommand.equals("ping")) { 
    if (argument.equals("")) { } 
    else { 
        long START = System.currentTimeMillis(); 
        try { 
            HttpConnection CONN = (HttpConnection) Connector.open(!argument.startsWith("http://") && !argument.startsWith("https://") ? "http://" + argument : argument); 
            CONN.setRequestMethod(HttpConnection.GET); 
            int responseCode = CONN.getResponseCode(); 
            CONN.close(); 
            print("Ping to " + argument + " successful, time=" + (System.currentTimeMillis() - START) + "ms", stdout); 
        } catch (IOException e) { 
            print("Ping to " + argument + " failed: " + getCatch(e), stdout); return 101; 
        } 
    } 
}

]]