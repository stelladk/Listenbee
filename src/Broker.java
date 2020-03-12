import java.util.*;

public class Broker extends Node{
    public static final List<Consumer> registeredUsers = new ArrayList<>();
    public static final List<Publisher> registeredPublishers = new ArrayList<>();

    public void calculateKeys(){

    }

    public Publisher acceptConnection(Publisher publisher){

        return null;
    }

    public Consumer acceptConnection(Consumer consumer){

        return null;
    }

    public void notifyPublisher(String message){

    }

    public void pull(ArtistName name){

    }
}
