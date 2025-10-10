    public class Stream {
        private boolean isOpen = true;
        public boolean verbose = false;
        public int __index = 1;
        private String name = "";

        private InputStream in;
        private OutputStream out;
        private SocketConnection socket;
        private FileConnection fs;
        private RecordStore rms;

        public Stream(String filename, boolean verbose) {
            if (filename == null || filename.length() == 0) { isOpen = false; return; }

            this.name = filename;
            this.verbose = verbose;

            try {
                if (filename.startsWith("/home/")) { filename = filename.substring(6); try { rms = RecordStore.openRecordStore(filename, false); } catch (RecordStoreNotFoundException e) { rms = null; } }
                else if (filename.startsWith("/mnt/")) { fs = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ_WRITE); if (fs.exists()) { } else { fs.close(); fs = null; } }
                else if (filename.startsWith("/tmp/")) { filename = filename.substring(5); if (tmp.containsKey(filename)) { byte[] buf = ((String) tmp.get(key)).getBytes("UTF-8"); in = new ByteArrayInputStream(buf); } else { isOpen = false; } }
                else if (filename.startsWith("/bin/") || filename.startsWith("/lib/")) {

                }
                else {
                    if (filename.startsWith("/dev/")) {
                        String dev = filename.substring(5), content = dev.equals("random") ? String.valueOf(random.nextInt(256)) : dev.equals("stdin") ? stdin.getString() : dev.equals("stdout") ? stdout.getText() : dev.equals("null") ? "\r" : dev.equals("zero") ? "\0" : null;
                        if (content != null) { in = new ByteArrayInputStream(content.getBytes("UTF-8")); return; }
                    }

                    in = getClass().getResourceAsStream(filename);
                    if (in == null) { isOpen = false; }
                }
            } catch (Exception e) { isOpen = false; if (verbose) { echoCommand(getCatch(e)); } }
        }

        public Stream(String filename, InputStream in, OutputStream out, boolean verbose) {  this.name = filename; this.in = in; this.out = out; this.verbose = verbose; }
        public Stream(String filename, SocketConnection socket, boolean verbose) { this.name = filename; this.socket = socket; this.verbose = verbose; try { this.in = socket.openInputStream(); this.out = socket.openOutputStream(); } catch (Exception e) { isOpen = false; } }

        public String read() {
            if (!isOpen || in == null) { return ""; }

            try {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] tmp = new byte[256];
                int len;
                while ((len = in.read(tmp)) != -1) { buf.write(tmp, 0, len); }
                return new String(buf.toByteArray(), "UTF-8");
            } catch (Exception e) { if (verbose) { echoCommand(getCatch(e)); } return ""; }
        }

        public Image readImg() { try { return Image.createImage(in); } catch (Exception e) { return Image.createImage(16, 16); } }

        public int write(String data, int id) { return write(data.getBytes(), id); }
        public int write(byte[] data, int id) {
            if (!isOpen) { return 69; }

            try {
                if (name.startsWith("/home/")) {
                    while (store.getNumRecords() < __index) { store.addRecord("".getBytes(), 0, 0); } store.setRecord(index, data, 0, data.length); 
                } 
                else if (name.startsWith("/mnt/")) {
                    if (fs == null) {
                        fs = (FileConnection) Connector.open("file:///" + name.substring(5), Connector.READ_WRITE);
                        if (fs.exists()) { } else { fs.create(); }
                    }
                    if (out == null) { OutputStream out = fs.openOutputStream(); }
                    out.write(data); out.flush(); 
                }
                else if (name.startsWith("/tmp/")) { tmp.put(name.substring(5), new String(data)); return 0; }
                else { 
                    if (name.startsWith("/dev/")) {
                        String dev = name.substring(5);
                        if (dev.equals("null")) { return 0; }
                        else if (dev.equals("stdout")) { stdout.setText(new String(data)); return 0; } 
                        else if (dev.equals("stdin")) { stdin.setString(new String(data)); return 0; }
                    }

                    if (verbose) { echoCommand("read-only storage"); } return 5; 
                }
            } catch (Exception e) { if (verbose) { echoCommand(getCatch(e)); } return 1; }

            return 0;
        }

        public int remove() {
            try {
                if (name.startsWith("/home/")) {
                    String filename = name.substring(6);
                    if (filename.equals("OpenRMS")) {
                        if (verbose) { echoCommand("Permission denied!"); }
                        return 13;
                    }
                    RecordStore.deleteRecordStore(filename);
                }
                else if (name.startsWith("/mnt/")) {
                    FileConnection fc = (FileConnection) Connector.open("file:///" + name.substring(5), Connector.READ_WRITE);
                    if (fc.exists()) fc.delete();
                    else {
                        
                        return 127;
                    }
                    fc.close();
                }
                else if (name.startsWith("/tmp/")) {
                    String key = name.substring(5);
                    if (tmp.containsKey(key)) { tmp.remove(key); }
                    else {
                        if (verbose) { echoCommand("rm: " + key + ": not found"); }
                        return 127;
                    }
                }
                else { if (verbose) { echoCommand("read-only storage"); } return 5; }
            } 
            catch (Exception e) { if (verbose) { echoCommand(getCatch(e)); } return 1; }
            return 0;
        }

        public int close() {
            try {
                if (in != null) { in.close(); }
                if (out != null) { out.close(); }
                if (fs != null) { fs.close(); }
                if (rms != null) { rms.closeRecordStore(); }
                if (socket != null) { socket.close(); }
            } catch (Exception e) { if (verbose) { echoCommand(getCatch(e)); } return 1; }
            isOpen = false;
            return 0;
        }
    }