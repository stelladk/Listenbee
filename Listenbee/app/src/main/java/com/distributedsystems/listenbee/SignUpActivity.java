package com.distributedsystems.listenbee;

import android.content.Intent;
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

import com.example.eventdeliverysystem.Broker;
import com.example.eventdeliverysystem.Consumer;
import com.example.eventdeliverysystem.Pair;
import com.example.eventdeliverysystem.Utilities;

import java.math.BigInteger;

public class SignUpActivity extends AppCompatActivity {

    Consumer consumer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_signup);

        final Button sign_up_btn = findViewById(R.id.signup_btn);
        final EditText username_form = findViewById(R.id.username_form);
        final EditText password_form = findViewById(R.id.password_form);
        final EditText email_form = findViewById(R.id.email_form);
        final EditText age_form = findViewById(R.id.age_form);
        sign_up_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               SignupTask signupTask = new SignupTask();
               signupTask.execute(username_form.getText(),password_form.getText(),email_form.getText(),age_form.getText());
            }
        });

        ImageButton back_btn = findViewById(R.id.back_btn);
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                previous(view);
            }
        });
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

    /**
     * Transfer user to main activity
     */
    public void previous(View view){
        Log.d("METHOD", "------ TO PREVIOUS ACTIVITY ------");

        Intent main_activity = new Intent(this, LoginActivity.class);
        startActivity(main_activity);
    }

    public class SignupTask extends AsyncTask<Editable, Void, Integer> {

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Editable... editables) {
            //get IP address
            WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
            String client_IP = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            Log.d("LOCALHOST", client_IP);
            String server_IP = "192.168.1.3";
            consumer = new Consumer(client_IP, server_IP, Broker.getToCliPort());

            //check required forms
            if(TextUtils.isEmpty(editables[0])){
                Log.d("SIGN UP", "Username is required!");
                Toast.makeText(SignUpActivity.this, "Username is required!", Toast.LENGTH_SHORT).show();
                return null;
            }
            if(TextUtils.isEmpty(editables[1])){
                Log.d("SIGN UP", "Password is required!");
                Toast.makeText(SignUpActivity.this, "Password is required!", Toast.LENGTH_SHORT).show();
                return null;
            }
            if(TextUtils.isEmpty(editables[2])){
                Log.d("SIGN UP", "E-mail is required!");
                Toast.makeText(SignUpActivity.this, "E-mail is required!", Toast.LENGTH_SHORT).show();
                return null;
            }
            if(TextUtils.isEmpty(editables[3])){
                Log.d("SIGN UP", "Age is required!");
                Toast.makeText(SignUpActivity.this, "Age is required!", Toast.LENGTH_SHORT).show();
                return null;
            }

            //connect with Broker
            String username = editables[0].toString();
            BigInteger password = Utilities.SHA1(editables[1].toString());
            String email = editables[2].toString();
            Integer age = Integer.parseInt(editables[3].toString());
            return consumer.registerUser(new Pair<String, BigInteger>(username,password), new Pair<String, Integer>(email,age));
        }

        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            if(res == -2){
                //email taken
                Log.d("SIGN UP", "E-mail already exists");
                Toast.makeText(SignUpActivity.this, "E-mail already exists", Toast.LENGTH_SHORT).show();
            }else if(res == -1){
                //username taken
                Log.d("SIGN UP", "Username already exists");
                Toast.makeText(SignUpActivity.this, "Username already exists", Toast.LENGTH_SHORT).show();
            }else if(res == 0){
                //no connection
                Log.d("SIGN UP", "Connection failed");
                Toast.makeText(SignUpActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
            }else if(res == 1){
                //successful sign up
                Log.d("SIGN UP", "Success!!!");
                MainActivity.setConsumer(consumer);
                toMainActivity(null);
            }
        }
    }

}
