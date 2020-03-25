import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;
import java.net.*;
import java.math.BigInteger; 
import java.security.MessageDigest; 
import java.security.NoSuchAlgorithmException;

public class Broker{
    public static final List<Consumer> loggedinUsers = new ArrayList<>();
    public static final List<Consumer> registeredUsers = new ArrayList<>();
    public static final List<Publisher> registeredPublishers = new ArrayList<>();

    private static final int InnerPORT = 1999; //port for publishers and inner broker communication
    private static final int ConsumerPORT = 2000;
    private String IP;
    private BigInteger HASH_VALUE;

    private List<Broker> brokersList; //list of available brokers
    private static HashMap<String, Broker> artistsToBrokers = new HashMap<>(); //artsist assigned to brokers
    private HashMap<String, Publisher> publishers; //artists assigned to publishers
    private boolean online;

    private ServerSocket innerServer; //publisher and broker server
    private ServerSocket publicServer; //consumer server

    ExecutorService threadPool = Executors.newCachedThreadPool();

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

    public void runServer() throws IOException{
        innerServer = new ServerSocket(InnerPORT);
        publicServer = new ServerSocket(ConsumerPORT);
        InnerConnections innerConn = new InnerConnections();
        ClientConnections clientConn = new ClientConnections();
        // innerConn.start();
        // clientConn.start();
        threadPool.execute(innerConn);
        threadPool.execute(clientConn);
    }

    public void acceptBrokerConnection(Socket conn, Broker broker) throws IOException{
        ObjectOutputStream out;
        ObjectInputStream in;
        List<String> artists;
        print("Processing Broker Connection");

        //get artists from other broker
        in = new ObjectInputStream(conn.getInputStream());
        try{
            artists = (List<String>) in.readObject();
        }catch(ClassNotFoundException e){
            e.printStackTrace();
            return;
        }

        setOuterArtistSource(artists, broker);
    }

