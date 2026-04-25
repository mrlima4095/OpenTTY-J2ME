--[[
   private int importScript(String script, int id) {
        if (script == null || script.length() == 0) { return 2; }

        Hashtable PKG = parseProperties(script);
        final String PID = genpid();
        // |
        // Verify current API version
        if (PKG.containsKey("api.version")) {
            String version = env("$VERSION"), apiVersion = (String) PKG.get("api.version"), mode = (String) PKG.get("api.match");
            if (mode == null || mode.length() == 0) mode = "exact-prefix";

            boolean fail = false;

            if (mode.equals("exact-prefix")) { fail = !version.startsWith(apiVersion); } 
            else if (mode.equals("minimum") || mode.equals("maximum")) {
                String[] currentParts = split(version, '.'), requiredParts = split(apiVersion, '.');
                if (mode.equals("minimum")) { if (currentParts.length < 2 || requiredParts.length < 2) { fail = true; } else { fail = getNumber(requiredParts[1]) > getNumber(currentParts[1]); } }
                else if (mode.equals("maximum")) { if (currentParts.length < 1 || requiredParts.length < 1) { fail = true; } else { fail = getNumber(requiredParts[0]) > getNumber(currentParts[0]); } }
            } 
            else if (mode.equals("exact-full")) { fail = !version.equals(apiVersion); } 
            else { return 1; }

            if (fail) { String error = (String) PKG.get("api.error"); processCommand(error != null ? error : "true", true, id); return 3; }
        }
        if (PKG.containsKey("api.require")) {
            String[] nodes = split((String) PKG.get("api.require"), ',');
            for (int i = 0; i < nodes.length; i++) {
                boolean fail = false;

                if (nodes[i].equals("lua") && javaClass("Lua") != 0) { fail = true; }
                if (nodes[i].equals("canvas") && javaClass("MIDletCanvas") != 0) { fail = true; }
                if (nodes[i].equals("devicefs") && javaClass("javax.microedition.io.FileConnection") != 0) { fail = true; } 
                if (nodes[i].equals("prg") && javaClass("javax.microedition.io.PushRegistry") != 0) { fail = true; }
                if (nodes[i].equals("") && javaClass("") != 0) { fail = true; }

                if (fail) { String error = (String) PKG.get("api.error"); processCommand(error != null ? error : "true", true, id); return 3; }             
            }
        }
        // |
        // Build dependencies
        if (PKG.containsKey("include")) { String[] include = split((String) PKG.get("include"), ','); for (int i = 0; i < include.length; i++) { int STATUS = importScript(getcontent(include[i]), id); if (STATUS != 0) { return STATUS; } } }
        // |
        // Start and handle APP process
        if (PKG.containsKey("process.name")) { start((String) PKG.get("process.name"), PID, (String) PKG.get("process.exit"), id); }
        if (PKG.containsKey("process.port")) { 
            String PORT = (String) PKG.get("process.port"), MOD = (String) PKG.get("process.db"); 
            if (((Hashtable) getobject("1", "sessions")).containsKey(PORT)) { MIDletLogs("add warn Application port is unavailable."); return 68; }
            
            new MIDletControl("bind", env(PORT + " " + (MOD == null ? "" : MOD)), id);
        }
        // |
        // Start Application
        if (PKG.containsKey("config")) { int STATUS = processCommand((String) PKG.get("config"), true, id); if (STATUS != 0) { return STATUS; } }
        if (PKG.containsKey("mod") && PKG.containsKey("process.name")) { new MIDletControl(PID, "MIDlet-MOD", (String) PKG.get("mod"), true, id); }
        // |
        // Generate items - Command & Files
        if (PKG.containsKey("command")) { String[] commands = split((String) PKG.get("command"), ','); for (int i = 0; i < commands.length; i++) { if (PKG.containsKey(commands[i])) { aliases.put(commands[i], env((String) PKG.get(commands[i]))); } else { MIDletLogs("add error Failed to create command '" + commands[i] + "' content not found"); } } }
        if (PKG.containsKey("file")) { String[] files = split((String) PKG.get("file"), ','); for (int i = 0; i < files.length; i++) { if (PKG.containsKey(files[i])) { int STATUS = writeRMS("/home/" + files[i], env((String) PKG.get(files[i])), id); } else { MIDletLogs("add error Failed to create file '" + files[i] + "' content not found"); } } }
        // |
        // Build APP Shell
        if (PKG.containsKey("shell.name") && PKG.containsKey("shell.args")) { String[] args = split((String) PKG.get("shell.args"), ','); Hashtable TABLE = new Hashtable(); for (int i = 0; i < args.length; i++) { String NAME = args[i].trim(), VALUE = (String) PKG.get(NAME); TABLE.put(NAME, (VALUE != null) ? VALUE : ""); } if (PKG.containsKey("shell.unknown")) { TABLE.put("shell.unknown", (String) PKG.get("shell.unknown")); } shell.put(((String) PKG.get("shell.name")).trim(), TABLE); }

        return 0;
    }
]]