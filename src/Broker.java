import java.util.*;
import java.net.*;
import java.math.BigInteger; 
import java.security.MessageDigest; 
import java.security.NoSuchAlgorithmException;

public class Broker extends Node{
    public static final List<Consumer> registeredUsers = new ArrayList<>();
    public static final List<Publisher> registeredPublishers = new ArrayList<>();

    private String hashValue;
    private HashMap<MusicFile, Publisher> files;

    private String ip;
    private int port;

    public Broker(int port){
        this.port = port;
        try{
            this.ip = InetAddress.getLocalHost().getHostAddress();
        }catch(UnknownHostException e){
            e.printStackTrace();
            return;
        }
        this.hashValue = SHA1(ip+""+port);
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
}
