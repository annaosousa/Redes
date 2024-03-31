import java.io.*;
import java.net.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class UDPServer {
    private static final int BUFFER_SIZE = 1024;

    public static void main(String args[]) throws Exception {
        int porta = 9876;

        DatagramSocket serverSocket = new DatagramSocket(porta);

        // Usar um ByteArrayOutputStream para acumular os dados recebidos
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] receiveData = null;
        byte[] fileData = null;

        while (true) {
            receiveData = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            System.out.println("Esperando por datagrama UDP na porta... " + porta);
            serverSocket.receive(receivePacket);

            // Process received data
            fileData = receivePacket.getData();
            int bytesRead = receivePacket.getLength();

            // Verificar se este é o último pacote
            if (bytesRead < BUFFER_SIZE) {
                // Trim excess empty bytes from the end
                fileData = java.util.Arrays.copyOf(fileData, bytesRead);
            }

            // Escrever os dados recebidos no ByteArrayOutputStream
            byteStream.write(fileData);

            // Imprimir os dados recebidos para fins de depuração
            // System.out.println("\n Dados recebidos: " + new String(fileData, 0,
            // bytesRead));

            // Enviar acknowledgment para o cliente
            InetAddress clientAddress = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();
            String ack = "ACK";
            byte[] ackData = ack.getBytes();
            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, clientAddress, clientPort);
            serverSocket.send(ackPacket);

            // Verificar se este é o último pacote
            if (bytesRead < BUFFER_SIZE) {
                break;
            }
        }

        // Escrever os dados acumulados no arquivo
        FileOutputStream fileOutputStream = new FileOutputStream("received_file.txt");
        byteStream.writeTo(fileOutputStream);

        System.out.println("Arquivo recebido pelo servidor.");

        // Calculate and display file size
        System.out.println("\n Dados recebidos: " + byteStream);
        System.err.println("\n Tamanho do arquivo recebido: " + byteStream.size() + " bytes.");

        System.out.println("\n Checksum do arquivo: " + getCRC32Checksum(fileData));

        byteStream.close();
        fileOutputStream.close();
        serverSocket.close();
        System.out.println("Socket servidor fechado!");
    }

    public static long getCRC32Checksum(byte[] bytes) {
        Checksum crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }
}
