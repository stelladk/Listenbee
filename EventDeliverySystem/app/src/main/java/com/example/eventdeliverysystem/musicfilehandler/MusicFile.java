package com.example.eventdeliverysystem.musicfilehandler;

import java.io.Serializable;

public class MusicFile implements Serializable {
    private String trackName;
    private String artistName;
    private String albumInfo;
    private String genre;
    private int duration;
    private byte[] cover;
    private byte[] metadata;
    private byte[] fileBytes;
    private int serial = 0; //serial number for chunks

    public MusicFile(String trackName, String artistName, String albumInfo, String genre, int duration, byte[] cover, byte[] metadata, byte[] fileBytes) {
        this.trackName = trackName;
        this.artistName = artistName;
        this.albumInfo = albumInfo;
        this.genre = genre;
        this.duration = duration;
        this.cover = cover;
        this.metadata = metadata;
        this.fileBytes = fileBytes;
    }

    public MusicFile(String trackName, String artistName, String albumInfo, String genre, int duration, byte[] cover, byte[] metadata, byte[] fileBytes, int serial) {
        this(trackName, artistName, albumInfo, genre, duration, cover, metadata, fileBytes);
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

    public int getDuration() {
        return duration;
    }

    public byte[] getCover() {return cover;}

    public byte[] getMetadata() {
        return metadata;
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

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setCover(byte[] cover) {this.cover = cover;}

    public void setMetadata(byte[] metadata) {
        this.metadata = metadata;
    }

    public void setFileBytes(byte[] fileBytes) {
        this.fileBytes = fileBytes;
    }

    public int compareTo(MusicFile b){
        return Integer.compare(this.getSerial(), b.getSerial());
    }
}
