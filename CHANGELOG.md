OpenTTY Java Edition 1.17  
Copyright (C) 2025 - Mr. Lima

---

## 📋 Commands

- `file [filename]`: Displays information about files
- `id [user]`: Shows user ID
- `rmsfix [option]`: Fixes RMS archive structure

---

## 🚀 Updates

### 📁 File System & Directories
- **Directory tree modified**
- Initialization script changed to `.initrc`
- `.initrc` will be executed considering shebangs
- Fixed `pushd` invalid directories stacking

### 🔒 Security
- **Fixed** Lua security error that allowed access to process **owner** field
- **Lua** functions `io.read` and `io.write` now require using `/dev/stdin` and `/dev/stdout`
- **Removed** default parser commands `vnt`
- **Removed** symbolic link `/bin/java`
- **Fixed** security bug with `case user (any name) ...` command getting root permission

### 🎨 Interface & Usability
- Default title for `view` command now matches Main Window title
- **File Explorer** improved
- **Font Generator** enhanced
- Process viewer added new **Refresh** button
- **File Explorer** Delete button no longer requests to remove `..` directory

### ⚙️ Features & API
- **Lua** added function `os.getcwd()` to read current working directory
- **Lua Graphics API** - _BuildScreen_ can now build Images from all sources
- **Lua** added default library `java` for minimal direct access and handling of Java VM resources
- **Lua** added function `graphics.render(file)` to get **Image** item
- **Lua** added new _type_ **Image**
- **Screens** event listeners no longer passing root to `xterm` command call
- **Lua** added support for **methods** and `:` token
- **Lua** added minimal support for **metatables**
- **Lua** fixed `break` token not working in `while` and `repeat` loops
- MIDlet initial message now comes from `/etc/motd` file
- Merged build tools **Lua**, **Shell Script** and **Packages**; utility `.` considers _shebangs_ for building
- `cp` command now using raw _byte-array_
- `bind` command must read port from argument line, no default port in environment key anymore
- Errors in `/etc/init` now throw a _Kernel Panic_ and cause **MIDlet** termination
- New default environment key `SHELL`
- **Super-user** now allowed to write to `/bin/` (**Applications**) and `/lib/` (**Package**)
- Fixed **MIDlet** login for user-only prompt
- Added support for `list.default` that runs by default on all List choices
- `query` command no longer consuming until connection end

### 📦 Packages
- **JBuntu** updated to _1.4_
- **JBenchmark** updated for _1.3_ (Migrated to Lua)
- **Bind** process objects `InputStream` and `OutputStream` moved to keys `in` and `out`

### 🏗️ Architecture
- Source code split into `OpenTTY.java` (Main Class), `Lua.java` (Lua Runtime) and `MIDletCanvas.java` (Canvas Builder)

---

## 💡 Notes

- Files written in **/tmp/** will be deleted when MIDlet closes
- **OpenTTY API** official development programming language is now **Lua**
- In limited memory devices you can disable shell **PATH** search by running `stty classpath=false`
- Documentation available on Github Wiki page

---

## 🔌 API Nodes

- `devicefs`: Checks if device supports **JSR-75** (_Access to Device File System on `/mnt/`_)

---

## 📦 Available Packages

- `Default`: All OpenTTY features
- `Lite`: **Lua Runtime**, **GoBuster**, **Port Scanner** and **X11 Canvas Service** not available