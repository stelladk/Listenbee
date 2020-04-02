import java.util.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

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
        Publisher pub = new Publisher(IP,"^[[a-jA-J] | [0-9]].*");
        List<String> serverIPs = new ArrayList<>();
        serverIPs.add("127.0.0.1");
        pub.init(serverIPs);
        pub.online();
    }
}
