import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

public class LocalhostServerMIDlet extends MIDlet {

    private StreamConnectionNotifier serverConnection;
    private boolean isRunning;
    private Display display;
    private Form form;

    public void startApp() {
        display = Display.getDisplay(this);
        form = new Form("Localhost Server");
        StringItem statusItem = new StringItem("Status", "Starting server...");
        form.append(statusItem);
        display.setCurrent(form);

        new Thread(new Runnable() {
            public void run() {
                try {
                    // Inicia o servidor socket na porta 1234
                    serverConnection = (StreamConnectionNotifier) Connector.open("socket://:1234");
                    statusItem.setText("Server running on localhost:1234");
                    isRunning = true;

                    while (isRunning) {
                        StreamConnection connection = serverConnection.acceptAndOpen();
                        handleClientConnection(connection);
                    }
                } catch (IOException e) {
                    statusItem.setText("Error: " + e.getMessage());
                }
            }
        }).start();
    }

    public void pauseApp() {
        // Pausa não implementada neste exemplo
    }

    public void destroyApp(boolean unconditional) {
        isRunning = false;
        if (serverConnection != null) {
            try {
                serverConnection.close();
            } catch (IOException e) {
                // Tratamento de erro de fechamento do servidor
            }
        }
    }

    private void handleClientConnection(StreamConnection connection) {
        try {
            InputStream input = connection.openInputStream();
            OutputStream output = connection.openOutputStream();

            byte[] buffer = new byte[256];
            int bytesRead = input.read(buffer);

            if (bytesRead != -1) {
                String receivedData = new String(buffer, 0, bytesRead);
                // Exibir a mensagem recebida no form
                form.append(new StringItem("Received:", receivedData));

                // Envia uma resposta ao cliente
                String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nHello from J2ME Server";
                output.write(response.getBytes());
            }

            input.close();
            output.close();
            connection.close();
        } catch (IOException e) {
            form.append(new StringItem("Error:", e.getMessage()));
        }
    }
}