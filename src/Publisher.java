import java.util.*;
import java.math.BigInteger; 
import java.security.MessageDigest; 
import java.security.NoSuchAlgorithmException;

public class Publisher implements Node{
    private List<ArtistName> artists;
    private List<MusicFile> files;

    private List<Broker> brokers;

    public Publisher(ArrayList<ArtistName> artists, ArrayList<MusicFile> files){
        this.artists = artists;
        this.files = files;
    }

    public void getBrokerList(){

    }

    public Broker hashTopic(ArtistName name){
        String hashValue = SHA1(name.artistName);
        for(Broker br: brokers){
            if(br.getHash() >= hashValue){ //mod
                return br;
            }
        }
        return null;
    }

    public void push(ArtistName name, MusicFile file){

    }
    
    public void notifyFailure(Broker broker){

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

    public void updateNodes(){

    }

    public void init(int num, List<String> IPs){

    }

    public List<Broker> getBrokers(){
        return null;
    }

    public void connect(){

    }

    public void disconnect(){

    }

}
