import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Created by EngMoazh on 4/25/17.
 */

public class ReliableServer {
    public final static int DATA_SIZE = 988;
    public final static int WIND_SIZE = 10;
    public final static int TIMEOUT = 1000;

    private int base = 0;
    private int nextSeqNum = 0;
    private ArrayList<byte[]> packetsList = new ArrayList<>(WIND_SIZE);
    private Timer timer;
    private Semaphore semaphore = new Semaphore(1);
    private boolean isTransferred = false;
    private DatagramSocket outputSocket, inputSocket;

    public ReliableServer() throws SocketException {

        outputSocket = new DatagramSocket();
        inputSocket = new DatagramSocket(2222);

        InputThread inputThread = new InputThread(inputSocket, this);
        OutputThread outputThread = new OutputThread(outputSocket, this);
        inputThread.start();
        outputThread.start();
    }

    public void setTimer(boolean isNew) {
        if (timer != null) timer.cancel();
        if (isNew) {
            timer = new Timer();
            timer.schedule(new Timeout(semaphore, this), TIMEOUT);
        }
    }

    public int getBase() {
        return base;
    }

    public void setBase(int base) {
        this.base = base;
    }

    public int getNextSeqNum() {
        return this.nextSeqNum;
    }

    public void setNextSeqNum(int nextSeqNum) {
        this.nextSeqNum = nextSeqNum;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    public void setSemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    public boolean isTransferred() {
        return isTransferred;
    }

    public void setTransferred(boolean transferred) {
        this.isTransferred = transferred;
    }

    public ArrayList<byte[]> getPacketsList() {
        return packetsList;
    }

    public void setPacketsList(ArrayList<byte[]> packetsList) {
        this.packetsList = packetsList;
    }
}