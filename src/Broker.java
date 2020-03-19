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

    private List<Broker> brokersList;
    private static HashMap<ArtistName, Broker> artistsToBrokers = new HashMap<>();
    private HashMap<ArtistName, Publisher> publishers; //maybe not nessasary
    private boolean online;

    private ServerSocket server; //publisher server

    public Broker(){

    }

    public Broker(String IP){
        this.IP = IP;
        this.HASH_VALUE = Utilities.SHA1(IP+""+PORT);
    }

    public void init(List<String> server_IPs){
        try{
            IP = InetAddress.getLocalHost().getHostAddress();
            online = true;
        }catch(UnknownHostException e){
            online = false;
            return;
        }
        HASH_VALUE = Utilities.SHA1(IP+""+PORT);
        
        acknowledgeServer(server_IPs);
    }

    //save data to hashmap files
    public void calculateKeys(){

    }

    public Publisher acceptConnection(Publisher publisher){
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                Socket socket;
                ObjectOutputStream out;
                ObjectInputStream in;
                String message;
                List<ArtistName> artists;
                while(true){
                    socket = server.accept(); //accept connection
                    // in = new ObjectInputStream(socket.getInputStream());
                    // try{
                    //     message = (String) in.readObject();
                    // }catch(ClassNotFoundException e){
                    //     e.printStackTrace();
                    //     continue();
                    // }
                    out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(brokersList);
                    out.flush();

                    //wait for artists
                    in = new ObjectInputStream(socket.getInputStream());
                    try{
                        artists = (List<ArtistName>) in.readObject();
                    }catch(ClassNotFoundException e){
                        e.printStackTrace();
                        //notifyFailure();
                        continue;
                    }
                    setArtistSource(artists, socket.getInetAddress().getHostAddress());
                    try{
                        socket.close();
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
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
        return artistsToBrokers;
    }

    public boolean isOnline(){
        return online;
    }

    public synchronized void setArtistSource(List<ArtistName> artists, String publisherIP){
        Publisher publisher = new Publisher(publisherIP); //make constructor
        for(ArtistName artist : artists){
            artistsToBrokers.put(artist, this);
            publishers.put(artist,publisher);
        }
    }

    //TODO: make method synchronizes and list static
    private void acknowledgeServer(List<String> server_IPs){
        brokersList = new ArrayList<>();
        for(String ip : server_IPs){
            brokersList.add(new Broker(ip));
        }
    }

}
