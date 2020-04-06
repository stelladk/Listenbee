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
    private static final ArrayList<Pair<String,BigInteger>> registeredUsers = new ArrayList<>(); //username and password for registered users

    private HashMap<String, String> artistsToPublishers; //artists assigned to publishers
    private HashMap<String, String> artistsToBrokers; //artists assigned to brokers

    private final ExecutorService threadPool;

    public Broker(String IP) {
        Utilities.print("BROKER: Construct broker");
        this.IP = IP;
        this.HASH_VALUE = Utilities.SHA1(IP+TO_PUB_PORT);
        artistsToPublishers = new HashMap<>();
        artistsToBrokers = new HashMap<>();
        threadPool = Executors.newCachedThreadPool();
    }

    /**
     * Initialize broker
     * register all available brokers
     * read all registered users
     * @param brokerIPs online broker IPs
     */
    public void init (ArrayList<String> brokerIPs) {
        Utilities.print("BROKER: Initialize broker");
        brokersList = brokerIPs;
        //TODO: Read registeredUsers
    }

    /**
     * Make broker online (await incoming connections)
     */
    public void runServer() {
        Utilities.print("BROKER: Make broker online");

        toPubConnection();
        toCliConnection();
    }

    /**
     * @return broker IP addresses and their artists
     */
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
    private void toPubConnection() {
        Utilities.print("BROKER: Make broker online for publishers/brokers");

        try {
            toPubServer = new ServerSocket(TO_PUB_PORT);
        }catch (IOException e) {
            Utilities.printError("BROKER: TO PUBLISHER CONNECTION: ERROR: Server could not go online for publishers/brokers");
             try {
                 if (toPubServer != null) toPubServer.close();
             } catch (IOException ex) {
                 Utilities.printError("BROKER: TO PUBLISHER CONNECTION: ERROR: Server could not shut down");
             }
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
                        Utilities.printError("BROKER: TO PUBLISHER CONNECTION: ERROR: Problem connecting");
                    }
                }
            }
        });
        threadPool.execute(task);
    }

    /**
     * Make broker online for consumers (await incoming connections)
     * Create a thread to accept each connection
     * Create a thread to process each accepted connection
     */
    private void toCliConnection() {
        Utilities.print("BROKER: Make broker online for consumers");

        try {
            toCliServer = new ServerSocket(TO_CLI_PORT);
        }catch (IOException e) {
            Utilities.printError("BROKER: TO CLIENT CONNECTION: ERROR: Server could not go online for consumers");
             try {
                 if (toCliServer != null) toCliServer.close();
             } catch (IOException ex) {
                 Utilities.printError("BROKER: TO CLIENT CONNECTION: ERROR: Server could not shut down");
             }
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
                        Utilities.printError("BROKER: TO CLIENT CONNECTION: ERROR: Problem connecting");
                    }
                }
            }
        });
        threadPool.execute(task);
    }

    /**
     * Connect with broker and fetch his artists
     * @param connection socket for connection
     * @param brokerIP connected broker
     */
    private void acceptBrokerConnection(Socket connection, String brokerIP) {
        Utilities.print("BROKER: Accept broker connection");

        ObjectInputStream in;
        ArrayList<String> artists;

        try {
            in = new ObjectInputStream(connection.getInputStream());
            artists = (ArrayList<String>) in.readObject();
            closeConnection(connection);
        } catch (IOException e) {
            Utilities.printError("BROKER: ERROR: ACCEPT BROKER CONNECTION: Could not read from stream");
            closeConnection(connection);
            return;
        } catch (ClassNotFoundException e) {
            Utilities.printError("BROKER: ERROR: ACCEPT BROKER CONNECTION: Could not cast to Object to ArrayList");
            closeConnection(connection);
            return;
        }

        setOuterArtistSource(artists, brokerIP);
    }

    /**
     * Write broker and its artists that is responsible for
     */
    private synchronized void setOuterArtistSource(ArrayList<String> artists, String broker) {
        for (String artist : artists){
            artistsToBrokers.put(artist, broker);
        }
    }

    /**
     * If publisher is registered fetch songs
     * Else send hash value
     * @param connection socket for connection
     */
    public void acceptPublisherConnection(Socket connection) {
        Utilities.print("BROKER: Accept publisher connection");

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
                Utilities.printError("BROKER: ACCEPT PUBLISHER CONNECTION: Could not read from stream");
                return;
            } catch (ClassNotFoundException e) {
                Utilities.printError("BROKER: ACCEPT PUBLISHER CONNECTION: Could not cast Object to ArrayList");
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
            Utilities.printError("BROKER: ACCEPT PUBLISHER CONNECTION: ERROR: Problem with output stream");
           closeConnection(connection);
        }
    }

    /**
     * Write new artist in list
     * Keep from which publisher the artist was fetched
     */
    public synchronized void setInnerArtistSource(ArrayList<String> artists, String publisher) {
        for(String artist : artists){
            artistsToBrokers.put(artist, getIP());
            artistsToPublishers.put(artist,publisher);
        }
    }

    /**
     * Send this broke's artists to the other brokers
     * @param artists cureent broker's artists
     */
    private void notifyBrokers(ArrayList<String> artists) {
        Utilities.print("BROKER: Notify brokers");

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
                                Utilities.printError("BROKER: NOTIFY BROKERS: ERROR: Problem notifying brokers");
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

    /**
     * Connect with consumer and process the request
     * @param connection socket for connection
     * @param consumer consumer IP address
     */
    public void acceptConsumerConnection(Socket connection, String consumer) {
        Utilities.print("BROKER: Accept consumer connection");

        ObjectInputStream in;
        try {
            in = new ObjectInputStream(connection.getInputStream());
            String request = (String) in.readObject();

            switch (request) {
                case "REGISTER":
                    registerUser(connection, consumer);
                    break;
                case "LOGIN":
                    loginUser(connection, consumer);
                    break;
                case "PULL":
                    Pair<String, String> song = (Pair) in.readObject();
                    pull(connection, song.getKey(), song.getValue());
                    break;
            }
        } catch(IOException e) {
            Utilities.printError("BROKER: ACCEPT CONSUMER CONNECTION: Could not read from stream");
        } catch(ClassNotFoundException e) {
            Utilities.printError("BROKER: ACCEPT PUBLISHER CONNECTION: Could not cast Object to Pair");
        }
    }

    /**
     * Save credentials in a file and inform user about his registration
     * @param connection socket for connection
     * @param clientIP consumer IP address
     */
    private synchronized void registerUser(Socket connection, String clientIP) {
        Utilities.print("BROKER: Register user");

        boolean verified = false;
        while(!verified) {
            try {
                ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(connection.getInputStream());

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
            } catch (IOException e) {
                Utilities.printError("BROKER: REGISTER USER: Could not use streams");
            } catch (ClassNotFoundException e) {
                Utilities.printError("BROKER: ACCEPT PUBLISHER CONNECTION: Could not cast Object to Pair");
            }
        }
        closeConnection(connection);
    }

    /**
     * Check credentials send by user and inform user about their validity
     * @param connection socket for connection
     * @param clientIP consumer IP address
     */
    private synchronized void loginUser(Socket connection, String clientIP) {
        Utilities.print("BROKER: Log in user");

        try {
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
            Pair<String,BigInteger> credentials = (Pair<String,BigInteger>) in.readObject();

            boolean registered = false;
            Pair<String,BigInteger> client = null;
            for (Pair<String,BigInteger> consumer : registeredUsers) {//check if consumer is registered
                if (consumer.getKey().equals(credentials.getKey())) { //check his username
                    registered = true;
                    if (credentials.getValue().equals(consumer.getValue())) { //check his password
                        client = consumer;
                    }
                }
            }

            //send message to consumer
            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
            String message = "FALSE"; //used for wrong credentials
            if (!registered) { //if not registered, sign consumer up
                message = "REGISTER";
            } else if (client != null) { //if consumer registered and used right credentials
                message = "VERIFIED";
            }
            out.writeObject(message);
            out.flush();

            closeConnection(connection);
        } catch (IOException e) {
            Utilities.printError("BROKER: LOGIN USER: Could not use streams");
        } catch (ClassNotFoundException e) {
            Utilities.printError("BROKER: LOGIN USER: Could not cast Object to Pair");
        }
    }

    /**
     * Get requested song from user <title,artist>
     * @param clientConnx socket for connection
     * @param title song title
     * @param artist artist name
     */
    public void pull(Socket clientConnx, String title, String artist) {
        Utilities.print("BROKER: Get requested song from user");

        String broker = artistsToBrokers.get(artist); //get broker (IP address) responsible for that artist
        if (broker == null){ //artist doesn't exist
            try {
                //inform consumer that you will send the brokers
                ObjectOutputStream cli_out = new ObjectOutputStream(clientConnx.getOutputStream());
                cli_out.writeObject("DECLINE");
                cli_out.flush();

                cli_out.writeObject(artistsToBrokers);
                cli_out.flush();

                closeConnection(clientConnx);
            } catch(IOException e){
                Utilities.printError("BROKER: PULL: ERROR: Could not use out stream");
            }
        } else if (broker.equals(getIP())){ //the current broker is responsible for the artist
            String publisher = artistsToPublishers.get(artist); //get publisher (IP address) responsible for this artist

            try {
                //inform consumer that you will send music files
                ObjectOutputStream cli_out = new ObjectOutputStream(clientConnx.getOutputStream());
                cli_out.writeObject("ACCEPT");
                cli_out.flush();

                Socket pubConnx = new Socket(publisher, Publisher.getPORT());

                //send request for music file to publisher
                ObjectOutputStream pub_out = new ObjectOutputStream(pubConnx.getOutputStream());
                pub_out.writeObject(new Pair<String,String>(title, artist));
                pub_out.flush();

                //get files from publisher
                ObjectInputStream pubIn = new ObjectInputStream(pubConnx.getInputStream());
                int counter = 0;
                MusicFile file;
                while (counter < 2){
                    try {
                        while((file = (MusicFile) pubIn.readObject()) != null){
                            //send file chunks back to consumer
                            cli_out.writeObject(file);
                            cli_out.flush();
                            counter = 0;
                        }
                    } catch(EOFException e){
                        ++counter;
                    }
                }

                closeConnection(pubConnx);
            }catch(IOException e) {
                Utilities.printError("BROKER: PULL: Could not use streams");
            } catch (ClassNotFoundException e) {
                Utilities.printError("BROKER: pull: Could not cast Object to MusicFile");
            }
        } else { //the current broker is not responsible for the artist
            try {
                //inform consumer that you will send brokers
                ObjectOutputStream clientOut = new ObjectOutputStream(clientConnx.getOutputStream());
                clientOut.writeObject("DECLINE");
                clientOut.flush();

                clientOut.writeObject(artistsToBrokers);
                clientOut.flush();
            }catch(IOException e){
                Utilities.printError("BROKER: PULL: Could not use streams");
            }
        }
        closeConnection(clientConnx);
    }

    /**
     * Close the connection established
     */
    private void closeConnection (Socket socket){
        Utilities.print("BROKER: Close socket connection");

        if (socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                Utilities.printError("BROKER: ERROR: Could not close socket connection");
            }
        }
    }

    @Override
    public String toString(){
        return "Broker@"+getIP()+"@"+getToPubPort()+"@"+getToCliPort()+"@"+getHash();
    }

}
