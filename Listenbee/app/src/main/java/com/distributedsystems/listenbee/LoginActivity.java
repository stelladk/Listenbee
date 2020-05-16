package com.distributedsystems.listenbee;

import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
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
import java.net.InetAddress;
import java.net.UnknownHostException;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        Button login_btn = findViewById(R.id.login_btn);
        Button signup_btn = findViewById(R.id.signup_btn);
        final EditText username_form = findViewById(R.id.username_form);
        final EditText password_form = findViewById(R.id.password_form);

        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread login_thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //get IP address
                            String client_IP = InetAddress.getLocalHost().getHostAddress();
                            Log.d("LOCALHOST", client_IP);
                            WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
                            client_IP = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
                            Log.d("LOCALHOST", client_IP);
                            String server_IP = "192.168.1.3";
                            final Consumer consumer = new Consumer(client_IP, server_IP, Broker.getToCliPort());

                            //check required forms
                            if(TextUtils.isEmpty(username_form.getText())){
                                Log.d("LOG IN", "Username is required!");
//                                Toast.makeText(LoginActivity.this, "Username is required!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if(TextUtils.isEmpty(password_form.getText())){
                                Log.d("LOG IN", "Password is required!");
//                                Toast.makeText(LoginActivity.this, "Password is required!", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            //connect with Broker
                            int res = consumer.loginUser(new Pair<String, BigInteger>(username_form.getText().toString(), Utilities.SHA1(password_form.getText().toString())));
                            if(res == -1){
                                //wrong credentials
                                Log.d("LOG IN", "Wrong credentials");
//                                Toast.makeText(LoginActivity.this, "Wrong credentials", Toast.LENGTH_SHORT).show();
                            }else if(res == 0){
                                //no account
                                Log.d("LOG IN", "No such account found please register");
//                                Toast.makeText(LoginActivity.this, "No such account found please register", Toast.LENGTH_SHORT).show();
                                toSignUpActivity(null);
                            }else if(res == 1){
                                //successful login
                                Log.d("LOG IN", "Success!!!");
                                MainActivity.setConsumer(consumer);
                                toMainActivity(null);
                            }
                        } catch (UnknownHostException e) {
                            Log.d("LOG IN", "No internet connection at the moment!");
//                            Toast.makeText(LoginActivity.this, "No internet connection at the moment!", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                });
                login_thread.start();
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
}