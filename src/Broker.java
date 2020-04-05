import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;
import java.net.*;
import java.math.BigInteger;
import javafx.util.*;
import musicFile.MusicFile;

public class Broker {
    private static final int TO_PUB_PORT = 1999; //port for publishers and inner broker communication
    private static final int TO_CLI_PORT = 2000; //port for consumer communication
    private final String IP;
    private final BigInteger HASH_VALUE;

    private ServerSocket toPubServer; //publisher and broker server
    private ServerSocket toCliServer; //consumer server

    private ArrayList<String> brokersList; //available brokers
    private static final ArrayList<String> registeredPublishers = new ArrayList<>();
    private static final ArrayList<Pair<String,BigInteger>> registeredUsers = new ArrayList<>(); //username and password for registered Users
    //private static final ArrayList<String> loggedinUsers = new ArrayList<>(); 

    private HashMap<String, String> artistsToPublishers; //artists assigned to publishers
    private HashMap<String, String> artistsToBrokers; //artists assigned to brokers

    private final ExecutorService threadPool;

    public Broker(String IP){
        print("BROKER: Construct broker");
        this.IP = IP;
        this.HASH_VALUE = Utilities.SHA1(IP+TO_PUB_PORT);
        artistsToPublishers = new HashMap<>();
        artistsToBrokers = new HashMap<>();
        threadPool = Executors.newCachedThreadPool();
    }

    /**
     * Initialize broker
     * register all available brokers
     * @param brokerIPs online broker IPs
     */
    public void init (ArrayList<String> brokerIPs){
        print("BROKER: Initialize broker");
        brokersList = brokerIPs;
        //TODO: Read registeredUsers
    }

    /**
     * Make broker online (await incoming connections)
     */
    public void runServer(){
        print("BROKER: Make broker online");

        toPubConnection();
        toCliConnection();
    }

    //send message to publisher for the artists that it handles
    public void notifyPublisher(String message){

    }

