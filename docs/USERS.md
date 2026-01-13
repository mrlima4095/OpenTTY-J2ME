# 👥 User System Documentation - OpenTTY

OpenTTY implements a multi-user system with different permission levels and security features. The system manages users through a combination of MIDlet RecordStore persistence and runtime user tables.

## 🔐 User Types

### **👑 Root User**
- **User ID**: `0`
- **Username**: `root`
- **Permissions**: Full system access
- **Special Characteristics**:
  - Can modify system files (`/bin/`, `/etc/`, `/lib/`)
  - Can kill any process
  - Can change passwords for any user
  - Can manage user accounts
  - Cannot be deleted

### **👤 Standard Users**
- **User ID**: `1000+`
- **Default User**: First created user gets ID `1000`
- **Permissions**: Limited access based on ownership
- **Characteristics**:
  - Can only modify their own files in `/home/`
  - Can only kill their own processes
  - Cannot modify system directories

## 📊 User Management Commands

### **User-related Functions in Lua**

#### `os.getuid()`
Returns the current user's ID.
```lua
uid = os.getuid([username])  -- Returns Double: 0 for root, 1000+ for users
```

#### `os.su(username, password)`
Switch user context.
```lua
status = os.su("root", "password123")  -- Returns 0 on success, 13 on failure
```

#### `java.midlet.uptime()`
Get system uptime (available to all users).
```lua
uptime = java.midlet.uptime()  -- Returns milliseconds since system start
```

#### **Kernel-level User Management** (Root only)
```lua
os.request(1, "useradd", "newuser")  -- Add new user
os.request(1, "userdel", "username") -- Delete user
os.request(1, "passwd", { old = "oldpass", new = "newpass"}) -- Change password
```

## 🔄 User Switching

### **Login Process**
1. System checks if credentials exist on first boot
2. If not, prompts for username/password creation
3. Credentials stored in `OpenRMS` RecordStore
4. Subsequent boots auto-login the created user (`id: 1000`)

### **SU Command Flow**
```lua
-- User tries to become root
if os.su("root", password) == 0 then
    print("Now root!")
else
    print("Permission denied")
end
```

## ⚠️ Security Notes

### **Restrictions**
- Only root can modify system binaries/configurations, if it's native filesystem
- Password changes require current password (or root privileges)
- User deletion requires root privileges

### **Recovery Options**
1. **Factory Reset**: Clears all user data via Recovery menu
2. **Password Reset**: Root can reset any password via kernel
3. **User Recreation**: Delete `OpenRMS` to trigger first-time setup

## 📝 Example User Session

```lua
-- Check current user
uid = os.getuid()
print("User ID:", uid)

-- Try privileged operation (fails if not root)
status = os.remove("/bin/systemfile")
if status == 13 then
    print("Permission denied - need root")
end

-- Switch to root
if os.su("root", "admin123") == 0 then
    print("Elevated to root privileges")
    -- Now can perform system operations
else
    print("Invalid credentials")
end

-- List all processes (shows owners)
procs = os.getproc()
for pid, name in pairs(procs) do
    print(pid, name)
end
```
