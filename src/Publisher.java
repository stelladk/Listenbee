import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Publisher{
    private static final int PORT = 1999;

    private ServerSocket server;
    private HashMap<ArtistName, List<MusicFile>> files; //hash with artistName
    private HashMap<Broker, List<ArtistName>> brokers;

    /**
     * Initialize publisher
     * load tracks, get all brokers, find with whom to connect and send them the artists
     * @param songs
     * @param IP
     */
    public void init (List<MusicFile> songs, String IP) {
        //load the specified songs
        loadTracks(songs);

        Socket socket = null;
        try {
            //open connection
            socket = new Socket(IP, PORT);

            List<Broker> brokerList =  getBrokerList(socket);

            closeConnection(socket);

            assignArtistToBroker(brokerList);

            for (Broker broker : brokers.keySet()){
                Thread task = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Socket socket_conn = null;
                        try {
                            socket_conn = new Socket(broker.getIP(), PORT);

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
            e.printStackTrace();
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

    private void loadTracks (List<MusicFile> songs){
        files = new HashMap<>();
        for (MusicFile song: songs){
            if (!files.containsKey(song.artistName)) {
                files.put(song.artistName, new ArrayList<MusicFile>());
            }
            files.get(song.artistName).add(song);
        }
    }

    //get brokers and their hashes usinf method from broker
    private List<Broker> getBrokerList(Socket socket) throws IOException {
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        try {
            return (ArrayList<Broker>) in.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void assignArtistToBroker (List<Broker> brokerList){
        //find broker whose hash value is greater than the others
        Broker maxBroker = brokerList.get(brokerList.size()-1);
        BigInteger maxBrokerHash = maxBroker.getHash();

        //find which broker is responsible for the specific artists
        for (ArtistName artist : files.keySet()){
            //if hash(artist_value) is greater than the maximum hash(broker)
            //modulo with the maximum broker so that in range [min_broker, max_broker]
            BigInteger hash_artist = Utilities.SHA1(artist.getName()).mod(maxBrokerHash);

            for (Broker broker : brokerList) {
                if (hash_artist.compareTo(broker.getHash()) < 0){
                    if (!brokers.containsKey(broker)){
                        brokers.put(broker, new ArrayList<>());
                    }
                    brokers.get(broker).add(artist);
                    break;
                }
            }
        }
    }

    private void closeConnection (Socket socket){
        if (socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
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
