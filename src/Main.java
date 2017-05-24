import java.net.SocketException;

/**
 * Created by EngMoazh on 4/25/17.
 */
public class Main {

    public static void main(String[] args) throws SocketException {

        new ReliableServer();
        new ReliableClient();
    }
}
