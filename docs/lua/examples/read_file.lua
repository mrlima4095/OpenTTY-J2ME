-- Lua J2ME - Code examples
-- This is an example of reading a file content with Lua API

local filename = "/home/OpenRMS"
local content = io.read(filename) -- Open file by name, read it and returns the content
print(content) -- Do anything with the string content