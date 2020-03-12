import java.util.*;

public interface Node {
    public static final List<Broker> brokers = new ArrayList<>();

    public void init(int num);

    public List<Broker> getBrokers();

    public void connect();

    public void disconnect();

    public void updateNodes();
}
