package com.distributedsystems.listenbee;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventdeliverysystem.musicFile.MusicFile;

import java.util.List;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.LibraryViewHolder> {
    private List<MusicFile> items;

    public LibraryAdapter(@NonNull List<MusicFile> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public LibraryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.track_preview, parent, false);
        return new LibraryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LibraryViewHolder holder, int position) {
        MusicFile track = items.get(position);
//        holder.image.setImageDrawable(track.getImage());
        holder.trackName.setText(track.getTrackName());
        holder.artistName.setText(track.getArtistName());
        holder.download_btn.setImageResource(R.drawable.download_ic);
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    static class LibraryViewHolder extends RecyclerView.ViewHolder{

        ImageView image;
        TextView trackName;
        TextView artistName;
        ImageButton download_btn;

        LibraryViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.preview_img);
            trackName = itemView.findViewById(R.id.preview_track);
            artistName = itemView.findViewById(R.id.preview_artist);
            download_btn = itemView.findViewById(R.id.download_btn);
        }
    }
}
