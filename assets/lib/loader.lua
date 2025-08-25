
function build(state_var, main, conf)
    local state = os.getenv(state_var)

    if state == nil then
        main()
    else 
        if conf == nil then error("config table cannot be nil") end

        for k,v in pairs(conf) do
            if state == k then
                v()
                
                break
            end
        end

    end
end