-- Test Script for BuildScreen: Testing all UI elements and features
-- This script creates a comprehensive screen with all supported field types:
-- - text (StringItem)
-- - image
-- - item (interactive button-like item)
-- - spacer
-- - gauge
-- - textfield
-- - choice (multiple selection)
-- It also tests back/button navigation, title, and form data collection on submit.

-- Define a callback function for the submit button (to handle form data)
local function on_submit(form_data)
    print("=== Form Data Submitted ===")
    for i = 1, #form_data do
        local item_data = form_data[i]
        print("Field " .. i .. ": " .. tostring(item_data))
    end
    print("===========================")
end

-- Define a callback for the interactive item click
local function on_item_click()
    print("Interactive item clicked!")
end

-- Main screen configuration table
local screen_config = {
    title = "BuildScreen Full Test",
    
    -- Back button config (returns to xterm or runs a command)
    back = {
        label = "Go Back"
        -- Optional: root = "some_command" or a function
    },
    
    -- Submit button config (triggers on_submit with form data)
    button = {
        label = "Submit Form",
        root = on_submit  -- This will receive the form_data vector on click
    },
    
    -- Fields: Array-like table of field configs (uses sequential numeric keys)
    fields = {
        -- 1. Text field (StringItem)
        {
            type = "text",
            label = "Description",
            value = "This is a static text item for testing.",
            style = "bold"  -- Optional: font style (if supported by midlet.newFont)
        },
        
        -- 2. Image field
        {
            type = "image",
            img = "/res/img/app.png"  -- Replace with a valid image path if available; otherwise, it may skip or error
        },
        
        -- 3. Interactive item (button-like)
        {
            type = "item",
            label = "Click Me!",
            root = on_item_click,  -- Calls the function on click
            style = "default"
        },
        
        -- 4. Spacer
        {
            type = "spacer",
            width = 5,
            height = 15  -- Note: 'heigth' in code, but using 'height' for clarity; adjust if needed
        },
        
        -- 5. Gauge (progress bar)
        {
            type = "gauge",
            label = "Progress Gauge",
            interactive = true,  -- User can adjust
            max = 100,
            value = 42
        },
        
        -- 6. TextField (single-line input)
        {
            type = "textfield",
            label = "Enter Name:",
            value = "Default Name",
            mode = "any"  -- Options: any, number, email, phone, decimal, password
        },
        
        -- 7. ChoiceGroup (multiple selection)
        {
            type = "choice",
            label = "Select Options (Multiple):",
            mode = "multiple",  -- exclusive, multiple, popup, implicit
            options = {  -- Table of options (array-like)
                "Option 1",
                "Option 2",
                "Option 3",
                "Option 4"
            }
            -- Optional: icon = "/icon.png" (applies to all choices if supported)
        }
    }
}

-- Build and display the screen
local screen = graphics.BuildScreen(screen_config)
graphics.display(screen)

-- Optional: Print confirmation
print("Test screen built and displayed. Interact with elements and submit to see data collection.")
print("Note: Image paths and font styles depend on your midlet implementation.")