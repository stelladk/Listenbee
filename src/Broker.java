import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;
import java.net.*;
import java.math.BigInteger; 
import java.security.MessageDigest; 
import java.security.NoSuchAlgorithmException;
import javafx.util.*;

public class Broker {
    private static final int TO_PUB_PORT = 1999; //port for publishers and inner broker communication
    private static final int TO_CLI_PORT = 2000; //port for consumer communication
    private final String IP;
    private final BigInteger HASH_VALUE;

    private ServerSocket toPubServer; //publisher and broker server
    private ServerSocket toCliServer; //consumer server

    private List<Broker> brokersList; //available brokers
    public static final List<Consumer> loggedinUsers = new ArrayList<>();
    public static final List<Consumer> registeredUsers = new ArrayList<>();
    public static final List<Publisher> registeredPublishers = new ArrayList<>();
    private HashMap<String, Publisher> publishers; //artists assigned to publishers
    private static HashMap<String, Broker> artistsToBrokers = new HashMap<>(); //artists assigned to brokers

    ExecutorService threadPool = Executors.newCachedThreadPool();

    public Broker(String IP){
        this.IP = IP;
        this.HASH_VALUE = Utilities.SHA1(IP+TO_PUB_PORT);
    }

    /**
     * Initialize broker
     * register all available brokers
     * @param brokerIPs online broker IPs
     */
    public void init (List<String> brokerIPs){
        acknowledgeServer(brokerIPs);
        //TODO
    }
    
    public void runServer(){
        toPubConnection();
        toCliConnection();
    }

    //send message to publisher for the artists that it handles
    public void notifyPublisher(String message){

    }

    //send data to consumer on consumer demand
    //(PREVIOUS) String --> ArtistName
    public void pull(String name){
        //request data from publisher using push method
        //find data in hashmap
        //send the entire list with astistName as key
    }

    //(PREVIOUS) String --> ArtistName
    public HashMap<String, Broker> getBrokers(){
        return artistsToBrokers;
    }

    /**
     * @return the port for inner broker communication and broker to publisher communication
     */
    public static int getToPubPort() {
        return TO_PUB_PORT;
    }
    
    /**
     * @return the port for broker to consumer communication
     */
    public static int getToCliPort() {
        return TO_CLI_PORT;
    }

    /**
     * @return broker IP address
     */
    public String  getIP(){
        return IP;
    }

    /**
     * @return broker hash value
     */
    public BigInteger getHash(){
        return HASH_VALUE;
    }

