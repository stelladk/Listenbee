import java.util.*;
import java.io.IOException;
import java.net.*;

public class Consumer{
    public static final int PORT = 2000;
    public String server_IP;

    private HashMap<ArtistName, Broker> brokers;

    public Consumer(String server_IP){
        this.server_IP = server_IP;
    }

    public void register(Broker broker, ArtistName name) throws IOException{
        //Socket conn = new Socket(server_IP, PORT);
    }

    public void disconnect(Broker broker, ArtistName name){

    }

    //request data from broke using method pull
    public void playData(ArtistName name, MusicFile files) throws IOException{
        //find valid broker using hashmap
        Broker broker = brokers.get(name);
        register(broker, name);
    }

    //{ListOfBrokers {IP,Port} , < BrokerId, ArtistName>}.
    //get brokers and their assigned artists
    public void getBrokers(){

    }

    public String getIP(){
        try{
            return InetAddress.getLocalHost().getHostAddress();
        }catch(UnknownHostException e){
            return null;
        }
    }
}
