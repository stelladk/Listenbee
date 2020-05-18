package com.distributedsystems.listenbee;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;

import com.example.eventdeliverysystem.Consumer;
import com.example.eventdeliverysystem.Utilities;

import java.math.BigInteger;

public class SignUpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_signup);

        final Consumer consumer = MainActivity.getConsumer();

        Button sign_up_btn = findViewById(R.id.signup_btn);
        final EditText username_form = findViewById(R.id.username_form);
        final EditText password_form = findViewById(R.id.password_form);
        final EditText email_form = findViewById(R.id.email_form);
        final EditText age_form = findViewById(R.id.age_form);
        sign_up_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(username_form.getText().toString() == ""){
                    Toast.makeText(SignUpActivity.this, "Username is required!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(password_form.getText().toString() == ""){
                    Toast.makeText(SignUpActivity.this, "Password is required!", Toast.LENGTH_SHORT).show();
                    return;//TODO
                }if(email_form.getText().toString() == ""){
                    Toast.makeText(SignUpActivity.this, "Password is required!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Pair<String,BigInteger> creds = new Pair<String, BigInteger>(username_form.getText().toString(), Utilities.SHA1(password_form.getText().toString()));
                Pair<String, Integer> extra = new Pair<String, Integer>(email_form.getText().toString(), Integer.parseInt(age_form.getText().toString()));
                int res = consumer.registerUser(creds, extra);
                if(res == 0){
                   Toast.makeText(SignUpActivity.this, "Username already exists!", Toast.LENGTH_SHORT).show();
                }else if(res == -1){
                    Toast.makeText(SignUpActivity.this, "Server is not available try again later", Toast.LENGTH_SHORT).show();
                }else if(res == 1){
                    toMainActivity(null);
                }
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

}
