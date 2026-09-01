# OpenTTY Roadmap

This document tracks the planned direction of OpenTTY. Items are grouped by
area; the most concrete, near-term items appear first.

## Releases

### 1.18.1 — current

- Network Controller

### 1.18.2 — planned (10/02/2026)

- Applications Menu-like `/bin/init`
- OpenSSH J2ME

---

## ELF Emulator

- [ ] Complete `fstat`, `stat`
- [ ] Network syscalls: `bind`, `listen`, `accept`, `recvfrom`, `sendto`,
      `getsockopt`, `setsockopt`
- [ ] Add support for `libc`

## Lua J2ME

- [ ] Coroutine support
- [ ] Metatable support
- [ ] `debug` table
- [ ] `collectgarbage` threshold mode
- [ ] `table.serialize` / `table.unserialize`
- [ ] Regular expressions
- [ ] LuaCanvas
- [ ] FTP API

## Kernel

- [ ] Pipes
- [ ] `bg` / `fg` commands
- [ ] Event handler
- [ ] Task scheduler
- [ ] Automatic update check
- [ ] Backup system
- [ ] Notify API
- [ ] Advanced scope
- [ ] Plugins
- [ ] Bluetooth API
- [ ] Power / backlight API
- [ ] ZIP-mountable filesystem
- [ ] Permission system
- [ ] Groups
- [ ] i18n translations

## X11

- [ ] Legacy screens
- [ ] Themes

## Boot Menu

**Title:** Setup Utility

**Screens:**
- About
- Menu
  - Boot Menu
  - Recovery Menu
