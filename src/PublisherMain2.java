import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class PublisherMain2 {

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

        Publisher pub = new Publisher(IP, "^[k-zK-Z].*");

        List<String> serverIPs; 
        if(args.length < 1){
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while(true){
                System.out.println("Server IPs (split with spases): ");
                try{
                    String[] input = reader.readLine().split(" ");
                    serverIPs = new ArrayList<>(Arrays.asList(input));
                    break;
                }catch(IOException e){
                    System.err.println("ERROR: Could not read input");
                }
            }
        }else{
            serverIPs = new ArrayList<>(Arrays.asList(args));
        }

        if (pub.init(serverIPs)){
            pub.online();
        }
    }
}
