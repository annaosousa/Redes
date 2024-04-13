import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ClientTCP {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 1224); // Conectar ao servidor local na porta 12345

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            String hash = new String();
            String fileName = new String();
            int fileSize = 0;

            System.out.println("Insira o nome do arquivo desejado: ");
            String userInputLine;
            while (((userInputLine = userInput.readLine()) != null)) {

                if (userInputLine.equalsIgnoreCase("sair")) {
                    break; // Sai do loop se o usuário inserir "sair"
                }

                out.println(userInputLine); // Envia a mensagem para o servidor
                System.out.println("Solicitação de arquivo enviada para o servidor.");

                String response = in.readLine(); // Lê a resposta do servidor
                System.out.println("\nResposta do servidor: " + response);

                if (response.startsWith("OK")) {
                    System.out.println(response);

                    System.out.println("Informações adicionais: ");
                    for (int i = 0; i < 3; i++) {
                        String info = in.readLine();
                        System.out.println(info);

                        if (i == 1) { // Se estiver lendo a segunda linha (tamanho do arquivo)
                            String sizeStr = info.split(": ")[1]; // Obtém a parte do tamanho da string
                            sizeStr = sizeStr.replaceAll("[^\\d]", ""); // Remove todos os caracteres não numéricos
                            fileSize = Integer.parseInt(sizeStr); // Converte para inteiro

                        }

                        if (i == 2) {
                            hash = info.split(": ")[1];
                        }
                    }

                    // Recebe dados do arquivo
                    byte[] fileData = new byte[(int) fileSize];
                    InputStream fileInputStream = socket.getInputStream();
                    FileOutputStream fileOutputStream = new FileOutputStream("received_" + fileName);

                    int bytesRead;
                    int totalBytesRead = 0;

                    while (totalBytesRead < fileSize && (bytesRead = fileInputStream.read(fileData, totalBytesRead,
                            (int) fileSize - totalBytesRead)) != -1) {
                        fileOutputStream.write(fileData, 0, bytesRead);
                        totalBytesRead += bytesRead;
                    }

                    fileOutputStream.close();

                    System.out.println("\n\n");
                    // Imprime o conteúdo do arquivo
                    System.out.println("Conteúdo do arquivo recebido:\n");
                    BufferedReader reader = new BufferedReader(new FileReader("received_" + fileName));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                    reader.close();
                }

                // Calcula Hash
                String calcultedHash = calculateHash("received_" + fileName);
                System.out.println("\n\nHash SHA256 do arquivo recebido: " + calcultedHash);

                // Verifica o arquivo
                if (calcultedHash.equals(hash)) {
                    System.out.println("Integridade do arquivo verificada: OK");
                } else {
                    System.out.println("Integridade do arquivo não verificada: NOK");
                }

                System.out.println("Insira o nome do arquivo desejado: ");

            }

            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para calcular o hash SHA256 de um arquivo
    private static String calculateHash(String fileName) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(fileName);
            byte[] dataBytes = new byte[1024];

            int bytesRead;
            while ((bytesRead = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, bytesRead);
            }

            byte[] mdBytes = md.digest();

            StringBuilder sb = new StringBuilder();
            for (byte mdByte : mdBytes) {
                sb.append(Integer.toString((mdByte & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
