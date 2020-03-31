import musicFile.MusicFile;

import java.util.*;

import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.*;

public class Consumer{
    public static final int PORT = 2000;
    public String server_IP;

    private HashMap<String, Broker> brokers; //brokers with artists

    public Consumer(String server_IP){
        this.server_IP = server_IP;
    }

    public void loginUser(){
        try{
            Socket conn = new Socket(server_IP, Broker.getToCliPort());
            while(true){
                //send credentials
                System.out.println("Please log in");
                ObjectOutputStream out = new ObjectOutputStream(conn.getOutputStream());
                out.writeObject(getCredentials());
                out.flush();
    
                //wait for confirmation
                ObjectInputStream in = new ObjectInputStream(conn.getInputStream());
                String message = (String) in.readObject();
                if(message.equals("REGISTER")) {
                    closeConnection(conn);
                    registerUser(conn);
                    break;
                }else if(message.equals("VERIFIED")){
                    
                    break;
                }else if(message.equals("FALSE")){
                    System.out.println("Could not login try again");
                }
            }
            closeConnection(conn);
        }catch(IOException | ClassNotFoundException e){
            System.err.println("REGISTRATION ERROR: Could not connect to server");
        }
    }

    // public void register(Broker broker, String artistName) throws IOException{
    //     //Socket conn = new Socket(server_IP, PORT);
    // }

    private void registerUser(Socket conn){
        try{
            while(true){
                //send credentials
                System.out.println("Please register");
                ObjectOutputStream out = new ObjectOutputStream(conn.getOutputStream());
                out.writeObject(getCredentials());
                out.flush();
    
                //wait for confirmation
                ObjectInputStream in = new ObjectInputStream(conn.getInputStream());
                boolean confirmed = (boolean) in.readObject();
                if(confirmed) break;
                System.out.println("Could not register try again");
            }
        }catch(IOException | ClassNotFoundException e){
            System.err.println("REGISTRATION ERROR: Could not connect to server");
        }
    }

    public void logoutUser(){
        try{
            Socket conn = new Socket(server_IP, Broker.getToCliPort());
            while(true){
                //send log-out message
                ObjectOutputStream out = new ObjectOutputStream(conn.getOutputStream());
                out.writeObject("OUT");
                out.flush();
    
                //wait for confirmation
                ObjectInputStream in = new ObjectInputStream(conn.getInputStream());
                boolean confirmed = (boolean) in.readObject();
                if(confirmed) break;
            }
            closeConnection(conn);
        }catch(IOException | ClassNotFoundException e){
            System.err.println("REGISTRATION ERROR: Could not connect to server");
        }
    }

    public void disconnect(Broker broker, String artistName){

    }

    //request data from broke using method pull
    public void playData(String artistName, MusicFile files) throws IOException{
        //find valid broker using hashmap
        Broker broker = brokers.get(artistName);
        //register(broker, artistName);
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

    private synchronized Pair<String,BigInteger> getCredentials(){
        try{
            InputStreamReader input = new InputStreamReader(System.in);
            BufferedReader buffer = new BufferedReader(input);
            System.out.println("Username: ");
            String user = buffer.readLine();
            System.out.println("Password: ");
            return new Pair<>(user, Utilities.SHA1(buffer.readLine()));
        }catch(IOException e){
            System.err.println("ERROR: Could not read credentials");
            return null;
        }
    }    
    
    /**
     * Close the connection established with the broker
     */
    private void closeConnection (Socket socket){
        System.out.println("CONSUMER: Close socket connection");

        if (socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("CONSUMER: ERROR: Could not close socket connection");
            }
        }
    }
}
