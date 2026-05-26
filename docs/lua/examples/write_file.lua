-- Lua J2ME - Code examples
-- This is an example of writing a file content with Lua API

local filename = "/tmp/write_test"
local content = "Test content"
local status = io.write(content, filename) -- Write and returns a int (exit code of write)
print(status) -- Do anything with the integer content

-- possible exit codes:
-- 0    success
-- 5    read-only storage
-- 13   permission denied