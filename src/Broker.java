import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;
import java.net.*;
import java.math.BigInteger;
import javafx.util.*;

public class Broker {
    private static final int TO_PUB_PORT = 1999; //port for publishers and inner broker communication
    private static final int TO_CLI_PORT = 2000; //port for consumer communication
    private final String IP;
    private final BigInteger HASH_VALUE;

    private ServerSocket toPubServer; //publisher and broker server
    private ServerSocket toCliServer; //consumer server

    private ArrayList<String> brokersList; //available brokers
    private static final ArrayList<Consumer> loggedinUsers = new ArrayList<>();
    private static final ArrayList<Consumer> registeredUsers = new ArrayList<>();
    private static final ArrayList<String> registeredPublishers = new ArrayList<>();

    private HashMap<String, String> artistsToPublishers; //artists assigned to publishers
    private HashMap<String, String> artistsToBrokers = new HashMap<>(); //artists assigned to brokers

    private ExecutorService threadPool = Executors.newCachedThreadPool();

    public Broker(String IP){
        System.out.println("BROKER: Construct broker");
        this.IP = IP;
        this.HASH_VALUE = Utilities.SHA1(IP+TO_PUB_PORT);
    }

    /**
     * Initialize broker
     * register all available brokers
     * @param brokerIPs online broker IPs
     */
    public void init (List<String> brokerIPs){
        System.out.println("BROKER: Initialize broker");

        acknowledgeServer(brokerIPs);
    }

