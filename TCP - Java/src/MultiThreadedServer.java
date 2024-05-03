import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MultiThreadedServer {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(1224); // Porta do servidor
            System.out.println("Servidor esperando conexões...");

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Aguarda uma conexão do cliente
                System.out.println("Cliente conectado: " + clientSocket);

                // Cria uma nova thread para lidar com o cliente
                Thread thread = new ClientHandler(clientSocket);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Classe para lidar com cada cliente em uma thread separada
    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private boolean isConnected = true;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String inputLine;
                while (((inputLine = in.readLine()) != null)) {
                    System.out.println("Solicitação de arquivo recebida do cliente: " + inputLine);

                    // Processar a solicitação
                    String response = processRequest(inputLine);
                    out.println(response);

                    if (response.startsWith("OK")) {
                        String[] lines = response.split("\n");
                        String fileName = lines[1].split(": ")[1];
                        long fileSize = Long.parseLong(lines[2].split(": ")[1].split(" ")[0]);

                        // Enviar o arquivo para o cliente
                        sendFile(fileName, out);
                    }

                    if (inputLine == null || inputLine.equalsIgnoreCase("sair")) {
                        isConnected = false;
                        System.out.println("Cliente desconectado: " + clientSocket);
                        break;
                    }
                }

                in.close();
                out.close();
                clientSocket.close();
                System.out.println("Conexão com o cliente fechada.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Processar a solicitação do cliente
        private String processRequest(String request) {
            request = request.trim(); // Remove espaços em branco extras
            String fileName = request;

            File file = new File(fileName);
            if (file.exists()) {
                long fileSize = file.length();
                String hash = calculateHash(fileName);

                StringBuilder responseBuilder = new StringBuilder();
                responseBuilder.append("OK\n");
                responseBuilder.append("Nome do arquivo: ").append(file.getName()).append("\n");
                responseBuilder.append("Tamanho: ").append(fileSize).append(" bytes\n");
                responseBuilder.append("Hash: ").append(hash).append("\n");

                return responseBuilder.toString();
            } else {
                return "Arquivo inexistente";
            }
        }

        // Enivar arquivo para o cliente
        private void sendFile(String fileName, PrintWriter out) {
            try {
                File file = new File(fileName);
                FileInputStream fileInputStream = new FileInputStream(file);
                OutputStream outputStream = clientSocket.getOutputStream();

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                fileInputStream.close();
                outputStream.flush();
                System.out.println("Arquivo enviado para o cliente");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Calcular o hash SHA
        private String calculateHash(String fileName) {
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
}
