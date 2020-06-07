package com.distributedsystems.listenbee;

import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventdeliverysystem.models.Broker;
import com.example.eventdeliverysystem.models.Consumer;
import com.example.eventdeliverysystem.utilities.Pair;
import com.example.eventdeliverysystem.utilities.Utilities;


public class LoginActivity extends AppCompatActivity {
    private String clientIP;
    private String brokerIP;
    private Consumer consumer;

    private EditText usernameForm;
    private EditText passwordForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        usernameForm = findViewById(R.id.username_form);
        passwordForm = findViewById(R.id.password_form);

        Button ipchanger = findViewById(R.id.ipchanger);
        ipchanger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IPDialog dialog = new IPDialog();
                dialog.show(getSupportFragmentManager(), "IP Changer");
            }
        });

        new InternetInfo().execute();
    }

    /**
     * Login user to the app
     */
    public void login(View view) {
        Log.d("METHOD", "------ LOG-IN ------");

        String username = usernameForm.getText().toString().trim();
        String password = passwordForm.getText().toString();

        new LogInTask().execute(username, password);
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
     * Transfer user to sign-up activity
     */
    public void toSignUpActivity(View view) {
        Log.d("METHOD", "------ TO SIGN-UP ACTIVITY ------");

        Intent signup_activity = new Intent(this, SignUpActivity.class);
        startActivity(signup_activity);
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

            brokerIP = IPDialog.serverIP();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            consumer = new Consumer(clientIP, brokerIP, Broker.getToCliPort());
        }
    }

    /**
     * Takes user credential, communicates with Broker and find if user exists
     */
    private class LogInTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... str) {
            return consumer.loginUser(new Pair<>(str[0], Utilities.SHA1(str[1])));
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
                //Connection failed
                case -2:
                    Log.d("LOG-IN", "Connection failed");
                    Toast.makeText(LoginActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
                    break;
                //wrong credentials
                case -1:
                    Log.e("LOG-IN", "Wrong credentials");
                    Toast.makeText(LoginActivity.this, "Wrong credentials", Toast.LENGTH_SHORT).show();
                    break;
                //no account
                case 0:
                    Log.e("LOG-IN", "No such account found");
                    Toast.makeText(LoginActivity.this, "No account found, please register", Toast.LENGTH_SHORT).show();
                    break;
                //successful login
                case 1:
                    Log.d("LOG-IN", "Success");
                    MainActivity.setConsumer(consumer);
                    toMainActivity();
            }
        }
    }
}