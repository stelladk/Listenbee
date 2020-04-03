package musicFile;

import java.io.Serializable;

public class MusicFile implements Serializable {
    private String trackName;
    private String artistName;
    private String albumInfo;
    private String genre;
    private byte[] fileBytes;
    private int serial = 0; //serial number for chunks

    public MusicFile(String trackName, String artistName, String albumInfo, String genre, byte[] fileBytes) {
        this.trackName = trackName;
        this.artistName = artistName;
        this.albumInfo = albumInfo;
        this.genre = genre;
        this.fileBytes = fileBytes;
    }

    public MusicFile(String trackName, String artistName, String albumInfo, String genre, byte[] fileBytes, int serial) {
        this(trackName, artistName, albumInfo, genre, fileBytes);
        this.serial = serial;
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

    //Mutator
    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public void setAlbumInfo(String albumInfo) {
        this.albumInfo = albumInfo;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setFileBytes(byte[] fileBytes) {
        this.fileBytes = fileBytes;
    }

    public int compareTo(MusicFile b){
        return (this.getSerial() < b.getSerial())? -1 : 0;
    }
}
