package com.distributedsystems.listenbee.adapters;

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

import com.distributedsystems.listenbee.MainActivity;
import com.distributedsystems.listenbee.R;

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
    public void onBindViewHolder(@NonNull LibraryViewHolder holder, final int position) {
        Uri track = items.get(position);
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        metadataRetriever.setDataSource(context, track);

        String title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        holder.song.setText(title);

        String artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        holder.artist.setText(artist);

        byte[] imageBytes = metadataRetriever.getEmbeddedPicture();
        BitmapFactory.Options config = new BitmapFactory.Options();
        if(imageBytes != null){
            holder.image.setImageBitmap(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, config));
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.playOnClick(view, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class LibraryViewHolder extends RecyclerView.ViewHolder{

        View itemView;
        ImageView image;
        TextView song;
        TextView artist;
        ImageButton downloadBtn;

        LibraryViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            image = itemView.findViewById(R.id.song_cover);
            song = itemView.findViewById(R.id.song_title);
            artist = itemView.findViewById(R.id.song_artist);
            downloadBtn = itemView.findViewById(R.id.download_btn);
        }
    }
}