    public void acceptPublisherConnection(Socket conn) throws IOException{
        ObjectOutputStream out;
        ObjectInputStream in;
        print("Processing Publisher Connection");

        String clientIP = conn.getInetAddress().getHostAddress();

        for(Publisher pub : registeredPublishers){
            //Case 1 : Publisher is registered
            if(pub.getIP().equals(clientIP)){ 
                List<String> artists;

                //wait for artists
                in = new ObjectInputStream(conn.getInputStream());
                try{
                    artists = (List<String>) ((Message<List<String>>) in.readObject()).getMessage();
                }catch(ClassNotFoundException e){
                    e.printStackTrace();
                    //notifyFailure();
                    return;
                }
        
                //save artists
                setInnerArtistSource(artists, pub);
                try{
                    conn.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
        
                //send info to other brokers
                notifyBrokers(artists);
                return;
            }
        }

        //Case 2 : Publisher is not registed
        
        //register publisher
        registerPublisher(clientIP);

        //send broker hashes
        Message<List<Broker>> msg = new Message<List<Broker>>(brokersList);
        out = new ObjectOutputStream(conn.getOutputStream());
        out.writeObject(msg);
        out.flush();
    }

    public void acceptConsumerConnection(Socket conn, Consumer consumer){
        ObjectOutputStream out;
        ObjectInputStream in;
        print("Processing Consumer Connection");
    }

    //send message to publisher for the artists that it handles
    public void notifyPublisher(String message){

    }

    //send data to consumer on consumer demand
    public void pull(String artist){
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

    public HashMap<String, Broker> getBrokers(){
        return artistsToBrokers;
    }

    public boolean isOnline(){
        return online;
    }

    //save your artists
    public synchronized void setInnerArtistSource(List<String> artists, Publisher publisher){
        for(String artist : artists){
            artistsToBrokers.put(artist, this);
            publishers.put(artist,publisher);
        }
    }

    //save other artists
    private synchronized void setOuterArtistSource(List<String> artists, Broker broker){
        for(String artist : artists){
            artistsToBrokers.put(artist, broker);
        }
    }

    private synchronized Publisher registerPublisher(String clientIP){
        Publisher publisher = new Publisher(clientIP); //make constructor
        registeredPublishers.add(publisher);
        return publisher;
    }

    private synchronized Consumer registerUser(Socket conn, String clientIP) throws IOException, ClassNotFoundException{
        ObjectInputStream in = new ObjectInputStream(conn.getInputStream());
        String username = (String) in.readObject();
        String password = (String) in.readObject();
        //TODO: handle passwords -> save in file
        //hash password
        Consumer consumer = new Consumer(clientIP);
        registeredUsers.add(consumer);
        return consumer;
    }

    private synchronized Consumer loginUser(Socket conn, String clientIP) throws IOException, ClassNotFoundException{
        ObjectInputStream in = new ObjectInputStream(conn.getInputStream());
        String username = (String) in.readObject();
        String password = (String) in.readObject();
        //TODO: check with registedUsers
        Consumer consumer = null;
        //if not registered send message to register
        loggedinUsers.add(consumer);
        return consumer;
    }

    private void notifyBrokers(List<String> artists) throws IOException{
        for(Broker broker: brokersList){
            if(broker != this){
                Thread notify = new Thread(new Runnable(){
                    @Override
                    public void run(){
                        Socket socket;
                        ObjectOutputStream out;
                        while(true){
                            try{
                                socket = new Socket(broker.getIP(), InnerPORT); //wait for connection with broker
                                out = new ObjectOutputStream(socket.getOutputStream());
                                out.writeObject(artists);
                                out.flush();
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

    //TODO: send messages to check availability
    private void acknowledgeServer(List<String> server_IPs){
        brokersList = new ArrayList<>();
        brokersList.add(this); //add yourself
        for(String ip : server_IPs){
            brokersList.add(new Broker(ip));
        }
    }

    public synchronized void print(String str){
        System.out.println(str);
    }

    //Thread to accept inner connections
    private class InnerConnections extends Thread{
        @Override
        public void run(){
            while(true){
                Socket socket;
                try{
                    socket = innerServer.accept(); //accept connection
                    print("Accepted Inner Connection");
                }catch(IOException e){
                    e.printStackTrace();
                    return;
                }
                //make thread to proccess connection
                Thread thread = new Thread(new Runnable(){
                    @Override
                    public void run(){
                        boolean proccessed = false;
                        String clientIP = socket.getInetAddress().getHostAddress();
                        for(Broker broker:brokersList){
                            if(broker.getIP().equals(clientIP)){
                                //process connection from broker
                                try{
                                    acceptBrokerConnection(socket, broker);
                                }catch(IOException e){
                                    e.printStackTrace();
                                }
                                proccessed = true;
                                break;
                            }
                        }
                        //process connection from publisher
                        if(!proccessed){
                            try{
                                acceptPublisherConnection(socket);
                            }catch(IOException e){
                                e.printStackTrace();
                            }
                        }

                        try{
                            print("Closing Inner Connection");
                            socket.close();
                        }catch(IOException e){
                            e.printStackTrace();
                        }
                    }
                });
                // thread.start();
                threadPool.execute(thread);
            }
        }
    }

    //TODO: check logged in clients and create different processing
    //Thread to accept connections from Consumers
    private class ClientConnections extends Thread{
        @Override
        public void run(){
            while(true){
                Socket socket;
                try{
                    socket = publicServer.accept(); //accept connection
                }catch(IOException e){
                    e.printStackTrace();
                    return;
                }
                //make thread to process connection
                Thread thread = new Thread(new Runnable(){
                    @Override
                    public void run(){
                        boolean processed = false;
                        String clientIP = socket.getInetAddress().getHostAddress();
                        //search in logged-in users
                        for(Consumer consumer: registeredUsers){
                            if(consumer.getIP().equals(clientIP)){
                                acceptConsumerConnection(socket, consumer);
                                processed = true;
                                break;
                            }
                        }

                        if(!processed){
                            try{
                                loginUser(socket, clientIP);
                            }catch(IOException e){
                                e.printStackTrace();
                            }catch(ClassNotFoundException e){
                                e.printStackTrace();
                            }
                        }

                        try{
                            socket.close();
                        }catch(IOException e){
                            e.printStackTrace();
                        }
                    }
                });
                // thread.start();
                threadPool.execute(thread);
            }
        }
    }
}
