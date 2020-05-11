package com.distributedsystems.listenbee;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
    }
    /**
     * TODO -- METHOD NAME & CODE
     * Transfer user to main activity
     */
    public void toMainActivity(View view) {
        Log.d("METHOD", "------ TO MAIN ACTIVITY ------");

        Intent main_activity = new Intent(this, MainActivity.class);
        startActivity(main_activity);
    }

    //todo
    public void saveInfo(View view) {

    }

    //todo
    public void uploadPhoto(View view) {

    }

}
