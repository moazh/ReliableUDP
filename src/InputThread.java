import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Created by EngMoazh on 4/25/17.
 */
public class InputThread extends Thread {

    private DatagramSocket inputSocket;
    private ReliableServer server;

    public InputThread(DatagramSocket inputSocket, ReliableServer server) {
        this.inputSocket = inputSocket;
        this.server = server;
    }

    int decodePacket(byte[] pkt) {
        byte[] receivedCSB = Arrays.copyOfRange(pkt, 0, 8);
        byte[] ackNumBytes = Arrays.copyOfRange(pkt, 8, 12);
        CRC32 crc32 = new CRC32();
        crc32.update(ackNumBytes);
        byte[] calculatedCSB = ByteBuffer.allocate(8).putLong(crc32.getValue()).array();
        if (Arrays.equals(receivedCSB, calculatedCSB))
            return ByteBuffer.wrap(ackNumBytes).getInt();
        else return -1;
    }

    public void run() {
        try {
            byte[] in_data = new byte[12];
            DatagramPacket inputPacket = new DatagramPacket(in_data, in_data.length);
            try {
                while (!server.isTransferred()) {

                    inputSocket.receive(inputPacket);
                    int ackNum = decodePacket(in_data);
                    System.out.println("ReliableServer: Received Ack " + ackNum);

                    if (ackNum != -1) {
                        if (server.getBase() == ackNum + 1) {
                            server.getSemaphore().acquire();
                            server.setTimer(false);
                            server.setNextSeqNum(server.getBase());
                            server.getSemaphore().release();
                        }
                        else if (ackNum == -2)
                            server.setTransferred(true);
                        else {
                            server.setBase(ackNum++);
                            server.getSemaphore().acquire();
                            if (server.getBase() == server.getNextSeqNum())
                                server.setTimer(false);
                            else
                                server.setTimer(true);
                            server.getSemaphore().release();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                inputSocket.close();
                System.out.println("ReliableServer: DatagramSocket closed!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
