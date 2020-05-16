package com.distributedsystems.listenbee;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventdeliverysystem.musicFile.MusicFile;

import android.net.Uri;
import java.util.List;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.LibraryViewHolder> {
    private Context context;
    private List<Uri> items;

    public LibraryAdapter(Context context, @NonNull List<Uri> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public LibraryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song, parent, false);
        return new LibraryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LibraryViewHolder holder, int position) {
        Uri track = items.get(position);
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        metadataRetriever.setDataSource(context, track);

        byte[] cover = metadataRetriever.getEmbeddedPicture();
        String title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

        if(cover != null){
            holder.image.setImageBitmap(BitmapFactory.decodeByteArray(cover, 0, cover.length));
        }
        holder.trackName.setText(title);
        holder.artistName.setText(artist);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class LibraryViewHolder extends RecyclerView.ViewHolder{

        ImageView image;
        TextView trackName;
        TextView artistName;
        ImageButton download_btn;

        LibraryViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.song_cover);
            trackName = itemView.findViewById(R.id.song_title);
            artistName = itemView.findViewById(R.id.song_artist);
            download_btn = itemView.findViewById(R.id.download_btn);
        }
    }
}
