import java.io.*;
import java.net.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class UDPClient {
        private static final int BUFFER_SIZE = 1024;
        private static final int TIMEOUT = 5000; // Timeout de 5 segundos

        public static void main(String args[]) throws Exception {
                int bytesRead = 0;
                int bytesDescarted = 0;
                BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

                System.out.println("Digite o endereço IP do servidor (e porta) no formato @ip:porta:");
                String serverAddress = inFromUser.readLine();

                // Parse server address and port
                String[] serverInfo = serverAddress.split(":");
                String serverIP = serverInfo[0];
                int serverPort = Integer.parseInt(serverInfo[1]);

                DatagramSocket clientSocket = new DatagramSocket();

                // Definir o timeout do socket
                clientSocket.setSoTimeout(TIMEOUT);

                InetAddress IPAddress = InetAddress.getByName(serverIP);

                System.out.println("Digite o caminho do arquivo a ser enviado ao servidor: ");
                String filePath = inFromUser.readLine();

                File file = new File(filePath);

                // Check if the file exists
                if (!file.exists()) {
                        System.out.println("Arquivo não encontrado.");
                        return;
                }

                // Read file content
                FileInputStream fileInputStream = new FileInputStream(file);

                // Descartar bytes no início do arquivo, se necessário
                System.out.println("Digite o número de bytes a serem descartados no início do arquivo (0 se nenhum):");
                int discardBytes = Integer.parseInt(inFromUser.readLine());
                if (discardBytes > 0) {
                        long skipped = fileInputStream.skip(discardBytes);
                        if (skipped < discardBytes) {
                                System.out.println(
                                                "Não foi possível descartar todos os bytes especificados. O arquivo pode ser muito curto.");
                        }
                }

                // Send file in parts
                int totalBytesSent = 0;
                int totalBytes = (int) (file.length() - discardBytes);
                byte[] sendData = new byte[BUFFER_SIZE];

                while (totalBytesSent < totalBytes) {
                        bytesRead = fileInputStream.read(sendData);

                        // Calculate checksum for the current chunk of data

                        // System.out.println("\n Checksum do arquivo (até agora): " +
                        // getCRC32Checksum(sendData, bytesRead));

                        DatagramPacket sendPacket = new DatagramPacket(sendData, bytesRead, IPAddress, serverPort);
                        clientSocket.send(sendPacket);
                        totalBytesSent += bytesRead;

                        // Receive ACK from server
                        byte[] receiveData = new byte[BUFFER_SIZE];
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        try {
                                clientSocket.receive(receivePacket);
                                String ack = new String(receivePacket.getData(), 0, receivePacket.getLength());
                                if (!ack.equals("ACK")) {
                                        System.out.println("Erro: ACK não recebido do servidor.");
                                        break;
                                }
                        } catch (SocketTimeoutException e) {
                                System.out.println("Tempo limite excedido ao aguardar ACK do servidor.");
                                break;
                        }
                }

                System.out.println("Arquivo enviado para o servidor.");

                // Receive and print response from server
                byte[] receiveData = new byte[BUFFER_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                try {
                        clientSocket.receive(receivePacket);
                        String serverResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        System.out.println("Resposta do servidor:");
                        System.out.println(serverResponse);
                } catch (SocketTimeoutException e) {
                        System.out.println("Tempo limite excedido ao aguardar resposta do servidor.");
                }

                // Save received file locally
                String receivedFilePath = "received_file.txt";
                FileOutputStream fileOutputStream = new FileOutputStream(receivedFilePath);
                fileOutputStream.write(receivePacket.getData(), 0, receivePacket.getLength());
                fileOutputStream.close();
                System.out.println("Arquivo recebido salvo como: " + receivedFilePath);

                // Print received file content
                BufferedReader reader = new BufferedReader(new FileReader(receivedFilePath));
                System.out.println("Conteúdo do arquivo recebido:");
                String line;
                while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                }
                reader.close();

                // Calculate and display file size
                System.out.println("Tamanho do arquivo inicial: " + file.length() + ". Tamanho do arquivo recebido: "
                                + totalBytes + " bytes");

                System.out.println("\n Checksum do arquivo: " + getCRC32Checksum(sendData, bytesRead));

                fileInputStream.close();
                clientSocket.close();
                System.out.println("Socket cliente fechado!");
        }

        public static long getCRC32Checksum(byte[] bytes, int length) {
                Checksum crc32 = new CRC32();
                crc32.update(bytes, 0, length);
                return crc32.getValue();
        }
}
