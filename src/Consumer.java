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
    private static String OUT = "LOGGED_OUT";
    private static String IN = "LOGGED_IN";
    private String STATE = OUT;

    public Consumer(String server_IP){
        this.server_IP = server_IP;
    }

    public void loginUser(){
        Socket conn = null;
        try{
            conn = new Socket(server_IP, PORT);
            while(true){
                //send credentials
                print("Please log in");
                ObjectOutputStream out = new ObjectOutputStream(conn.getOutputStream());
                out.writeObject(getCredentials());
                out.flush();
    
                //wait for confirmation
                ObjectInputStream in = new ObjectInputStream(conn.getInputStream());
                String message = (String) in.readObject();
                if(message.equals("REGISTER")) {
                    closeConnection(conn);
                    registerUser();
                    break;
                }else if(message.equals("VERIFIED")){
                    STATE = IN;
                    break;
                }else if(message.equals("FALSE")){
                    print("Could not login try again");
                }
            }
        }catch(IOException | ClassNotFoundException e){
            System.err.println("LOGIN ERROR: Could not connect to server");
        }
        closeConnection(conn);
    }

    // public void register(Broker broker, String artistName) throws IOException{
    //     //Socket conn = new Socket(server_IP, PORT);
    // }

    private void registerUser(){
        Socket conn = null;
        try{
            conn = new Socket(server_IP, PORT);
            ObjectOutputStream out = new ObjectOutputStream(conn.getOutputStream());
            out.writeObject("REGISTER");
            out.flush();
            while(true){
                //send credentials
                print("Please register");
                out = new ObjectOutputStream(conn.getOutputStream());
                out.writeObject(getCredentials());
                out.flush();
    
                //wait for confirmation
                ObjectInputStream in = new ObjectInputStream(conn.getInputStream());
                boolean confirmed = (boolean) in.readObject();
                if(confirmed) break;
                print("Could not register try again");
            }
        }catch(IOException | ClassNotFoundException e){
            System.err.println("REGISTRATION ERROR: Could not connect to server");
        }
        closeConnection(conn);
    }

    public void logoutUser(){
        STATE = OUT;
        // Socket conn = null;
        // try{
        //     while(true){
        //         conn = new Socket(server_IP, PORT);
        //         //send log-out message
        //         ObjectOutputStream out = new ObjectOutputStream(conn.getOutputStream());
        //         out.writeObject("OUT");
        //         out.flush();
    
        //         //wait for confirmation
        //         ObjectInputStream in = new ObjectInputStream(conn.getInputStream());
        //         boolean confirmed = (boolean) in.readObject();
        //         closeConnection(conn);
        //         if(confirmed){
        //             break;
        //         }
        //     }
        // }catch(IOException | ClassNotFoundException e){
        //     System.err.println("REGISTRATION ERROR: Could not connect to server");
        // }
        // closeConnection(conn);
    }

    public void disconnect(Broker broker, String artistName){

    }

    //request data from broke using method pull
    public void playData(String artistName, MusicFile files) throws IOException{
        if(isLoggedIn()){
            //find valid broker using hashmap
            Broker broker = brokers.get(artistName);
            //register(broker, artistName);
        }
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

    public boolean isLoggedIn(){
        return STATE.equals(IN);
    }

    private synchronized Pair<String,BigInteger> getCredentials(){
        try{
            InputStreamReader input = new InputStreamReader(System.in);
            BufferedReader buffer = new BufferedReader(input);
            print("Username: ");
            String user = buffer.readLine();
            print("Password: ");
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
        print("CONSUMER: Close socket connection");

        if (socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("CONSUMER: ERROR: Could not close socket connection");
            }
        }
    }
    
    public synchronized void print(String str){
        System.out.println(str);
    }
}
