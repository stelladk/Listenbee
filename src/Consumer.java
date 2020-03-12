public interface Consumer extends Node{
    public void register(Broker broker, ArtistName name);

    public void disconnect(Broker broker, ArtistName name);

    public void playData(ArtistName name, Value value);
}
