import java.io.Serializable;

public class MusicFile implements Serializable {
    private String trackName;
    private String artistName;
    private String albumInfo;
    private String genre;
    private byte[] fileBytes;

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
}
