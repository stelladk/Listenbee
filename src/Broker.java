import java.util.*;
import java.io.*;
import java.net.*;
import java.math.BigInteger; 
import java.security.MessageDigest; 
import java.security.NoSuchAlgorithmException;

public class Broker{
    public static final List<Consumer> registeredUsers = new ArrayList<>();
    public static final List<Publisher> registeredPublishers = new ArrayList<>();

    private static final int InnerPORT = 1999; //port for publishers and inner broker communication
    private static final int ConsumerPORT = 2000;
    private String IP;
    private BigInteger HASH_VALUE;

    private List<Broker> brokersList; //list of available brokers
    private static HashMap<ArtistName, Broker> artistsToBrokers = new HashMap<>(); //artsist assigned to brokers
    private HashMap<ArtistName, Publisher> publishers; //artists assigned to publishers
    private boolean online;

    private ServerSocket innerServer; //publisher and broker server
    private ServerSocket publicServer; //consumer server

    public Broker(){

    }

    public Broker(String IP){
        this.IP = IP;
        this.HASH_VALUE = Utilities.SHA1(IP+""+InnerPORT);
    }

    public void init(List<String> server_IPs){
        try{
            IP = InetAddress.getLocalHost().getHostAddress();
            online = true;
        }catch(UnknownHostException e){
            online = false;
            return;
        }
        HASH_VALUE = Utilities.SHA1(IP+""+InnerPORT);
        
        acknowledgeServer(server_IPs);
    }

    //save data to hashmap files
    public void calculateKeys(){

    }

    public void runServer(){
        innerServer = new ServerSocket(InnerPORT);
        publicServer = new ServerSocket(ConsumerPORT);
        InnerConnections innerConn = new InnerConnections();
        ClientConnections clientConn = new ClientConnections();
        innerConn.start();
        clientConn.start();
    }

    public void acceptBrokerConnection(Socket conn, Broker broker){
        ObjectOutputStream out;
        ObjectInputStream in;
        List<ArtistName> artists;

        //get artists from other broker
        in = new ObjectInputStream(conn.getInputStream());
        try{
            artists = (List<ArtistName>) in.readObject();
        }catch(ClassNotFoundException e){
            e.printStackTrace();
            return;
        }

        setOuterArtistSource(artists, broker);
    }

    public void acceptPublisherConnection(Socket conn){
        ObjectOutputStream out;
        ObjectInputStream in;

        //register publisher
        String clientIP = conn.getInetAddress().getHostAddress();
        Publisher publisher = new Publisher(clientIP);
        registeredPublishers.add(publisher); //make constructor

        //send broker hashes
        List<ArtistName> artists;
        out = new ObjectOutputStream(conn.getOutputStream());
        out.writeObject(brokersList);
        out.flush();

        //wait for artists
        in = new ObjectInputStream(conn.getInputStream());
        try{
            artists = (List<ArtistName>) in.readObject();
        }catch(ClassNotFoundException e){
            e.printStackTrace();
            //notifyFailure();
            return;
        }

        //save artists
        setInnerArtistSource(artists, publisher);
        try{
            conn.close();
        }catch(IOException e){
            e.printStackTrace();
        }

        //send info to other brokers
        notifyBrokers(artists);
    }

    public void acceptConsumerConnection(Socket conn){
        ObjectOutputStream out;
        ObjectInputStream in;
        
    }

    //send message to publisher for the artists that it handles
    public void notifyPublisher(String message){

    }

    //send data to consumer on consumer demand
    public void pull(ArtistName name){
        //request data from publisher using push method
        //find data in hashmap
        //send the entire list with astistName as key
    }

    public static int getInnerPORT(){
        return InnerPORT;
    }

    public static int getConsumerPORT(){
        return ConsumerPORT;
    }

    public String  getIP(){
        return IP;
    }

    public BigInteger getHash(){
        return HASH_VALUE;
    }

    public HashMap<ArtistName, Broker> getBrokers(){
        return artistsToBrokers;
    }

    public boolean isOnline(){
        return online;
    }

    //save your artists
    public synchronized void setInnerArtistSource(List<ArtistName> artists, Publisher publisher){
        for(ArtistName artist : artists){
            artistsToBrokers.put(artist, this);
            publishers.put(artist,publisher);
        }
    }

    //save other artists
    private synchronized void setOuterArtistSource(List<ArtistName> artists, Broker broker){
        for(ArtistName artist : artists){
            artistsToBrokers.put(artist, broker);
        }
    }

    private void notifyBrokers(List<ArtistName> artists){
        for(Broker broker: brokersList){
            if(broker != this){
                Thread notify = new Thread(new Runnable(){
                    @Override
                    public void run(){
                        Socket socket;
                        ObjectOutputStream out;
                        while(true){
                            socket = new Socket(broker.getIP(), InnerPORT); //wait for connection with broker
                            out = new ObjectOutputStream(socket.getOutputStream());
                            out.writeObject(artists);
                            out.flush();
                            try{
                                socket.close();
                            }catch(IOException e){
                                e.printStackTrace();
                            }
                        }
                    }
                });
                notify.start();
            }
        }
    }

    //TODO: make method synchronizes and list static
    private void acknowledgeServer(List<String> server_IPs){
        brokersList = new ArrayList<>();
        brokersList.add(this); //add yourself
        for(String ip : server_IPs){
            brokersList.add(new Broker(ip));
        }
    }

    //Thread to accept inner connections
    private class InnerConnections extends Thread{
        @Override
        public void run(){
            Socket socket;
            while(true){
                socket = innerServer.accept(); 
                //make thread to proccess connection
                Thread thread = new Thread(new Runnable(){
                    @Override
                    public void run(){
                        boolean proccessed = false;
                        String clientIP = socket.getInetAddress().getHostAddress();
                        for(Broker broker:brokersList){
                            if(broker.getIP().equals(clientIP)){
                                //process connection from broker
                                acceptBrokerConnection(socket, broker);
                                proccessed = true;
                                break;
                            }
                        }
                        //process connection from publisher
                        if(!proccessed){
                            acceptPublisherConnection(socket);
                        }

                        try{
                            socket.close();
                        }catch(IOException e){
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            }
        }
    }

    //TODO: check logged in clients and create different processing
    //Thread to accept connections from Consumers
    private class ClientConnections extends Thread{
        @Override
        public void run(){
            Socket socket;
            while(true){
                socket = publicServer.accept();
                //make thread to process connection
                Thread thread = new Thread(new Runnable(){
                    @Override
                    public void run(){
                        acceptConsumerConnection(socket);
                        try{
                            socket.close();
                        }catch(IOException e){
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            }
        }
    }
}
