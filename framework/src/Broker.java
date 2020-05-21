import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.*;
import java.net.*;
import java.math.BigInteger;

import musicFile.MusicFile;

public class Broker {
    private static final int TO_PUB_PORT = 1999; // port for publishers and inner broker communication
    private static final int TO_CLI_PORT = 2000; // port for consumer communication
    private final String IP;
    private final BigInteger HASH_VALUE;

    private ServerSocket toPubServer; // publisher and broker server
    private ServerSocket toCliServer; // consumer server

    private ArrayList<String> brokersList; // available brokers
    private static final ArrayList<String> registeredPublishers = new ArrayList<>();
    private static final HashMap<Pair<String, BigInteger>, Pair<String, Integer>> registeredUsers = new HashMap<>(); // username
                                                                                                                     // and
                                                                                                                     // password
                                                                                                                     // for
                                                                                                                     // registered
                                                                                                                     // users

    private final String userFileIntro = "/* User Credentials */";
    private static File userFile; // registered users
    private static BufferedWriter userWriter; // writer for user file
    private static BufferedReader userReader; // reader for user file

    private HashMap<String, String> artistsToPublishers; // artists assigned to publishers
    private HashMap<String, String> artistsToBrokers; // artists assigned to brokers

    private final ExecutorService threadPool;

    public Broker(String IP) {
        Utilities.print("BROKER: Construct broker");
        this.IP = IP;
        this.HASH_VALUE = Utilities.SHA1(IP + TO_PUB_PORT);
        artistsToPublishers = new HashMap<>();
        artistsToBrokers = new HashMap<>();
        threadPool = Executors.newCachedThreadPool();
    }

