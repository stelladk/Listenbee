package com.distributedsystems.listenbee.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.distributedsystems.listenbee.ExploreAdapter;
import com.distributedsystems.listenbee.LibraryAdapter;
import com.distributedsystems.listenbee.MainActivity;
import com.distributedsystems.listenbee.R;
import com.example.eventdeliverysystem.Consumer;
import com.example.eventdeliverysystem.musicFile.MusicFile;

import java.util.ArrayList;
import java.util.List;

public class ForYouFragment extends Fragment {

    private static Consumer consumer;
    private List<MusicFile> available_songs = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.foryou_fragment, container, false);
        loadLibrary();
        return view;
    }

    public void loadLibrary(){
        consumer = MainActivity.getConsumer();
        available_songs = consumer.loadLibrary();

        if(available_songs != null){
            ExploreAdapter adapter = new ExploreAdapter(getContext(), available_songs);
        }else{
            Log.d("LOAD LIBRARY", "Could not get available songs");
        }
    }

    public static void getData(String trackName, String artistName){
        ArrayList<MusicFile> track = consumer.playData(trackName, artistName, "ONLINE");
        MainActivity.playOnClick(track.get(0));
    }
}
