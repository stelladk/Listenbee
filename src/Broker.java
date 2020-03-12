import java.util.*;
import java.net.*;
import java.math.BigInteger; 
import java.security.MessageDigest; 
import java.security.NoSuchAlgorithmException;

public class Broker implements Node{
    public static final List<Consumer> registeredUsers = new ArrayList<>();
    public static final List<Publisher> registeredPublishers = new ArrayList<>();

    private static final int PORT = 2000;
    private String IP;
    private String HASH_VALUE;

    private List<Broker> brokers;
    private HashMap<ArtistName, MusicFile> files;
    private boolean online;

    public Broker(){
        try{
            IP = InetAddress.getLocalHost().getHostAddress();
            online = true;
        }catch(UnknownHostException e){
            online = false;
            return;
        }
        HASH_VALUE = SHA1(IP+""+PORT);
    }

    public Broker(String IP){
        this.IP = IP;
        this.HASH_VALUE = SHA1(IP+""+PORT);
    }

    public void calculateKeys(){

    }

    public Publisher acceptConnection(Publisher publisher){

        return null;
    }

    public Consumer acceptConnection(Consumer consumer){

        return null;
    }

    public void notifyPublisher(String message){

    }

    public void pull(ArtistName name){

    }

    private String SHA1(String value){
        try { 
            MessageDigest md = MessageDigest.getInstance("SHA-1"); 
  
            byte[] messageDigest = md.digest(value.getBytes()); 
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16); 
            while (hashtext.length() < 32) { 
                hashtext = "0" + hashtext; 
            } 
            return hashtext; 
        }catch (NoSuchAlgorithmException e) { 
            throw new RuntimeException(e); 
        }
    }

    public String getHash(){
        return HASH_VALUE;
    }

    public void updateNodes(){

    }

    public void init(int num, List<String> IPs){
        brokers = new ArrayList<Broker>();
        for(String ip : IPs){
            brokers.add(new Broker(ip)); //check if its online
        }
    }

    public List<Broker> getBrokers(){
        return brokers;
    }

    public void connect(){

    }

    public void disconnect(){

    }
}
