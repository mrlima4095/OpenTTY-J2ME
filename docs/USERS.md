# OpenTTY User System

OpenTTY implements a multi-user model with different permission levels, managed
through a combination of MIDlet RecordStore persistence and runtime user tables.

## User Types

### Root User (`UID 0`)

- **Username**: `root`
- **Permissions**: full system access
- Can modify system files (`/bin/`, `/etc/`, `/lib/`) and create VFS subdirectories
- Can kill any process and manage all users
- Can change any user's password
- **Cannot be deleted**

### Standard Users (`UID 1000+`)

- The first created user gets `UID 1000`; later users get higher IDs
- Can only modify their own files in `/home/`
- Can only kill their own processes
- Cannot modify system directories (exit code `13` / permission denied)

## User Management

### Lua Functions

#### `os.getuid(...)`

Returns the current user's ID — `0` for root, `1000+` for regular users.

```lua
uid = os.getuid()
print("User ID:", uid)
```

#### `os.su(username, password)`

Switch the current user context.

```lua
status = os.su("root", "password123")  -- 0 on success, 13 on failure
```

#### Kernel-level management (root only)

```lua
os.request(1, "useradd", "newuser")                     -- add a user
os.request(1, "userdel", "username")                    -- delete a user
os.request(1, "passwd", { old = "old", new = "new" })   -- change password
```

### First Login

1. On first boot OpenTTY checks whether credentials exist.
2. If not, it prompts for a username and password.
3. Credentials are stored (hashed) in the `OpenRMS` RecordStore.
4. Subsequent boots auto-log in the created user (`UID 1000`).

### Shell

```bash
whoami              # current username
id                  # current user ID
su                  # become root (asks for password)
sudo <cmd>          # run a command as root
useradd <name>      # add a user (root)
userdel <name>      # delete a user (root)
passwd              # change password
pkg install <p>     # requires root
```

## Security Notes

- Only root can modify system directories or create VFS subdirectories under
  `/bin/`, `/etc/`, `/lib/`, `/root/`.
- Password changes require the current password (or root privileges).
- Deleting a user requires root privileges.
- `/root/` is inaccessible to regular users: entering it returns exit code `13`
  and the shell prints `cd: <dir>: permission denied`.

### Recovery Options

1. **Factory reset** — clears user data via the Recovery menu.
2. **Password reset** — root can reset any password via the kernel.
3. **User recreation** — delete the `OpenRMS` store to trigger first-time setup.

## Example Session

```lua
-- Check current user
print("User ID:", os.getuid())

-- Try a privileged operation
local status = os.remove("/bin/systemfile")
if status == 13 then
    print("Permission denied — need root")
end

-- Switch to root
if os.su("root", "admin123") == 0 then
    print("Elevated to root privileges")
else
    print("Invalid credentials")
end
```
