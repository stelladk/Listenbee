package com.distributedsystems.listenbee.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.distributedsystems.listenbee.ExploreAdapter;
import com.distributedsystems.listenbee.MainActivity;
import com.distributedsystems.listenbee.R;
import com.example.eventdeliverysystem.Consumer;
import com.example.eventdeliverysystem.musicFile.MusicFile;

import java.util.ArrayList;
import java.util.List;

public class ForYouFragment extends Fragment {

    private View view;
    private static Consumer consumer;
    private List<MusicFile> available_songs = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.view = inflater.inflate(R.layout.foryou_fragment, container, false);
        loadLibrary();
        return this.view;
    }

    public void loadLibrary(){
        consumer = MainActivity.getConsumer();
        Log.d("LOAD", ""+(consumer==null));
        //TODO DELETE DEBUG
//        if(consumer == null) consumer = new Consumer("192.168.1.4", "192.168.1.3", 2000); //DEBUG
        LoadTask loadTask = new LoadTask();
        loadTask.execute();
    }

    public static void getData(String trackName, String artistName){
        ArrayList<MusicFile> track = consumer.playData(trackName, artistName, "ONLINE");
        MainActivity.playOnClick(track.get(0));
    }

    public class LoadTask extends AsyncTask<Void, Void, Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if(available_songs == null){
                available_songs = consumer.loadLibrary();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if(available_songs != null){
                ExploreAdapter adapter = new ExploreAdapter(getContext(), available_songs);
                RecyclerView library = view.findViewById(R.id.library_view);
                LinearLayoutManager layoutManager = new LinearLayoutManager(view.getContext());
                library.setLayoutManager(layoutManager);
                library.setHasFixedSize(true);
                library.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }else{
                Log.d("LOAD LIBRARY", "Could not get available songs");
            }
        }
    }
}
