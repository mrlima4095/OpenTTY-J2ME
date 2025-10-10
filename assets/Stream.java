public class Stream {
        private boolean isOpen = true;
        public boolean verbose = false;

        private String id = "";
        private InputStream in;
        private OutputStream out;
        private SocketConnection socket;
        private RecordStore rms;

        public Stream(String filename) { 
            if (file == null || file.length() == 0) { isOpen = false; } 
            else { 
                this.id = filename; 

                if (filename.startsWith("/home/")) {
                    RecordStore rs = null;
                    try {
                        rs = RecordStore.openRecordStore(filename.substring(6), false);
                        if (rs.getNumRecords() > 0) { return new ByteArrayInputStream(rs.getRecord(1)); }
                    } finally { if (rs != null) { rs.closeRecordStore(); } }

                    return null;
                } 
                else if (filename.startsWith("/mnt/")) { return ((FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ)).openInputStream(); } 
                else if (filename.startsWith("/tmp/")) { return tmp.containsKey(filename = filename.substring(5)) ? new ByteArrayInputStream(((String) tmp.get(filename)).getBytes("UTF-8")) : null; } 
                else {
                    if (filename.startsWith("/dev/")) {
                        filename = filename.substring(5);
                        String content = filename.equals("random") ? String.valueOf(random.nextInt(256)) : filename.equals("stdin") ? stdin.getString() : filename.equals("stdout") ? stdout.getText() : filename.equals("null") ? "\r" : filename.equals("zero") ? "\0" : null;
                        if (content != null) { in = new ByteArrayInputStream(content.getBytes("UTF-8")); }

                        filename = "/dev/" + filename;
                    } 

                    in = getClass().getResourceAsStream(filename);
                }
            } 
        }
        public Stream(String filename, SocketConnection socket) { this.id = filename; this.socket = socket; try { this.in = socket.openInputStream(); this.out = socket.openOutputStream(); } catch (IOException e) { isOpen = false; } }
        public Stream(String filename, InputStream in, OutputStream out) { this.id = filename; this.in = in; this.out = out; }
    
        // Métodos de leitura
        public byte[] read(int bufferSize) {
            if (!isOpen || in == null) { return null; }
            try {
                byte[] buffer = new byte[bufferSize];
                int readBytes = in.read(buffer);
                if (readBytes < bufferSize) {
                    byte[] result = new byte[readBytes];
                    System.arraycopy(buffer, 0, result, 0, readBytes);
                    return result;
                }
                return buffer;
            } 
            catch (Exception e) { return null; }
        }

        public String read() {
            if (!isOpen || in == null) return "";
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[256];
                int r;
                while ((r = in.read(buffer)) != -1) {
                    baos.write(buffer, 0, r);
                }
                return new String(baos.toByteArray(), "UTF-8");
            } catch (Exception e) {
                return "";
            }
        }


        public int write(byte[] data) { 
            if (id.startsWith("/mnt/")) { try { FileConnection CONN = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ_WRITE); if (!CONN.exists()) { CONN.create(); } OutputStream OUT = CONN.openOutputStream(); OUT.write(data); OUT.flush(); OUT.close(); CONN.close(); } catch (Exception e) { echoCommand(getCatch(e)); return (e instanceof SecurityException) ? 13 : 1; } } 
            else if (id.startsWith("/home/")) { return writeRMS(id.substring(6), data, 1); } 
            else if (id.startsWith("/dev/")) { String filename = id.substring(5); if (filename.equals("")) { return 2; } else if (filename.equals("null")) { } else if (filename.equals("stdin")) { stdin.setString(new String(data)); } else if (filename.equals("stdout")) { stdout.setText(new String(data)); } else { echoCommand("read-only storage"); return 5; } }
            else if (id.startsWith("/tmp/")) { String filename = id.substring(5); if (filename.equals("")) { return 2; } else { tmp.put(filename, new String(data)); } }
            else if (id.startsWith("/")) { echoCommand("read-only storage"); return 5; } 
            else { return writeRMS(path + filename, data); } return 0; }

        public int remove() {
            int status = 0;
            String message = "", filename = id;

            try {
                if (filename.startsWith("/home/")) { 
                    filename = filename.substring(6); 
                    
                    if (filename.equals("")) { status = 2; }
                    if (filename.equals("OpenRMS")) { status = 13; message = "Permission denied!"; } 
                    
                    RecordStore.deleteRecordStore(filename); 
                }
                else if (filename.startsWith("/mnt/")) { 
                    FileConnection CONN = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ_WRITE); 
                    if (CONN.exists()) { CONN.delete(); } 
                    else { status = 127; message = "rm: " + basename(filename) + ": not found"; } 
                    
                    CONN.close(); 
                } 
                else if (filename.startsWith("/tmp/")) {
                    filename = filename.substring(5);
                    if (filename.equals("")) { status = 2; }
                    else if (tmp.containsKey(filename)) { tmp.remove(filename); }
                    else { status = 127; message = "rm: " + filename + ": not found"; }
                }
                else if (filename.startsWith("/")) { status = 5; message = "read-only storage"; }
            } 
            catch (Exception e) { message = getCatch(e); status = e instanceof SecurityException ? 13 : 1; } 

            if (verbose && !message.equals("")) { echoCommand(message); }

            return status; 
        }

        public int close() { try { if (in != null) in.close(); } catch (Exception e) { } try { if (out != null) out.close(); } catch (Exception e) { } try { if (rms != null) rms.close(); } catch (Exception e) { } try { if (socket != null) socket.close(); } catch (Exception e) { } isOpen = false; return 0; }
        public boolean status() { return isOpen; }
        

        public String read(String filename) {
            try {
                if (filename.startsWith("/tmp/")) {
                    return tmp.containsKey(filename = filename.substring(5)) ? (String) tmp.get(filename) : "";
                }
                InputStream is = readRaw(filename);
                if (is == null) return "";
                InputStreamReader reader = new InputStreamReader(is, "UTF-8");
                StringBuffer sb = new StringBuffer();
                int ch;
                while ((ch = reader.read()) != -1) sb.append((char) ch);
                reader.close();
                is.close();
                return filename.startsWith("/home/") ? sb.toString() : env(sb.toString());
            } catch (Exception e) {
                return "";
            }
        }

        public Image readImg(String filename) {
            try {
                InputStream is = readRaw(filename);
                Image img = Image.createImage(is);
                is.close();
                return img;
            } catch (Exception e) {
                return Image.createImage(16, 16);
            }
        }

        public static int writeRMS(String filename, byte[] data, int index) {
            try {
                RecordStore rs = RecordStore.openRecordStore(filename, true);
                while (rs.getNumRecords() < index) { rs.addRecord("".getBytes(), 0, 0); }
                rs.setRecord(index, data, 0, data.length);
                if (rs != null) rs.closeRecordStore();
            } 
            catch (Exception e) { return 1; }
            return 0;
        }

    }