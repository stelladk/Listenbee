package com.distributedsystems.listenbee.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.distributedsystems.listenbee.MainActivity;
import com.distributedsystems.listenbee.R;

import com.example.eventdeliverysystem.musicfilehandler.MusicFile;

public class NotificationCreator {
    public static final String CHANNEL_ID = "player";
    public static final String ACTION_PLAY = "actionplay";
    public static final String ACTION_FAST_FORWARD = "actionfastforward";

    private static Notification notification;


    public static void createNotification(Context context, Uri track, int play_btn){
        //get song metadata
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(context, track);

        String songTitle = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String songArtist =  metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        byte[] imageBytes = metaRetriever.getEmbeddedPicture();

        MusicFile musicFile = new MusicFile(songTitle, songArtist, null, null, imageBytes, null, null);
        createNotification(context, musicFile, play_btn);
    }

    public static void createNotification(Context context, MusicFile track, int play_btn){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            MediaSessionCompat mediaSession = new MediaSessionCompat(context, "tag");

            Intent tapIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingTapIntent = PendingIntent.getActivity(context, 0, tapIntent, 0);


            byte[] bytes = track.getCover();
            Bitmap cover = null;
            if(bytes != null){
                cover = BitmapFactory.decodeByteArray(track.getCover(), 0, track.getCover().length);
            }

            PendingIntent pendingPlay;
            Intent intentPlay = new Intent(context, NotificationActionService.class)
                    .setAction(ACTION_PLAY);
            pendingPlay = PendingIntent.getBroadcast(context, 0, intentPlay, PendingIntent.FLAG_UPDATE_CURRENT);


            Intent intentFastForward = new Intent(context, NotificationActionService.class)
                    .setAction(ACTION_FAST_FORWARD);
            PendingIntent pendingFastForward = PendingIntent.getBroadcast(context, 0, intentFastForward, PendingIntent.FLAG_UPDATE_CURRENT);

            boolean persistent = (play_btn == R.drawable.pause_ic);

            notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(track.getTrackName())
                    .setContentText(track.getArtistName())
                    .setLargeIcon(cover)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false)
                    .addAction(play_btn, "Play/Pause", pendingPlay)
                    .addAction(R.drawable.fast_forward_ic, "Fast Forward", pendingFastForward)
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1)
                        .setMediaSession(mediaSession.getSessionToken()))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(pendingTapIntent)
                    .setOngoing(persistent)
                    .build();

            manager.notify(1, notification);

        }
    }

}
