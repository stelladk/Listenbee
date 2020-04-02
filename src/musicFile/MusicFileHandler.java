package musicFile;

import com.mpatric.mp3agic.*;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MusicFileHandler {

    /**
     * Reads music files into a Map, with artist as the key and artist's songs as values
     * @return a Map with the artists and their songs
     */
    public static Map<String, ArrayList<MusicFile>> read(String range){
        System.out.println("HANDLER: Reading music files");

        //get the directory
        File dir = new File("./res/dataset1/");
        if (!dir.exists()) {
            System.err.println("HANDLER: READ: ERROR: Directory doesn't exist");
            return null;
        }
        //get all files in the directory
        File[] files = dir.listFiles();

        Map<String, ArrayList<MusicFile>> songs = new HashMap<>();
        if (files != null){
            for (File file : files) {
                try {
                    FileInputStream stream = new FileInputStream(file);
                    Mp3File mp3 = new Mp3File(file);

                    if (mp3.hasId3v2Tag()) {
                        ID3v2 tag = mp3.getId3v2Tag();

                        String artist = tag.getArtist();
                        if (artist == null){
                            stream.close();
                            continue;
                        }

                        //if artist name not in range continue to next file
                        if (!artist.matches(range)) {
                            stream.close();
                            continue;
                        }


                        //if title is null then get it from file signature
                        String title = tag.getTitle();
                        if (title == null || title.isEmpty()) {
                            title = file.getName().substring(0, file.getName().indexOf('.'));
                            tag.setTitle(title);
                        }

                        if (!songs.containsKey(artist)){ //if artist doesn't exist make a new record
                            songs.put(artist, new ArrayList<MusicFile>());
                        }
                        songs.get(artist).add(new MusicFile(title, artist, tag.getAlbum(), tag.getGenreDescription(), Files.readAllBytes(file.toPath())));

                        stream.close();
                    }
                } catch (IOException | UnsupportedTagException | InvalidDataException e) {
                    System.err.println("HANDLER: READ: ERROR: Could not parse file");
                    return null;
                }
            }
        }
        return !songs.isEmpty() ? songs : null;
    }

    /**
     * Save a song to Downloads directory
     * @param file the music file
     * @return true if the song was saved to directory
     */
    public static boolean write(MusicFile file) {
        System.out.println("HANDLER: Writing music file");

        //if the file is null then cancel the activity
        if (file == null) {
            System.err.println("HANDLER: WRITE: ERROR: Null object passed");
            return false;
        }

        if (file.getTrackName() == null || file.getArtistName() == null){
            System.err.println("HANDLER: WRITE: ERROR: Null metadata in file");
            return false;
        }

        //create directory if it doesn't exist
        File dir = new File("./res/Downloads/");
        if (!dir.exists()) {
           if (!dir.mkdir()) {
               System.err.println("HANDLER: WRITE: ERROR: Could not create directory");
               return false;
           }
        }

        //create file
        File savedFile = new File(dir, file.getTrackName() + ".mp3");

        try {
            //save file
            Mp3File mp3 = new Mp3File("./res/dataset1/" + file.getTrackName() + ".mp3");
            ID3v2 tag = new ID3v24Tag();
            mp3.setId3v2Tag(tag);

            //set tags
            tag.setTitle(file.getTrackName());
            tag.setArtist(file.getArtistName());
            if (file.getAlbumInfo() != null) tag.setAlbum(file.getAlbumInfo());
            if (file.getGenre() != null) tag.setGenreDescription(file.getGenre());

            //save mp3 file
            mp3.save(savedFile.toString());

            return true;
        } catch (IOException | UnsupportedTagException | InvalidDataException | NotSupportedException e) {
            System.out.println("HANDLER: WRITE: ERROR: Could not write file to directory");
            return false;
        }
    }

    /**
     * Splits a file into chunks of 512KB size
     * 512KB = 512000 bytes
     * @param file music file
     * @return a list with file chunks
     */
    public static ArrayList<MusicFile> split (MusicFile file) {
        System.out.println("HANDLER: Splitting music file");

        //if the file is null then cancel the activity
        if (file == null) {
            System.err.println("HANDLER: WRITE: ERROR: Null object passed");
            return null;
        }

        final byte[] songBytes = file.getFileBytes();

        //if file doesn't contain a byte array
        if ((songBytes == null) || (songBytes.length == 0)){
            System.err.println("HANDLER: SPLIT: ERROR: File without byte array");
            return null;
        }

        int chunkSize = 512000; //512KB
        ArrayList<MusicFile> chunks = new ArrayList<>();

        int start = 0;
        while (start < songBytes.length){
            chunks.add(new MusicFile(
                            file.getTrackName(),
                            file.getArtistName(),
                            file.getAlbumInfo(),
                            file.getGenre(),
                            Arrays.copyOfRange(songBytes, start, Math.min(start + chunkSize, songBytes.length)) //if end index is greater than the array length then use the array length
                    )
            );
            start += chunkSize;
        }

        return chunks;
    }
}
