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
        return inflater.inflate(R.layout.library_fragment, container, false);
    }


    /**
     * FIXME
     * Read downloaded songs from directory
     * Add these songs to library
     * @return true if operation was successful
     */
    public boolean loadMusicFiles() {
        Log.d("METHOD", "------ LOAD MUSIC FILES ------");

        songs = MainActivity.getSongs();

        if (songs != null){

            //Library
            RecyclerView library = getView().findViewById(R.id.library_view);
            LibraryAdapter adapter = new LibraryAdapter(getContext(), songs);
            library.setAdapter(adapter);
        }
        return true;
    }

}
