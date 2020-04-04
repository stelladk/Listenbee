import musicFile.MusicFile;

import java.util.*;
import javafx.util.Pair;
import musicFile.MusicFileHandler;

import javax.rmi.CORBA.Util;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.*;

public class Consumer {

    private final int PORT;
    private final String IP;
    private final String SERVER_IP;

    private String STATE;
    private static String OUT = "LOGGED_OUT";
    private static String IN = "LOGGED_IN";

    private HashMap<String, String> artists = null; //artists assigned to brokers

    public Consumer(String IP, String SERVER_IP, int PORT) {
        Utilities.print("CONSUMER: Create consumer");
        this.IP = IP;
        this.SERVER_IP = SERVER_IP;
        this.PORT = PORT;
        STATE = OUT;

    }

    /**
     * Register user to responsible broker
     */
    public void registerUser(Pair<String, BigInteger> credentials) {
        Utilities.print("CONSUMER: Register user");

        Socket connection = null;
        try {
            //open connection
            connection = new Socket(SERVER_IP, PORT);

            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
            out.writeObject("REGISTER");
            out.flush();

            //send credentials to responsible broker
            out = new ObjectOutputStream(connection.getOutputStream());
            out.writeObject(credentials);
            out.flush();

            //wait for confirmation
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
            boolean confirmed = (boolean) in.readObject();
            //user registration was successful
            if (confirmed) {
                STATE = IN;
                return;
            }
            //user registration was unsuccessful
            Utilities.printError("CONSUMER: REGISTER: ERROR: Could not register, try again");
        } catch(IOException e) {
            Utilities.printError("CONSUMER: REGISTER: ERROR: Could not get streams");
        } catch (ClassNotFoundException e){
            Utilities.printError("CONSUMER: REGISTER: ERROR: Could not cast Object to boolean");
        }
        closeConnection(connection);
    }

    /**
     * User enters credentials
     * Search whether user is registered or not
     * If user is registered login else register him
     */
    public void loginUser(Pair<String, BigInteger> credentials) {
        Utilities.print("CONSUMER: Log in user");

        Socket connection = null;
        try {
            connection = new Socket(SERVER_IP, PORT);
            while(true){
                //get credentials from user and send them to responsible broker
                ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
                out.writeObject(credentials);
                out.flush();
    
                //wait for confirmation
                ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
                String message = (String) in.readObject();
                switch (message){
                    //user hasn't been registered
                    case "REGISTER":
                        closeConnection(connection);
                        registerUser(credentials);
                        break;
                    //user has been registered
                    case "VERIFIED":
                        STATE = IN;
                        break;
                    //user registered but wrong credentials
                    case "FALSE":
                        Utilities.printError("CONSUMER: LOGIN: ERROR: Could not login try again");
                }
            }
        } catch(IOException e){
            Utilities.printError("CONSUMER: LOGIN: ERROR: Could not get streams");
        }catch(ClassNotFoundException e){
            Utilities.printError("CONSUMER: LOGIN: ERROR: Could not cast Object to String");
        }
        closeConnection(connection);
    }

    /**
     * Logout user
     */
    public void logoutUser() {
        Utilities.print("CONSUMER: Log out user");

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


    /**
     * Request song from main broker
     * If song is in main broker it is received
     * Else broker sends a list of other brokers which will be queried
     * @param track song's title
     * @param artist song's artist
     */
    public void playData (String track, String artist, String mode) {
        Utilities.print("CONSUMER: Song request");

        try {
            //CASE 1
            //consumer hasn't asked for a song yet
            //ask your main broker for song
            if (artists == null || artists.isEmpty()){
                //open connection
                Socket connection = new Socket(SERVER_IP, PORT);

                //request song
                ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
                out.writeObject(new Pair<>(track, artist));
                out.flush();

                //get answer from broker
                ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
                String message = (String) in.readObject();
                switch (message){
                    //broker has the song --> send it
                    case "ACCEPT":
                        receiveData(in, mode);
                        closeConnection(connection);
                        return;
                    //broker doesn't have the song --> send other brokers
                    case "DECLINE":
                        getBrokers(in);
                }
                closeConnection(connection);
            }

            //CASE 2
            //check the artists list to choose the right broker
            String brokerIP = artists.get(artist);
            Socket connection = new Socket(brokerIP, PORT);

            //request song
            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
            out.writeObject(new Pair<>(track, artist));
            out.flush();

            //get answer from broker
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
            String message = (String) in.readObject();
            switch (message) {
                case "ACCEPT":
                    receiveData(in, mode);
                    break;
                default:
                    Utilities.printError("CONSUMER: PLAY: ERROR: INCONSISTENCY IN BROKERS");
            }
            closeConnection(connection);
        } catch(IOException e){
            Utilities.printError("CONSUMER: PLAY: ERROR: Could not get streams");
        } catch (ClassNotFoundException e){
            Utilities.printError("CONSUMER: PLAY: ERROR: Could not cast Object to String");
        }
    }

    /**
     * @return consumer IP address
     */
    public String getIP(){
        return IP;
    }

    /**
     * @return true if user is logged in, else return false
     */
    public boolean isLoggedIn(){
        return STATE.equals(IN);
    }

    private void receiveData (ObjectInputStream in, String mode) {
        ArrayList<MusicFile> chunks = new ArrayList<>();
        MusicFile file;
        try {
            //TODO FIND SOLUTION FOR MULTIPLE SONGS
            //get chunks from stream
            while ((file = (MusicFile) in.readObject()) != null){
                chunks.add(file);
            }

            //TODO MODE CHECK

            //TODO DO NOT MERGE DIFFERENT SONGS
        } catch (IOException e) {
            Utilities.printError("CONSUMER: LOGIN: ERROR: Could not get streams");
        } catch (ClassNotFoundException e) {
            Utilities.printError("CONSUMER: LOGIN: ERROR: Could not cast Object to String");
        }
    }

    /**
     * Get brokers and their artists
     * @param in socket input stream
     */
    private void getBrokers (ObjectInputStream in) {
        try {
            artists = (HashMap) in.readObject();
        } catch (IOException e) {
            Utilities.printError("CONSUMER: LOGIN: ERROR: Could not get streams");
        } catch (ClassNotFoundException e) {
            Utilities.printError("CONSUMER: LOGIN: ERROR: Could not cast Object to String");
        }
    }

    private void requestSong(){

    }

    /**
     * Close the connection established with the broker
     */
    private void closeConnection (Socket socket){
        Utilities.print("CONSUMER: Close socket connection");

        if (socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                Utilities.printError("PUBLISHER: ERROR: Could not close socket connection");
            }
        }
    }
}
