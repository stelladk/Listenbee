import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicFileHandler {

    /**
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
                    if (artist != null && title != null){
                        if (!songs.containsKey(artist)){ //if artist doesn't exist make a new record
                            songs.put(artist, new ArrayList<MusicFile>());
                        }
                        songs.get(artist).add(new MusicFile(title, artist, metadata.get("xmpDM:album"), metadata.get("xmpDM:genre"), Files.readAllBytes(file.toPath())));
                    }
                } catch (IOException | SAXException | TikaException e) {
                    System.err.println("ERROR: Could not parse file");
                    return null;
                }
            }
        }
        return !songs.isEmpty() ? songs : null;
    }

    public static void write(MusicFile file){

    }
}
