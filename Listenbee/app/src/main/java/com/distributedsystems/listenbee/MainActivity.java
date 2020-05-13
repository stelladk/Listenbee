package com.distributedsystems.listenbee;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.distributedsystems.listenbee.fragments.LibraryFragment;
import com.distributedsystems.listenbee.fragments.ProfileFragment;
import com.distributedsystems.listenbee.fragments.SearchFragment;
import com.example.eventdeliverysystem.Broker;
import com.example.eventdeliverysystem.Consumer;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private BottomNavigationView tabs;

    private static Consumer consumer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container_fragment, new LibraryFragment())
                .addToBackStack(null)
                .commit();

        //Bottom navigation toolbar
        tabs = findViewById(R.id.bottom_navigation);
        tabs.setOnNavigationItemSelectedListener(this);

        String server_IP = "127.0.0.1";
        String client_IP = "127.0.0.1";
        try {
            client_IP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        //Initialise Consumer
        consumer = new Consumer(client_IP, server_IP, Broker.getToCliPort());
        //TODO: Inflate login activity
        //TODO: Load artists to library (not yet ready)
    }

    //todo
    public void play(View view) {

    }

    //todo
    public void pause(View view) {

    }

    //todo
    public void fastForward(View view) {

    }

    //todo
    public void fastRewind(View view) {

    }


    //todo
    public void toSettings(View view) {
        Log.d("METHOD", "------ TO SETTINGS ACTIVITY ------");

        Intent settings_activity = new Intent(this, SettingsActivity.class);
        startActivity(settings_activity);
    }

    public void openPlayer(View view) {
        setContentView(R.layout.music_player);
    }

    public void minimizePlayer(View view) {
        setContentView(R.layout.activity_main);
    }



//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//
//    }

    /**
     * Handles bottom navigation bar item clicks.
     * Loads the right fragment for each item
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item){
        Fragment selectedFragment;

        switch(item.getItemId()){
            case R.id.action_library:
                selectedFragment = new LibraryFragment();
                break;
            case R.id.action_profile:
                selectedFragment = new ProfileFragment();
                break;
            case R.id.action_search:
                selectedFragment = new SearchFragment();
                break;
            default:
                return false;
        }

        //load the right fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container_fragment, selectedFragment)
                .addToBackStack(null)
                .commit();
        return true;
    }

    public static Consumer getConsumer(){
        return consumer;
    }
}
