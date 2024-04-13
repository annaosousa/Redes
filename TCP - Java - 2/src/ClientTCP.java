import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientTCP {
    public static void main(String[] args) {
        try {
            // Verifica se o usuário forneceu o caminho do arquivo como argumento de linha
            // de comando
            String filePath;
            if (args.length > 0) {
                filePath = args[0];
            } else {
                // Se não, solicita ao usuário que insira o caminho do arquivo
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Digite o caminho do arquivo que deseja solicitar: ");
                filePath = userInput.readLine();
            }

            Socket socket = new Socket("localhost", 8080); // Conectar ao servidor local na porta 12345

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Envia uma solicitação GET para o servidor
            out.println("GET " + "/" + filePath + " HTTP/1.0");
            out.println(); // É importante enviar uma linha em branco para indicar o fim da solicitação

            String responseLine;
            boolean headersComplete = false;

            while ((responseLine = in.readLine()) != null) {
                System.out.println("Resposta do servidor: " + responseLine);

                if (responseLine.isEmpty()) {
                    if (responseLine.isEmpty()) {
                        headersComplete = true;
                        System.out.println("Corpo da resposta:");
                    } else {
                        System.out.println("Cabeçalho da resposta: " + responseLine);
                    }
                } else {
                    // Corpo da resposta (conteúdo do arquivo)
                    System.out.println(responseLine);
                    // byte[] buffer = new byte[1024];
                    // InputStream inputStream = socket.getInputStream();
                    // int bytesRead;
                    // while ((bytesRead = inputStream.read(buffer)) != -1) {
                    // System.out.write(buffer, 0, bytesRead);
                    // }
                    // System.out.flush();
                    // break; // Saia do loop após ler todo o conteúdo
                }
            }

            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