    /**
     * Initialize broker register all available brokers read all registered users
     * 
     * @param brokerIPs online broker IPs
     */
    public void init(ArrayList<String> brokerIPs) {
        Utilities.print("BROKER: Initialize broker");
        brokersList = brokerIPs;

        // create directory
        File dir = new File("../res/data/");
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                Utilities.printError("BROKER: ERROR: Could not create directory");
                return;
            }
        }

        // create user file
        userFile = new File(dir, "users.txt");
        try {
            if (userFile.createNewFile()) {
                writeToUserFile(userFileIntro);
            }
        } catch (IOException e) {
            Utilities.printError("ERROR: Could not create user file");
        }

        // read user credentials
        readUsers();
    }

    /**
     * Make broker online (await incoming connections)
     */
    public void runServer() {
        Utilities.print("BROKER: Make broker online");

        toPubConnection();
        toCliConnection();
    }

    /**
     * @return broker IP addresses and their artists
     */
    public HashMap<String, String> getBrokers() {
        return artistsToBrokers;
    }

    /**
     * @return the port for inner broker communication and broker to publisher
     *         communication
     */
    public static int getToPubPort() {
        return TO_PUB_PORT;
    }

    /**
     * @return the port for broker to consumer communication
     */
    public static int getToCliPort() {
        return TO_CLI_PORT;
    }

    /**
     * @return broker IP address
     */
    public String getIP() {
        return IP;
    }

    /**
     * @return broker hash value
     */
    public BigInteger getHash() {
        return HASH_VALUE;
    }

    /**
     * Make broker online for publishers and other brokers (await incoming
     * connections) Create a thread to accept each connection Create a thread to
     * process each accepted connection
     */
    private void toPubConnection() {
        Utilities.print("BROKER: Make broker online for publishers/brokers");

        try {
            toPubServer = new ServerSocket(TO_PUB_PORT);
        } catch (IOException e) {
            Utilities.printError(
                    "BROKER: TO PUBLISHER CONNECTION: ERROR: Server could not go online for publishers/brokers");
            try {
                if (toPubServer != null)
                    toPubServer.close();
            } catch (IOException ex) {
                Utilities.printError("BROKER: TO PUBLISHER CONNECTION: ERROR: Server could not shut down");
            }
        }

        // create a thread to await connections from publishers/brokers
        Thread task = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Socket connection;
                    try {
                        connection = toPubServer.accept();

                        // create thread to process connection
                        Thread processTask = new Thread(new Runnable() {
                            @Override
                            public void run() {

                                // get connected client IP address
                                String clientIP = connection.getInetAddress().getHostAddress();

                                // CASE 1
                                // IP belongs to broker
                                if (brokersList.contains(clientIP)) {
                                    // process connection from broker
                                    acceptBrokerConnection(connection, clientIP);
                                    return;
                                }

                                // CASE 2
                                // IP belongs to publisher
                                acceptPublisherConnection(connection);

                            }
                        });
                        threadPool.execute(processTask);
                    } catch (IOException e) {
                        Utilities.printError("BROKER: TO PUBLISHER CONNECTION: ERROR: Problem connecting");
                    }
                }
            }
        });
        threadPool.execute(task);
    }

    /**
     * Make broker online for consumers (await incoming connections) Create a thread
     * to accept each connection Create a thread to process each accepted connection
     */
    private void toCliConnection() {
        Utilities.print("BROKER: Make broker online for consumers");

        try {
            toCliServer = new ServerSocket(TO_CLI_PORT);
        } catch (IOException e) {
            Utilities.printError("BROKER: TO CLIENT CONNECTION: ERROR: Server could not go online for consumers");
            try {
                if (toCliServer != null)
                    toCliServer.close();
            } catch (IOException ex) {
                Utilities.printError("BROKER: TO CLIENT CONNECTION: ERROR: Server could not shut down");
            }
        }

        // create a thread to await connections from consumers
        Thread task = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Socket connection = toCliServer.accept();

                        // create thread to process connection
                        Thread processTask = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String clientIP = connection.getInetAddress().getHostAddress();

                                acceptConsumerConnection(connection, clientIP);

                                try {
                                    connection.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                        });
                        threadPool.execute(processTask);
                    } catch (IOException e) {
                        Utilities.printError("BROKER: TO CLIENT CONNECTION: ERROR: Problem connecting");
                    }
                }
            }
        });
        threadPool.execute(task);
    }

    /**
     * Connect with broker and fetch his artists
     * 
     * @param connection socket for connection
     * @param brokerIP   connected broker
     */
    private void acceptBrokerConnection(Socket connection, String brokerIP) {
        Utilities.print("BROKER: Accept broker connection");

        ObjectInputStream in;
        ArrayList<String> artists;

        try {
            in = new ObjectInputStream(connection.getInputStream());
            artists = (ArrayList<String>) in.readObject();
            closeConnection(connection);
        } catch (IOException e) {
            Utilities.printError("BROKER: ERROR: ACCEPT BROKER CONNECTION: Could not read from stream");
            closeConnection(connection);
            return;
        } catch (ClassNotFoundException e) {
            Utilities.printError("BROKER: ERROR: ACCEPT BROKER CONNECTION: Could not cast to Object to ArrayList");
            closeConnection(connection);
            return;
        }

        setOuterArtistSource(artists, brokerIP);
    }

    /**
     * Write broker and its artists that is responsible for
     */
    private synchronized void setOuterArtistSource(ArrayList<String> artists, String broker) {
        for (String artist : artists) {
            artistsToBrokers.put(artist, broker);
        }
    }

    /**
     * If publisher is registered fetch songs Else send hash value
     * 
     * @param connection socket for connection
     */
    public void acceptPublisherConnection(Socket connection) {
        Utilities.print("BROKER: Accept publisher connection");

        ObjectOutputStream out;
        ObjectInputStream in;
        String clientIP = connection.getInetAddress().getHostAddress();

        // CASE 1
        // Publisher is registered
        if (registeredPublishers.contains(clientIP)) {
            ArrayList<String> artists;

            // wait for artists
            try {
                in = new ObjectInputStream(connection.getInputStream());
                artists = (ArrayList<String>) in.readObject();
            } catch (IOException e) {
                Utilities.printError("BROKER: ACCEPT PUBLISHER CONNECTION: Could not read from stream");
                return;
            } catch (ClassNotFoundException e) {
                Utilities.printError("BROKER: ACCEPT PUBLISHER CONNECTION: Could not cast Object to ArrayList");
                return;
            }

            // save artists
            setInnerArtistSource(artists, clientIP);

            // close connection
            closeConnection(connection);

            // send info to other brokers
            notifyBrokers(artists);
            return;
        }

        // CASE 2
        // Publisher is not registered
        registerPublisher(clientIP);
        // send broker hashes

        // send your hash code
        try {
            out = new ObjectOutputStream(connection.getOutputStream());
            out.writeObject(getHash());
            out.flush();

            closeConnection(connection);
        } catch (IOException e) {
            Utilities.printError("BROKER: ACCEPT PUBLISHER CONNECTION: ERROR: Problem with output stream");
            closeConnection(connection);
        }
    }

    /**
     * Write new artist in list Keep from which publisher the artist was fetched
     */
    public synchronized void setInnerArtistSource(ArrayList<String> artists, String publisher) {
        for (String artist : artists) {
            artistsToBrokers.put(artist, getIP());
            artistsToPublishers.put(artist, publisher);
        }
    }

    /**
     * Send this broke's artists to the other brokers
     * 
     * @param artists current broker's artists
     */
    private void notifyBrokers(ArrayList<String> artists) {
        Utilities.print("BROKER: Notify brokers");

        for (String broker : brokersList) {
            if (!broker.equals(getIP())) { // if broker is not the current one
                Thread notify = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Socket socket;
                        ObjectOutputStream out;
                        // try until you get it
                        while (true) {
                            try {
                                socket = new Socket(broker, TO_PUB_PORT); // open connection

                                // send current broker's artists to other brokers
                                out = new ObjectOutputStream(socket.getOutputStream());
                                out.writeObject(artists);
                                out.flush();

                                closeConnection(socket);
                                break;
                            } catch (IOException e) {
                                Utilities.printError("BROKER: NOTIFY BROKERS: ERROR: Problem notifying brokers");
                            }
                        }
                    }
                });
                threadPool.execute(notify);
            }
        }
    }

    /**
     * Pass publisher in broker's registered publisher list
     * 
     * @param clientIP publisher's IP address
     */
    private synchronized void registerPublisher(String clientIP) {
        registeredPublishers.add(clientIP);
    }

    /**
     * Connect with consumer and process the request
     * 
     * @param connection socket for connection
     * @param consumer   consumer IP address
     */
    public void acceptConsumerConnection(Socket connection, String consumer) {
        Utilities.print("BROKER: Accept consumer connection");

        ObjectInputStream in;
        try {
            in = new ObjectInputStream(connection.getInputStream());
            String request = (String) in.readObject();

            switch (request) {
                case "REGISTER":
                    registerUser(connection, consumer);
                    break;
                case "LOGIN":
                    loginUser(connection, consumer);
                    break;
                case "UPDATE_P":
                    addUserPhoto(connection, in);
                    break;
                case "PULL":
                    Pair<String, String> song = (Pair) in.readObject();
                    pull(connection, song.getKey(), song.getValue());
                    break;
                case "INIT":
                    pull(connection, null, "_INIT");
                    break;
            }
        } catch (IOException e) {
            Utilities.printError("BROKER: ACCEPT CONSUMER CONNECTION: Could not read from stream");
        } catch (ClassNotFoundException e) {
            Utilities.printError("BROKER: ACCEPT PUBLISHER CONNECTION: Could not cast Object to Pair");
        }
    }

    /**
     * Save credentials in a file and inform user about his registration
     * 
     * @param connection socket for connection
     * @param clientIP   consumer IP address
     */
    private void registerUser(Socket connection, String clientIP) {
        Utilities.print("BROKER: Register user");

        try {
            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());

            Pair<String, BigInteger> credentials = (Pair<String, BigInteger>) in.readObject();
            Pair<String, Integer> extra = (Pair<String, Integer>) in.readObject();

            String message = writeUser(credentials, extra);

            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            Utilities.printError("BROKER: REGISTER USER: Could not use streams");
        } catch (ClassNotFoundException e) {
            Utilities.printError("BROKER: ACCEPT PUBLISHER CONNECTION: Could not cast Object to Pair");
        }

        closeConnection(connection);
    }

    /**
     * Check credentials send by user and inform user about their validity
     * 
     * @param connection socket for connection
     * @param clientIP   consumer IP address
     */
    private synchronized void loginUser(Socket connection, String clientIP) {
        Utilities.print("BROKER: Log in user");

        try {
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
            Pair<String, BigInteger> credentials = (Pair<String, BigInteger>) in.readObject();

            boolean registered = false;
            Pair<String, BigInteger> client = null;
            for (Pair<String, BigInteger> consumer : registeredUsers.keySet()) {// check if consumer is registered
                if (consumer.getKey().equals(credentials.getKey())) { // check his username
                    registered = true;
                    if (credentials.getValue().equals(consumer.getValue())) { // check his password
                        client = consumer;
                    }
                }
            }

            // send message to consumer
            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
            String message = "FALSE"; // used for wrong credentials
            if (!registered) { // if not registered, sign consumer up
                message = "REGISTER";
            } else if (client != null) { // if consumer registered and used right credentials
                message = "VERIFIED";
            }
            out.writeObject(message);
            out.flush();

            closeConnection(connection);
        } catch (IOException e) {
            Utilities.printError("BROKER: LOGIN USER: Could not use streams");
        } catch (ClassNotFoundException e) {
            Utilities.printError("BROKER: LOGIN USER: Could not cast Object to Pair");
        }
    }

    /**
     * Get requested song from user <title,artist>
     * 
     * @param clientConnx socket for connection
     * @param title       song title
     * @param artist      artist name
     */
    private void pull(Socket clientConnx, String title, String artist) {
        Utilities.print("BROKER: Get requested song from user");

        String broker = artistsToBrokers.get(artist); // get broker (IP address) responsible for that artist

        if (artist.equals("_INIT")) { // consumer wants the list of brokers
            broker = "0.0.0.0";
        }

        if (broker == null) { // artist doesn't exist
            try {
                // inform consumer that you will send the brokers
                ObjectOutputStream cli_out = new ObjectOutputStream(clientConnx.getOutputStream());
                cli_out.writeObject("FAILURE");
                cli_out.flush();

                closeConnection(clientConnx);
            } catch (IOException e) {
                Utilities.printError("BROKER: PULL: ERROR: Could not use out stream");
            }
        } else if (broker.equals(getIP())) { // the current broker is responsible for the artist
            String publisher = artistsToPublishers.get(artist); // get publisher (IP address) responsible for this
                                                                // artist

            try {
                // inform consumer that you will send music files
                ObjectOutputStream cli_out = new ObjectOutputStream(clientConnx.getOutputStream());
                cli_out.writeObject("ACCEPT");
                cli_out.flush();

                Socket pubConnx = new Socket(publisher, Publisher.getPORT());

                // send request for music file to publisher
                ObjectOutputStream pub_out = new ObjectOutputStream(pubConnx.getOutputStream());
                pub_out.writeObject(new Pair<String, String>(title, artist));
                pub_out.flush();

                // get files from publisher
                ObjectInputStream pubIn = new ObjectInputStream(pubConnx.getInputStream());
                int counter = 0;
                MusicFile file;
                while (counter < 2) {
                    try {
                        while ((file = (MusicFile) pubIn.readObject()) != null) {
                            // send file chunks back to consumer
                            cli_out.writeObject(file);
                            cli_out.flush();
                            counter = 0;
                        }
                        cli_out.writeObject(null);
                        cli_out.flush();
                    } catch (EOFException e) {
                        ++counter;
                    }
                }

                closeConnection(pubConnx);
            } catch (IOException e) {
                Utilities.printError("BROKER: PULL: Could not use streams");
            } catch (ClassNotFoundException e) {
                Utilities.printError("BROKER: pull: Could not cast Object to MusicFile");
            }
        } else { // the current broker is not responsible for the artist
            try {
                // inform consumer that you will send brokers
                ObjectOutputStream clientOut = new ObjectOutputStream(clientConnx.getOutputStream());
                clientOut.writeObject("DECLINE");
                clientOut.flush();

                clientOut.writeObject(artistsToBrokers);
                clientOut.flush();
            } catch (IOException e) {
                Utilities.printError("BROKER: PULL: Could not use streams");
            }
        }
        closeConnection(clientConnx);
    }

    /**
     * Read user file to get user credentials
     * 
     * @return true if the operation was successful
     */
    private synchronized boolean readUsers() {
        try {
            userReader = new BufferedReader(new FileReader(userFile));
            String line = userReader.readLine();
            if (!line.contains(userFileIntro)) {
                Utilities.printError("Not a valid user file");
                return false;
            }
            String username;
            String password;
            String email;
            String age;
            Pair<String, BigInteger> creds;
            Pair<String, Integer> extra;
            while ((line = userReader.readLine()) != null) {
                if (line.equals("<User>")) { // begining of user
                    line = userReader.readLine();
                    if (line.startsWith("<username>") && line.endsWith("</username>")) {
                        username = line.substring(line.indexOf(">") + 1, line.lastIndexOf("<")); // username
                        line = userReader.readLine(); // next line
                        if (line.startsWith("<password>") && line.endsWith("</password>")) {
                            password = line.substring(line.indexOf(">") + 1, line.lastIndexOf("<")); // password
                            line = userReader.readLine(); // next line
                            if (line.startsWith("<email>") && line.endsWith("</email>")) {
                                email = line.substring(line.indexOf(">") + 1, line.lastIndexOf("<")); // email
                                line = userReader.readLine(); // next line
                                if (line.startsWith("<age>") && line.endsWith("</age>")) {
                                    age = line.substring(line.indexOf(">") + 1, line.lastIndexOf("<")); // age
                                    creds = new Pair<String, BigInteger>(username, new BigInteger(password));
                                    extra = new Pair<String, Integer>(email, Integer.parseInt(age));
                                    registeredUsers.put(creds, extra);
                                }
                            }
                        }
                    }
                }
            }
            userReader.close();
            return true;
        } catch (IOException e) {
            Utilities.printError("ERROR: Could not read user file");
            return false;
        }
    }

    private synchronized void addUserPhoto(Socket clientConnx, ObjectInputStream in) {

        try {
            Pair<String, BigInteger> user = (Pair<String, BigInteger>) in.readObject();
            byte[] send_photo = (byte[]) in.readObject();

            File saved_photo = new File("../res/data/" + user.getKey() + ".jpg");
            FileOutputStream fout = new FileOutputStream(saved_photo);
            fout.write(send_photo);
            fout.close();
            
            ObjectOutputStream out = new ObjectOutputStream(clientConnx.getOutputStream());
            out.writeObject("TRUE");
            out.flush();
            closeConnection(clientConnx);
            return;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            ObjectOutputStream out = new ObjectOutputStream(clientConnx.getOutputStream());
            out.writeObject("FALSE");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        closeConnection(clientConnx);
    }

    // private synchronized void updateUser(Socket clientConnx, ObjectInputStream in) {
    //     try {
    //         Pair<String, BigInteger> user = (Pair<String, BigInteger>) in.readObject();
    //         int age = (int) in.readObject();

    //         Pair<String, Integer> extra = registeredUsers.get(user);
    //         if(extra != null){ //valid user
    //             extra = new Pair<String, Integer>(extra.getKey(), age);
    //             if(extra.getValue() != age && age > 0){
    //                 registeredUsers.put(user, extra);
    //                 userFile.setWritable(true);
    //                 userWriter = new BufferedWriter(new FileWriter(userFile, false));
    //                 userWriter.write(userFileIntro);
    //                 for(Pair<String, BigInteger> creds : registeredUsers.keySet()){
    //                     writeUser(creds, registeredUsers.get(creds));
    //                 }
    //             }
    //         }
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     } catch (ClassNotFoundException e) {
    //         e.printStackTrace();
    //     }

    // }

    /**
     * Write user credentials to file
     * @param credentials user's username and password
     * @return true if the operation was successful
     */
    private synchronized String writeUser(Pair<String,BigInteger> credentials, Pair<String, Integer> extra){
        for(Pair<String,BigInteger> user : registeredUsers.keySet()){ //check username
            if(credentials.getKey().equals(user.getKey())){
                return "EXISTS_U";
            }
            if(extra.getKey().equals(registeredUsers.get(user).getKey())){ //check email
                return "EXISTS_E";
            }
        }
        boolean processed = true;
        processed = writeToUserFile("<User>") && processed;
        processed = writeToUserFile("<username>"+credentials.getKey()+"</username>") && processed;
        processed = writeToUserFile("<password>"+credentials.getValue()+"</password>") && processed;
        processed = writeToUserFile("<email>"+extra.getKey()+"</email>") && processed;
        processed = writeToUserFile("<age>"+extra.getValue()+"</age>") && processed;
        processed = writeToUserFile("</User>") && processed;
        if(processed) registeredUsers.put(credentials, extra);
        return processed? "TRUE" : "FALSE";
    }

    /**
     * Append to user file
     * @param str String to be appended
     * @return true if the orepation was successful
     */
    private synchronized boolean writeToUserFile(String str){
        try{
            userFile.setWritable(true);
            userWriter = new BufferedWriter(new FileWriter(userFile, true));
            userWriter.write(str);
            userWriter.newLine();
            userWriter.close();
            userFile.setWritable(false);
            return true;
        }catch(IOException e){
            userFile.setWritable(false);
            Utilities.printError("ERROR: Could not write to user file");
            return false;
        }
    }

    /**
     * Close the connection established
     */
    private void closeConnection (Socket socket){
        Utilities.print("BROKER: Close socket connection");

        if (socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                Utilities.printError("BROKER: ERROR: Could not close socket connection");
            }
        }
    }

    @Override
    public String toString(){
        return "Broker@"+getIP()+"@"+getToPubPort()+"@"+getToCliPort()+"@"+getHash();
    }

}
