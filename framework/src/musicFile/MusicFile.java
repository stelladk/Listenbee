package musicFile;

import java.awt.image.BufferedImage;
import java.io.Serializable;

public class MusicFile implements Serializable {
    private String trackName;
    private String artistName;
    private String albumInfo;
    private String genre;
    private byte[] cover;
    private byte[] metadata;
    private byte[] fileBytes;
    private BufferedImage image = null;
    private int serial = 0; //serial number for chunks

    public MusicFile(String trackName, String artistName, String albumInfo, String genre, byte[] cover, byte[] metadata, byte[] fileBytes) {
        this.trackName = trackName;
        this.artistName = artistName;
        this.albumInfo = albumInfo;
        this.genre = genre;
        this.cover = cover;
        this.metadata = metadata;
        this.fileBytes = fileBytes;
    }

    public MusicFile(String trackName, String artistName, String albumInfo, String genre, byte[] cover, byte[] metadata, byte[] fileBytes, int serial) {
        this(trackName, artistName, albumInfo, genre, cover, metadata, fileBytes);
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

    public byte[] getCover() {return cover;}

    public byte[] getMetadata() {
        return metadata;
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }

    public BufferedImage getImage(){
        return image;
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

    public void setCover(byte[] cover) {this.cover = cover;}

    public void setMetadata(byte[] metadata) {
        this.metadata = metadata;
    }

    public void setFileBytes(byte[] fileBytes) {
        this.fileBytes = fileBytes;
    }

    public void setImage(BufferedImage image){
        this.image = image;
    }

    public int compareTo(MusicFile b){
        return Integer.compare(this.getSerial(), b.getSerial());
    }
}
