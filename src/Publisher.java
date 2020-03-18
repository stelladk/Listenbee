import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.util.*;

public class Publisher{
    private static final int PORT = 1999;

    private HashMap<ArtistName, List<MusicFile>> files; //hash with artistName
    private HashMap<Broker, List<ArtistName>> brokers;

    public void init (List<MusicFile> songs, String IP) {
        files = new HashMap<>();
        for (MusicFile song: songs){
            if (!files.containsKey(song.artistName)) {
                files.put(song.artistName, new ArrayList<MusicFile>());
            }
            files.get(song.artistName).add(song);
        }

        /////////////////////////////////////////////////////////////////////////////////
        try (Socket socket = new Socket(IP, PORT)){
            OutputStream out = socket.getOutputStream();
            Writer writer = new OutputStreamWriter(out);
            writer = new BufferedWriter(writer);

            writer.write("Online");
            writer.flush();

            while (true){
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                List<Broker> tempBrokers = null;
                try {
                    tempBrokers = (ArrayList<Broker>) in.readObject();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                Broker max = tempBrokers.get(tempBrokers.size()-1);
                BigInteger maxHash = max.getHash();

                for (ArtistName artist : files.keySet()){
                    BigInteger hash_artist = Utilities.SHA1(artist.getName()).mod(maxHash);

                    for (Broker broker : tempBrokers) {
                        if (hash_artist.compareTo(broker.getHash()) == -1){
                            if (!brokers.containsKey(broker)){
                                brokers.put(broker, new ArrayList<>());
                            }
                            brokers.get(broker).add(artist);
                            break;
                        }
                    }

                }
            }
        } catch(IOException e){
            e.printStackTrace();
        }


    }

//    //get brokers and their hashes usinf method from broker
//    public void getBrokerList(){
//
//    }
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
