OpenTTY Java Edition 1.17  
Copyright (C) 2025 - Mr. Lima

---

The new OpenTTY

- OpenTTY now is totally controlled by Lua, shell and commands are provided by lua scripts and binaries on `/bin/`


## 🚀 Updates

### 📁 File System & Directories
- **Directory tree modified**
- User init script changed by lua script in `/home/.initrc` 
- File System optimized and new `/etc/fstab` format

### 🔒 Security
- **Fixed** Lua security error that allowed access to process **owner** field

### 🎨 Interface & Usability
- Default title for `view` command now matches Main Window title
- **Font Generator** enhanced
- Support for **Screen**, **List**, **Quest** and **Canvas** removed, usage **Lua Graphics API**

### ⚙️ Features & API
- **Lua** added function `ipairs`
- **Lua** added function `os.getcwd()` to read current working directory
- **Lua** functions `io.read` and `io.write` now require using `/dev/stdin` and `/dev/stdout`
- **Lua** added default library `java` for direct access and handling resources of Java VM
- **Lua** added function `graphics.render(file)` to get **Image** item
- **Lua** function `random(max)` moved to `math.random()` by default
- **Lua** function `require` searching module on `/lib/` if file not found in current working directory 
- **Lua** added new _type_ **Image**
- **Lua** added support for **methods** and `:` token
- **Lua** added minimal support for **metatables**
- **Lua** fixed `break` token not working in `while` and `repeat` loops
- **Lua** functions `socket.peer` and `socket.device` now returns **IP Address** and **Connection Port**
- **Lua** function `os.execute` sends commands to shell
- **Lua** added function `string.uuid()`
- **Lua** added function `os.getuid()` thats returns the ID of current running user
- MIDlet initial message now comes from `/etc/motd` file
- MIDlet is launched by `/etc/init` if an error occured here, it crashes
- New default environment key `SHELL`
- **Super-user** now allowed to write to `/bin/` (**Applications**), `/etc/` (**Settings**) and `/lib/` (**Package**)
- Fixed **MIDlet** login for user-only prompt

### 🏗️ Architecture
- Source code split into `OpenTTY.java` (Main Class), `Lua.java` (Lua Runtime) and `ELF.java` (ELF ARM 32 Emulator)
- Support for native OpenTTY applications have been removed, usage Lua
- **X Server** is not native anymore, install it with `yang`, native interfaces using Lua
- All commands written with Java have been removed

Native shell commands: 

- `exec [commands...]`: run more then 1 commands in a single prompt
- `gc`: run garbage collector
- `ps`: show running process
- `su [password]`: switch to root and default user
- `whoami`: prints user name
- `id`: prints id of logged user
- `exit`: exit from MIDlet (OpenTTY)
- `xterm`: display main screen
- `warn [title] [message]`: display an alert with title and message
- `title`: changes title of current screen
- `alias [name]=<expression>`: manage aliases
- `unalias [alias]`: delete an alias
- `env [key]`: prints environment key value
- `set [key]=[value]`: create an environment key
- `unset [key]`: delete an environment key
- `eval [expression]`: evaluate an expression on shell
- `echo [message]`: prints message
- `date`: prints current date-time
- `clear`: clears default stdout
- `pwd`: prints current path
- `cd [path]`: change current working directory
- `cat [file]`: prints file content
- `ls`: prints directory content
- `open [uri]`: request device API to open URI 
- `true`: do nothing, sucessfully
- `false`: do nothing, but fail

Programs included on `/bin/`:

- `curl`: make requests to web, socket and OpenTTY services
- `kill`: kill a process
- `lua`: lua interative terminall
- `rm`: remove a file or directory
- `sh`: shell
- `touch`: create or clear a file content
- `yang`: package manager


Commands that is already portable and can be installed with yang:

- `autogc`: program garbage collector to run periodically
- `cmatrix`: matrix effect
- `expr [expression]`: build a lua expression and prints result
- `find`:
- `grep`:
- `hash [file]`: prints hash code of file content 
- `head [file]`: prints first lines of a file
- `hostname`: prints and changes hostname
- `htop`: memory panel
- `jdb [commands]`: OpenTTY debuggers
- `nano [file]`: file editor
- `pastebin`: pastebin client
- `ping [url]`: pings a website
- `sed [expr] [file]`: string editor
- `sdk`: Lua SDK
- `sync`: check for updates of OpenTTY
- `uname`: informations about software and device
- `axrz`: ViaVersion client interface
- `shprxy [password]`: WebProxy server 
- `wget [url] [file]`: Download binaries

---

## 💡 Notes

- Files written in **/tmp/** will be deleted when MIDlet closes
- **OpenTTY API** official development programming language is now **Lua**
- A **Lua** service call `arg[1]` is argument `--deamon`
- In limited memory devices you can disable reading cache and Lua 
