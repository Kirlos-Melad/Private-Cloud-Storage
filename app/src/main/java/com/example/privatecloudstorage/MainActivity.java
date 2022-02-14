package com.example.privatecloudstorage;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * main class to start program
 */
public class MainActivity extends AppCompatActivity {
    private Button _SignIn, _SignUp;
    private FirebaseAuthenticationManager mFirebaseAuthenticationManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _SignIn = findViewById(R.id.signInMain);
        _SignUp = findViewById(R.id.signUpMain);

        mFirebaseAuthenticationManager = FirebaseAuthenticationManager.getInstance();

        if(mFirebaseAuthenticationManager.getCurrentUser() != null){
            startActivity(new Intent(getApplicationContext(),HomePageActivity.class));
        }

        _SignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),SignInActivity.class));
            }
        });

        _SignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),SignUpActivity.class));
            }
        });

    }
}
