import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicFileHandler {

    /**
     * TODO keep specific files according to publisher's interests
     * Reads music files into a Map, with artist as the key and artist's songs as values
     * @return a Map with the artists and their songs
     */
    public static Map<String, List<MusicFile>> read(){
        //get the directory
        File dir = new File("./res/dataset2/");
        if (!dir.exists()) {
            System.err.println("ERROR: Directory doesn't exist");
            return null;
        }
        //get all files in the directory
        File[] files = dir.listFiles();

        Map<String, List<MusicFile>> songs = new HashMap<>();
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

                    //if title is null then get it from file signature
                    if (title == null) {
                        title = file.getName().substring(0, file.getName().indexOf('.'));
                        metadata.set("title", title);
                    }

                    if (artist != null){
                        if (!songs.containsKey(artist)){ //if artist doesn't exist make a new record
                            songs.put(artist, new ArrayList<MusicFile>());
                        }
                        songs.get(artist).add(new MusicFile(title, artist, metadata.get("xmpDM:album"), metadata.get("xmpDM:genre"), Files.readAllBytes(file.toPath())));
                    }

                    stream.close();
                } catch (IOException | SAXException | TikaException e) {
                    System.err.println("ERROR: Could not parse file");
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
            System.err.println("ERROR: Null object passed");
            return  false;
        }

        //create directory if it doesn't exist
        File dir = new File("./res/Downloads/");
        if (!dir.exists()) {
           if (!dir.mkdir()) {
               System.err.println("ERROR: Could not create directory");
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
            System.out.println("ERROR: Could not write file to directory");

            try {
                //close streams
               if (out != null) out.close();
               if (stream != null) stream.close();
            } catch (IOException ex) {
                System.out.println("ERROR: Could not close streams");
            }

            return false;
        }
    }
}
