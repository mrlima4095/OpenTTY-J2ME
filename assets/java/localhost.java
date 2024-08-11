import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class localhost {

    public static void main(String[] args) throws Exception {
        // Gerar uma porta aleatória entre 8000 e 9000
        int port = new Random().nextInt(1000) + 8000;

        // Criar um servidor socket que escuta na porta gerada
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server started at http://localhost:" + port);

        while (true) {
            // Aceitar conexões de clientes
            Socket clientSocket = serverSocket.accept();
            
            // Criar a resposta HTTP
            String httpResponse = "HTTP/1.1 200 OK\r\n\r\n" +
                                  "<html><body><h1>Java localhost server example</h1></body></html>";
            
            // Enviar a resposta para o cliente
            OutputStream clientOutput = clientSocket.getOutputStream();
            clientOutput.write(httpResponse.getBytes("UTF-8"));
            clientOutput.close();
            
            // Fechar a conexão com o cliente
            clientSocket.close();
        }
    }
}