import java.util.*;

public abstract interface Node {
    public static final List<Broker> brokers = new ArrayList<>();

    public void init(int num, List<String> IPs);

    public List<Broker> getBrokers();

    public void connect();

    public void disconnect();

    public void updateNodes();
}
