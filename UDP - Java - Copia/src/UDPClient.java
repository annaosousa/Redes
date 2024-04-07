import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.net.*;
import java.io.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class UDPClient {
        public static void main(String[] args) {
                DatagramSocket socket = null;
                DatagramPacket response = null;
                int packetNumber = 0;

                try {
                        InetAddress serverAddress = InetAddress.getByName("localhost"); // Endereço do servidor
                        int serverPort = 9876; // Porta do servidor

                        socket = new DatagramSocket();

                        socket.setSoTimeout(10000);

                        // Usuário digita o nome do arquivo
                        Scanner scanner = new Scanner(System.in);
                        System.out.print("Digite o nome do arquivo: ");
                        String fileName = scanner.nextLine();

                        System.out.print(
                                        "Deseja descartar uma parte do arquivo? ");
                        String discardBytes = scanner.nextLine();

                        // Construindo a requisição com o nome do arquivo e o número de bytes a serem
                        // descartados
                        String requestData = "GET /" + fileName + " " + discardBytes;
                        DatagramPacket request = new DatagramPacket(requestData.getBytes(), requestData.length(),
                                        serverAddress, serverPort);
                        socket.send(request);

                        // Receber e montar o arquivo
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        byte[] checksumBuffer = new byte[64];
                        List<Integer> missingPackets = new ArrayList<>(); // Lista para armazenar números de sequência
                        // de pacotes faltantes

                        while (true) {
                                packetNumber++;
                                // Receber o pacote de dados
                                response = new DatagramPacket(buffer, buffer.length);
                                socket.receive(response);

                                outputStream.write(response.getData(), 0, response.getLength());

                                // Receber o pacote com o checksum enviado pelo servidor
                                DatagramPacket checksumPacket = new DatagramPacket(checksumBuffer,
                                                checksumBuffer.length);
                                socket.receive(checksumPacket);
                                long receivedChecksum = Long.parseLong(
                                                new String(checksumPacket.getData(), 0, checksumPacket.getLength()));

                                // Exibir checksum do pacote atual
                                long localChecksum = getCRC32Checksum(buffer, buffer.length);
                                System.out.println("Checksum do pacote " + packetNumber + ": " + localChecksum
                                                + " (Recebido do servidor: " + receivedChecksum + ")");

                                if (receivedChecksum != localChecksum) {
                                        System.out.println("Erro de transmissão no pacote " + packetNumber);
                                        // lógica para lidar com o erro de transmissão
                                } else {
                                        String confirmation = "ACK";
                                        DatagramPacket confirmationPacket = new DatagramPacket(confirmation.getBytes(),
                                                        confirmation.length(), response.getAddress(),
                                                        response.getPort());
                                        socket.send(confirmationPacket);
                                        System.out.println("entrei na confirmacao");
                                }

                                if ((receivedChecksum == localChecksum) && (response.getLength() < buffer.length)) {
                                        break;
                                }
                        }

                        // Exibir o conteúdo do arquivo recebido do servidor
                        byte[] fileData = outputStream.toByteArray();
                        String responseData = new String(fileData);
                        System.out.println("Conteúdo do arquivo recebido do servidor:");
                        System.out.println(responseData);

                } catch (IOException e) {
                        e.printStackTrace();
                } finally {
                        if (socket != null) {
                                socket.close();
                        }
                }
        }

        public static long getCRC32Checksum(byte[] bytes, int length) {
                Checksum crc32 = new CRC32();
                crc32.update(bytes, 0, length);
                return crc32.getValue();
        }
}
