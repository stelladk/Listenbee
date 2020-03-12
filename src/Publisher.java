public interface Publisher extends Node{

    public void getBrokerList();

    public Broker hashTopic(ArtistName name);

    public void push(ArtistName name, Value value);
    
    public void notifyFailure(Broker broker);

}
