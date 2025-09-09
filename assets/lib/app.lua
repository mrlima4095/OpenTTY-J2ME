function QuestAPI()
    graphics.display(graphics.BuildEdit({
        title = "Insert your PasteBin API Key",

        back = {
            label = "Back",
            root = os.exit
        },

        button = {
            label = "Confirm",
            root = function (input)
                os.execute("set PASTEBIN_API=" .. input)
                PASTEBIN_API = input
            end
        }
    }))
end

QuestAPI()