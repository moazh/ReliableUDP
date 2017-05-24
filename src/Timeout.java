import java.util.TimerTask;
import java.util.concurrent.Semaphore;

/**
 * Created by EngMoazh on 4/25/17.
 */

public class Timeout extends TimerTask {

    private Semaphore semaphore;
    private ReliableServer server;

    public Timeout(Semaphore semaphore, ReliableServer server) {

        this.semaphore = semaphore;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            semaphore.acquire();
            System.out.println("ReliableServer: Timeout Occurred!");
            server.setNextSeqNum(server.getBase());
            semaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
