import java.util.*;
import java.math.BigInteger; 
import java.security.MessageDigest; 
import java.security.NoSuchAlgorithmException;

public class Publisher{
    private HashMap<ArtistName,List<MusicFile>> files; //hash with artistName

    private HashMap<ArtistName, Broker> brokers;

    public Publisher(ArrayList<MusicFile> songs){
        files = new HashMap<>();
        List<MusicFile> list;
        for(MusicFile song: songs){
            if(!files.contains(song.artistName)){
                list = new ArrayList<MusicFile>();
            }
            list = files.get(song.artistName);
            list.add(song);
            files.push(song.artistName, list);
        }
    }

    //get brokers and their hashes usinf method from broker
    public void getBrokerList(){

    }

    //find the right broker using getBrokerList
    public Broker hashTopic(ArtistName name){
        String hashValue = SHA1(name.artistName);
        for(Broker br: brokers){
            if(br.getHash() >= hashValue){ //mod
                return br;
            }
        }
        return null;
    }

    //transfer data to broker on broker demand
    public void push(ArtistName name, MusicFile file){
        //send file using different threads
        //each song raises a thread that raises mupliple threads
        //send all the songs with artistName as key

        //search hashmap to choose Broker

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

}
