import java.io.*;
import java.net.*;
import java.util.Scanner;

public class RemoteApp {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private String username;
    private String path;

    public RemoteApp(String host, int port) {
        try {
            socket = new Socket(host, port);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            connect();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void connect() {
        username = get("logname").trim();
        path = get("pwd").trim();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            try {
                System.out.print(username + " " + path + " $ ");
                String command = scanner.nextLine().trim();
                command = get(command).trim();

                if (!command.isEmpty()) {
                    System.out.println(command);
                }
            } catch (Exception e) {
                System.out.println("\nConnection Closed");
                close();
                break;
            }
        }

        scanner.close();
    }

    private String get(String command) {
        try {
            output.println(command);
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = input.readLine()) != null && !line.isEmpty()) {
                response.append(line).append("\n");
            }

            return response.toString().trim();
        } catch (IOException e) {
            close();
            return "";
        }
    }


    private void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("> java RemoteApp [HOST] [PORT]");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        new RemoteApp(host, port);
    }
}
