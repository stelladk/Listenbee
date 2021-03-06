package com.example.eventdeliverysystem.models;

import com.example.eventdeliverysystem.musicfilehandler.*;
import com.example.eventdeliverysystem.utilities.Pair;
import com.example.eventdeliverysystem.utilities.Utilities;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.*;

public class Consumer {

    private final int PORT;
    private final String IP;
    private final String SERVER_IP;

    private Pair<String, BigInteger> user_credentials = null;
    private String STATE;
    private static String OUT = "LOGGED_OUT";
    private static String IN = "LOGGED_IN";

    private HashMap<String, String> artists = null; //artists assigned to brokers (IP addresses)


    private List<MusicFile> preview_tracks; //list of preview tracks for library
    private final List<MusicFile> shared_chunks; //shared arraylist with streaming chunks
    private Boolean streaming_done = true;

    public Consumer(String IP, String SERVER_IP, int PORT) {
        Utilities.print("CONSUMER: Create consumer");
        this.IP = IP;
        this.SERVER_IP = SERVER_IP;
        this.PORT = PORT;
        STATE = OUT;
        shared_chunks = new ArrayList<>();
    }

    /**
     * Register user to responsible broker
     * @param credentials pair of username and hashed password
     * @return 1 if registration was successful,
     *  0 if registration failed,
     *  -1 if username already exists,
     *  -2 if email already exists
     */
    public int registerUser(Pair<String, BigInteger> credentials, Pair<String, Integer> extra) {
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

            out.writeObject(extra);
            out.flush();

            //wait for confirmation
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
            String message = (String) in.readObject();
            if(message.equals("EXISTS_U")){
                //username already exists
                Utilities.printError("CONSUMER: REGISTER: This username already exists try again");
                closeConnection(connection);
                return -1;
            }else if(message.equals("EXISTS_E")){
                //email already exists
                Utilities.printError("CONSUMER: REGISTER: This email already exists try again");
                closeConnection(connection);
                return -2;
            }else if(message.equals("TRUE")){
                //user registration was successful
                STATE = IN;
                this.user_credentials = credentials;
                closeConnection(connection);
                return 1;
            }else if(message.equals("FALSE")){
                //user registration was unsuccessful
                Utilities.printError("CONSUMER: REGISTER: ERROR: Could not register, try again");
                closeConnection(connection);
                return 0;
            }
        } catch(IOException e) {
            Utilities.printError("CONSUMER: REGISTER: ERROR: Could not get streams");
        } catch (ClassNotFoundException e){
            Utilities.printError("CONSUMER: REGISTER: ERROR: Could not cast Object to boolean");
        }
        closeConnection(connection);
        return -1;
    }

    /**
     * User enters credentials
     * Search whether user is registered or not
     * If user is registered login else register him
     * @param credentials pair of username and hashed password
     * @return 1 if login was successful,
     *  0 if username does not exist and user has to register,
     *  -1 if password was wrong
     *  -2 if login failed
     */
    public int loginUser(Pair<String, BigInteger> credentials) {
        Utilities.print("CONSUMER: Log in user");

        Socket connection = null;
        try {
            connection = new Socket(SERVER_IP, PORT);
            // boolean processed = false;
            while(true){
                //get credentials from user and send them to responsible broker
                ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
                out.writeObject("LOGIN");
                out.flush(); 

                out = new ObjectOutputStream(connection.getOutputStream());
                out.writeObject(credentials);
                out.flush();
    
                //wait for confirmation
                ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
                String message = (String) in.readObject();
                switch (message){
                    //user hasn't been registered
                    case "REGISTER":
                        closeConnection(connection);
                        // registerUser(credentials);
                        // processed = true;
                        closeConnection(connection);
                        return 0;
                    //user has been registered
                    case "VERIFIED":
                        STATE = IN;
                        this.user_credentials = credentials;
                        // processed = true;
                        closeConnection(connection);
                        return 1;
                    //user registered but wrong credentials
                    case "FALSE":
                        Utilities.printError("CONSUMER: LOGIN: ERROR: Could not login try again");
                        closeConnection(connection);
                        return -1;
                }
            }
        } catch(IOException e){
            Utilities.printError("CONSUMER: LOGIN: ERROR: Could not get streams");
        }catch(ClassNotFoundException e){
            Utilities.printError("CONSUMER: LOGIN: ERROR: Could not cast Object to String");
        }
        closeConnection(connection);
        return -2;
    }
    
    /**
     * Logout user
     */
    public void logoutUser() {
        Utilities.print("CONSUMER: Log out user");
        STATE = OUT;
        this.user_credentials = null;
    }

    /**
     * Request song from main broker
     * If song is in main broker it is received
     * Else broker sends a list of other brokers which will be queried
     * @param track song's title
     * @param artist song's artist
     * @return true if operation was successful
     */
    public ArrayList<MusicFile> playData (String track, String artist, String mode) {
        Utilities.print("CONSUMER: Song request");
        ArrayList<MusicFile> songs = null;

        // boolean state = false;
        try {
            //CASE 1
            //consumer hasn't asked for a song yet
            //ask your main broker for song
            if (artists == null || artists.isEmpty()){
                //open connection
                Socket connection = new Socket(SERVER_IP, PORT);

                //request song
                ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
                out.writeObject("PULL");
                out.flush();
                out.writeObject(new Pair<>(track, artist));
                out.flush();

                //get answer from broker
                ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
                String message = (String) in.readObject();
                switch (message){
                    //broker has the song --> send it
                    case "ACCEPT":
                        songs = receiveData(in, mode);
                        closeConnection(connection);
                        return songs;
                    //artists doen't exist
                    case "FAILURE":
                        Utilities.printError("Artist doesn't exist");
                        closeConnection(connection);
                        return null;
                    //broker doesn't have the song --> send other brokers
                    case "DECLINE":
                        getBrokers(in);
                        
                }
                closeConnection(connection);
            }

            //CASE 2
            //check the artists list to choose the right broker
            String brokerIP = artists.get(artist);
            if (brokerIP == null) {
                Utilities.printError("Artist doesn't exist");
                return null;
            }
            Socket connection = new Socket(brokerIP, PORT);

            //request song
            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
            out.writeObject("PULL");
            out.flush();
            out.writeObject(new Pair<>(track, artist));
            out.flush();

            //get answer from broker
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
            String message = (String) in.readObject();
            switch (message) {
                case "ACCEPT":
                    songs = receiveData(in, mode);
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
        return songs;
    }

    /**
     * Request artists from main broker
     * @return list of available artists
     */
    public List<MusicFile> loadLibrary(){
        //open connection
        Socket connection = null;
        try {
            connection = new Socket(SERVER_IP, PORT);
            
            //request brokers list
            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
            out.writeObject("INIT");
            out.flush();
            
            //get answer from broker
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
            String message = (String) in.readObject();
            //broker will send artists
            if(message.equals("DECLINE")){
                getBrokers(in);
            }
            closeConnection(connection);
            preview_tracks = new ArrayList<>();
            for(String artistName : artists.keySet()){
                playData("", artistName, "INFO");
            }
            return preview_tracks;
        } catch(IOException e){
            Utilities.printError("CONSUMER: LOAD: ERROR: Could not get streams");
        } catch (ClassNotFoundException e){
            Utilities.printError("CONSUMER: LOAD: ERROR: Could not cast Object to String");
        }
        
        closeConnection(connection);
        return null;
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
    

    /**
     * Get file chunks from stream
     * If online mode is chosen then save each chunk
     * If offline mode is chosen the merge the chunks and save the music file
     * @param in input stream
     * @param mode online or offline
     */
    private ArrayList<MusicFile> receiveData (ObjectInputStream in, String mode) {
        ArrayList<MusicFile> returned = new ArrayList<>(); //arraylist with merged songs
        ArrayList<MusicFile> chunks = new ArrayList<>(); //temp chunks arraylist

        resetChunks();
        //beginStreaming(); fixme

        MusicFile file;
        int counter = 0; //when counter == 2 then end of all file chunks
        try {
            while (counter < 2) {
                try {
                    while ((file = (MusicFile) in.readObject()) != null) {
                        chunks.add(file);
                        if(mode.equals("ONLINE")){
                            addChunk(file);
                        }
                        counter = 0;
                    }

                    if (!chunks.isEmpty()) {
                        if (mode.equals("OFFLINE")) { //merge chunks and save the music file
                            MusicFile merged = MusicFileHandler.merge(chunks);
                            returned.add(merged);
                        } else if(mode.equals("INFO")){
                            MusicFile preview = chunks.get(0);
                            preview.setTrackName(preview.getTrackName().substring(2));
                            preview.setAlbumInfo(null);
                            preview.setGenre(null);
                            preview.setMetadata(null);
                            preview.setFileBytes(null);
                            preview_tracks.add(preview);
                        }
                    }

                    chunks.clear();
                }catch(EOFException e){
                    ++counter;
                }
                if (counter >= 2) break;
            }

            //endStreaming(); fixme
            return returned;
        } catch (IOException e) {
            Utilities.printError("CONSUMER: RECEIVE DATA: ERROR: Could not get streams");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            Utilities.printError("CONSUMER: RECEIVE DATA: ERROR: Could not cast Object to MusicFile");
        }

        //endStreaming(); fixme
        return null;
    }

    public boolean isStreamingDone() {
        synchronized (streaming_done){
            return streaming_done;
        }
    }

    private void beginStreaming() {
        synchronized (streaming_done){
            streaming_done = false;
        }
    }

    private void endStreaming() {
        synchronized (streaming_done) {
            streaming_done = true;
        }
    }

    /**
     * @return size of shared_chunks arraylist
     */
    public int getChunkListSize() {
        synchronized (shared_chunks){
            return shared_chunks.size();
        }
    }

    /**
     * Get and remove first chunk
     * @return first chunk of shared_chunks arraylist
     */
    public MusicFile getNextChunk() {
        synchronized (shared_chunks){
            return shared_chunks.remove(0);
        }
    }

    /**
     * Get first chunk
     * @return first chunk of shared_chunks arraylist
     */
    public MusicFile viewNextChunk() {
        synchronized (shared_chunks){
            return shared_chunks.get(0);
        }
    }

    /**
     * Add chunk to shared_chunks arraylist
     * @param chunk to be added
     */
    private void addChunk(MusicFile chunk) {
        synchronized (shared_chunks){
            shared_chunks.add(chunk);
        }
    }

    /**
     * Empty shared_chunks arraylist
     */
    private void resetChunks() {
        synchronized (shared_chunks){
            shared_chunks.clear();
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
            Utilities.printError("CONSUMER: LOGIN: ERROR: Could not cast Object to HashMap");
        }
    }

    /**
     * Close the connection established with the broker
     */
    private void closeConnection (Socket socket) {
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
