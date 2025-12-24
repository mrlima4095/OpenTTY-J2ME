public int processCommand(String command, boolean enable, int id, String pid, Object stdout, Hashtable scope) {
    command = command.startsWith("exec") ? command.trim() : env(command.trim());
    String mainCommand = getCommand(command), argument = getpattern(getArgument(command));
    String[] args = splitArgs(getArgument(command));

    if (username.equals("root")) { id = 0; } if (scope == null) { scope = globals; }
    if (command.endsWith("&")) { return processCommand("bg " + command.substring(0, command.length() - 1), enable, id, pid, stdout, scope); }

    if (mainCommand.equals("") || mainCommand.equals("true") || mainCommand.equals("#")) { }
    else if (classpath && file("/bin/" + mainCommand) && !mainCommand.equals("sh") && !mainCommand.equals("lua") && !mainCommand.startsWith(".")) { return processCommand(". /bin/" + command, enable, id, pid, stdout, scope); }

    // Shell Utilities
    //else if (mainCommand.equals("set")) { if (argument.equals("")) { } else { int INDEX = argument.indexOf('='); if (INDEX == -1) { for (int i = 0; i < args.length; i++) { attributes.put(args[i], ""); } } else { attributes.put(argument.substring(0, INDEX).trim(), getpattern(argument.substring(INDEX + 1).trim())); } } } 
    //else if (mainCommand.equals("unset")) { if (argument.equals("")) { } else { for (int i = 0; i < args.length; i++) { if (attributes.containsKey(args[i])) { attributes.remove(args[i]); } else { } } } }
    else if (mainCommand.equals("export")) { return processCommand(argument.equals("") ? "env" : "set " + argument, false, id, pid, stdout, scope); }
    //else if (mainCommand.equals("env")) { if (argument.equals("")) { for (Enumeration KEYS = attributes.keys(); KEYS.hasMoreElements();) { String KEY = (String) KEYS.nextElement(), VALUE = (String) attributes.get(KEY); if (!KEY.equals("OUTPUT") && !VALUE.equals("")) { print(KEY + "=" + VALUE.trim(), stdout); } } } else { for (int i = 0; i < args.length; i++) { if (attributes.containsKey(args[i])) { print(args[i] + "=" + (String) attributes.get(args[i]), stdout); } else { print("env: " + args[i] + ": not found", stdout); return 127; } } } }
    // | (Scope Management)

    // | (Tools)
    //else if (mainCommand.equals("echo")) { print(argument, stdout); }
    //else if (mainCommand.equals("buff")) { stdin.setString(argument); }
    else if (mainCommand.equals("help")) { print(read("/res/docs"), stdout); }
    //else if (mainCommand.equals("date")) { print(new java.util.Date().toString(), stdout); }
    //else if (mainCommand.equals("locale")) { print((String) attributes.get("LOCALE"), stdout); }
    //else if (mainCommand.equals("clear")) { if (argument.equals("")) { this.stdout.setText(""); } else { for (int i = 0; i < args.length; i++) { if (args[i].equals("stdout")) { this.stdout.setText(""); } else if (args[i].equals("stdin")) { stdin.setString(""); } else if (args[i].equals("history")) { getprocess("1").put("history", new Vector()); } else if (args[i].equals("cache")) { cache = new Hashtable(); } else if (args[i].equals("logs")) { logs = ""; } else { print("clear: " + args[i] + ": not found", stdout); return 127; } } } }
    // | (Chain)
    //else if (mainCommand.startsWith("exec")) { if (argument.equals("")) { } else { if (mainCommand.equals("execute")) { args = split(argument, ';'); } for (int i = 0; i < args.length; i++) { int STATUS = processCommand(args[i].trim(), enable, id, pid, stdout, scope); if (STATUS != 0) { return STATUS; } } } }
    else if (mainCommand.equals("catch")) { if (argument.equals("")) { } else { try { processCommand(argument, enable, id, pid, stdout, scope); } catch (Throwable e) { print(getCatch(e), stdout); } } }
    //else if (mainCommand.equals("builtin") || mainCommand.equals("command")) { if (argument.equals("")) { } else { return processCommand(argument, false, id, pid, stdout, scope); } }
    //else if (mainCommand.equals("eval")) { if (argument.equals("")) { } else { print("" + processCommand(argument, enable, id, pid, stdout, scope), stdout); } }
    // | (Sessions)
    //else if (mainCommand.equals("whoami") || mainCommand.equals("logname")) { print(id == 0 ? "root" : username, stdout); }
    else if (mainCommand.equals("sudo")) { if (argument.equals("")) { } else if (id == 0) { return processCommand(argument, enable, id, pid, stdout, scope); } else { new MIDletControl(argument, enable, pid, stdout, scope); } }
    //else if (mainCommand.equals("su")) { if (id == 0) { username = username.equals("root") ? read("/home/OpenRMS") : "root"; return processCommand(". /bin/sh", false, id, pid, stdout, scope); } else { print("Permission denied!", stdout); return 13; } }
    //else if (mainCommand.equals("sh") || mainCommand.equals("login")) { return argument.equals("") ? processCommand(". /bin/sh", false, id, pid, stdout, scope) : runScript(argument, id, pid, stdout, scope); }
    //else if (mainCommand.equals("id")) { String ID = argument.equals("") ? String.valueOf(id) : argument.equals("root") ? "0" : argument.equals(read("/home/OpenRMS")) ? "1000" : null; if (ID == null) { print("id: '" + argument + "': no such user", stdout); return 127; } print(ID, stdout); }
    else if (mainCommand.equals("passwd")) { if (argument.equals("")) { } else if (id == 0) { writeRMS("OpenRMS", argument.getBytes(), 2); } else { print("Permission denied!", stdout); return 13; } }
    else if (mainCommand.equals("logout")) { if (read("/home/OpenRMS").equals(username)) { if (id == 0) { writeRMS("/home/OpenRMS", "".getBytes(), id); destroyApp(false); } else { print("Permission denied!", stdout); return 13; } } else { username = read("/home/OpenRMS"); return processCommand(". /bin/sh", false, id, pid, stdout, scope); } }
    else if (mainCommand.equals("exit")) { if (read("/home/OpenRMS").equals(username)) { destroyApp(false); } else { username = read("/home/OpenRMS"); return processCommand(". /bin/sh", false, id, pid, stdout, scope); } }
    else if (mainCommand.equals("quit")) { destroyApp(false); }
    //else if (mainCommand.equals("false")) { return 255; }
    // |
    // |
    // -=-=-=-=-=-=-=-=-=-=-=-
    // API 001 - Kernel
    // | (Client)
    //else if (mainCommand.equals("top")) { return kernel(argument, id, pid, stdout, scope);  }
    // | (Process)
    //else if (mainCommand.equals("ps")) { print("PID\tPROCESS", stdout); for (Enumeration KEYS = sys.keys(); KEYS.hasMoreElements();) { String PID = (String) KEYS.nextElement(); print(PID + "\t" + (String) ((Hashtable) sys.get(PID)).get("name"), stdout); } }
    //else if (mainCommand.equals("start") || mainCommand.equals("stop") || mainCommand.equals("kill")) { for (int i = 0; i < args.length; i++) { int STATUS = mainCommand.equals("start") ? start(args[i], id, genpid(), null, stdout, scope) : mainCommand.equals("stop") ? stop(args[i], id, stdout, scope) : kill(args[i], true, id, stdout, scope); if (STATUS != 0) { return STATUS; } } } 
    // | (Memory)
    //else if (mainCommand.equals("gc")) { System.gc(); }
    // | (Threads)
    else if (mainCommand.equals("throw")) { Thread.currentThread().interrupt(); }
    else if (mainCommand.equals("bg")) { if (argument.equals("")) { } else { new MIDletControl("Background", argument, enable, id, stdout, scope); } }
    else if (mainCommand.equals("time")) { if (argument.equals("")) { } else { long START = System.currentTimeMillis(); int STATUS = processCommand(argument, enable, id, pid, stdout, scope); print("at " + (System.currentTimeMillis() - START) + "ms", stdout); return STATUS; } }
    else if (mainCommand.equals("mmspt") || mainCommand.equals("chrt")) { if (argument.equals("")) { print(getThreadName(Thread.currentThread()), stdout); } else if (argument.equals("priority")) { print("" + Thread.currentThread().getPriority(), stdout); } else { int value = getNumber(argument, Thread.NORM_PRIORITY, null); if (value > 10 || value < 1) { return 2; } else { Thread.currentThread().setPriority(value); } } }
    else if (mainCommand.equals("uptime")) {
        long totalSeconds = (System.currentTimeMillis() - uptime) / 1000, hours = totalSeconds / 3600, minutes = (totalSeconds % 3600) / 60, seconds = totalSeconds % 60;
        
        print((hours > 0 ? hours + "h " : "") + (minutes > 0 || hours > 0 ? minutes + "m " : "") + seconds + "s", stdout);
    }
    // |
    else if (mainCommand.equals("cron")) { if (argument.equals("")) { } else { return processCommand("execute sleep " + getCommand(argument) + "; " + getArgument(argument), enable, id, pid, stdout, scope); } }
    else if (mainCommand.equals("sleep")) { if (argument.equals("")) { } else { try { Thread.sleep(Integer.parseInt(argument) * 1000); } catch (Exception e) { print(getCatch(e), stdout); return 2; } } }
    // | (TTY Emulation)
    // | (Logging)
    else if (mainCommand.equals("log")) { return MIDletLogs(argument, id, stdout); }
    else if (mainCommand.equals("logcat")) { print(logs, stdout); }
    // | (Permission Nodes)
    else if (mainCommand.equals("chmod")) { 
        if (argument.equals("")) { } 
        else if (argument.equals("*")) { return processCommand("chmod http socket file prg", false, id, pid, stdout, scope); }
        else { 
            Hashtable NODES = parseProperties("http=javax.microedition.io.Connector.http\nsocket=javax.microedition.io.Connector.socket\nfile=javax.microedition.io.Connector.file\nprg=javax.microedition.io.PushRegistry"); 
            
            int STATUS = 0; 
            for (int i = 0; i < args.length; i++) {
                String NODE = (String) NODES.get(args[i]);
                
                if (NODES.containsKey(args[i])) { 
                    try { 
                        if (args[i].equals("http")) { ((HttpConnection) Connector.open("http://google.com")).close(); } 
                        else if (args[i].equals("socket")) { ((SocketConnection) Connector.open(env("socket://" + ((attributes.containsKey("REPO") && !(env("$REPO").equals("") && !(env("$REPO").equals("$REPO"))) ? "$REPO" : "1.1.1.1:53"))))).close(); } 
                        else if (args[i].equals("file")) { FileSystemRegistry.listRoots(); } 
                        else if (args[i].equals("prg")) { PushRegistry.registerAlarm(getClass().getName(), System.currentTimeMillis() + 1000); } 
                    } 
                    catch (Exception e) { STATUS = (e instanceof SecurityException) ? 13 : (e instanceof IOException) ? 1 : 3; } 
                } 
                else { print("chmod: " + args[i] + ": not found", stdout); return 127; } 
                
                if (STATUS == 0) { MIDletLogs("add info Permission '" + NODE + "' granted", id, stdout); } 
                else if (STATUS == 1) { MIDletLogs("add debug Permission '" + NODE + "' granted with exceptions", id, stdout); } 
                else if (STATUS == 13) { MIDletLogs("add error Permission '" + NODE + "' denied", id, stdout); } 
                else if (STATUS == 3) { MIDletLogs("add warn Unsupported API '" + NODE + "'", id, stdout); } 
                
                if (STATUS > 1) { break; } 
            }

            return STATUS; 
        } 
    }
    // | (Services)
    else if (mainCommand.equals("trap")) {
        if (argument.equals("")) {
            Hashtable signals = (Hashtable) getobject(pid, "signals");
            if (signals != null && !signals.isEmpty()) {
                for (Enumeration signalKeys = signals.keys(); signalKeys.hasMoreElements();) {
                    String signal = (String) signalKeys.nextElement();
                    print(signal + "\t" + ((String) signals.get(signal)), stdout);
                }
            } else { print("No traps defined", stdout); }
        } 
        else if (pid.equals("1")) { print("Permission denied!", stdout); return 13; } 
        else {
            if (args.length < 2) { print("trap: usage: trap \"command\" SIGNAL", stdout); return 2; }
            
            args[1] = args[1].toUpperCase();
            if (args[1].equals("TERM")) {
                Hashtable signals = (Hashtable) getobject(pid, "signals");
                if (signals == null) { signals = new Hashtable(); getprocess(pid).put("signals", signals); }
                
                if (args[0].equals("-") || args[0].equals("true") || args[0].equals("")) {
                    signals.remove(args[1]);
                    if (signals.isEmpty()) { getprocess(pid).remove("signals"); }
                } else { signals.put(args[1], args[0]); }
            } else {
                print("trap: " + args[1] + ": invalid signal specification", stdout);
            }
        }
    } 
    else if (mainCommand.equals("svchost")) {
        if (argument.equals("")) { }
        else if (argument.equals("set")) { print("svchost: set: in dev", stdout); }
        else {
            if (args.length < 2) { print("svchost [pid] [request]", stdout); return 2; }
            else if (sys.containsKey(args[0])) {
                Hashtable proc = (Hashtable) sys.get(args[0]);
                if (proc.containsKey("lua") && proc.containsKey("handler")) {
                    Lua lua = (Lua) proc.get("lua");
                    Vector arg = new Vector(); arg.addElement(args[1]); arg.addElement("shell"); arg.addElement(pid); arg.addElement(id);
                    Object response = null;

                    try { response = ((Lua.LuaFunction) proc.get("handler")).call(arg); }
                    catch (Exception e) { print(getCatch(e), stdout); return 1; } 
                    catch (Error e) { if (e.getMessage() != null) { print(e.getMessage(), stdout); } return lua.status; }
                } else { print("svchost: " + args[0] + ": not a service", stdout); return 2; }
            } else { print("svchost: " + args[0] + ": not found", stdout); return 127; }
        }
    }
    // | (Informations)
    //else if (mainCommand.equals("hostname")) { return processCommand(argument.equals("") ? "echo $HOSTNAME" : "set HOSTNAME=" + getCommand(argument), false, id, pid, stdout, scope); }
    else if (mainCommand.equals("hostid")) { String DATA = env("$TYPE$CONFIG$PROFILE"); int HASH = 7; for (int i = 0; i < DATA.length(); i++) { HASH = HASH * 31 + DATA.charAt(i); } print(Integer.toHexString(HASH).toLowerCase(), stdout); }
    // |
    // | -=-=-=-=-=-=-=-=-=-=-
    // API 002 - X Server
    // | (Client)
    //else if (mainCommand.equals("xterm")) { display.setCurrent(xterm); }
    // | (Screens)
    //else if (mainCommand.equals("warn")) { return warn(xterm.getTitle(), argument); }
    //else if (mainCommand.equals("view")) { if (argument.equals("")) { } else { viewer(extractTitle(env(argument), xterm.getTitle()), html2text(env(argument))); } }
    // | (Window Modificators)
    //else if (mainCommand.equals("title")) { xterm.setTitle(argument.equals("") ? env("OpenTTY $VERSION") : argument.equals("hide") ? null : argument); }
    // |
    // | -=-=-=-=-=-=-=-=-=-=-
    // API 003 - File System
    // | (Structure)
    // | (Listings)
    // | (Navigation)
    //else if (mainCommand.equals("pwd")) { print((String) scope.get("PWD"), stdout); }
    else if (mainCommand.equals("cd")) { 
        String pwd = (String) scope.get("PWD");
        if (argument.equals("") && mainCommand.equals("cd")) { scope.put("PWD", "/home/"); } 
        else if (argument.equals("")) { print(readStack(scope) == null || readStack(scope).length() == 0 ? "pushd: missing directory" : readStack(scope), stdout); }
        else if (argument.equals("..")) { 
            if (((String) scope.get("PWD")).equals("/")) { return 0; } 

            int lastSlashIndex = pwd.lastIndexOf('/', pwd.endsWith("/") ? pwd.length() - 2 : pwd.length() - 1); 
            scope.put("PWD", (lastSlashIndex <= 0) ? "/" : pwd.substring(0, lastSlashIndex + 1)); 
        } 
        else { 
            String TARGET = argument.startsWith("/") ? argument : (pwd.endsWith("/") ? pwd + argument : pwd + "/" + argument); 
            if (!TARGET.endsWith("/")) { TARGET += "/"; } 
            if (fs.containsKey(TARGET)) { scope.put("PWD", TARGET); } 

            else if (TARGET.startsWith("/mnt/")) { 
                try { 
                    FileConnection fc = (FileConnection) Connector.open("file:///" + TARGET.substring(5), Connector.READ); 
                    if (fc.exists() && fc.isDirectory()) { scope.put("PWD", TARGET); } 
                    else { print(mainCommand + ": " + basename(TARGET) + ": not " + (fc.exists() ? "a directory" : "found"), stdout); return 127; } 

                    fc.close(); 
                } 
                catch (IOException e) { 
                    print(mainCommand + ": " + basename(TARGET) + ": " + getCatch(e), stdout); 

                    return 1; 
                } 
            }
            else { print(mainCommand + ": " + basename(TARGET) + ": not accessible", stdout); return 127; } 

        }
    }
    // | (Tools)
    else if (mainCommand.equals("rm")) { if (argument.equals("")) { } else { for (int i = 0; i < args.length; i++) { int STATUS = deleteFile(args[i], id, stdout); if (STATUS != 0) { return STATUS; } } } }
    else if (mainCommand.equals("touch")) { if (argument.equals("")) { } else { for (int i = 0; i < args.length; i++) { int STATUS = write(argument, "", id); if (STATUS != 0) { return STATUS; } } } }
    else if (mainCommand.equals("mkdir")) { if (argument.equals("")) { } else { argument = argument.endsWith("/") ? argument : argument + "/"; argument = argument.startsWith("/") ? argument : ((String) scope.get("PWD")) + argument; if (argument.startsWith("/mnt/")) { try { FileConnection CONN = (FileConnection) Connector.open("file:///" + argument.substring(5), Connector.READ_WRITE); if (!CONN.exists()) { CONN.mkdir(); CONN.close(); } else { print("mkdir: " + basename(argument) + ": found", stdout); } CONN.close(); } catch (Exception e) { print(getCatch(e), stdout); return (e instanceof SecurityException) ? 13 : 1; } } else if (argument.startsWith("/home/") || argument.startsWith("/tmp/")) { print("Unsupported API", stdout); return 3; } else if (argument.startsWith("/")) { print("read-only storage", stdout); return 5; } } }
    else if (mainCommand.equals("cp")) {
        if (argument.equals("")) { print("cp: missing [origin]", stdout); } 
        else {
            try {
                String origin = args[0], target = (args.length > 1 && !args[1].equals("")) ? args[1] : origin + "-copy";

                InputStream in = getInputStream(origin.startsWith("/") ? origin : ((String) scope.get("PWD")) + origin);
                if (in == null) { print("cp: " + origin + ": not found", stdout); return 1; }

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] tmpBuf = new byte[4096];
                int len;
                while ((len = in.read(tmpBuf)) != -1) { buffer.write(tmpBuf, 0, len); }
                in.close();

                return write(target.startsWith("/") ? target : ((String) scope.get("PWD")) + target, buffer.toByteArray(), id);
            } catch (Exception e) { print(getCatch(e), stdout); return e instanceof SecurityException ? 13 : 1; }
        }
    }
    else if (mainCommand.equals("du")) { 
        if (argument.equals("")) { } 
        else { 
            try {
                InputStream in = getInputStream(argument); 
                if (in == null) { print("du: " + basename(argument) + ": not found", stdout); return 127; } 
                else { print("" + in.available(), stdout); } 
            } catch (Exception e) { print(getCatch(e), stdout); return e instanceof SecurityException ? 13 : 1; }
        } 
    }
    // | (File)
    else if (mainCommand.equals("cat")) { if (argument.equals("")) { } else { for (int i = 0; i < args.length; i++) { print(getcontent(args[i], scope), stdout); } } }
    else if (mainCommand.equals("read")) { if (argument.equals("") || args.length < 2) { return 2; } else { attributes.put(args[0], getcontent(args[1], scope)); } }
    else if (mainCommand.equals("head")) { if (argument.equals("")) { } else { String CONTENT = getcontent(args[0], scope); String[] LINES = split(CONTENT, '\n'); int COUNT = Math.min(args.length > 1 ? getNumber(args[1], 10, null) : 10, LINES.length); for (int i = 0; i < COUNT; i++) { print(LINES[i], stdout); } } }
    else if (mainCommand.equals("tail")) { if (argument.equals("")) { } else { String CONTENT = getcontent(args[0], scope); String[] LINES = split(CONTENT, '\n'); int COUNT = args.length > 1 ? getNumber(args[1], 10, null) : 10; COUNT = Math.max(0, LINES.length - COUNT); for (int i = COUNT; i < LINES.length; i++) { print(LINES[i], stdout); } } }
    else if (mainCommand.equals("diff")) { if (argument.equals("") || args.length < 2) { return 2; } else { String[] LINES1 = split(getcontent(args[0], scope), '\n'), LINES2 = split(getcontent(args[1], scope), '\n'); int MAX_RANGE = Math.max(LINES1.length, LINES2.length); for (int i = 0; i < MAX_RANGE; i++) { String LINE1 = i < LINES1.length ? LINES1[i] : "", LINE2 = i < LINES2.length ? LINES2[i] : ""; if (!LINE1.equals(LINE2)) { print("--- Line " + (i + 1) + " ---\n< " + LINE1 + "\n" + "> " + LINE2, stdout); } if (i > LINES1.length || i > LINES2.length) { break; } } } }
    else if (mainCommand.equals("wc")) { if (argument.equals("")) { } else { int MODE = args[0].indexOf("-c") != -1 ? 1 : args[0].indexOf("-w") != -1 ? 2 : args[0].indexOf("-l") != -1 ? 3 : 0; if (MODE != 0) { argument = join(args, " ", 1); } String CONTENT = getcontent(argument, scope), FILENAME = basename(argument); int LINES = 0, WORDS = 0, CHARS = CONTENT.length(); String[] LINE_ARRAY = split(CONTENT, '\n'); LINES = LINE_ARRAY.length; for (int i = 0; i < LINE_ARRAY.length; i++) { String[] WORD_ARRAY = split(LINE_ARRAY[i], ' '); for (int j = 0; j < WORD_ARRAY.length; j++) { if (!WORD_ARRAY[j].trim().equals("")) { WORDS++; } } } print(MODE == 0 ? LINES + "\t" + WORDS + "\t" + CHARS + "\t" + FILENAME : MODE == 1 ? CHARS + "\t" + FILENAME : MODE == 2 ? WORDS + "\t" + FILENAME : LINES + "\t" + FILENAME, stdout); } }
    // | (Utilities)
    else if (mainCommand.equals("basename")) { print(basename(argument), stdout); }
    // | (Informations)
    else if (mainCommand.equals("fdisk")) { return processCommand("ls /mnt/", false, id, pid, stdout, scope); }
    else if (mainCommand.equals("lsblk")) { if (argument.equals("") || argument.equals("-x")) { print(replace("MIDlet.RMS.Storage", ".", argument.equals("-x") ? ";" : "\t"), stdout); } else { print("lsblk: " + argument + ": not found", stdout); return 127; } }
    // | (Archive Structure Management)
    else if (mainCommand.equals("cache")) { if (argument.equals("")) { print("Cache: " + (useCache ? "enabled (" + cache.size() + " items)" : "disabled"), stdout); } else if (argument.equals("on")) { useCache = true; } else if (argument.equals("off")) { useCache = false; cache = new Hashtable(); } else if (argument.equals("clear")) { cache = new Hashtable(); } else { print("cache: " + args[0] + ": not found", stdout); return 127; } } 
    else if (mainCommand.equals("rmsfix")) {
        if (argument.equals("")) { }
        else if (id != 0) { print("Permission denied!", stdout); return 13; }
        else if (args[0].equals("read")) { if (args.length < 2) { return 2; } else { args[1] = args[1].endsWith("/") ? args[1] : args[1] + "/"; print(loadRMS("OpenRMS", args[1].equals("/bin/") ? 3 : args[1].equals("/etc/") ? 5 : 4), stdout); } }
        else if (args[0].equals("swap")) { if (args.length < 3) { return 2; } else { write(args[2].startsWith("/") ? args[2] : ((String) scope.get("PWD")) + args[2], loadRMS("OpenRMS", args[1].equals("/bin/") ? 3 : 4), id); } }
        else if (args[0].startsWith("/")) {
            args[0] = args[0].endsWith("/") ? args[0] : args[0] + "/";
            if (args[0].equals("/bin/") || args[0].equals("/etc/") || args[0].equals("/lib/")) { return writeRMS("OpenRMS", new byte[0], args[0].equals("/bin/") ? 3 : args[1].equals("/etc/") ? 5 : 4); }
            else { print("rmsfix: " + args[0] + ": not found", stdout); return 127; }
        }
        else { print("rmsfix: " + args[0] + ": not found", stdout); return 127; }
    }
    // | (Codec)
    else if (mainCommand.equals("audio")) { return audio(argument, id, pid, stdout, scope); }
    // |
    // | -=-=-=-=-=-=-=-=-=-=-
    // API 004 - Network
    // | (Client)
    else if (mainCommand.equals("ip")) {

    }
    // | (Interfaces)
    else if (mainCommand.equals("gaddr")) { return GetAddress(argument, id, pid, stdout, scope); }
    else if (mainCommand.equals("who")) { print("PORT\tADDRESS", stdout); Hashtable sessions = (Hashtable) getobject("1", "sessions"); boolean all = argument.indexOf("-a") != -1; for (Enumeration KEYS = sessions.keys(); KEYS.hasMoreElements();) { String PORT = (String) KEYS.nextElement(), ADDR = (String) sessions.get(PORT); if (!all && ADDR.equals("nobody")) { } else { print(PORT + "\t" + ADDR, stdout); } } }
    else if (mainCommand.equals("ifconfig")) { if (argument.equals("")) { argument = "1.1.1.1:53"; } try { SocketConnection CONN = (SocketConnection) Connector.open("socket://" + argument); print(CONN.getLocalAddress(), stdout); CONN.close(); } catch (Exception e) { print("null", stdout); return 101; } }
    else if (mainCommand.equals("netstat")) { 
        if (argument.equals("") || args[0].equals("-a")) {
            print("Active Connections:\nPID\tName\tLocal\tRemote\tState", stdout);
            
            for (Enumeration keys = sys.keys(); keys.hasMoreElements();) {
                String pid = (String) keys.nextElement();
                Hashtable proc = (Hashtable) sys.get(pid);
                String procName = (String) proc.get("name");
                
                if (proc.containsKey("socket")) {
                    String state = "ESTABLISHED", localAddr = "N/A", remoteAddr = "N/A";
                    
                    try { if (proc.get("socket") != null) { SocketConnection conn = (SocketConnection) proc.get("socket"); localAddr = conn.getLocalAddress() + ":" + conn.getLocalPort(); remoteAddr = conn.getAddress() + ":" + conn.getLocalPort(); } } 
                    catch (Exception e) { }
                    
                    print(pid + "\t" + procName + "\t" + localAddr + "\t" + remoteAddr + "\t" + state, stdout);
                }
            }
            
            print("\nActive Servers:\nPID\tType\tPort\tStatus", stdout);
            
            Hashtable sessions = (Hashtable) getobject("1", "sessions");
            for (Enumeration ports = sessions.keys(); ports.hasMoreElements();) {
                String port = (String) ports.nextElement(), status = sessions.get(port).equals("nobody") ? "LISTENING" : "ESTABLISHED";
                
                for (Enumeration pids = sys.keys(); pids.hasMoreElements();) {
                    String pid = (String) pids.nextElement();
                    Hashtable proc = (Hashtable) sys.get(pid);
                    if (proc.get("port") != null && proc.get("port").equals(port)) { print(pid + "\tbind\t" + port + "\t" + status, stdout); }
                }
            }
        } else if (args[0].equals("-c")) {
            if (args.length < 2) { return 2; }

            if (sys.containsKey(args[1])) {
                
            } else {
                print("netstat: " + args[1] + ": not found", stdout);
            }
        } else {
            int STATUS = 0; 
            try { 
                HttpConnection CONN = (HttpConnection) Connector.open(!argument.startsWith("http://") && !argument.startsWith("https://") ? "http://" + argument : argument); 
                CONN.setRequestMethod(HttpConnection.GET); 
                if (CONN.getResponseCode() == HttpConnection.HTTP_OK) { } 
                else { STATUS = 101; } CONN.close(); 
            } 
            catch (SecurityException e) { STATUS = 13; } 
            catch (Exception e) { STATUS = 101; } 
            
            print(STATUS == 0 ? "true" : "false", stdout); 
            return STATUS; 
        }
        
    }
    // | (Device API)
    else if (mainCommand.equals("call")) { if (argument.equals("")) { } else { try { platformRequest("tel:" + argument); } catch (Exception e) { } } }
    else if (mainCommand.equals("open")) { if (argument.equals("")) { } else { try { platformRequest(argument); } catch (Exception e) { print("open: " + argument + ": not found", stdout); return 127; } } }
    // | (PushRegistry & Wireless)
    else if (mainCommand.equals("prg")) { if (argument.equals("")) { argument = "5"; } try { PushRegistry.registerAlarm(getArgument(argument).equals("") ? "OpenTTY" : getArgument(argument), System.currentTimeMillis() + Integer.parseInt(getCommand(argument)) * 1000); } catch (ClassNotFoundException e) { print("prg: " + getArgument(argument) + ": not found", stdout); return 127; } catch (NumberFormatException e) { print(getCatch(e), stdout); return 2; } catch (Exception e) { print(getCatch(e), stdout); return 3; } }
    else if (mainCommand.equals("wrl")) { String ID = System.getProperty("wireless.messaging.sms.smsc"); if (ID == null) { print("Unsupported API", stdout); return 3; } else { print(ID, stdout); } }
    // |
    // | -=-=-=-=-=-=-=-=-=-=-
    // Lua J2ME
    else if (mainCommand.equals("lua")) { 
        if (argument.equals("")) { }
        if (javaClass("Lua") == 0) { 
            Lua lua = new Lua(this, id, stdout, scope); 

            Hashtable arg = new Hashtable();
            String source, code;
            if (args[0].equals("-e")) { source = "stdin"; code = argument.substring(3).trim(); arg.put(new Double(0), "/dev/stdin"); } 
            else { source = args[0]; code = getcontent(source, scope); arg.put(new Double(0), source); for (int i = 1; i < args.length; i++) { arg.put(new Double(i), args[i]); } }

            if (code.trim().equals("")) { return 0; }
            
            return (Integer) lua.run(source, code, arg).get("status"); 
        } 
        else { print("This MIDlet Build don't have Lua", stdout); return 3; } 
    }
    // |
    // |
    else if (mainCommand.equals("@exec")) { commandAction(EXECUTE, display.getCurrent()); }
    else if (mainCommand.equals("@alert")) { display.vibrate(argument.equals("") ? 500 : getNumber(argument, 0, stdout) * 100); }
    else if (mainCommand.startsWith("@")) { print("mod: " + mainCommand.substring(1) + ": not found", stdout); }
    // |
    else if (mainCommand.equals("!")) { print(env("main/$RELEASE"), stdout); }
    else if (mainCommand.equals("!!")) { stdin.setString((argument.equals("") ? "" : argument + " ") + getLastHistory()); } 
    else if (mainCommand.equals(".")) { return run(argument, args, id, pid, stdout, scope); }
    // |
    else { print(mainCommand + ": not found", stdout); return 127; }

    return 0;
}

// Good morning, afternoon, evening for everybody
// Blackout
// WTF WAS THAT
// God forbid me I will never lost
// Saturday = Saturn day
// Only to confirm commit streak, lol!
// New week yeah
// On school, but dont have class
// I am a Time Traveler
// Coding 2 live
// Go around
// Weekend bro
// Saturn day again