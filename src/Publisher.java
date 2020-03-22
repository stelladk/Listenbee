import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Publisher{
    private final int PORT;
    private final String IP;

    private ServerSocket server;

    private Map<String, List<MusicFile>> files; //hash with artistName
    private Map<Broker, List<String>> brokers;

    public Publisher(String IP, int PORT){
        this.IP = IP;
        this.PORT = PORT;
    }

    /**
     * Initialize publisher
     * load tracks, get all brokers, find with whom to connect and send them the artists
     * @param brokerIP
     * @param brokerPort
     */
    public void init (String brokerIP, int brokerPort) {
        //load the specified songs
       files =  MusicFileHandler.read();

        Socket socket = null;
        try {
            //open connection with broker
            socket = new Socket(brokerIP, brokerPort);

            //get all active brokers
            List<Broker> brokerList =  getBrokerList(socket);

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

            //TODO
            //connect with responsible brokers
            for (Broker broker : brokers.keySet()){
                Thread task = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Socket socket_conn = null;
                        try {
                            socket_conn = new Socket(broker.getIP(), broker.getPort());

                            ObjectOutputStream out = new ObjectOutputStream(socket_conn.getOutputStream());
                            out.writeObject(brokers.get(broker));
                            out.flush();
                        } catch (IOException e){
                            e.printStackTrace();
                        } finally {
                            closeConnection(socket_conn);
                        }
                    }
                });
                task.start();
            }
        } catch(IOException e){
            System.err.println("ERROR: Could not initialize broker");
        }
    }

    public void online (){
        try {
            server = new ServerSocket(PORT);
            while (true) {

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get all active brokers from a random broker
     * @param socket broker's socket
     * @return a list with all active brokers
     */
    private List<Broker> getBrokerList(Socket socket) {
        List<Broker> brokers;
        ObjectInputStream in = null;
        //open input stream with the broker to accept input
        try {
            in = new ObjectInputStream(socket.getInputStream());
            brokers = (List<Broker>) in.readObject();
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
    private void assignArtistToBroker (List<Broker> brokerList){
        //find broker whose hash value is greater than the others
        Broker maxBroker = brokerList.get(brokerList.size()-1);
        BigInteger maxBrokerHash = maxBroker.getHash();

        for (String artist : files.keySet()){
            //if hash(artist_name) > maximum hash(broker)
            //modulo with the maximum broker so that hash(artist_name) is in range [min_broker, max_broker]
            BigInteger hashArtist = Utilities.SHA1(artist).mod(maxBrokerHash);

            for (Broker broker : brokerList) {
                if (hashArtist.compareTo(broker.getHash()) < 0){
                    if (!brokers.containsKey(broker)){
                        brokers.put(broker, new ArrayList<>());
                    }
                    brokers.get(broker).add(artist);
                    break;
                }
            }
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
