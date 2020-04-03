package musicFile;

import java.io.Serializable;
import java.util.Comparator;

public class MusicFile implements Serializable {
    //private static final long serialVersionUID = 2741844127133820194L;
    private String trackName;
    private String artistName;
    private String albumInfo;
    private String genre;
    private byte[] fileBytes;
    private int serial = 0; //serial number for chunks

    public MusicFile(String trackName, String artistName, String albumInfo, String genre, byte[] fileBytes, int serial) {
        this(trackName, artistName, albumInfo, genre, fileBytes);
    }

    public MusicFile(String trackName, String artistName, String albumInfo, String genre, byte[] fileBytes) {
        this.trackName = trackName;
        this.artistName = artistName;
        this.albumInfo = albumInfo;
        this.genre = genre;
        this.fileBytes = fileBytes;
    }

    //Accessors
    public String getTrackName() {
        return trackName;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getAlbumInfo() {
        return albumInfo;
    }

    public String getGenre() {
        return genre;
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }

    public int getSerial(){
        return serial;
    }

    public int compareTo(MusicFile b){
        return (this.getSerial() < b.getSerial())? -1 : 0;
    }
}
