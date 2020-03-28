import java.net.InetAddress;
import java.net.UnknownHostException;

public class PublisherMain {

    public static void main(String[] args) {
        String IP;
        try {
            IP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println("ERROR: Publisher down");
            System.err.println("ERROR: Could not get IP address");
            return;
        }

        if (IP.equals("127.0.0.1")) {
            System.err.println("ERROR: Publisher down");
            System.err.println("ERROR: IP is loopback address");
            return;
        }

        

        //initialize broker
        Publisher pub = new Publisher(IP);
        pub.init("127.0.0.1", Broker.getToPubPort());
        pub.online();
    }
}
