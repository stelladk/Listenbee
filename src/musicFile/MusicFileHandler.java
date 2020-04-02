package musicFile;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

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
                    BodyContentHandler handler = new BodyContentHandler();
                    Metadata metadata = new Metadata();
                    FileInputStream stream = new FileInputStream(file);
                    ParseContext context = new ParseContext();

                    //parse the mp3 file
                    Mp3Parser Mp3Parser = new  Mp3Parser();
                    Mp3Parser.parse(stream, handler, metadata, context);

                    String title = metadata.get("title");
                    String artist = metadata.get("xmpDM:artist");

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
                    if (title == null || title.isEmpty()) {
                        title = file.getName().substring(0, file.getName().indexOf('.'));
                        metadata.set("title", title);
                    }

                    if (!songs.containsKey(artist)){ //if artist doesn't exist make a new record
                        songs.put(artist, new ArrayList<MusicFile>());
                    }
                    songs.get(artist).add(new MusicFile(title, artist, metadata.get("xmpDM:album"), metadata.get("xmpDM:genre"), Files.readAllBytes(file.toPath())));

                    stream.close();
                } catch (IOException | SAXException | TikaException e) {
                    System.err.println("HANDLER: READ: ERROR: Could not parse file");
                    return null;
                }
            }
        }
        return !songs.isEmpty() ? songs : null;
    }

    /**
     * FIXME when file is saved the properties (title, genre, image) aren't saved
     * Save a song to Downloads directory
     * @param file the music file
     * @return true if the song was saved to directory
     */
    public static boolean write(MusicFile file) {
        //if the file is null then cancel the activity
        if (file == null) {
            System.err.println("HANDLER: WRITE: ERROR: Null object passed");
            return false;
        }

        //TODO CHECK MORE FOR NULL

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

        FileOutputStream stream = null;
        ObjectOutputStream out = null;
        try{
            stream = new FileOutputStream(savedFile);
            out = new ObjectOutputStream(stream);
            out.writeObject(file);

            out.close();
            stream.close();

            return true;
        } catch (IOException e) {
            System.out.println("HANDLER: WRITE: ERROR: Could not write file to directory");

            try {
                //close streams
               if (out != null) out.close();
               if (stream != null) stream.close();
            } catch (IOException ex) {
                System.out.println("HANDLER: WRITE: ERROR: Could not close streams");
            }

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
