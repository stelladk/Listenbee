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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;

import com.example.eventdeliverysystem.Broker;
import com.example.eventdeliverysystem.Consumer;
import com.example.eventdeliverysystem.Utilities;

import java.math.BigInteger;

public class LoginActivity extends AppCompatActivity {

    Consumer consumer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        Button login_btn = findViewById(R.id.login_btn);
        Button signup_btn = findViewById(R.id.signup_btn);
        final EditText username_form = findViewById(R.id.username_form);
        final EditText password_form = findViewById(R.id.password_form);
//        toMainActivity(null);


        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoginTask loginTask = new LoginTask();
                loginTask.execute(username_form.getText(), password_form.getText());
            }
        });

        signup_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toSignUpActivity(view);
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
     * Transfer user to sign-up activity
     */
    public void toSignUpActivity(View view) {
        Log.d("METHOD", "------ TO SIGN-UP ACTIVITY ------");

        Intent signup_activity = new Intent(this, SignUpActivity.class);
        startActivity(signup_activity);
    }

    public class LoginTask extends AsyncTask<Editable, Void, Integer>{

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
                Log.d("LOG IN", "Username is required!");
//                                Toast.makeText(LoginActivity.this, "Username is required!", Toast.LENGTH_SHORT).show();
                return null;
            }
            if(TextUtils.isEmpty(editables[1])){
                Log.d("LOG IN", "Password is required!");
//                                Toast.makeText(LoginActivity.this, "Password is required!", Toast.LENGTH_SHORT).show();
                return null;
            }

            //connect with Broker
            String username = editables[0].toString();
            BigInteger password = Utilities.SHA1(editables[1].toString());
            return consumer.loginUser(new Pair<>(username, password));
        }

        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            if(res == -2){
                //Connection failed
                Log.d("LOG IN", "Connection failed");
                Toast.makeText(LoginActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
            }else if(res == -1){
                //wrong credentials
                Log.d("LOG IN", "Wrong credentials");
                Toast.makeText(LoginActivity.this, "Wrong credentials", Toast.LENGTH_SHORT).show();
            }else if(res == 0){
                //no account
                Log.d("LOG IN", "No such account found please register");
                Toast.makeText(LoginActivity.this, "No such account found please register", Toast.LENGTH_SHORT).show();
                toSignUpActivity(null);
            }else if(res == 1){
                //successful login
                Log.d("LOG IN", "Success!!!");
                MainActivity.setConsumer(consumer);
                toMainActivity(null);
            }
        }
    }
}