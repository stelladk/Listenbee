import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ConsumerMain {
    private static BufferedReader reader;
    private static int input;

    private static Pair<String, BigInteger> credentials;

    public static void main(String[] args) {
        String IP;
        try {
            IP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println("ERROR: Consumer down");
            System.err.println("ERROR: Could not get IP address");
            return;
        }

        if (IP.equals("127.0.0.1")) {
            System.err.println("ERROR: Consumer down");
            System.err.println("ERROR: IP is loopback address");
            return;
        }

        Consumer consumer = new Consumer(IP, "127.0.0.1", 2000);

        reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            //while consumer not logged in keep him in main menu
            while (!consumer.isLoggedIn()) {
                input = menu(0);
                switch (input) {
                    //sign up user
                    case 1:
                        credentials = getCredentials();
                        consumer.registerUser(credentials);
                        break;
                    //login user
                    case 2:
                        credentials = getCredentials();
                        consumer.loginUser(credentials);
                        break;
                    //exit from app
                    case 0:
                        try {
                            reader.close();
                        } catch (IOException e) {
                            System.err.println("ERROR: Could not close buffer");
                        }
                        System.exit(0);
                    //wrong input choice
                    default:
                        System.out.println("Wrong input try again");
                }
            }

            //when user has logged in give him access
            while (consumer.isLoggedIn()) {
                input = menu(1);
                switch (input) {
                    //let user search for song and artist
                    case 1:
                        String title;
                        String artist;
                        input = menu(2);
                        switch (input) {
                            //online
                            case 1:
                                System.out.println("Title: ");
                                title = input();
                                System.out.println("Artist: ");
                                artist = input();
                                consumer.playData(title, artist, "ONLINE");
                                break;
                            //offline
                            case 2:
                                System.out.println("Title: ");
                                title = input();
                                System.out.println("Artist: ");
                                artist = input();
                                consumer.playData(title, artist, "OFFLINE");
                                break;
                            //default
                            default:
                                System.out.println("Wrong inputS");
                        }
                        break;
                    //log out user from up
                    case 0:
                        consumer.logoutUser();
                        break;
                    //wrong input choice
                    default:
                        System.out.println("Wrong input try again");
                }
            }
        }

    }

    /**
     * @return username and password the user gave
     */
    private static Pair<String, BigInteger> getCredentials() {
            System.out.println("Username: ");
            String username = input();

            System.out.println("Password: ");
            BigInteger password = Utilities.SHA1(input());

            return new Pair<>(username, password);
    }

    private static int menu(int choice) {
        if (choice == 0) {
            System.out.println("---------- MENU ----------");
            System.out.println("1\tSign Up");
            System.out.println("2\tLog in");
            System.out.println("0\tExit");

            String userInput = input();
            return userInput != null ? Integer.parseInt(userInput) : 0;
        } else if (choice == 1) {
            System.out.println("---------- MENU ----------");
            System.out.println("1\tSearch for song/artist");
            System.out.println("0\tLog out");

            String userInput = input();
            return userInput != null ? Integer.parseInt(userInput) : 0;
        } else {
            System.out.println("1\tListen online");
            System.out.println("2\tListen offline");

            String userInput = input();
            return userInput != null ? Integer.parseInt(userInput) : 0;
        }
    }

    /**
     * @return user input
     */
    private static String input() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            System.err.println("ERROR: Could not read input from user");
            return null;
        }
    }
}
