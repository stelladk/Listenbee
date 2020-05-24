package com.example.eventdeliverysystem.filehandler;

import com.example.eventdeliverysystem.utilities.Pair;
import com.example.eventdeliverysystem.utilities.Utilities;

import java.io.*;
import java.math.BigInteger;
import java.util.HashMap;

public class FileHandler {

    /**
     * Create a file for user credentials
     *
     * @return file for user credentials
     */
    public static File createUserFile() {
        // create directory for user credentials
        File dir = new File("./res/data/");
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                Utilities.printError("FILEHANDLER: ERROR: Could not create directory");
                return null;
            }
        }

        // create user credentials file
        File userFile = new File(dir, "users.txt");
        try {
            if (userFile.createNewFile()) {
                writeToUserFile("/* User Credentials */", userFile);
            }
        } catch (IOException e) {
            Utilities.printError("FILEHANDLER: ERROR: Could not create user file");
            return null;
        }

        return userFile;
    }

    /**
     * Read user file to get user credentials
     *
     * @param file file to read
     * @return HashMap with users
     */
    public static synchronized HashMap<Pair<String, BigInteger>, Pair<String, Integer>> readUsers(File file) {
        try {
            BufferedReader userReader = new BufferedReader(new FileReader(file));

            String line = userReader.readLine();
            if (!line.contains("/* User Credentials */")) {
                Utilities.printError("FILEHANDLER: ERROR: Not a valid user file");
                return null;
            }

            HashMap<Pair<String, BigInteger>, Pair<String, Integer>> users = null;
            String username;
            String password;
            String email;
            String age;
            Pair<String, BigInteger> creds;
            Pair<String, Integer> extra;
            while ((line = userReader.readLine()) != null) {
                if (line.equals("<User>")) { // beginning of user
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

                                    users = new HashMap<>();
                                    users.put(creds, extra);
                                }
                            }
                        }
                    }
                }
            }

            userReader.close();
            return users;
        } catch (IOException e) {
            Utilities.printError("FILEHANDLER: ERROR: Could not read user file");
            return null;
        }
    }

    /**
     * Write user credentials to file
     *
     * @param credentials username and password
     * @return true if the operation was successful
     */
    public static synchronized String writeUser(File file, Pair<String, BigInteger> credentials, Pair<String, Integer> extra, HashMap<Pair<String, BigInteger>, Pair<String, Integer>> registered) {
        for (Pair<String, BigInteger> user : registered.keySet()){ //check username
            if (credentials.getKey().equals(user.getKey())){
                return "EXISTS_U";
            }
            if (extra.getKey().equals(registered.get(user).getKey())){ //check email
                return "EXISTS_E";
            }
        }

        boolean processed = true;
        processed = writeToUserFile("<User>", file) && processed;
        processed = writeToUserFile("<username>"+credentials.getKey()+"</username>", file) && processed;
        processed = writeToUserFile("<password>"+credentials.getValue()+"</password>", file) && processed;
        processed = writeToUserFile("<email>"+extra.getKey()+"</email>", file) && processed;
        processed = writeToUserFile("<age>"+extra.getValue()+"</age>", file) && processed;
        processed = writeToUserFile("</User>", file) && processed;

        if (processed) registered.put(credentials, extra);

        return processed? "TRUE" : "FALSE";
    }

    /**
     * Append to user file
     *
     * @param str string to be appended
     * @return true if the operation was successful
     */
    private static synchronized boolean writeToUserFile(String str, File file){
        try {
            file.setWritable(true);

            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            writer.write(str);
            writer.newLine();

            writer.close();
            file.setWritable(false);

            return true;
        } catch(IOException e) {
            file.setWritable(false);
            Utilities.printError("FILEHANDLER: ERROR: Could not write to user file");
            return false;
        }
    }
}
