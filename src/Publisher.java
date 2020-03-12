import java.util.*;

public class Publisher extends Node{
    private List<ArtistName> artists;
    private List<MusicFile> files;

    public Publisher(ArrayList<ArtistName> artists, ArrayList<MusicFile> files){
        this.artists = artists;
        this.files = files;
    }

    public void getBrokerList(){

    }

    public Broker hashTopic(ArtistName name){

        return null;
    }

    public void push(ArtistName name, MusicFile file){

    }
    
    public void notifyFailure(Broker broker){

    }

}
