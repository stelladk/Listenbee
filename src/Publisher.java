import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import javafx.util.*;

import musicFile.MusicFile;
import musicFile.MusicFileHandler;

public class Publisher {
    private static final int PORT = 2001;
    private final String IP;

    private ServerSocket server;

    private ArrayList<Pair<String,BigInteger>> brokerList; //active brokers
    private Map<String, ArrayList<MusicFile>> files;
    private Map<String, ArrayList<String>> brokers; //artists assigned to brokers IPs

    public Publisher(String IP){
        System.out.println("PUBLISHER: Construct publisher");
        this.IP = IP;
    }

    /**
     * Initialize publisher
     * load tracks, get all brokers, find with whom to connect and send them the artists
     * @param brokerIP broker's IP address
     * @param brokerPort broker's port number
     */
    public void init (List<String> brokerIPs) {
        System.out.println("PUBLISHER: Initialize publisher");

        //load the specified songs
        files =  MusicFileHandler.read();

        //get all active brokers
        getBrokerList(brokerIPs);
        
        //find the brokers that are responsible for this publisher
        if (brokerList != null){
            if (brokerList.isEmpty()) {
                System.err.println("PUBLISHER: ERROR: No brokers found for this publisher");
                return;
            }
            assignArtistToBroker(brokerList);
        } else {
            System.err.println("PUBLISHER: ERROR: No brokers initialized");
            return;
        }
        
        //connect with responsible brokers
        //and send them publisher's artists
        informBrokers();
    }

    /**
     * Make publisher online (await incoming connections)
     * Get the song, search for it and push it to broker
     */
    public void online(){
        System.out.println("PUBLISHER: Make publisher online");

        try {
            //open server socket
            server = new ServerSocket(PORT);

            //await for connections
            while (true) {
                try {
                    Socket connection = server.accept();

                    //for each connection create a new thread
                    Thread task = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
                                /* TODO
                                 *get input from broker (song etc artist asked for)
                                 *search for them
                                 *send them back using push
                                */
                            }catch (IOException e){
                                //TODO
                            }finally {
                                closeConnection(connection);
                            }
                        }
                    });
                    task.start();
                } catch(IOException e) {
                    //TODO
                }
            }
        } catch (IOException e) {
            System.err.println("PUBLISHER: ERROR: Server could not go online");
        } finally {
            try {
                if (server != null) server.close();
            } catch (IOException e) {
                System.err.println("PUBLISHER: ERROR: Server could not shut down");
            }
        }
    }

    /**
     * @return publisher's IP address
     */
    public String getIP() {
        return IP;
    }

    /**
     * @return publisher's port number
     */
    public static int getPORT() {
        return PORT;
    }

    /**
     * Get all active brokers from a random broker
     * @param socket broker's socket
     * @return a list with all active brokers
     */
    private void getBrokerList(List<String> serverIPs) {
        System.out.println("PUBLISHER: Fetching brokers");

        brokerList = new ArrayList<>();
        ArrayList<Thread> threads = new ArrayList<>();

        for(String IP: serverIPs){
            threads.add(getServerHash(IP));
        }
        for(Thread t : threads){
            try{
                t.join();
            }catch(InterruptedException e){
                System.err.println("ERROR@getBrokerList: Thread Interrupted");
            }
        }

        Collections.sort(brokerList, new Comparator<Pair<String,BigInteger>>() {
            @Override
            public int compare(Pair<String,BigInteger> a, Pair<String,BigInteger> b){
                return a.getValue().compareTo(b.getValue());
            }
        });

    }

    private Thread getServerHash(String serverIP){
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                Socket connection;
                ObjectInputStream in;
                BigInteger HASH;
                try{
                    connection = new Socket(IP, Broker.getToPubPort());

                    //get hash code
                    in = new ObjectInputStream(connection.getInputStream());
                    HASH = (BigInteger) in.readObject();
                    updateBrokerList(IP, HASH);
                }catch(IOException | ClassNotFoundException e){
                    System.err.println("ERROR: Could not get hash of server "+IP);
                }
                
            }
        });
        thread.start();
        return thread;
    }

    private synchronized void updateBrokerList(String IP, BigInteger HASH){
        brokerList.add(new Pair<>(IP, HASH));
    }

    /**
     * Find the brokers that are responsible for this artist
     * hash(artist_name) < hash(broker_IP + broker_port)
     * @param brokerList list with active brokers
     */
    private void assignArtistToBroker (ArrayList< Pair<String,BigInteger> > brokerList){
        System.out.println("PUBLISHER: Assign artists to responsible brokers");

        brokers = new HashMap<>();
        //find broker whose hash value is greater than the others
        Pair<String,BigInteger> maxBroker = brokerList.get(brokerList.size() - 1);
        BigInteger maxBrokerHash = maxBroker.getValue();

        for (String artist : files.keySet()){
            //if hash(artist_name) > maximum hash(broker)
            //modulo with the maximum broker so that hash(artist_name) is in range [min_broker, max_broker]
            BigInteger hashArtist = Utilities.SHA1(artist).mod(maxBrokerHash);

            for (Pair<String,BigInteger> broker : brokerList) { //for each broker IP address
                if (hashArtist.compareTo(broker.getValue()) < 0){
                    if (!brokers.containsKey(broker.getKey())){
                        brokers.put(broker.getKey(), new ArrayList<>());
                    }
                    brokers.get(broker.getKey()).add(artist);
                    break;
                }
            }
        }
    }

    /**
     * Connect with responsible brokers and send them publisher's artists
     */
    private void informBrokers(){
        System.out.println("PUBLISHER: Inform brokers for their artists");

        for (String broker : brokers.keySet()){
            Thread task = new Thread(new Runnable() {
                @Override
                public void run() {
                    Socket socket_conn = null;
                    try {
                        socket_conn = new Socket(broker, Broker.getToPubPort());

                        ObjectOutputStream out = new ObjectOutputStream(socket_conn.getOutputStream());
                        out.writeObject(brokers.get(broker));
                        out.flush();
                    } catch (IOException e){
                        System.err.println("PUBLISHER: ERROR: Could not communicate artists to broker");
                    } finally {
                        closeConnection(socket_conn);
                    }
                }
            });
            task.start();
        }
    }

    /**
     * Close the connection established with the broker
     */
    private void closeConnection (Socket socket){
        System.out.println("PUBLISHER: Close socket connection");

        if (socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("PUBLISHER: ERROR: Could not close socket connection");
            }
        }
    }


//    //find the right broker using getBrokerList
//    public Broker hashTopic(ArtistName name){
//        String hashValue = SHA1(name.artistName);
//        for(Broker br: brokers){
//            if(br.getHash() >= hashValue){ //mod
//                return br;
//            }
//        }
//        return null;
//    }

//    //transfer data to broker on broker demand
//    public void push(ArtistName name, handler.MusicFile file){
//        //send file using different threads
//        //each song raises a thread that raises mupliple threads
//        //send all the songs with artistName as key
//
//        //search hashmap to choose Broker
//
//    }

//    public void notifyFailure(Broker broker){
//
//    }

}
