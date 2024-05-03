import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MultiThreadedServer {
    public static void main(String[] args) {
        int port = 8080;

        try {
            ServerSocket serverSocket = new ServerSocket(port); // Porta do servidor
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

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                OutputStream out = clientSocket.getOutputStream();

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Requisição recebida do cliente: " + inputLine);
                    if (inputLine.isEmpty()) {
                        break;
                    }

                    String[] requestParts = inputLine.split(" ");
                    if (requestParts.length == 3 && requestParts[0].equals("GET")) {
                        String path = requestParts[1].substring(1);
                        File file = new File(path);

                        if (file.exists() && !file.isDirectory()) {
                            // Arquivo encontrado, calcular checksums MD5 e SHA
                            String sha = calculateChecksum(file, "SHA-256");

                            // Enviar resposta 200 OK com o conteúdo do arquivo e checksums
                            sendResponse(out, "HTTP/1.0 200 OK\r\n", getContentType(path), readFile(file), sha);
                        } else {
                            // Arquivo não encontrado, enviar resposta 404 Not Found
                            sendResponse(out, "HTTP/1.0 404 Not Found\r\n", "text/html",
                                    "<html><body><h1>404 Not Found</h1></body></html>".getBytes(), null);
                        }
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

        private String getContentType(String path) {
            if (path.endsWith(".html") || path.endsWith(".htm")) {
                return "text/html";
            } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                return "image/jpeg";
            } else {
                return "application/octet-stream"; // Tipo de conteúdo padrão
            }
        }

        private byte[] readFile(File file) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length());
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }

            bis.close();
            return bos.toByteArray();
        }

        private String calculateChecksum(File file, String algorithm) {
            try {
                MessageDigest digest = MessageDigest.getInstance(algorithm);
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }

                byte[] hash = digest.digest();
                StringBuilder hexString = new StringBuilder();

                for (byte b : hash) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) {
                        hexString.append('0');
                    }
                    hexString.append(hex);
                }

                fis.close();
                return hexString.toString();
            } catch (NoSuchAlgorithmException | IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void sendResponse(OutputStream out, String statusLine, String contentType, byte[] content,
                String shaChecksum) throws IOException {
            out.write(statusLine.getBytes());
            out.write(("Content-Type: " + contentType + "\r\n").getBytes());
            out.write(("Content-Length: " + content.length + "\r\n").getBytes());
            if (shaChecksum != null) {
                out.write(("SHA-256-Checksum: " + shaChecksum + "\r\n").getBytes());
            }
            out.write("\r\n".getBytes());
            out.write(content);
            out.flush();
        }
    }
}
