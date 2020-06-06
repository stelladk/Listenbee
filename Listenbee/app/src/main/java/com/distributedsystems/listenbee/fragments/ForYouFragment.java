package com.distributedsystems.listenbee.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.distributedsystems.listenbee.adapters.ExploreAdapter;
import com.distributedsystems.listenbee.MainActivity;
import com.distributedsystems.listenbee.R;
import com.example.eventdeliverysystem.models.Consumer;
import com.example.eventdeliverysystem.musicfilehandler.MusicFile;


import java.util.List;

public class ForYouFragment extends Fragment {

    private View view;
    private RecyclerView library;
    private static Consumer consumer = MainActivity.getConsumer();
    private static List<MusicFile> availableSongs = null;
    private static List<MusicFile> toDownload = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.view = inflater.inflate(R.layout.foryou_fragment, container, false);
        loadLibrary();
        return this.view;
    }

    public static void getData(String trackName, String artistName) {
        new ReceiveTask().execute(trackName, artistName, "ONLINE");
    }

    public static void downloadData(String trackName, String artistName){
        new DownloadTask().execute(trackName, artistName);
    }

    /**
     * Load available online songs
     * Add these songs to for you fragment
     */
    private void loadLibrary() {
        view.findViewById(R.id.waiting).setVisibility(View.VISIBLE);
        library = view.findViewById(R.id.foryou_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(view.getContext());
        library.setLayoutManager(layoutManager);
        library.setHasFixedSize(true);
        if(availableSongs == null){
//            consumer = MainActivity.getConsumer();
            new LoadTask().execute();
        }else{
            ExploreAdapter adapter = new ExploreAdapter(getContext(), availableSongs);
            library.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
    }

    private class LoadTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            if (availableSongs == null) {
                availableSongs = consumer.loadLibrary();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {
            if(availableSongs != null){
                ExploreAdapter adapter = new ExploreAdapter(getContext(), availableSongs);
                library.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }else{
                Log.e("ForYouFragment@LoadTask@onPostExecution@Error", "Could not get available songs");
            }
            view.findViewById(R.id.waiting).setVisibility(View.GONE);
        }
    }

    private static class DownloadTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            String trackName = strings[0];
            String artistName = strings[1];
            String mode = "OFFLINE";
            toDownload = consumer.playData(trackName, artistName, mode);
            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {
            for(MusicFile file : toDownload){
                MainActivity.addSong(file);
            }
        }
    }


    private static class ReceiveTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            String trackName = strings[0];
            String artistName = strings[1];
            String mode = strings[2];
            consumer.playData(trackName, artistName, mode);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            MainActivity.playOnClick(consumer.getNextChunk());
        }
    }
}
