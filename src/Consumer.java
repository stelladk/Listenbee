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

    private HashMap<String, String> artists = null; //artists assigned to brokers
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

    //request data from broker using method pull
    public void playData(String artistName, String trackName) throws IOException{
        if(isLoggedIn()){
            try{
                if(artists == null | artists.isEmpty()){
                    //CASE 1: ask your main server for song
                    //make connection with server
                    Socket conn = new Socket(server_IP, PORT);

                    //request song
                    ObjectOutputStream out = new ObjectOutputStream(conn.getOutputStream());
                    out.writeObject(new Pair<String,String>(artistName, trackName));
                    out.flush();
                    
                    ObjectInputStream in = new ObjectInputStream(conn.getInputStream());
                    String message = (String) in.readObject();
                    if(message.equals("ACCEPT")){
                        receiveData(in);
                        closeConnection(conn);
                        return;
                    }else if(message.equals("DECLINE")){
                        getBrokers(in);
                    }
                    closeConnection(conn);
                }
                //CASE 2: check with artists list to choose the right server
                String brokerIP = artists.get(artistName);
                Socket conn = new Socket(brokerIP, PORT);

                //request song
                ObjectOutputStream out = new ObjectOutputStream(conn.getOutputStream());
                out.writeObject(new Pair<String,String>(artistName, trackName));
                out.flush();

                ObjectInputStream in = new ObjectInputStream(conn.getInputStream());
                String message = (String) in.readObject();
                if(message.equals("ACCEPT")){
                    receiveData(in);
                }else{
                    System.err.println("INCONSISTENCY IN BROKERS");
                }
                closeConnection(conn);

            }catch(IOException | ClassNotFoundException e){
                //TODO
            }
            
        }
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

    private void receiveData(ObjectInputStream in) throws IOException, ClassNotFoundException{
        ArrayList<MusicFile> song = new ArrayList<>();
        MusicFile file;
        while((file = (MusicFile) in.readObject()) != null){
            song.add(file);
        }
    }
    
    //get brokers and their assigned artists
    private void getBrokers(ObjectInputStream in) throws IOException, ClassNotFoundException{
        HashMap<String, String> artists = (HashMap) in.readObject();
        if(artists != null){
            this.artists = artists;
            return;
        }
        throw new ClassNotFoundException();
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
