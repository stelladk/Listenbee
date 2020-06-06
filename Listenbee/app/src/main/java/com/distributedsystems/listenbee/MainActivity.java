package com.distributedsystems.listenbee;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.distributedsystems.listenbee.notification.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.eventdeliverysystem.musicfilehandler.MusicFile;
import com.example.eventdeliverysystem.models.Consumer;
import com.distributedsystems.listenbee.fragments.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    //code numbers for permissions
    private static final int READ_STORAGE_PERMISSION_CODE = 100;
    private static final int WRITE_STORAGE_PERMISSION_CODE = 200;

    private static List<Uri> songs = new ArrayList<>();
    private static String songTitle;
    private static String songArtist;
    private static Bitmap songCover;
    private static MediaPlayer mp3;
    private static Uri current;
    private static MusicFile file;
    private static Consumer consumer;
    private static int duration;
    private static boolean isSreaming;

    private BottomNavigationView tabs;
    private static Fragment activeFragment;
    private ProgressBar musicBar;
    private NotificationManager notificationManager;
    private static MainActivity self;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        activeFragment = new LibraryFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container_fragment, activeFragment)
                .addToBackStack(null)
                .commit();

        //volume buttons functionality
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        //Bottom navigation toolbar
        tabs = findViewById(R.id.bottom_navigation);
        tabs.setOnNavigationItemSelectedListener(this);

        self = MainActivity.this;

        //Ask for storage permission the first time the app opens
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, READ_STORAGE_PERMISSION_CODE);
        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_STORAGE_PERMISSION_CODE);

        //read downloaded songs from directory
        readMusicFiles();

        mp3 = new MediaPlayer();

        //Notification banner
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            registerReceiver(receiver, new IntentFilter("Notice"));
            startService(new Intent(getBaseContext(), OnClearFromRecentService.class));
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            notificationManager.cancelAll();
        }
        unregisterReceiver(receiver);
    }

    /**
     * Read downloaded songs from directory
     * Add these songs to library
     */
    public void readMusicFiles() {
        Log.d("METHOD", "------ READ MUSIC FILES ------");

        //get all downloaded songs from directory
        String dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/Listenbee/";
        Log.d("READPATH", dirPath);
        File dir = new File(dirPath);
        File[] files = dir.listFiles();

        if (files != null){
            for (File file : files) {
                //get song uri
                Uri fileUri = Uri.fromFile(file);
                songs.add(fileUri);
            }
            return;
        }

        Log.e("readMusicFiles@Error", "Could not read music files");
    }

    /**
     * FIXME notification
     * Play the song that was clicked from library
     */
    public static void playOnClick(View view, int position) {
        ImageButton play_btn = self.findViewById(R.id.play_btn);
        ImageButton pause_btn = self.findViewById(R.id.pause_btn);

        if (mp3.isPlaying()) {
            mp3.pause();
            mp3.reset();

            pause_btn.setVisibility(View.GONE);
            play_btn.setVisibility(View.VISIBLE);
        }

        current = songs.get(position);
        try {
            mp3.reset();
            mp3.setDataSource(view.getContext(), current);
        } catch (IOException e) {
            Log.e("ERROR", "Could not set data to mp3 player");
        }

        //get song metadata
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(view.getContext(), current);

        songTitle = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        TextView titleView = self.findViewById(R.id.song_title);
        titleView.setText(songTitle);

        songArtist =  metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

        byte[] imageBytes = metaRetriever.getEmbeddedPicture();
        BitmapFactory.Options config = new BitmapFactory.Options();
        if (imageBytes != null) {
            songCover = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, config);
            ImageView coverView = self.findViewById(R.id.song_cover);
            coverView.setImageBitmap(songCover);
        }

        try {
            mp3.prepare();
        } catch (IOException e) {
            Log.e("ERROR", "Could not play song");
        }

        duration = mp3.getDuration();

        mp3.start();

        play_btn.setVisibility(View.GONE);
        pause_btn.setVisibility(View.VISIBLE);

        //Notification
        //NotificationCreator.createNotification(self, current, R.drawable.pause_ic);

    }

    /**
     * FIXME notification
     * Play the song that was clicked from for you
     */
    public static void playOnClick(MusicFile track) {
        ImageButton play_btn = self.findViewById(R.id.play_btn);
        ImageButton pause_btn = self.findViewById(R.id.pause_btn);

        if (mp3.isPlaying()) {
            mp3.pause();
            mp3.reset();

            pause_btn.setVisibility(View.GONE);
            play_btn.setVisibility(View.VISIBLE);
        }

        file = track;

        duration = file.getDuration();

        songTitle = file.getTrackName().substring(2);
        TextView titleView = self.findViewById(R.id.song_title);
        titleView.setText(songTitle);

        songArtist =  file.getArtistName();

        byte[] imageBytes = file.getCover();
        BitmapFactory.Options config = new BitmapFactory.Options();
        if (imageBytes != null) {
            songCover = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, config);
            ImageView coverView = self.findViewById(R.id.song_cover);
            coverView.setImageBitmap(songCover);
        }

        play_btn.setVisibility(View.GONE);
        pause_btn.setVisibility(View.VISIBLE);

        new Stream().execute();

        //Notification
        //NotificationCreator.createNotification(self, current, R.drawable.pause_ic);
    }

    /**
     * Open music player and set content
     *
     * @param view button view
     */
    public void openPlayer(View view) {
        setContentView(R.layout.music_player);

        if (mp3.isPlaying()) {
            ImageButton playbtn = findViewById(R.id.play_btn);
            playbtn.setVisibility(View.GONE);
            ImageButton pausebtn = findViewById(R.id.pause_btn);
            pausebtn.setVisibility(View.VISIBLE);

            //get progress bar
            musicBar = findViewById(R.id.progressBar);
            musicBar.setMax(duration);
            new Progress().execute();
        }

        //set title text view
        TextView titleView = findViewById(R.id.song_title);
        titleView.setText(songTitle);

        //set artist text view
        TextView artistView = findViewById(R.id.artist_title);
        artistView.setText(songArtist);

        //set image view
        ImageView coverView = findViewById(R.id.song_cover);
        coverView.setImageBitmap(songCover);

        volume();
    }

    /**
     * Minimize music player and set content
     *
     * @param view button view
     */
    public void minimizePlayer(View view) {
        setContentView(R.layout.activity_main);

        //re-establish active fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container_fragment, activeFragment)
                .addToBackStack(null)
                .commit();

        if (mp3.isPlaying()) {
            ImageButton playbtn = findViewById(R.id.play_btn);
            playbtn.setVisibility(View.GONE);
            ImageButton pausebtn = findViewById(R.id.pause_btn);
            pausebtn.setVisibility(View.VISIBLE);
        }

        //set title text view
        TextView titleView = findViewById(R.id.song_title);
        titleView.setText(songTitle);

        //set image view
        ImageView coverView = findViewById(R.id.song_cover);
        coverView.setImageBitmap(songCover);
    }

    /**
     * FIXME notification
     * Play the song
     *
     * @param view play button
     */
    public void play(View view) {
        Log.d("METHOD", "------ PLAY ------");

        if (!mp3.isPlaying() && current != null) {
            try {
                mp3.setDataSource(getApplicationContext(), current);
            } catch (IOException e) {
                Log.e("play@Error", "Could not set data to mp3 player");
            }

            try {
                mp3.prepare();
            } catch (IOException e) {
                Log.e("play@Error", "Could not play song");
            }

            mp3.start();

            view.setVisibility(View.GONE);
            ImageButton pausebtn = findViewById(R.id.pause_btn);
            pausebtn.setVisibility(View.VISIBLE);

            //Notification
            //NotificationCreator.createNotification(self, current, R.drawable.pause_ic);
        }
    }

    /**
     * FIXME notification
     * Pause the song
     *
     * @param view pause button
     */
    public void pause(View view) {
        Log.d("METHOD", "------ PAUSE ------");

        if (mp3.isPlaying()) {
            mp3.pause();
            mp3.reset();

            view.setVisibility(View.GONE);
            ImageButton playbtn = findViewById(R.id.play_btn);
            playbtn.setVisibility(View.VISIBLE);

            //Notification
            //NotificationCreator.createNotification(self, current, R.drawable.play_ic);
        }
    }

    /**
     * Fast forward song
     *
     * @param view fast forward button
     */
    public void fastForward(View view) {
        Log.d("METHOD", "------ FAST FORWARD ------");

        if (mp3.isPlaying()) {
            final int seekForwardTime = 5 * 1000;

            int currentPosition = mp3.getCurrentPosition();
            if (currentPosition + seekForwardTime <= mp3.getDuration()) {
                mp3.seekTo(currentPosition + seekForwardTime);
            } else {
                mp3.seekTo(mp3.getDuration());
            }
        }
    }

    /**
     * Fast rewind song
     *
     * @param view fast rewind button
     */
    public void fastRewind(View view) {
        Log.d("METHOD", "------ FAST REWIND ------");

        if (mp3.isPlaying()) {
            final int seekBackwardTime = 5 * 1000;

            int currentPosition = mp3.getCurrentPosition();
            if (currentPosition - seekBackwardTime >= 0) {
                mp3.seekTo(currentPosition - seekBackwardTime);
            } else {
                mp3.seekTo(0);
            }
        }
    }

    /**
     * Handles volume
     */
    public void volume() {
        Log.d("METHOD", "------ VOLUME ------");

        //get system audio service
        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        //get volume seekbar
        SeekBar volumeBar = (SeekBar) findViewById(R.id.volume_bar);
        volumeBar.setMax(maxVolume);
        volumeBar.setProgress(curVolume);
        //implement seekbar volume functionality
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekbar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }
        });
    }

    /**
     * TODO
     * Add song to library
     *
     * @param file musicfile to be added to library
     */
    public static void addSong(MusicFile file) {
        //create new directory according to path
        String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/Listenbee/";
        File directory = new File(dir);
        if (!directory.exists()){
            if (!directory.mkdir()){
                Log.e("addSong@Error", "Error occurred in creating dir");
                return;
            }
        }

        File output = new File(directory, songTitle + ".mp3");
        Log.d("ADDSONG","Created file");
        FileOutputStream stream;
        try {
            byte[] allBytes = new byte[file.getMetadata().length + file.getFileBytes().length];
            Log.d("ADDSONG","Initialised bytes array");
            //insert metadata
            for (int i = 0; i < file.getMetadata().length; i++) allBytes[i] = file.getMetadata()[i];
            Log.d("ADDSONG","Inserted metadata");
            //insert rest of data
            for (int i = file.getMetadata().length, j = 0; i < allBytes.length; i++, j++) allBytes[i] = file.getFileBytes()[j];
            Log.d("ADDSONG","Inserted data");
            /* FROM JAVA PROJECT

            ByteBuffer buffer = ByteBuffer.wrap(allBytes);

            WritableByteChannel channel = Files.newByteChannel(output.toPath(), EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.APPEND));
            channel.write(buffer);

            channel.close();
            buffer.clear();

            */

            stream = new FileOutputStream(output);
            Log.d("ADDSONG"," post Stream create");
            stream.write(allBytes);
            Log.d("ADDSONG"," post Stream write");
            //clear streams
            stream.flush();
            stream.close();
            Log.d("ADDSONG","post Flush close");
        } catch (IOException e) {
            Log.e("addSong@Error", "Problem while saving song");
        }
    }

    /**
     * @return songs' uri
     */
    public static List<Uri> getSongs(){
        return songs;
    }

    /**
     * @return Consumer object
     */
    public static Consumer getConsumer(){
        return consumer;
    }

    /**
     * Set Consumer object
     * @param client new Consumer object
     */
    public static void setConsumer(Consumer client){
        consumer = client;
    }

    /**
     * Handles bottom navigation bar item clicks.
     * Loads the right fragment for each item
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment;

        switch(item.getItemId()){
            case R.id.action_library:
                selectedFragment = new LibraryFragment();
                break;
            case R.id.action_profile:
                selectedFragment = new ProfileFragment();
                break;
            case R.id.action_foryou:
                selectedFragment = new ForYouFragment();
                break;
            default:
                return false;
        }

        //load the right fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container_fragment, selectedFragment)
                .addToBackStack(null)
                .commit();
        activeFragment = selectedFragment;

        return true;
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case READ_STORAGE_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) { // permission denied
                    //TODO inform about the importance of this permission etc.
                }
            case WRITE_STORAGE_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) { // permission denied
                    //TODO inform about the importance of this permission etc.
                }
        }
    }

    /**
     * Check whether a permission has been granted
     *
     * @param permission permission to be checked
     * @param requestCode permission code
     */
    private void checkPermission(String permission, int requestCode) {

        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{permission},
                    requestCode
            );
        }
    }

    /**
     * Create the NotificationChannel
     */
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NotificationCreator.CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getExtras().getString("action");

            switch(action){
                case NotificationCreator.ACTION_PLAY:
                    if(!mp3.isPlaying()){
                        play(findViewById(R.id.play_btn));
                        NotificationCreator.createNotification(MainActivity.this, current, R.drawable.pause_ic);
                    }else{
                        pause(findViewById(R.id.pause_btn));
                        NotificationCreator.createNotification(MainActivity.this, current, R.drawable.play_ic);
                    }
                    break;
                case NotificationCreator.ACTION_FAST_FORWARD:
                    fastForward(findViewById(R.id.fast_forward_btn));
                    NotificationCreator.createNotification(MainActivity.this, current, R.drawable.pause_ic);
                    break;
            }
        }
    };

    /**
     * Class that handles a progress bar asynchronously
     * The progress bar displays the song current position
     */
    private class Progress extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            //online streaming
            while (isSreaming) {
                publishProgress(mp3.getCurrentPosition());
            }

            //offline streaming
            while (mp3.isPlaying()) {
                publishProgress(mp3.getCurrentPosition());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            musicBar.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(Void result) {
            mp3.stop();
            mp3.reset();
        }
    }

    /**
     * Class that plays song chunks asynchronously
     */
    private static class Stream extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            //wait for 2 chunks to arrive
            while(consumer.getChunkListSize() < 2);

            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {
            Thread stream = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (consumer.getChunkListSize() >= 0) {
                        File curMp3;
                        try {
                            //save temporary in cache
                            curMp3 = File.createTempFile(file.getTrackName() + "_chunk", "mp3", self.getCacheDir());
                            curMp3.deleteOnExit();

                            FileOutputStream out_cur = new FileOutputStream(curMp3);
                            out_cur.write(file.getFileBytes());
                            out_cur.close();

                            //get file uri
                            current = Uri.fromFile(curMp3);
                        } catch (IOException e) {
                            Log.e("ERROR", "Could not save in cache");
                        }

                        //prepare mp3 player
                        try {
                            mp3.reset();
                            mp3.setDataSource(self, current);
                            mp3.prepare();
                        } catch (IOException e) {
                            Log.e("ERROR", "Could not play song");
                        }

                        mp3.start();
                        isSreaming = true;

                        //wait for chunk to finish playing
                        while (mp3.isPlaying());
                        if (consumer.getChunkListSize() == 0) break;
                        if (consumer.getChunkListSize() > 0) file = consumer.getNextChunk();
                    }

                    isSreaming = false;

                    ImageButton play_btn = self.findViewById(R.id.play_btn);
                    ImageButton pause_btn = self.findViewById(R.id.pause_btn);

//                    pause_btn.setVisibility(View.GONE);
//                    play_btn.setVisibility(View.VISIBLE);
                }
            });
            stream.start();
        }
    }
}
