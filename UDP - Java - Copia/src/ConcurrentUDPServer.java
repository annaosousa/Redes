import java.net.*;
import java.io.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ConcurrentUDPServer {
    public static void main(String[] args) throws InterruptedException {
        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket(9876); // Porta escolhida para comunicação
            // socket.setSoTimeout(50000);

            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                // inicia a thread e faz com que cada processamento seja independente
                Thread requestHandler = new RequestHandler(socket, request);
                requestHandler.start();
                requestHandler.join();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}

class RequestHandler extends Thread {
    private DatagramSocket socket;
    private DatagramPacket request;

    public RequestHandler(DatagramSocket socket, DatagramPacket request) {
        this.socket = socket;
        this.request = request;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int packetNumber = 0;
        int bytesSent = 0;
        byte[] buffer_origin = new byte[1024];

        try {
            String requestData = new String(request.getData(), 0, request.getLength());

            // Separar a requisição em partes usando o espaço como delimitador
            String[] parts = requestData.split(" ");

            // Verificar se a requisição tem o formato correto
            if (parts.length == 3 && parts[0].equals("GET")) {
                // Extrair o nome do arquivo da requisição
                String fileName = parts[1].substring(1); // Remover a barra inicial
                int discardBytes = Integer.parseInt(parts[2]);

                // Lendo o conteúdo do arquivo para a memória
                File file = new File("src/" + fileName);

                if (file.exists()) {
                    // abre o arquivo, lê e fecha posteriormente
                    FileInputStream fis = new FileInputStream(file);
                    byte[] fileData = new byte[(int) file.length()];
                    fis.read(fileData);
                    fis.close();

                    byte[] trimmedData = fileData;

                    // Aplicar deleção de bytes, se necessário
                    if (discardBytes > 0 && discardBytes < fileData.length) {
                        trimmedData = new byte[fileData.length - discardBytes];
                        System.arraycopy(fileData, 0, trimmedData, 0, trimmedData.length);
                    }

                    // Enviar o arquivo para o cliente em pacotes
                    while (bytesSent < fileData.length) {
                        packetNumber++;
                        int remainingBytes = trimmedData.length - bytesSent;
                        int remainingBytes_Origin = fileData.length - bytesSent;

                        // quantidade de bytes restantes no arquivo que ainda não foram enviadas ao
                        // cliente
                        int bytesToSend = Math.min(remainingBytes, buffer.length);
                        int bytesToSend_Origin = Math.min(remainingBytes_Origin, buffer.length);

                        System.arraycopy(trimmedData, bytesSent, buffer, 0, bytesToSend);
                        System.arraycopy(fileData, bytesSent, buffer_origin, 0, bytesToSend_Origin);

                        // Calcular checksum do pacote atual (checksum do arquivo original)
                        long checksum = getCRC32Checksum(buffer_origin, buffer_origin.length);

                        // Enviar parte do arquivo para o cliente
                        DatagramPacket response = new DatagramPacket(buffer, bytesToSend, request.getAddress(),
                                request.getPort());
                        socket.send(response);

                        DatagramPacket ack = new DatagramPacket(new byte[1], 1);
                        socket.receive(ack);

                        // Enviar checksum para o pacote atual
                        System.out.println("Checksum do pacote " + packetNumber + ": " + checksum);
                        String checksumString = Long.toString(checksum);
                        DatagramPacket checksumPacket = new DatagramPacket(checksumString.getBytes(),
                                checksumString.length(),
                                request.getAddress(), request.getPort());
                        socket.send(checksumPacket);

                        // Aguardar confirmação do cliente
                        byte[] confirmationBuffer = new byte[1024];
                        DatagramPacket confirmationPacket = new DatagramPacket(confirmationBuffer,
                                confirmationBuffer.length);
                        try {
                            socket.receive(confirmationPacket);
                            String confirmation = new String(confirmationPacket.getData(), 0,
                                    confirmationPacket.getLength());
                            if (!confirmation.equals("ACK")) {
                                System.out.println(
                                        "Pacote " + packetNumber + " não confirmado pelo cliente. Retransmitindo...");
                                response = new DatagramPacket(buffer_origin, bytesToSend_Origin, request.getAddress(),
                                        request.getPort());
                                socket.send(response);

                                // Enviar checksum para o pacote atual
                                System.out.println("Checksum do pacote " + packetNumber + ": " + checksum);
                                checksumString = Long.toString(checksum);
                                checksumPacket = new DatagramPacket(checksumString.getBytes(),
                                        checksumString.length(),
                                        request.getAddress(), request.getPort());
                                socket.send(checksumPacket);

                                bytesSent += bytesToSend_Origin;
                                continue;
                            } else {
                                bytesSent += bytesToSend;
                            }
                        } catch (SocketTimeoutException e) {
                            System.out.println(
                                    "Timeout para confirmação do pacote " + packetNumber + ". Retransmitindo...");
                            continue;
                        }
                    }
                } else if (!file.exists()) {
                    // Se o arquivo não existe, enviar uma mensagem de erro para o cliente
                    String errorMessage = "404";
                    DatagramPacket response = new DatagramPacket(errorMessage.getBytes(), errorMessage.length(),
                            request.getAddress(), request.getPort());
                    socket.send(response);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static long getCRC32Checksum(byte[] bytes, int length) {
        Checksum crc32 = new CRC32();
        crc32.update(bytes, 0, length);
        return crc32.getValue();
    }
}
