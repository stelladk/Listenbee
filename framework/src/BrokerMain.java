import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class BrokerMain {
    public static void main(String[] args) {
        int numBrokers = args.length + 1;
        System.out.println("Number of brokers: " + numBrokers);
        ArrayList<String> brokerIPs = new ArrayList<>();
        for (String IP : args){
            if (isValidIP(IP)){
                brokerIPs.add(IP);
            }
        }

        String IP;
        try {
            IP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println("ERROR: Broker shutdown");
            System.err.println("ERROR: Could not get IP address");
            return;
        }

        if (IP.equals("127.0.0.1")) {
            System.err.println("ERROR: Broker shutdown");
            System.err.println("ERROR: IP is loopback address");
            return;
        }

        Broker broker = new Broker(IP);
        broker.init(brokerIPs);
        broker.runServer();

    }

    /**
     * Check if given IP is valid
     * @param IP IP address
     * @return true if IP is a valid IP address
     */
    private static boolean isValidIP (String IP) {
            String[] parts = IP.split( "." );
            if (parts.length != 4) return false;

            for (String part : parts) {
                int value = Integer.parseInt(part);
                if ((value < 0) || (value > 255)) return false;
            }

            if (IP.endsWith(".")) return false;

            return true;
    }
}

