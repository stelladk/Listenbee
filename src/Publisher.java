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
    private final String RANGE; //range of artists

    private ServerSocket server;

    private ArrayList< Pair<String,BigInteger> > brokerList; //active brokers
    private Map<String, ArrayList<MusicFile>> files;
    private Map<String, ArrayList<String>> brokers; //artists assigned to brokers IPs

    private final ExecutorService threadPool;

    public Publisher(String IP, String RANGE){
        Utilities.print("PUBLISHER: Construct publisher");
        this.IP = IP;
        this.RANGE = RANGE;
        threadPool = Executors.newCachedThreadPool();
    }

    /**
     * Initialize publisher
     * load tracks, get all brokers, find with whom to connect and send them the artists
     * @param brokerIPs broker's IP address
     */
    public boolean init (List<String> brokerIPs) {
        Utilities.print("PUBLISHER: Initialize publisher");

        //load the specified songs
        files =  MusicFileHandler.read(RANGE);
        if (files == null || files.isEmpty()){
            Utilities.printError("PUBLISHER: ERROR: No available songs");
            return false;
        }

        //get all active brokers
        getBrokerList(brokerIPs);
        
        //find the brokers that are responsible for this publisher
        if (brokerList != null){
            if (brokerList.isEmpty()) {
                Utilities.printError("PUBLISHER: ERROR: No brokers found for this publisher");
                return false;
            }
            assignArtistToBroker(brokerList);
        } else {
            Utilities.printError("PUBLISHER: ERROR: No brokers initialized");
            return false;
        }
        
        //connect with responsible brokers
        //and send them publisher's artists
        informBrokers();
        return true;
    }

    /**
     * Make publisher online (await incoming connections)
     * Get the song, search for it and push it to broker
     */
    public void online(){
        Utilities.print("PUBLISHER: Make publisher online");

        try {
            //open server socket
            server = new ServerSocket(PORT);
        } catch (IOException e) {
            Utilities.printError("PUBLISHER: ERROR: Server could not go online");
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
                                        Utilities.printError("PUBLISHER: ONLINE: ERROR: Could not read from stream");
                                    } catch (ClassNotFoundException e) {
                                        Utilities.printError("PUBLISHER: ONLINE: ERROR: Could not cast Object to Pair");
                                    } finally {
                                        closeConnection(connection);
                                    }
                                }
                            });
                            threadPool.execute(processTask);
                        } catch(IOException e) {
                            Utilities.printError("PUBLISHER: ONLINE: ERROR: Could not accept connection");
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
        Utilities.print("PUBLISHER: Fetching brokers");

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
                Utilities.printError("PUBLISHER: ERROR: Thread Interrupted");
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
        Utilities.print("PUBLISHER: Get server hash value");

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
                    Utilities.printError("PUBLISHER: ERROR: Could not get hash of server " + serverIP);
                }
            }
        });
        //threadPool.execute(thread);
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
        Utilities.print("PUBLISHER: Assign artists to responsible brokers");

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
        Utilities.print("PUBLISHER: Inform brokers for their artists");

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
                        Utilities.printError("PUBLISHER: ERROR: Could not communicate artists to broker");
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
        Utilities.print("PUBLISHER: Push song to broker");

        //if artist doesn't exist notify about failure
        if (!files.containsKey(artist)){
            Utilities.printError("PUBLISHER: ERROR: No such artist exists"+artist);
            notifyFailure(connection);
            return;
        }

        boolean found = false;
        ArrayList<MusicFile> chunks = null;

        //search for the song and fetch the music file
        for (MusicFile song : files.get(artist)) {
            //song title was null user might be searching for artist
            if (title == null) break;

            if (song.getTrackName().equals(title)){
                found = true;

                //split the song into chunks
                chunks = MusicFileHandler.split(song);

                if (chunks == null) {
                    Utilities.printError("PUBLISHER: ERROR: Song could not be broken to chunks");
                    notifyFailure(connection);
                    return;
                }
            }
        }

        //if song doesn't exist OR user searched for artist return all songs
        if (!found) {
            chunks = new ArrayList<>();
            for (MusicFile song : files.get(artist)) {
                chunks.addAll(MusicFileHandler.split(song));
                // chunks.add(null); //end of chunks of specific song
            }
            // chunks.add(null); //end of all chunks
        }

        //send music file to broker
        try {
            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());

            for (MusicFile chunk : chunks){
                out.writeObject(chunk);
                out.flush();
            }

            chunks.clear(); //clear chunk list
        } catch (IOException e) {
            Utilities.printError("PUBLISHER: ERROR: PUSH: Could not send file chunks");
            chunks.clear(); //clear chunk list
        }
    }

    /**
     * When a file with specific metadata doesn't exist send null
     * @param connection open connection with broker
     */
    private void notifyFailure(Socket connection){
        Utilities.print("PUBLISHER: Notify that song doesn't exist");

        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(connection.getOutputStream());
            out.writeObject(null);
            out.flush();
            out.writeObject(null);
            out.flush();
        } catch (IOException e) {
            Utilities.printError("PUBLISHER: ERROR: PUSH: Could not send file chunks");
        }
    }

    /**
     * Close the connection established with the broker
     */
    private void closeConnection (Socket socket){
        Utilities.print("PUBLISHER: Close socket connection");

        if (socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                Utilities.printError("PUBLISHER: ERROR: Could not close socket connection");
            }
        }
    }
}