    //send data to consumer on consumer demand
    //(PREVIOUS) String --> ArtistName
    public void pull(Socket clientConnx, String trackName, String artistName){

        String broker = artistsToBrokers.get(artistName);
        if(broker == null){ //artist doesn't exist
            try{
                //inform consumer that you will send hashmap
                ObjectOutputStream clientOut = new ObjectOutputStream(clientConnx.getOutputStream());
                clientOut.writeObject("DECLINE");
                clientOut.flush();

                clientOut.writeObject(artistsToBrokers);
                clientOut.flush();
                closeConnection(clientConnx);
            }catch(IOException e){
                //TODO exw hasei ti mpala me ta system.err help me
            }
        }else if(broker.equals(getIP())){ //the current broker is responsible for the artist
            String publisher = artistsToPublishers.get(artistName);
            try{
                //inform consumer that you will send files
                ObjectOutputStream clientOut = new ObjectOutputStream(clientConnx.getOutputStream());
                clientOut.writeObject("ACCEPT");
                clientOut.flush();

                Socket PubConnx = new Socket(publisher, Publisher.getPORT());

                //send request for music file
                ObjectOutputStream pubOut = new ObjectOutputStream(PubConnx.getOutputStream());
                pubOut.writeObject(new Pair<String,String>(trackName,artistName));
                pubOut.flush();

                //get files from publisher
                ObjectInputStream pubIn = new ObjectInputStream(PubConnx.getInputStream());
                int counter = 0;
                MusicFile file;
                while(counter < 2){
                    try{
                        while((file = (MusicFile) pubIn.readObject()) != null){
                            //send files back to consumer
                            clientOut.writeObject(file);
                            clientOut.flush();
                            counter = 0;
                        }
                    }catch(EOFException e){
                        ++counter;
                    }
                }
                closeConnection(PubConnx);
                // closeConnection(clientConnx);

            }catch(IOException | ClassNotFoundException e){
                //TODO exw hasei ti mpala me ta system.err help me
                e.printStackTrace();
            }

        }else{
            //the current broker is not responsible for the artist
            try{
                //inform consumer that you will send hashmap
                ObjectOutputStream clientOut = new ObjectOutputStream(clientConnx.getOutputStream());
                clientOut.writeObject("DECLINE");
                clientOut.flush();

                clientOut.writeObject(artistsToBrokers);
                clientOut.flush();
                // closeConnection(clientConnx);
            }catch(IOException e){
                //TODO exw hasei ti mpala me ta system.err help me
            }
        }
        closeConnection(clientConnx);
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
        print("BROKER: Make broker online for publishers/brokers");

        try {
            toPubServer = new ServerSocket(TO_PUB_PORT);
        }catch (IOException e) {
            System.err.println("BROKER: ERROR: Server could not go online for publishers/brokers");
            //TODO return statement (maybe)
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

                                //get connected client IP address
                                String clientIP = connection.getInetAddress().getHostAddress();

                                // CASE 1
                                // IP belongs to broker
                                if (brokersList.contains(clientIP)){
                                    //process connection from broker
                                    acceptBrokerConnection(connection, clientIP);
                                    return;
                                }

                                // CASE 2
                                // IP belongs to publisher
                                acceptPublisherConnection(connection);
                                
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
        print("BROKER: Make broker online for consumers");

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
                                String clientIP = connection.getInetAddress().getHostAddress();

                                acceptConsumerConnection(connection, clientIP);
        
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
     * Connect with broker and fetch his artists
     * @param connection socket for connection
     * @param brokerIP connected broker
     */
    private void acceptBrokerConnection(Socket connection, String brokerIP){
        print("BROKER: Accept broker connection");

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
    private synchronized void setOuterArtistSource(ArrayList<String> artists, String broker){
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
        print("BROKER: Accept publisher connection");

        ObjectOutputStream out;
        ObjectInputStream in;
        String clientIP = connection.getInetAddress().getHostAddress();

        // CASE 1
        // Publisher is registered
        if (registeredPublishers.contains(clientIP)){
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
            setInnerArtistSource(artists, clientIP);

            //close connection
            closeConnection(connection);

            //send info to other brokers
            notifyBrokers(artists);
            return;
        }

        // CASE 2
        // Publisher is not registered
        registerPublisher(clientIP);
        //send broker hashes
        
        //send your hash code
        try {
            out = new ObjectOutputStream(connection.getOutputStream());
            out.writeObject(getHash());
            out.flush();

            closeConnection(connection);
        } catch (IOException e) {
           print("BROKER: ACCEPT PUBLISHER CONNECTION: ERROR: Problem with output stream");
           closeConnection(connection);
        }
    }


    /**
     * Write new artist in list
     * Keep from which publisher the artist was fetched
     */
    public synchronized void setInnerArtistSource(ArrayList<String> artists, String publisher){
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
        print("BROKER: Notify brokers");

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
                threadPool.execute(notify);
            }
        }
    }

    /**
     * Pass publisher in broker's registered publisher list
     * @param clientIP publisher's IP address
     */
    private synchronized void registerPublisher(String clientIP){
        registeredPublishers.add(clientIP);
    }

    //TODO -------------------------------------- JAVADOC --------------------------------------
    public void acceptConsumerConnection(Socket conn, String consumer){
        ObjectInputStream in;
        print("Processing Consumer Connection");

        try{
            in = new ObjectInputStream(conn.getInputStream());
            String request = (String) in.readObject();
            if(request.equals("REGISTER")) registerUser(conn, consumer);
            else if(request.equals("LOGIN")) loginUser(conn, consumer);
            //else if(request.equals("LOGOUT")) logoutUser(conn, consumer);
            else if(request.equals("PULL")){
                // in = new ObjectInputStream(conn.getInputStream());
                Pair<String,String> song = (Pair) in.readObject();
                pull(conn, song.getKey(), song.getValue());
            }
        }catch(IOException | ClassNotFoundException e){
            //TODO exw hasei ti mpala me ta system.err help me
            System.err.println();
        }
    }

    //TODO -------------------------------------- JAVADOC --------------------------------------
    private synchronized void registerUser(Socket conn, String clientIP) throws IOException, ClassNotFoundException{
        boolean verified = false;
        while(!verified){
            ObjectOutputStream out = new ObjectOutputStream(conn.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(conn.getInputStream());
            Pair<String,BigInteger> credentials = (Pair<String,BigInteger>) in.readObject();
            //TODO: handle passwords -> save in file
            // try{
            //     //write to file
            //     verified = true;
            // }catch(IOException e){
            //     System.err.println("EROOR: Couls not register user to file");
            //     out.writeObject(verified);
            //     out.flush();
            // }
            
            registeredUsers.add(credentials);
            verified = true;

            out.writeObject(verified);
            out.flush();
        }
        closeConnection(conn);
    }

    //TODO -------------------------------------- JAVADOC --------------------------------------
    private synchronized void loginUser(Socket conn, String clientIP) throws IOException, ClassNotFoundException{
        ObjectInputStream in = new ObjectInputStream(conn.getInputStream());
        Pair<String,BigInteger> credentials = (Pair<String,BigInteger>) in.readObject();
        //check if registered
        boolean registered = false;
        Pair<String,BigInteger> client = null;
        for(Pair<String,BigInteger> consumer : registeredUsers){
            if(consumer.getKey().equals(credentials.getKey())){
                registered = true;
                if(credentials.getValue().equals(consumer.getValue())){
                    client = consumer;
                }
            }
        }
        //send message to consumer
        ObjectOutputStream out = new ObjectOutputStream(conn.getOutputStream());
        String message = "FALSE"; //wrong credentials
        if(!registered){
            message = "REGISTER"; //not registered
            out.writeObject(message);
            out.flush();
        }
        else if(client != null) {
            message = "VERIFIED"; //successful log-in
            out.writeObject(message);
            out.flush();
            //loggedinUsers.add(clientIP);
        }
        out.writeObject(message);
        out.flush();
        
        closeConnection(conn);
    }

    // private synchronized void logoutUser(Socket conn, String clientIP){
    //     boolean verified = false;
    //     for(int i = 0; i < loggedinUsers.size(); ++i){
    //         if(loggedinUsers.get(i).equals(clientIP)){
    //             loggedinUsers.remove(i);
    //             verified = true;
    //             break;
    //         }
    //     }
    //     //send message to consumer
    //     try{
    //         ObjectOutputStream out = new ObjectOutputStream(conn.getOutputStream());
    //         out.writeObject(verified);
    //         out.flush();
    //     }catch(IOException e){
    //         //TODO exw hasei ti mpala me ta system.err help me
    //     }
    //     closeConnection(conn);
    // }

    /**
     * Close the connection established
     */
    private void closeConnection (Socket socket){
        print("BROKER: Close socket connection");

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