    /**
     * Make broker online (await incoming connections)
     */
    public void runServer(){
        System.out.println("BROKER: Make broker online");

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
    public HashMap<String, String> getBrokers(){
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
        System.out.println("BROKER: Make broker online for publishers/brokers");

        try {
            toPubServer = new ServerSocket(TO_PUB_PORT);
        }catch (IOException e) {
            System.err.println("BROKER: ERROR: Server could not go online for publishers/brokers");
        }

        //create a thread to await connections from publishers/brokers
        Thread task = new Thread(new Runnable(){
            @Override
            public void run() {
                while (true){
                    Socket connection;
                    try {
                        connection = toPubServer.accept();
                    
                        //create thread to process connection
                        Thread processTask = new Thread(new Runnable() {
                            @Override
                            public void run() {

                                boolean proccessed = false;
                                //get connected client IP address
                                String clientIP = connection.getInetAddress().getHostAddress();
                                //find if the IP is registered to a broker
                                //TODO BETTER WAY (NEED TO CHANGE LIST<BROKER> --> LIST<STRING> W/O FOR-LOOP)
                                for (String brokerIP : brokersList){
                                    if (brokerIP.equals(clientIP)){
                                        //process connection from broker
                                        acceptBrokerConnection(connection, brokerIP);
                                        proccessed = true;
                                        break;
                                    }
                                }
                                //if not then the IP is from publisher
                                if(!proccessed){
                                    acceptPublisherConnection(connection);
                                }
                            }
                        });
                        threadPool.execute(processTask);
                    }catch (IOException e){
                        //TODO
                    }
                }
            }
        });
        threadPool.execute(task);
        // finally {
        //     try {
        //         if (toPubServer != null) toPubServer.close();
        //     } catch (IOException e) {
        //         System.err.println("ERROR: Server could not shut down");
        //     }
        // }
    }
    
    //TODO: check logged in clients and create different processing
    //Thread to accept connections from Consumers
    /**
     * Make broker online for consumers (await incoming connections)
     * Create a thread to accept each connection
     * Create a thread to process each accepted connection
     */
    private void toCliConnection(){
        System.out.println("BROKER: Make broker online for consumers");

        try {
            toCliServer = new ServerSocket(TO_CLI_PORT);
        }catch (IOException e) {
            System.err.println("BROKER: ERROR: Server could not go online for consumers");
        }

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
        // finally {
        //     try {
        //         if (toCliServer != null) toCliServer.close();
        //     } catch (IOException e) {
        //         System.err.println("ERROR: Server could not shut down");
        //     }
        // }
    }

    /**
     * TODO: send messages to check availability (ALL BROKERS INITIALIZED ARE ONLINE)
     * Register all available brokers
     * @param brokerIPs available broker IPs
     */
    private void acknowledgeServer(List<String> brokerIPs){
        brokersList = new ArrayList<>();
        brokersList.add(getIP()); //add yourself
        for (String IP : brokerIPs){
            brokersList.add(IP);
        }
    }

    /**
     * Connect with broker and fetch his artists
     * @param connection socket for connection
     * @param broker connected broker
     */
    private void acceptBrokerConnection(Socket connection, String brokerIP){
        System.out.println("BROKER: Accept broker connection");

        ObjectOutputStream out;
        ObjectInputStream in;
        ArrayList<String> artists;

        try {
            in = new ObjectInputStream(connection.getInputStream());
            artists = (ArrayList<String>) in.readObject();
            closeConnection(connection);
        } catch (IOException e) {
            System.err.println("BROKER: ERROR: ACCEPT BROKER CONNECTION: Could not read from stream");
            closeConnection(connection);
            return;
        } catch (ClassNotFoundException e) {
            System.err.println("BROKER: ERROR: ACCEPT BROKER CONNECTION: Could not cast to Object to List");
            closeConnection(connection);
            return;
        }

        setOuterArtistSource(artists, brokerIP);
    }

    /**
     * Write broker and its artists that is responsible for
     */
    private synchronized void setOuterArtistSource(List<String> artists, String broker){
        for(String artist : artists){
            artistsToBrokers.put(artist, broker);
        }
    }

    /**
     * If publisher is registered fetch songs
     * Else send hash value
     * @param connection socket for connection
     */
    public void acceptPublisherConnection(Socket connection){
        System.out.println("BROKER: Accept publisher connection");

        ObjectOutputStream out;
        ObjectInputStream in;
        String clientIP = connection.getInetAddress().getHostAddress();


        // CASE 1
        // Publisher is registered
        //TODO MAYBE WE COULD DO IT BETTER W/O FOR-LOOP
        for(String pubIP : registeredPublishers){
            if (pubIP.equals(clientIP)){
                ArrayList<String> artists;

                //wait for artists
                try {
                    in = new ObjectInputStream(connection.getInputStream());
                    artists = (ArrayList<String>) in.readObject();
                } catch (IOException e) {
                   System.err.println("BROKER: ACCEPT PUBLISHER CONNECTION: Could not read from stream");
                   return;
                } catch (ClassNotFoundException e) {
                    System.err.println("BROKER: ACCEPT PUBLISHER CONNECTION: Could not cast Object to List");
                    return;
                }

                //save artists
                setInnerArtistSource(artists, pubIP);

                //close connection
                closeConnection(connection);

                //send info to other brokers
                notifyBrokers(artists);
                return;
            }
        }

        // CASE 2
        // Publisher is not registered
        registerPublisher(clientIP);
        //send broker hashes
        //TODO CAN BE DONE BETTER (GET PART INSTEAD CREATING NEW LIST)
        
        //send your hash code
        try {
            out = new ObjectOutputStream(connection.getOutputStream());
            out.writeObject(getHash());
            out.flush();

            closeConnection(connection);
        } catch (IOException e) {
           System.out.println("BROKER: ACCEPT PUBLISHER CONNECTION: ERROR: Problem with output stream");
           closeConnection(connection);
        }
    }


    /**
     * Write new artist in list
     * Keep from which publisher the artist was fetched
     */
    public synchronized void setInnerArtistSource(List<String> artists, String publisher){
        for(String artist : artists){
            artistsToBrokers.put(artist, getIP());
            artistsToPublishers.put(artist,publisher);
        }
    }

    /**
     * Send this broke's artists to the other brokers
     * @param artists cureent broker's artists
     */
    private void notifyBrokers(ArrayList<String> artists){
        System.out.println("BROKER: Notify brokers");

        for (String broker: brokersList){
            if (!broker.equals(getIP())) { //if broker is not the current one
                Thread notify = new Thread(new Runnable(){
                    @Override
                    public void run(){
                        Socket socket;
                        ObjectOutputStream out;
                        //try until you get it
                        while(true) {
                            try{
                                socket = new Socket(broker, TO_PUB_PORT); //open connection

                                //send current broker's artists to other brokers
                                out = new ObjectOutputStream(socket.getOutputStream());
                                out.writeObject(artists);
                                out.flush();

                                closeConnection(socket);
                                break;
                            }catch(IOException e){
                                System.err.println("BROKER: NOTIFY BROKERS: ERROR: Problem notifying brokers");
                            }
                        }
                    }
                });
                notify.start();
            }
        }
    }

    /**
     * TODO PUBLISHER OBJECT IS NOT THE SHAME WITH THE REAL ONE --> SHOULD WE KEEP A STRING ?
     * FIXME
     * Pass publisher in broker's registered publisher list
     * @param clientIP publisher's IP address
     */
    private synchronized void registerPublisher(String clientIP){
        registeredPublishers.add(clientIP);
    }

    //TODO -------------------------------------- JAVADOC --------------------------------------
    public void acceptConsumerConnection(Socket conn, Consumer consumer){
        ObjectOutputStream out;
        ObjectInputStream in;
        print("Processing Consumer Connection");
    }

    //TODO -------------------------------------- JAVADOC --------------------------------------
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

    //TODO -------------------------------------- JAVADOC --------------------------------------
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

    /**
     * Close the connection established
     */
    private void closeConnection (Socket socket){
        System.out.println("BROKER: Close socket connection");

        if (socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("BROKER: ERROR: Could not close socket connection");
            }
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
