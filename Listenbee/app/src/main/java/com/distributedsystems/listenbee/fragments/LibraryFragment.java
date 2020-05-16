package com.distributedsystems.listenbee.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.distributedsystems.listenbee.LibraryAdapter;
import com.distributedsystems.listenbee.MainActivity;
import com.distributedsystems.listenbee.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment {

    private List<Uri> songs = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.library_fragment, container, false);
        loadMusicFiles(view);
        return view;
    }


    /**
     * FIXME
     * Read downloaded songs from directory
     * Add these songs to library
     * @return true if operation was successful
     */
    public boolean loadMusicFiles(View view) {
        Log.d("METHOD", "------ LOAD MUSIC FILES ------");

        songs = MainActivity.getSongs();

        if (songs != null){

            //Library
            LibraryAdapter adapter = new LibraryAdapter(getContext(), songs);
            Log.d("Adapter null", ""+(adapter == null));
            RecyclerView library = view.findViewById(R.id.library_view);
            LinearLayoutManager layoutManager = new LinearLayoutManager(view.getContext());
            library.setLayoutManager(layoutManager);
            library.setHasFixedSize(true);
            library.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
        return true;
    }

}
