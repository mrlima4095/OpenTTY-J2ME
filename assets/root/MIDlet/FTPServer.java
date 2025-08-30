import javax.microedition.*;
import javax.microedition.io.*;
import javax.bluetooth.*;

public class FTPServer implements Runnable {
    private String port;
    private SocketConnection clientSocket;
    private InputStream is;
    private OutputStream os;

    private ServerSocketConnection dataServerSocket = null;
    private SocketConnection dataConnection = null;
    private InputStream dataIs = null;
    private OutputStream dataOs = null;

    public FTPServer(String port) {
        this.port = (port == null || port.equals("") || port.equals("$PORT")) ? "21" : port;
        new Thread(this, "FTP").start();
    }

    public void run() {
        ServerSocketConnection serverSocket = null;
        try {
            serverSocket = (ServerSocketConnection) Connector.open("socket://:" + port);
            echoCommand("[+] listening at port " + port);
            MIDletLogs("add info Server listening at port " + port);

            start("ftp");
            while (trace.containsKey("ftp")) {
                boolean logged = false;
                String ftpUser = null;

                clientSocket = (SocketConnection) serverSocket.acceptAndOpen();
                echoCommand("[+] " + clientSocket.getAddress() + " connected");
                is = clientSocket.openInputStream();
                os = clientSocket.openOutputStream();

                send("220 Welcome to OpenTTY FTP");

                while (trace.containsKey("ftp")) {
                    String cmd = readLine();
                    if (cmd == null || cmd.length() == 0) continue;

                    String command = getCommand(cmd).toUpperCase();
                    String argument = getArgument(cmd);

                    if (command.equals("USER")) {
                        if (argument.equals(username)) {
                            ftpUser = argument;
                            send("331 Username OK, need password");
                        } else {
                            send("530 Invalid user");
                        }
                    } else if (command.equals("PASS")) {
                        if (ftpUser != null && ftpUser.equals(username)) {
                            send("230 Login successful");
                            logged = true;
                        } else {
                            send("530 Login incorrect");
                        }

                    } else if (!logged) {
                        send("530 Please login first");
                    } else if (command.equals("PWD")) {
                        send("257 \"" + path + "\" is current directory");
                    } else if (command.equals("CWD")) {
                        String cache = path;
                        processCommand("cd " + argument, false);
                        if (!path.equals(cache)) {
                            send("250 Directory changed to " + path);
                        } else {
                            send("550 Access denied");
                        }
                    } else if (command.equals("TYPE")) {
                        send("200 Type set to " + argument);
                    } else if (command.equals("NOOP")) {
                        send("200 OK");
                    } else if (command.equals("DELE")) {
                        processCommand("rm " + argument, false);
                        send("250 File deleted");
                    } else if (command.equals("MKD")) {
                        processCommand("mkdir " + argument, false);
                        send("257 \"" + argument + "\" directory created");
                    } else if (command.equals("RMD")) {
                        processCommand("rm " + argument, false);
                        send("250 Directory removed");
                    } else if (command.equals("PASV")) {
                        try {
                            if (dataServerSocket != null) dataServerSocket.close();
                            dataServerSocket = (ServerSocketConnection) Connector.open("socket://:2121");
                            String localIP = clientSocket.getLocalAddress().replace('.', ',');
                            int p1 = 2121 / 256;
                            int p2 = 2121 % 256;
                            send("227 Entering Passive Mode (" + localIP + "," + p1 + "," + p2 + ")");
                        } catch (IOException e) {
                            send("425 Can't open data connection");
                        }
                    } else if (command.equals("LIST")) {
                        send("150 Here comes directory listing");
                        if (openDataConnection()) {
                            String before = stdout != null ? stdout.getText() : "";
                            processCommand("dir -v");
                            String after = stdout != null ? stdout.getText() : "";
                            String listing = replace(after.length() >= before.length()
                                    ? after.substring(before.length()).trim() + "\n" : "\n", "\t", "\n");
                            dataOs.write(listing.getBytes());
                            dataOs.flush();
                            closeDataConnection();
                        }
                        send("226 Directory send OK");
                    } else if (command.equals("RETR")) {
                        String content = getcontent(argument);
                        if (content == null) {
                            send("550 File not found");
                        } else {
                            send("150 Opening data connection");
                            if (openDataConnection()) {
                                dataOs.write(content.getBytes());
                                dataOs.flush();
                                closeDataConnection();
                            }
                            send("226 Transfer complete");
                        }
                    } else if (command.equals("STOR")) {
                        send("150 Ready to receive " + argument);
                        if (openDataConnection()) {
                            byte[] buffer = new byte[4096];
                            int len = dataIs.read(buffer);
                            String data = len > 0 ? new String(buffer, 0, len) : "";
                            writeRMS(argument, data);
                            closeDataConnection();
                            send("226 Upload complete");
                        } else {
                            send("425 Can't open data connection");
                        }
                    } else if (command.equals("QUIT")) {
                        send("221 Goodbye");
                        break;
                    } else {
                        send("502 Command not implemented");
                    }
                }

                is.close();
                os.close();
                clientSocket.close();
                if (dataServerSocket != null) dataServerSocket.close();
                if (dataConnection != null) dataConnection.close();
                echoCommand("[-] " + clientSocket.getAddress() + " disconnected");
            }

        } catch (IOException e) {
            echoCommand("[-] " + e.getMessage());
        }
    }

    private boolean openDataConnection() {
        try {
            dataConnection = (SocketConnection) dataServerSocket.acceptAndOpen();
            dataIs = dataConnection.openInputStream();
            dataOs = dataConnection.openOutputStream();
            return true;
        } catch (IOException e) {
            echoCommand("[-] data connection error: " + e.getMessage());
            return false;
        }
    }

    private void closeDataConnection() {
        try {
            if (dataIs != null) dataIs.close();
            if (dataOs != null) dataOs.close();
            if (dataConnection != null) dataConnection.close();
        } catch (IOException ignored) {}
    }

    private String readLine() throws IOException {
        StringBuffer sb = new StringBuffer();
        int c;
        while ((c = is.read()) != -1 && c != '\n') {
            if (c != '\r') sb.append((char) c);
        }
        return sb.toString().trim();
    }

    private void send(String msg) throws IOException {
        os.write((msg + "\r\n").getBytes());
        os.flush();
    }
}
