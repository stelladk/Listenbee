package com.distributedsystems.listenbee;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.distributedsystems.listenbee.fragments.ForYouFragment;
import com.example.eventdeliverysystem.musicFile.MusicFile;

import java.util.List;

public class ExploreAdapter extends RecyclerView.Adapter<ExploreAdapter.ExploreViewHolder> {
    private Context context;
    private List<MusicFile> items;

    public ExploreAdapter(Context context, @NonNull List<MusicFile> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ExploreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song, parent, false);
        return new ExploreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExploreViewHolder holder, final int position) {
        final MusicFile track_preview = items.get(position);

        byte[] cover = track_preview.getCover();
        final String title = track_preview.getTrackName();
        final String artist = track_preview.getArtistName();

        if(cover != null){
            holder.image.setImageBitmap(BitmapFactory.decodeByteArray(cover, 0, cover.length));
        }
        holder.trackName.setText(title);
        holder.artistName.setText(artist);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ForYouFragment.getData(title, artist);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ExploreViewHolder extends RecyclerView.ViewHolder{

        View itemView;
        ImageView image;
        TextView trackName;
        TextView artistName;
        ImageButton download_btn;

        ExploreViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            image = itemView.findViewById(R.id.song_cover);
            trackName = itemView.findViewById(R.id.song_title);
            artistName = itemView.findViewById(R.id.song_artist);
            download_btn = itemView.findViewById(R.id.download_btn);
        }
    }
}
