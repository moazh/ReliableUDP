import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Created by EngMoazh on 4/25/17.
 */

public class OutputThread extends Thread {

    private DatagramSocket outputSocket;
    private ReliableServer server;

    public OutputThread(DatagramSocket outputSocket, ReliableServer server) {
        this.outputSocket = outputSocket;
        this.server = server;
    }

    public byte[] generatePacket(int seqNum, byte[] dataBytes) {
        byte[] seqNumBytes = ByteBuffer.allocate(4).putInt(seqNum).array();

        CRC32 CS = new CRC32();
        CS.update(seqNumBytes);
        CS.update(dataBytes);
        byte[] checksumBytes = ByteBuffer.allocate(8).putLong(CS.getValue()).array();

        ByteBuffer packetBuffer = ByteBuffer.allocate(8 + 4 + dataBytes.length);
        packetBuffer.put(checksumBytes);
        packetBuffer.put(seqNumBytes);
        packetBuffer.put(dataBytes);
        return packetBuffer.array();
    }

    public void run() {
        try {
            FileInputStream fis = new FileInputStream(new File("file.png"));

            try {
                while (!server.isTransferred()) {
                    if (server.getNextSeqNum() < server.getBase() + server.TIMEOUT) {

                        server.getSemaphore().acquire();
                        if (server.getBase() == server.getNextSeqNum())

                            server.setTimer(true);

                        byte[] outData = new byte[10];
                        boolean isFinalSeqNum = false;

                        if (server.getNextSeqNum() < server.getPacketsList().size()) {
                            outData = server.getPacketsList().get(server.getNextSeqNum());
                        }
                        else {
                            if (server.getNextSeqNum() == 0) {
                                String fileName = "file3.png";
                                byte[] fileNameBytes = fileName.getBytes();
                                byte[] fileNameLengthBytes = ByteBuffer.allocate(4).putInt(fileNameBytes.length).array();
                                byte[] dataBuffer = new byte[server.DATA_SIZE];
                                int dataLength = fis.read(dataBuffer, 0, server.DATA_SIZE - 4 - fileNameBytes.length);
                                byte[] dataBytes = Arrays.copyOfRange(dataBuffer, 0, dataLength);
                                ByteBuffer BB = ByteBuffer.allocate(4 + fileNameBytes.length + dataBytes.length);
                                BB.put(fileNameLengthBytes);    // file name length
                                BB.put(fileNameBytes);            // file name
                                BB.put(dataBytes);                // file data slice
                                outData = generatePacket(server.getNextSeqNum(), BB.array());
                            }
                            else {
                                byte[] dataBuffer = new byte[server.DATA_SIZE];
                                int dataLength = fis.read(dataBuffer, 0, server.DATA_SIZE);
                                if (dataLength == -1) {
                                    isFinalSeqNum = true;
                                    outData = generatePacket(server.getNextSeqNum(), new byte[0]);
                                }
                                else {
                                    byte[] dataBytes = Arrays.copyOfRange(dataBuffer, 0, dataLength);
                                    outData = generatePacket(server.getNextSeqNum(), dataBytes);
                                }
                            }
                            server.getPacketsList().add(outData);
                        }

                        outputSocket.send(new DatagramPacket(outData, outData.length, InetAddress.getByName("localhost"), 1111));
                        System.out.println("ReliableServer: Sent seqNum " + server.getNextSeqNum());

                        if (!isFinalSeqNum) server.setNextSeqNum(server.getNextSeqNum() + 1);
                        server.getSemaphore().release();    /***** leave CS *****/
                    }
                    sleep(5);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                server.setTimer(false);
                outputSocket.close();
                fis.close();
                System.out.println("ReliableServer: outputSocket closed!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
