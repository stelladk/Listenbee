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
        broker.init(brokerIPs);
        broker.runServer();
    }
}
