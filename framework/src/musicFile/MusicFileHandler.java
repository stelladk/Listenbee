package musicFile;

import com.mpatric.mp3agic.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class MusicFileHandler {

    /**
     * Reads music files into a Map, with artist as the key and artist's songs as values
     * @return a Map with the artists and their songs
     */
    public static Map<String, ArrayList<MusicFile>> read(String range) {
        System.out.println("HANDLER: Reading music files");

        //get the directory
        File dir = new File("../res/dataset1/");
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
                    Mp3File mp3 = new Mp3File(file);

                    if (mp3.hasId3v2Tag()) {
                        ID3v2 tag = mp3.getId3v2Tag();

                        String artist = tag.getArtist();
                        if (artist == null){
                            continue;
                        }

                        //if artist name not in range continue to next file
                        if (!artist.matches(range)) {
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
                        //get song cover
                        byte[] cover = tag.getAlbumImage();
                        //get all the file bytes
                        byte[] allBytes = Files.readAllBytes(file.toPath());
                        //position of song data
                        int offset = mp3.getStartOffset();
                        int end = mp3.getEndOffset();
                        //array of metadata
                        byte[] metadata = Arrays.copyOfRange(allBytes, 0, offset);
                        //array without metadata
                        byte[] bytes = Arrays.copyOfRange(allBytes, offset, end-1);

                        songs.get(artist).add(new MusicFile(title, artist, tag.getAlbum(), tag.getGenreDescription(), cover, metadata, bytes));
                    }
                } catch (IOException | UnsupportedTagException | InvalidDataException e) {
                    System.err.println("HANDLER: READ: ERROR: Could not parse file");
                    e.printStackTrace();
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
        File dir = new File("../res/Download/");
        if (!dir.exists()) {
           if (!dir.mkdir()) {
               System.err.println("HANDLER: WRITE: ERROR: Could not create directory");
               return false;
           }
        }

        //create file
        File savedFile = new File(dir, file.getTrackName() + ".mp3");

        try {
            byte[] allBytes = new byte[file.getMetadata().length + file.getFileBytes().length];
            //insert metadata
            for (int i = 0; i < file.getMetadata().length; i++) allBytes[i] = file.getMetadata()[i];
            //insert rest of data
            for (int i = file.getMetadata().length, j = 0; i < allBytes.length; i++, j++) allBytes[i] = file.getFileBytes()[j];

            ByteBuffer buffer = ByteBuffer.wrap(allBytes);

            WritableByteChannel channel = Files.newByteChannel(savedFile.toPath(), EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.APPEND));
            channel.write(buffer);

            channel.close();
            buffer.clear();

            return true;
        } catch (IOException e) {
            System.out.println("HANDLER: WRITE: ERROR: Could not write file to directory");
            return false;
        }
    }

    /**
     * Save song chunks to Stream directory
     * @param chunks music file chunks
     * @return true if the chunks were saved to directory
     */
    public static boolean write(ArrayList<MusicFile> chunks) {
        System.out.println("HANDLER: Writing chunks");

        //if the chunk list is null then cancel the activity
        if (chunks == null || chunks.isEmpty()) {
            System.err.println("HANDLER: WRITE CHUNKS: ERROR: Null object passed");
            return false;
        }

        //create directory if it doesn't exist
        File dir = new File("../res/Stream/");
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                System.err.println("HANDLER: WRITE CHUNKS: ERROR: Could not create directory");
                return false;
            }
        }

        for (MusicFile chunk : chunks){
            //create file
            File savedFile = new File(dir, chunk.getTrackName() + ".mp3");

            try {
                ByteBuffer buffer = ByteBuffer.wrap(chunk.getFileBytes());

                WritableByteChannel channel = Files.newByteChannel(savedFile.toPath(), EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.APPEND));
                channel.write(buffer);

                channel.close();
                buffer.clear();
            } catch (IOException e) {
                System.out.println("HANDLER: WRITE CHUNK: ERROR: Could not write file to directory");
                return false;
            }
        }
        return true;
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
        int serial = 0;
        while (start < songBytes.length){
            chunks.add(new MusicFile(
                    serial + " " + file.getTrackName(),
                            file.getArtistName(),
                            file.getAlbumInfo(),
                            file.getGenre(),
                            file.getCover(),
                            file.getMetadata(),
                            Arrays.copyOfRange(songBytes, start, Math.min(start + chunkSize, songBytes.length)), //if end index is greater than the array length then use the array length
                            serial
                    )
            );
            start += chunkSize;
            ++serial;
        }

        return chunks;
    }

    /**
     * Merge the file chunks into one file
     * @param chunks file chunks
     * @return merged file
     */
    public static MusicFile merge (ArrayList<MusicFile> chunks){
        System.out.println("HANDLER: Merging music file chunks");

        //if the file is null then cancel the activity
        if (chunks == null || chunks.size() == 0) {
            System.err.println("HANDLER: MERGE: ERROR: No chunks where passed");
            return null;
        }

        if (chunks.size() == 1) { //if list contains one chunk only
            //write correct title
            chunks.get(0).setTrackName(chunks.get(0).getTrackName().substring(chunks.get(0).getTrackName().indexOf(" ") + 1));
            return chunks.get(0);
        }

        //sort chunks according to serial number
        chunks.sort(new Comparator<MusicFile>() {
            @Override
            public int compare(MusicFile a, MusicFile b) {
                return a.compareTo(b);
            }
        });

        //get metadata
        String title = chunks.get(0).getTrackName().substring(chunks.get(0).getTrackName().indexOf(" ") + 1);
        String artist = chunks.get(0).getArtistName();
        String album = chunks.get(0).getAlbumInfo();
        String genre = chunks.get(0).getGenre();
        byte[] cover = chunks.get(0).getCover();
        byte[] metadata = chunks.get(0).getMetadata();

        //get bytes from chunks
        ArrayList<Byte> temp = new ArrayList<>();
        for (MusicFile chunk : chunks){
            if (chunk != null){
                for (byte b : chunk.getFileBytes()){
                    temp.add(b);
                }
            }
        }

        //transfer them to an array
        byte[] bytes = new byte[temp.size()];
        for (int i = 0; i < temp.size(); i++) bytes[i] = temp.get(i);

        //clear the list
        temp.clear();

        return new MusicFile(title, artist, album, genre, cover, metadata, bytes);
    }
}
