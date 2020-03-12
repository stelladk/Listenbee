import java.util.*;

public interface Broker extends Node{
    public static final List<Consumer> registeredUsers = new ArrayList<>();
    public static final List<Publisher> registeredPublishers = new ArrayList<>();

    public void calculateKeys();

    public Publisher acceptConnection(Publisher publisher);

    public Consumer acceptConnection(Consumer consumer);

    public void notifyPublisher(String message);

    public void pull(ArtistName name);
}
