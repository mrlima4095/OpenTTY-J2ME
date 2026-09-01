# OpenTTY File System

OpenTTY's **virtual file system** provides a unified, hierarchical interface over
several storage backends: **RMS RecordStores** (`/home/`, `/bin/`, `/etc/`,
`/lib/`), the **device file system** (`/mnt/`, JSR-75), **virtual files**
(`/proc/`, `/dev/`), and **in-memory storage** (`/tmp/`).

## Layout

```
/
├── /home/   # RMS RecordStores — user files (persistent)
├── /root/   # Protected home of the root user (RMS)
├── /tmp/    # Temporary in-memory storage (volatile)
├── /mnt/    # Real device file system (JSR-75)
├── /bin/    # Executables & scripts (packaged in RMS)
├── /etc/    # Configuration files (RMS)
├── /lib/    # Lua libraries & modules (RMS)
├── /dev/    # Virtual devices
└── /proc/   # Virtual process / system-info files
```

## Directory Details

### `/home/` — User storage

- **Backend**: RMS RecordStores
- **Permissions**: read/write for the current user
- Persistent across sessions; user-specific data.

### `/root/` — Root home

- **Backend**: RMS (OpenRMS index 6)
- **Permissions**: root-only (UID 0). Regular users cannot read, write, or enter it.

### `/tmp/` — Temporary storage

- **Backend**: in-memory table
- **Permissions**: read/write for all users
- Volatile — lost when the MIDlet exits.

### `/mnt/` — Device file system

- **Backend**: JSR-75 `FileConnection` API
- **Permissions**: device-dependent
- Access to the real device's files and directories.

### `/bin/`, `/etc/`, `/lib/` — System directories

- **Backend**: packaged scripts stored in RMS
- **Permissions**: read-only for regular users; writable by root
- VFS **subdirectories** (e.g. `/bin/tools/`) are supported at any depth; each
  maps to its own page of the `OpenRMS` store via a stable path hash, and they
  persist across restarts into `/etc/vfs.conf`.

### `/dev/` — Virtual devices

- `stdin` — command input
- `stdout` — output display
- `null` — null device
- `random` — random bytes
- `zero` — zero bytes
- `tty` — terminal source

### `/proc/` — Virtual process files

- `cpuinfo`, `meminfo`, `uptime`, `version`
- per-process dirs `/proc/<pid>/` containing `cmdline`, `comm`, `stat`, `status`
- Regular users only see their own processes; root sees all.

## File Operations

```bash
ls /home/                 # List user files
ls /mnt/                  # List device roots

cat /home/notes.txt       # Read an RMS file
cat /tmp/buffer           # Read a temporary file

echo "hi" > /home/f.txt   # Write to a file
touch /tmp/tempfile       # Create a temporary file

mkdir /home/projects      # Create a directory
rm /home/oldfile.txt      # Delete a file
rm -r /bin/tools          # Remove a VFS subdirectory (root)
```

## Permissions Model

| Area | Regular user | Root (UID 0) |
|------|--------------|--------------|
| `/home/` | own files read/write | all |
| `/tmp/` | read/write | all |
| `/mnt/` | device-dependent | device-dependent |
| `/bin/`, `/etc/`, `/lib/` | read-only | read/write (+subdirs) |
| `/root/` | denied | read/write |
| `/proc/` | own processes only | all |

## Special Features

- **VFS subdirectories**: `/bin|etc|lib/...` folders persist into `/etc/vfs.conf`
- **Mount system**: create virtual directories via configuration
- **Path resolution**: automatic path completion
- **Storage management**: multiple physical files in a single RecordStore; memory
  managed through garbage collection

## Usage Examples

```bash
pwd                    # Show current directory
cd /home/              # Change to home directory
ls -a                  # List all files (including hidden)

cp /home/source.txt /tmp/copy.txt   # Copy files
rmsfix swap /bin/ /home/backup/     # Backup system files
```

## Limitations

- J2ME device capabilities constrain the filesystem
- RMS storage size limits apply
- Large files are limited by available memory
- Device security may restrict some operations

> `io.dirs(path)` returns entries only for `/tmp/`, `/mnt/<sub>`, and exactly
> `/bin/`, `/etc/`, `/lib/`, `/home/`. Any other path yields an empty table.
