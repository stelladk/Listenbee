import java.util.*;
import java.net.*;
import java.math.BigInteger; 
import java.security.MessageDigest; 
import java.security.NoSuchAlgorithmException;

public class Broker{
    public static final List<Consumer> registeredUsers = new ArrayList<>();
    public static final List<Publisher> registeredPublishers = new ArrayList<>();

    private static final int PORT = 2000;
    private String IP;
    private BigInteger HASH_VALUE;

    // private List<Broker> brokers;
    private HashMap<ArtistName, Broker> brokers;
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
        HASH_VALUE = Utilities.SHA1(IP+""+PORT);
    }

    public Broker(String IP){
        this.IP = IP;
        this.HASH_VALUE = Utilities.SHA1(IP+""+PORT);
    }

    //save data to hashmap files
    public void calculateKeys(){

    }

    public Publisher acceptConnection(Publisher publisher){

        return null;
    }

    public Consumer acceptConnection(Consumer consumer){

        return null;
    }

    //send message to publisher for the artists that it handles
    public void notifyPublisher(String message){

    }

    //send data to consumer on consumer demand
    public void pull(ArtistName name){
        //request data from publisher using push method
        //find data in hashmap
        //send the entire list with astistName as key
    }

    public static int getPORT(){
        return PORT;
    }

    public String  getIP(){
        return IP;
    }

    public BigInteger getHash(){
        return HASH_VALUE;
    }

    public HashMap<ArtistName, Broker> getBrokers(){
        return brokers;
    }

    public boolean isOnline(){
        return online;
    }
}
