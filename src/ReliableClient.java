import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Created by EngMoazh on 4/25/17.
 */

public class ReliableClient {

    private final static int PACKET_SIZE = 1000;
    private DatagramSocket inputSocket, outputSocket;
    private int previousSeqNum = -1;
    private int nextSeqNum = 0;
    private boolean isTransferred = false;

    // ReliableClient constructor
    public ReliableClient() {

        // create sockets
        try {
            inputSocket = new DatagramSocket(1111);
            outputSocket = new DatagramSocket();
            System.out.println("ReliableClient: Listening");
            try {
                byte[] in_data = new byte[PACKET_SIZE];
                DatagramPacket inputSocket = new DatagramPacket(in_data, in_data.length);

                FileOutputStream fos = null;

                while (!isTransferred) {
                    this.inputSocket.receive(inputSocket);

                    byte[] receivedCS = Arrays.copyOfRange(in_data, 0, 8);
                    CRC32 CS = new CRC32();
                    CS.update(Arrays.copyOfRange(in_data, 8, inputSocket.getLength()));
                    byte[] calculatedCS = ByteBuffer.allocate(8).putLong(CS.getValue()).array();

                    if (Arrays.equals(receivedCS, calculatedCS)) {
                        int seqNum = ByteBuffer.wrap(Arrays.copyOfRange(in_data, 8, 12)).getInt();
                        System.out.println("ReliableClient: Received sequence number: " + seqNum);

                        if (seqNum == nextSeqNum) {
                            if (inputSocket.getLength() == 12) {
                                byte[] acknowledgmentPacket = generatePacket(-2);
                                for (int i = 0; i < 20; i++)
                                    outputSocket.send(new DatagramPacket(acknowledgmentPacket, acknowledgmentPacket.length, InetAddress.getByName("localhost"), 2222));
                                isTransferred = true;
                                System.out.println("ReliableClient: All packets received! File Created!");
                                continue;
                            }
                            else {
                                byte[] acknowledgmentPacket = generatePacket(seqNum);
                                outputSocket.send(new DatagramPacket(acknowledgmentPacket, acknowledgmentPacket.length, InetAddress.getByName("localhost"), 2222));
                                System.out.println("ReliableClient: Sent Ack " + seqNum);
                            }

                            if (seqNum == 0 && previousSeqNum == -1) {
                                int fileNameLength = ByteBuffer.wrap(Arrays.copyOfRange(in_data, 12, 16)).getInt();    // 0-8:CS, 8-12:seqnum
                                File file = new File("fileOutput.png");

                                fos = new FileOutputStream(file);

                                fos.write(in_data, 16 + fileNameLength, inputSocket.getLength() - 16 - fileNameLength);
                            }

                            else fos.write(in_data, 12, inputSocket.getLength() - 12);

                            nextSeqNum++;
                            previousSeqNum = seqNum;
                        }

                        else {
                            byte[] ackPkt = generatePacket(previousSeqNum);
                            outputSocket.send(new DatagramPacket(ackPkt, ackPkt.length, InetAddress.getByName("localhost"), 2222));
                            System.out.println("ReliableClient: Sent duplicate Ack " + previousSeqNum);
                        }
                    }
                    else {
                        System.out.println("ReliableClient: Corrupt packet dropped");
                        byte[] acknowledgmentPacket = generatePacket(previousSeqNum);
                        outputSocket.send(new DatagramPacket(acknowledgmentPacket, acknowledgmentPacket.length, InetAddress.getByName("localhost"), 2222));
                        System.out.println("ReliableClient: Sent duplicate Ack " + previousSeqNum);
                    }
                }
                if (fos != null) fos.close();
            } catch (Exception e) {
                System.out.println("An error occurred in client please restart the server and client");
            } finally {
                inputSocket.close();
                System.out.println("ReliableClient: inputSocket closed!");
                outputSocket.close();
                System.out.println("ReliableClient: outputSocket closed!");
            }
        } catch (SocketException e1) {
            System.out.println("An error occurred in client please restart the server and client");
        }
    }

    public byte[] generatePacket(int ackNum) {
        byte[] acknowledgmentNumByte = ByteBuffer.allocate(4).putInt(ackNum).array();
        CRC32 CS = new CRC32();
        CS.update(acknowledgmentNumByte);
        ByteBuffer packetBuffer = ByteBuffer.allocate(12);
        packetBuffer.put(ByteBuffer.allocate(8).putLong(CS.getValue()).array());
        packetBuffer.put(acknowledgmentNumByte);
        return packetBuffer.array();
    }
}