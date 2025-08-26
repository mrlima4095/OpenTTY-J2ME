local function build(state_var, config, throw)
    if state_var == nil or config == nil then error("Build: config need to be a table") end

    local state = os.getenv(state_var)
    
    if state == nil then
        return config:main()
    else
        for k,v in pairs(config) do
            if k == state then return v() end
        end

        if throw ~= nil or throw ~= false then error("Build: invalid state at config '" .. state .. "'") end
    end
end

return build