package com.distributedsystems.listenbee.adapters;

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
import com.distributedsystems.listenbee.R;
import com.distributedsystems.listenbee.fragments.ForYouFragment;
import com.example.eventdeliverysystem.musicfilehandler.MusicFile;

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
        MusicFile track_preview = items.get(position);

        final String title = track_preview.getTrackName();
        holder.song.setText(title);

        final String artist = track_preview.getArtistName();
        holder.artist.setText(artist);

        byte[] imageBytes = track_preview.getCover();
        BitmapFactory.Options config = new BitmapFactory.Options();
        if(imageBytes != null){
            holder.image.setImageBitmap(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, config));
        }

        holder.downloadBtn.setVisibility(View.VISIBLE);

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
        TextView song;
        TextView artist;
        ImageButton downloadBtn;

        ExploreViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            image = itemView.findViewById(R.id.song_cover_item);
            song = itemView.findViewById(R.id.song_title_item);
            artist = itemView.findViewById(R.id.song_artist_item);
            downloadBtn = itemView.findViewById(R.id.download_btn);
        }
    }
}
