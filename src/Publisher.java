import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import javafx.util.*;

public class Publisher{
    private static final int PORT = 2001;
    private final String IP;

    private ServerSocket server;

    private Map<String, List<MusicFile>> files;
    private Map<String, List<String>> brokers; //artists assigned to brokers IPs

    public Publisher(String IP){
        this.IP = IP;
    }

    /**
     * Initialize publisher
     * load tracks, get all brokers, find with whom to connect and send them the artists
     * @param brokerIP
     * @param brokerPort
     */
    public void init (String brokerIP, int brokerPort) {
        //load the specified songs
        System.out.println("Loading songs");
        files =  MusicFileHandler.read();

        Socket socket = null;
        try {
            //open connection with broker
            socket = new Socket(brokerIP, brokerPort);
            
            //get all active brokers
            ArrayList<Pair<String,BigInteger>> brokerList =  getBrokerList(socket);
            
            //close connection with broker
            closeConnection(socket);
            
            //find the brokers that are responsible for this publisher
            if (brokerList != null){
                if (brokerList.isEmpty()) {
                    return;
                }
                assignArtistToBroker(brokerList);
            } else {
                return;
            }
            
            //connect with responsible brokers
            //and send them publisher's artists
            informBrokers();
        } catch(IOException e){
            System.err.println("ERROR: Could not initialize broker");
        }
            
    }

    /**
     * Make publisher online (await incoming connections)
     * Get the song, search for it and push it to broker
     */
    public void online(){
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
            System.err.println("ERROR: Server could not go online");
        } finally {
            try {
                if (server != null) server.close();
            } catch (IOException e) {
                System.err.println("ERROR: Server could not shut down");
            }
        }
    }

    public String getIP() {
        return IP;
    }

    public static int getPORT() {
        return PORT;
    }

    /**
     * Get all active brokers from a random broker
     * @param socket broker's socket
     * @return a list with all active brokers
     */
    private ArrayList<Pair<String,BigInteger>> getBrokerList(Socket socket) {
        ArrayList<Pair<String,BigInteger>> brokers;
        ObjectInputStream in = null;
        //open input stream with the broker to accept input
        try {
            in = new ObjectInputStream(socket.getInputStream());
            brokers = (ArrayList) in.readObject();
        } catch (ClassNotFoundException | IOException e) {
            System.err.println("ERROR: Could not cast Object to List");
            return null;
        }
        return brokers;
    }

    /**
     * Find the brokers that are responsible for this artist
     * hash(artist_name) < hash(broker_IP + broker_port)
     * @param brokerList list with active brokers
     */
    private void assignArtistToBroker (ArrayList<Pair<String,BigInteger>> brokerList){
        brokers = new HashMap<>();
        //find broker whose hash value is greater than the others
        Pair<String,BigInteger> maxBroker = brokerList.get(brokerList.size()-1);
        BigInteger maxBrokerHash = maxBroker.getValue();

        for (String artist : files.keySet()){
            //if hash(artist_name) > maximum hash(broker)
            //modulo with the maximum broker so that hash(artist_name) is in range [min_broker, max_broker]
            BigInteger hashArtist = Utilities.SHA1(artist).mod(maxBrokerHash);

            for (Pair<String,BigInteger> broker : brokerList) {
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
                        System.err.println("ERROR: Could not communicate artists to broker");
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
     * @param socket broker's socket
     */
    private void closeConnection (Socket socket){
        if (socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("ERROR: Could not close socket");
            }
        }
    }

//
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
//
//    //transfer data to broker on broker demand
//    public void push(ArtistName name, MusicFile file){
//        //send file using different threads
//        //each song raises a thread that raises mupliple threads
//        //send all the songs with artistName as key
//
//        //search hashmap to choose Broker
//
//    }
//
//    public void notifyFailure(Broker broker){
//
//    }

}