    /**
     * Make broker online for publishers and other brokers (await incoming connections)
     * Create a thread to accept each connection
     * Create a thread to process each accepted connection
     */
    private void toPubConnection(){
        try {
            toPubServer = new ServerSocket(TO_PUB_PORT);

            //create a thread to await connections from publishers/brokers
            Thread task = new Thread(new Runnable(){
                @Override
                public void run() {
                    while (true){
                        try {
                            Socket connection = toPubServer.accept();

                            //create thread to process connection
                            Thread processTask = new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    boolean proccessed = false;
                                    String clientIP = connection.getInetAddress().getHostAddress();
                                    for(Broker broker:brokersList){
                                        if(broker.getIP().equals(clientIP)){
                                            //process connection from broker
                                            try{
                                                acceptBrokerConnection(connection, broker);
                                            }catch(IOException e){
                                                e.printStackTrace();
                                            }
                                            proccessed = true;
                                            break;
                                        }
                                    }
                                    //process connection from publisher
                                    if(!proccessed){
                                        try{
                                            System.out.println("Got publisher request");
                                            acceptPublisherConnection(connection);
                                        }catch(IOException e){
                                            e.printStackTrace();
                                        }
                                    }
            
                                    try{
                                        print("Closing Inner Connection");
                                        connection.close();
                                    }catch(IOException e){
                                        e.printStackTrace();
                                    }
                                }
                            });
                            threadPool.execute(processTask);
                        } catch (IOException e){
                            //TODO
                        }
                    }
                }
            });
            threadPool.execute(task);
        }catch (IOException e) {
            System.err.println("ERROR: Server could not go online");
        } finally {
            try {
                if (toPubServer != null) toPubServer.close();
            } catch (IOException e) {
                System.err.println("ERROR: Server could not shut down");
            }
        }
    }
    
    //TODO: check logged in clients and create different processing
    //Thread to accept connections from Consumers
    /**
     * Make broker online for consumers (await incoming connections)
     * Create a thread to accept each connection
     * Create a thread to process each accepted connection
     */
    private void toCliConnection(){
        try {
            toCliServer = new ServerSocket(TO_CLI_PORT);

            //create a thread to await connections from consumers
            Thread task = new Thread(new Runnable(){
                @Override
                public void run() {
                    while (true){
                        try {
                            Socket connection = toCliServer.accept();

                            //create thread to process connection
                            Thread processTask = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    boolean processed = false;
                                    String clientIP = connection.getInetAddress().getHostAddress();
                                    //search in logged-in users
                                    for(Consumer consumer: registeredUsers){
                                        if(consumer.getIP().equals(clientIP)){
                                            acceptConsumerConnection(connection, consumer);
                                            processed = true;
                                            break;
                                        }
                                    }
            
                                    if(!processed){
                                        try{
                                            loginUser(connection, clientIP);
                                        }catch(IOException e){
                                            e.printStackTrace();
                                        }catch(ClassNotFoundException e){
                                            e.printStackTrace();
                                        }
                                    }
            
                                    try{
                                        connection.close();
                                    }catch(IOException e){
                                        e.printStackTrace();
                                    }

                                }
                            });
                            threadPool.execute(processTask);
                        } catch (IOException e){
                            //TODO
                        }
                    }
                }
            });
            threadPool.execute(task);
        }catch (IOException e) {
            System.err.println("ERROR: Server could not go online");
        } finally {
            try {
                if (toCliServer != null) toCliServer.close();
            } catch (IOException e) {
                System.err.println("ERROR: Server could not shut down");
            }
        }
    }
    
    //save data to hashmap files
    public void calculateKeys(){

    }
    
    public void acceptBrokerConnection(Socket conn, Broker broker) throws IOException{
        ObjectOutputStream out;
        ObjectInputStream in;
        List<String> artists;
        print("Processing Broker Connection");

        //get artists from other broker
        in = new ObjectInputStream(conn.getInputStream());
        try{
            artists = (List<String>) in.readObject();
        }catch(ClassNotFoundException e){
            e.printStackTrace();
            return;
        }

        setOuterArtistSource(artists, broker);
    }

    public void acceptPublisherConnection(Socket conn) throws IOException{
        ObjectOutputStream out;
        ObjectInputStream in;
        print("Processing Publisher Connection");

        String clientIP = conn.getInetAddress().getHostAddress();

        for(Publisher pub : registeredPublishers){
            //Case 1 : Publisher is registered
            if(pub.getIP().equals(clientIP)){ 
                List<String> artists;

                //wait for artists
                in = new ObjectInputStream(conn.getInputStream());
                try{
                    artists = (List<String>) in.readObject();
                }catch(ClassNotFoundException e){
                    e.printStackTrace();
                    //notifyFailure();
                    return;
                }
        
                //save artists
                setInnerArtistSource(artists, pub);
                try{
                    conn.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
                
                //send info to other brokers
                notifyBrokers(artists);
                return;
            }
        }
        
        //Case 2 : Publisher is not registed
        
        //register publisher
        registerPublisher(clientIP);
        System.out.println("Accepted request from Publisher");
        //send broker hashes
        ArrayList<Pair<String,BigInteger>> brokers = new ArrayList<>();
        for(Broker broker : brokersList){
            brokers.add(new Pair<>(broker.getIP(), broker.getHash()));
        }
        out = new ObjectOutputStream(conn.getOutputStream());
        out.writeObject(brokers);
        out.flush();
    }
    
    public void acceptConsumerConnection(Socket conn, Consumer consumer){
        ObjectOutputStream out;
        ObjectInputStream in;
        print("Processing Consumer Connection");
    }
    
    //save your artists
    public synchronized void setInnerArtistSource(List<String> artists, Publisher publisher){
        for(String artist : artists){
            artistsToBrokers.put(artist, this);
            publishers.put(artist,publisher);
        }
    }
    
    //save other artists
    private synchronized void setOuterArtistSource(List<String> artists, Broker broker){
        for(String artist : artists){
            artistsToBrokers.put(artist, broker);
        }
    }
    
    private synchronized Publisher registerPublisher(String clientIP){
        Publisher publisher = new Publisher(clientIP); //make constructor
        registeredPublishers.add(publisher);
        return publisher;
    }
    
    private synchronized Consumer registerUser(Socket conn, String clientIP) throws IOException, ClassNotFoundException{
        ObjectInputStream in = new ObjectInputStream(conn.getInputStream());
        String username = (String) in.readObject();
        String password = (String) in.readObject();
        //TODO: handle passwords -> save in file
        //hash password
        Consumer consumer = new Consumer(clientIP);
        registeredUsers.add(consumer);
        return consumer;
    }
    
    private synchronized Consumer loginUser(Socket conn, String clientIP) throws IOException, ClassNotFoundException{
        ObjectInputStream in = new ObjectInputStream(conn.getInputStream());
        String username = (String) in.readObject();
        String password = (String) in.readObject();
        //TODO: check with registedUsers
        Consumer consumer = null;
        //if not registered send message to register
        loggedinUsers.add(consumer);
        return consumer;
    }

    private void notifyBrokers(List<String> artists) throws IOException{
        for(Broker broker: brokersList){
            if(broker.equals(this)){
                Thread notify = new Thread(new Runnable(){
                    @Override
                    public void run(){
                        Socket socket;
                        ObjectOutputStream out;
                        while(true){
                            try{
                                socket = new Socket(broker.getIP(), TO_PUB_PORT); //wait for connection with broker
                                out = new ObjectOutputStream(socket.getOutputStream());
                                out.writeObject(artists);
                                out.flush();
                                socket.close();
                            }catch(IOException e){
                                e.printStackTrace();
                            }
                        }
                    }
                });
                notify.start();
            }
        }
    }

    //TODO: send messages to check availability (ALL BROKERS INITIALIZED ARE ONLINE)
    /**
     * Register all available brokers
     * @param brokerIPs available broker IPs
     */
    private void acknowledgeServer(List<String> brokerIPs){
        brokersList = new ArrayList<>();
        brokersList.add(this); //add yourself
        for (String IP : brokerIPs){
            brokersList.add(new Broker(IP));
        }
    }

    @Override
    public String toString(){
        return "Broker@"+getIP()+"@"+getToPubPort()+"@"+getToCliPort()+"@"+getHash();
    }

    public synchronized void print(String str){
        System.out.println(str);
    }

}
