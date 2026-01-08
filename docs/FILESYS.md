# 📁 OpenTTY File System

The OpenTTY file system is a **virtual file system** that provides a unified interface to access different types of storage resources within the J2ME environment. It combines **RMS storage**, **device file systems**, **virtual directories**, and **temporary storage** into a single hierarchical structure.

## 🗂️ File System Structure

### 📍 Root Directories

```
/
├── /home/          # 📝 RMS Record Stores (User Files)
├── /tmp/           # 🗑️ Temporary In-Memory Storage
├── /mnt/           # 💾 Device File System (JSR-75)
├── /bin/           # ⚡ Executable Scripts & Applications
├── /lib/           # 📚 Library Packages
├── /dev/           # 🔧 Special Devices
└── /res/           # 📦 Resource Files (JAR Resources)
```

## 🔧 Directory Details

### 🏠 `/home/` - User Storage
- **Storage**: RMS Record Stores
- **Permissions**: Read/Write for current user
- **Features**:
  - Persistent storage across sessions
  - File management via RecordStore API
  - User-specific data storage

### 🗑️ `/tmp/` - Temporary Storage
- **Storage**: In-memory Hashtable
- **Permissions**: Read/Write
- **Features**:
  - Volatile storage (lost on exit)
  - Fast access for temporary files
  - Session-specific data

### 💾 `/mnt/` - Device File System
- **Storage**: JSR-75 FileConnection API
- **Permissions**: Device-dependent
- **Features**:
  - Access to device file system
  - Directory listing and file operations
  - Cross-platform file access

### ⚡ `/bin/` - Executables
- **Storage**: Packaged scripts in RMS
- **Permissions**: Read-only (root can modify)
- **Features**:
  - Shell commands and scripts
  - Auto-execution via classpath
  - System utilities

### 📚 `/lib/` - Libraries
- **Storage**: Packaged libraries in RMS
- **Permissions**: Read-only (root can modify)
- **Features**:
  - Shared packages and modules
  - Function libraries
  - Extension packages

### 🔧 `/dev/` - Special Devices
- **Virtual Devices**:
  - `stdin` - Command input field
  - `stdout` - Output display
  - `null` - Empty device
  - `random` - Random number generator
  - `zero` - Zero byte source
  - `tty` - Terminal source


## 🛠️ File Operations

### 📋 Listing Files
```bash
ls /home/          # List user files
ls /mnt/           # List device roots
ls                 # View files in folder 
```

### 📄 Reading Files
```bash
cat /home/notes.txt     # Read RMS file
cat /mnt/root/file.txt  # Read device file
cat /tmp/buffer         # Read temporary file
```

### ✏️ Writing Files
```bash
sed s/pattern/new/ file         # Edit file content
echo "hi" > /home/file.txt      # Write "hi" in file
touch /tmp/tempfile             # Create temporary file
```

### 🗑️ Deleting Files
```bash
rm /home/oldfile.txt    # Delete RMS record
rm /mnt/file.txt       # Delete device file
rm /tmp/tempfile       # Remove temporary file
```

## 🔐 Permissions Model

### 👤 User Types
- **Root** (id=0): Full system access
- **Regular User** (id=1000): Limited access

### 🔒 Permission Rules
- `/home/`: User can read/write their own files
- `/tmp/`: User can read/write all files
- `/mnt/`: Device-dependent permissions
- `/bin/`, `/lib/`: Read-only for users, writable by root
- System files: Read-only for all users

## 🎮 Special Features

### 📁 Virtual Directories
- **Mount System**: Create virtual directories via configuration
- **Path Resolution**: Automatic path completion
- **Directory Stack**: `pushd`/`popd` for navigation

### 🔄 File Types & Detection
- **Automatic MIME type detection**
- **File extension mapping**
- **Binary vs Text classification**

### 💽 Storage Management
- **RMS Compression**: Multiple files in single RecordStore
- **Memory Management**: Automatic garbage collection
- **Storage Quotas**: Configurable limits

## 🚀 Usage Examples

### 🔍 Basic Navigation
```bash
pwd                    # Show current directory
cd /home/              # Change to home directory
ls -a                  # List all files (including hidden)
```

### 📊 File Information
```bash
file /home/document.txt    # Get file type information
wc /home/script.sh         # Count lines, words, characters
du /mnt/file.txt          # Get file size
```

### 🔄 Advanced Operations
```bash
cp /home/source.txt /tmp/copy.txt    # Copy files
mount /home/filesystem.conf          # Mount virtual directories
rmsfix swap /bin/ /home/backup/      # Backup system files
```

## ⚠️ Limitations

- **J2ME Constraints**: Limited by device capabilities
- **Storage Size**: RMS storage limitations apply
- **File Size**: Memory constraints for large files
- **Permissions**: Device security restrictions

The OpenTTY file system provides a **Unix-like experience** within J2ME constraints, enabling powerful file management capabilities on mobile devices!