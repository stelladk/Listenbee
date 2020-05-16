import musicFile.MusicFile;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import javafx.util.Pair;
import musicFile.MusicFileHandler;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.ImageIcon;
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

    private List<MusicFile> temp_tracks;

    public Consumer(String IP, String SERVER_IP, int PORT) {
        Utilities.print("CONSUMER: Create consumer");
        this.IP = IP;
        this.SERVER_IP = SERVER_IP;
        this.PORT = PORT;
        STATE = OUT;
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
     *  -1 if login failed
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
                }
            }
        } catch(IOException e){
            Utilities.printError("CONSUMER: LOGIN: ERROR: Could not get streams");
        }catch(ClassNotFoundException e){
            Utilities.printError("CONSUMER: LOGIN: ERROR: Could not cast Object to String");
        }
        closeConnection(connection);
        return -1;
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
     * Update profile photo or age
     * @param username consumer username
     * @param age updated age
     * @param photo updated photo
     * @return true if operation was successful
     */
    public boolean updateProfile(int age, File photo){
        if(user_credentials == null){return false;}
        boolean processed = true;
        if(photo != null){
            processed = updatePhoto(user_credentials, photo) && processed;
        }
        if(age != -1){
            processed = updateAge(user_credentials, age) && processed;
        }
        return processed;
    }

    //TODO: delete
    /**
     * Update profile age
     * @param username consumer username
     * @param age updated age
     * @return true if operation was successful
     */
    private boolean updateAge(Pair<String, BigInteger> credentials, int age){
        Socket connection = null;
        try {
            //open connection
            connection = new Socket(SERVER_IP, PORT);

            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
            out.writeObject("UPDATE_A");
            out.flush();

            //send username and age to responsible broker
            out = new ObjectOutputStream(connection.getOutputStream());
            out.writeObject(credentials);
            out.flush();

            out.writeObject(age);
            out.flush();

            //wait for confirmation
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
            String message = (String) in.readObject();
            switch(message){
                case "TRUE":
                    Utilities.print("CONSUMER: UPDATE AGE: Age changed");
                    return true;
                case "FALSE":
                    Utilities.print("CONSUMER: UPDATE AGE: Could not update age");
                    return false;
            }
        }catch(IOException e){
            Utilities.printError("CONSUMER: UPDATE AGE: ERROR: Could not get streams");
        }catch(ClassNotFoundException e){
            Utilities.printError("CONSUMER: UPDATE AGE: ERROR: Could not cast Object to String");
        }
        return false;
    }

    /**
     * Update profile photograph
     * @param username consumer username
     * @param photo updated photo
     * @return true if operation was successful
     */
    private boolean updatePhoto(Pair<String, BigInteger> credentials, File photo){
        Socket connection = null;
        try {
            //open connection
            connection = new Socket(SERVER_IP, PORT);

            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
            out.writeObject("UPDATE_P");
            out.flush();

            //send username and photo to responsible broker
            out = new ObjectOutputStream(connection.getOutputStream());
            out.writeObject(credentials);
            out.flush();

            byte[] buffer = Files.readAllBytes(photo.toPath());
            out.writeObject(buffer);
            out.flush();

            //wait for confirmation
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
            String message = (String) in.readObject();
            switch(message){
                case "TRUE":
                    Utilities.print("CONSUMER: UPDATE PHOTO: Photo changed");
                    return true;
                case "FALSE":
                    Utilities.print("CONSUMER: UPDATE PHOTO: Could not update photo");
                    return false;
            }
        }catch(IOException e){
            Utilities.printError("CONSUMER: UPDATE PHOTO: ERROR: Could not get streams");
        }catch(ClassNotFoundException e){
            Utilities.printError("CONSUMER: UPDATE PHOTO: ERROR: Could not cast Object to String");
        }
        return false;
    }

    /**
     * Request song from main broker
     * If song is in main broker it is received
     * Else broker sends a list of other brokers which will be queried
     * @param track song's title
     * @param artist song's artist
     * @return true if operation was successful
     */
    public boolean playData (String track, String artist, String mode) {
        Utilities.print("CONSUMER: Song request");

        boolean state = false;
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
                        state = receiveData(in, mode);
                        closeConnection(connection);
                        return state;
                    //artists doen't exist
                    case "FAILURE":
                        Utilities.printError("Artist doesn't exist");
                        closeConnection(connection);
                        return state;
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
                return false;
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
            state = false;
            switch (message) {
                case "ACCEPT":
                    state = receiveData(in, mode);
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
        return state;
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
            temp_tracks = new ArrayList<>();
            for(String artistName : artists.keySet()){
                playData("", artistName, "INFO");
            }
            return temp_tracks;
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
    private boolean receiveData (ObjectInputStream in, String mode) {
        ArrayList<MusicFile> chunks = new ArrayList<>();
        MusicFile file;
        int counter = 0; //when counter == 2 then end of all file chunks
        try {
            while (counter < 2) {
                try {
                    while ((file = (MusicFile) in.readObject()) != null) {
                        chunks.add(file);
                        counter = 0;
                    }

                    if (!chunks.isEmpty()) {
                        if (mode.equals("ONLINE")) { //save music file chunks
                            MusicFileHandler.write(chunks);
                        } else if (mode.equals("OFFLINE")) { //merge chunks and save the music file
                            MusicFile merged = MusicFileHandler.merge(chunks);
                            MusicFileHandler.write(merged);
                        } else if(mode.equals("INFO")){
                            MusicFile preview = chunks.get(0);
                            preview.setAlbumInfo(null);
                            preview.setGenre(null);
                            preview.setMetadata(null);
                            preview.setFileBytes(null);
                            temp_tracks.add(preview);
                        }
                    }

                    chunks.clear();
                }catch(EOFException e){
                    ++counter;
                }
                if (counter >= 2) break;
            }
            return true;
        } catch (IOException e) {
            Utilities.printError("CONSUMER: RECEIVE DATA: ERROR: Could not get streams");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            Utilities.printError("CONSUMER: RECEIVE DATA: ERROR: Could not cast Object to MusicFile");
        }
        return false;
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
