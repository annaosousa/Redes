import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.net.*;
import java.io.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class UDPClient {
        public static void main(String[] args) throws InterruptedException {
                DatagramSocket socket = null;
                DatagramPacket response = null;
                int packetNumber = 0;

                try {
                        byte[] ip = { 127, 0, 0, 1 };
                        InetAddress serverAddress = InetAddress.getByAddress(ip); // Endereço do servidor
                        int serverPort = 9876; // Porta do servidor

                        socket = new DatagramSocket();

                        socket.setSoTimeout(10000);

                        // Usuário digita o nome do arquivo
                        Scanner scanner = new Scanner(System.in);
                        System.out.print("Digite o nome do arquivo: ");
                        String fileName = scanner.nextLine();

                        System.out.print(
                                        "Digite o número de bytes para descartar uma parte do arquivo? ");
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

                        while (true) {
                                packetNumber++;
                                // Receber o pacote de dados
                                response = new DatagramPacket(buffer, buffer.length);
                                socket.receive(response);

                                // Verificar se é um código de erro
                                String responseData = new String(response.getData(), 0, response.getLength());
                                if (responseData.equals("404")) {
                                        // Arquivo não encontrado, exibir mensagem de erro e encerrar
                                        System.out.println("Arquivo não encontrado no servidor.");
                                        return;
                                }

                                // adiciona os dados recebidos ao fluxo de saída
                                outputStream.write(response.getData(), 0, response.getLength());

                                DatagramPacket ack = new DatagramPacket(new byte[1], 1, serverAddress, serverPort);
                                socket.send(ack);

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
                                        String confirmation = "NACK";
                                        DatagramPacket confirmationPacket = new DatagramPacket(confirmation.getBytes(),
                                                        confirmation.length(), response.getAddress(),
                                                        response.getPort());
                                        socket.send(confirmationPacket);
                                } else {
                                        String confirmation = "ACK";
                                        DatagramPacket confirmationPacket = new DatagramPacket(confirmation.getBytes(),
                                                        confirmation.length(), response.getAddress(),
                                                        response.getPort());
                                        socket.send(confirmationPacket);
                                }

                                // final do arquivo
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
