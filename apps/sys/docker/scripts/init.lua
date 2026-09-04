#!/bin/lua

os.setproc("name", "container-init")

local container_id = arg[2]
local image_path = arg[1]

if not image_path or not container_id then
    print("init: usage: init <image> <container_id>")
    os.exit(2)
end

local content = io.read(image_path)
if not content then
    print("init: " .. image_path .. ": image not found")
    os.exit(127)
end

local ok, image = pcall(load, "return " .. content)
if not ok or type(image) ~= "function" then
    print("init: invalid image format")
    os.exit(2)
end

ok, image = pcall(image)
if not ok or type(image) ~= "table" then
    print("init: failed to load image")
    os.exit(2)
end

local root = "/mnt/docker/" .. container_id .. "/"
os.mkdir(root)
os.mkdir(root .. "bin/")
os.mkdir(root .. "dev/")
os.mkdir(root .. "etc/")
os.mkdir(root .. "home/")
os.mkdir(root .. "lib/")
os.mkdir(root .. "mnt/")
os.mkdir(root .. "tmp/")

local fs = image["fs"] or {}
for path, entries in pairs(fs) do
    local full = root
    if path ~= "/" then
        full = root .. string.sub(path, 2)
    end
    os.mkdir(full)
    if type(entries) == "table" then
        for name, entry_content in pairs(entries) do
            if type(entry_content) == "string" then
                if string.sub(name, -1) == "/" then
                    os.mkdir(full .. name)
                elseif entry_content ~= "" then
                    io.write(entry_content, full .. name)
                end
            end
        end
    end
end

if image["scope"] then
    local scope = os.scope()
    for k, v in pairs(image["scope"]) do
        scope[k] = v
    end
    scope["ROOT"] = root
    scope["VERSION"] = image["version"] or "unknown"
    scope["PATCH"] = image["patch"] or ""
    os.scope(scope)
end

print("Container " .. container_id .. " initialized")
print("Image: " .. (image["name"] or "unknown") .. " " .. (image["version"] or ""))
