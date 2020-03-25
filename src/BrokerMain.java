import java.io.IOException;
import java.util.*;

public class BrokerMain {
    public static void main(String[] args) {
        int num = args.length + 1;
        System.out.println("Number of brokers: "+num);

        List<String> brokerIPs = new ArrayList<String>();
        for(String IP : args){
            if(Utilities.isValidIP(IP)){
                brokerIPs.add(IP);
            }
        }

        Broker broker = new Broker();
        System.out.println("Constructed Broker");
        broker.init(brokerIPs);
        System.out.println("Initialisez Broker");
        try{
            broker.runServer();
            System.out.println("Run Server");
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
