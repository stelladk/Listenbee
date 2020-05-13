package com.distributedsystems.listenbee;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;

import com.example.eventdeliverysystem.Consumer;
import com.example.eventdeliverysystem.Utilities;

import java.math.BigInteger;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        final Consumer consumer = MainActivity.getConsumer();

        Button login_btn = findViewById(R.id.login_btn);
        Button signup_btn = findViewById(R.id.signup_btn);
        final EditText username_form = findViewById(R.id.email_form);
        final EditText password_form = findViewById(R.id.password_form);

        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(username_form.getText().toString() == ""){
                    Toast.makeText(LoginActivity.this, "Username is required!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(password_form.getText().toString() == ""){
                    Toast.makeText(LoginActivity.this, "Password is required!", Toast.LENGTH_SHORT).show();
                    return;
                }
                int res = consumer.loginUser(new Pair<String, BigInteger>(username_form.getText().toString(), Utilities.SHA1(password_form.getText().toString())));
                if(res == -1){
                    Toast.makeText(LoginActivity.this, "Wrong credentials", Toast.LENGTH_SHORT).show();
                }else if(res == 0){
                    Toast.makeText(LoginActivity.this, "No such account found please register", Toast.LENGTH_SHORT).show();
                    toSignUpActivity(null);
                }else if(res == 1){
                    toMainActivity(null);
                }
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