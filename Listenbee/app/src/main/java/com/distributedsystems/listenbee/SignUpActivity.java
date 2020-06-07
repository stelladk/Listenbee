package com.distributedsystems.listenbee;

import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventdeliverysystem.models.Broker;
import com.example.eventdeliverysystem.models.Consumer;
import com.example.eventdeliverysystem.utilities.Pair;
import com.example.eventdeliverysystem.utilities.Utilities;

import java.math.BigInteger;

public class SignUpActivity extends AppCompatActivity {
    private String clientIP;
    private String brokerIP;
    private Consumer consumer;

    private EditText usernameForm;
    private EditText passwordForm;
    private EditText emailForm;
    private EditText ageForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_signup);

        usernameForm = findViewById(R.id.username_form);
        passwordForm = findViewById(R.id.password_form);
        emailForm = findViewById(R.id.email_form);
        ageForm = findViewById(R.id.age_form);

        new InternetInfo().execute();
    }

    /**
     * Sign-up user to the app
     */
    public void signup(View view) {
        Log.d("METHOD", "------ SIGN-UO ------");

        String username = usernameForm.getText().toString();
        String password = passwordForm.getText().toString();
        String email = emailForm.getText().toString();
        String age = ageForm.getText().toString();

        new SignupTask().execute(username, password, email, age);
    }

    /**
     * Transfer user to main activity
     */
    public void toMainActivity() {
        Log.d("METHOD", "------ TO MAIN ACTIVITY ------");

        Intent main_activity = new Intent(this, MainActivity.class);
        startActivity(main_activity);
    }

    /**
     * Transfer user to login
     */
    public void previous(View view){
        Log.d("METHOD", "------ TO PREVIOUS ACTIVITY ------");

        Intent login_activity = new Intent(this, LoginActivity.class);
        startActivity(login_activity);
    }

    /**
     * Class that gets mobiles IP address
     */
    private class InternetInfo extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = manager.getConnectionInfo();
            clientIP = Formatter.formatIpAddress(manager.getConnectionInfo().getIpAddress());

            brokerIP = "192.168.1.4";
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            consumer = new Consumer(clientIP, brokerIP, Broker.getToCliPort());
        }
    }

    /**
     * Takes user credential, communicates with Broker and sign up user to the app
     */
    public class SignupTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... str) {
            return consumer.registerUser(new Pair<String, BigInteger>(str[0], Utilities.SHA1(str[1])), new Pair<String, Integer>(str[2], Integer.parseInt(str[3])));
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
                //email has been taken from other user
                case -2:
                    Log.d("SIGN-UP", "E-mail already exists");
                    Toast.makeText(SignUpActivity.this, "E-mail already exists", Toast.LENGTH_SHORT).show();
                //username has been taken from other user
                case -1:
                    Log.e("SIGN-UP", "Username already exists");
                    Toast.makeText(SignUpActivity.this, "Username already exists", Toast.LENGTH_SHORT).show();
                    break;
                //no connection
                case 0:
                    Log.e("SIGN-UP", "Connection failed");
                    Toast.makeText(SignUpActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
                    break;
                //successful sign up
                case 1:
                    Log.d("SIGN-UP", "Success");
                    MainActivity.setConsumer(consumer);
                    toMainActivity();
            }
        }
    }

}
