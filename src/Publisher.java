import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.util.*;

import musicFile.MusicFile;
import musicFile.MusicFileHandler;

public class Publisher {
    private static final int PORT = 2001;
    private final String IP;

    private ServerSocket server;

    private ArrayList< Pair<String,BigInteger> > brokerList; //active brokers
    private Map<String, ArrayList<MusicFile>> files;
    private Map<String, ArrayList<String>> brokers; //artists assigned to brokers IPs

    private final ExecutorService threadPool;

    public Publisher(String IP){
        System.out.println("PUBLISHER: Construct publisher");
        this.IP = IP;
        threadPool = Executors.newCachedThreadPool();
    }

    /**
     * Initialize publisher
     * load tracks, get all brokers, find with whom to connect and send them the artists
     * @param brokerIPs broker's IP address
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
        } catch (IOException e) {
            System.err.println("PUBLISHER: ERROR: Server could not go online");
            //TODO return statement (maybe)
        }

            //create a thread to await connections from brokers
            Thread task = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        Socket connection;
                        try {
                            connection = server.accept();

                            //for each connection create a new thread
                            Thread processTask = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        //get <title, artist> from broker
                                        ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
                                        Pair<String, String> song = (Pair<String, String>) in.readObject();

                                        //send the music file to broker
                                        push(song.getKey(), song.getValue(), connection);
                                    }catch (IOException e){
                                        System.err.println("PUBLISHER: ONLINE: ERROR: Could not read from stream");
                                    } catch (ClassNotFoundException e) {
                                        System.err.println("PUBLISHER: ONLINE: ERROR: Could not cast Object to Pair");
                                    } finally {
                                        closeConnection(connection);
                                    }
                                }
                            });
                            threadPool.execute(processTask);
                        } catch(IOException e) {
                            System.err.println("PUBLISHER: ONLINE: ERROR: Could not accept connection");
                        }
                    }
                }
            });
            threadPool.execute(task);
//        }finally {
//            try {
//                if (server != null) server.close();
//            } catch (IOException e) {
//                System.err.println("PUBLISHER: ERROR: Server could not shut down");
//            }
//        }
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
     * Get all brokers and sort them according to their IP addresses
     * @param serverIPs brokers' IP addresses
     */
    private void getBrokerList(List<String> serverIPs) {
        System.out.println("PUBLISHER: Fetching brokers");

        brokerList = new ArrayList<>();
        ArrayList<Thread> threads = new ArrayList<>();

        for(String IP: serverIPs){
            threads.add(getServerHash(IP));
        }

        //before you continue wait for all threads to end
        for(Thread t : threads){
            try {
                t.join();
            }catch(InterruptedException e){
                System.err.println("PUBLISHER: ERROR: Thread Interrupted");
            }
        }

        brokerList.sort(new Comparator<Pair<String, BigInteger>>() {
            @Override
            public int compare(Pair<String, BigInteger> a, Pair<String, BigInteger> b) {
                return a.getValue().compareTo(b.getValue());
            }
        });
    }

    /**
     * Connect with broker and get its hash value
     * @param serverIP broker's IP address
     */
    private Thread getServerHash(String serverIP){
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                Socket connection;
                ObjectInputStream in;
                BigInteger hashValue;
                try{
                    connection = new Socket(serverIP, Broker.getToPubPort());

                    //get hash code
                    in = new ObjectInputStream(connection.getInputStream());
                    hashValue = (BigInteger) in.readObject();
                    updateBrokerList(serverIP, hashValue);
                }catch(IOException | ClassNotFoundException e){
                    System.err.println("PUBLISHER: ERROR: Could not get hash of server " + serverIP);
                }
            }
        });
        threadPool.execute(thread);
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
            threadPool.execute(task);
        }
    }

    /**
     * Find the song through the artist
     * Break the music file into chunks and send them to broker
     * If a problem occurs (ex. song doesn't exist notify about failure via sending null
     * @param title song title
     * @param artist artist name
     * @param connection open connection with broker
     */
    private void push (String title, String artist, Socket connection){
        System.out.println("PUBLISHER: Push song to broker");

        //if artist doesn't exist notify about failure
        if (!files.containsKey(artist)){
            System.out.println("PUBLISHER: No such artist exists");
            notifyFailure(connection);
            return;
        }

        boolean found = false;
        ArrayList<MusicFile> chunks = null;

        //search for the song and fetch the music file
        for (MusicFile song : files.get(artist)){
            if (song.getTrackName().equals(title)){
                found = true;

                //split the song into chunks
                chunks = MusicFileHandler.split(song);

                if (chunks == null) {
                    System.out.println("PUBLISHER: Song could not be broken to chunks");
                    notifyFailure(connection);
                    return;
                }
            }
        }

        //if song doesn't exist notify about failure
        if (!found) {
            System.out.println("PUBLISHER: No such song exists");
            notifyFailure(connection);
            return;
        }

        //send music file to broker
        try {
            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
            out.writeObject(chunks);
        } catch (IOException e) {
            System.out.println("PUBLISHER: ERROR: PUSH: Could not send file chunks");
        }
    }

    /**
     * When a file with specific metadata doesn't exist send null
     * @param connection open connection with broker
     */
    private void notifyFailure(Socket connection){
        System.out.println("PUBLISHER: Notify that song doesn't exist");

        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(connection.getOutputStream());
            out.writeObject(null);
        } catch (IOException e) {
            System.out.println("PUBLISHER: ERROR: PUSH: Could not send file chunks");
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
}
