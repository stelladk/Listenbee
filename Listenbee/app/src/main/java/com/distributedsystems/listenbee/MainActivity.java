package com.distributedsystems.listenbee;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.distributedsystems.listenbee.fragments.LibraryFragment;
import com.distributedsystems.listenbee.fragments.ProfileFragment;
import com.distributedsystems.listenbee.fragments.SearchFragment;
import com.example.eventdeliverysystem.Broker;
import com.example.eventdeliverysystem.Consumer;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private BottomNavigationView tabs;

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

        Consumer consumer = new Consumer("127.0.0.1", "127.0.0.1", Broker.getToCliPort());
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
}
