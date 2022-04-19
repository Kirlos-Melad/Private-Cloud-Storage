package com.example.privatecloudstorage.controller;

//android libraries
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.example.privatecloudstorage.databinding.ActivitySignInBinding;
import com.example.privatecloudstorage.model.FirebaseAuthenticationManager;

import java.util.Observable;
import java.util.Observer;


/**
 * signs users in in case if them email and password are valid
 * , if not it tell them that there's invalid name or password
 */
public class SignInActivity extends AppCompatActivity implements Observer {
    private static final String TAG = "SignInActivity";
    private @NonNull ActivitySignInBinding _ActivitySignInBinding;

    FirebaseAuthenticationManager mFirebaseAuthenticationManager;
    /**
     * handle sign up activity
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _ActivitySignInBinding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(_ActivitySignInBinding.getRoot());


        mFirebaseAuthenticationManager=FirebaseAuthenticationManager.getInstance();
        mFirebaseAuthenticationManager.addObserver(this);

        if(mFirebaseAuthenticationManager.getCurrentUser() != null && mFirebaseAuthenticationManager.getCurrentUser().isEmailVerified()){
            startActivity(new Intent(SignInActivity.this,GroupListActivity.class));
            finish();
        }

        /**
         * handle Forget Password Text press
         */
        _ActivitySignInBinding.forgetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _ActivitySignInBinding.ProgressBarForgetPass.setVisibility(View.VISIBLE);
                mFirebaseAuthenticationManager.ForgetPassword(_ActivitySignInBinding.Email.getText().toString().trim(), _ActivitySignInBinding.ProgressBarForgetPass, SignInActivity.this);
            }
        });

        /**
         * handle Sign up Text press
         */
        _ActivitySignInBinding.SignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(SignInActivity.this,SignUpActivity.class);
                startActivity(intent);
            }
        });


        /**
         * handle Sign In button press
         */
        _ActivitySignInBinding.SignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                String email=_ActivitySignInBinding.Email.getText().toString().trim();
                String password=_ActivitySignInBinding.SignInPassword.getText().toString().trim();

                    if (TextUtils.isEmpty(email)) {
                        _ActivitySignInBinding.emailLayout.setError("Email is required");
                        return;
                    } else if (TextUtils.isEmpty(password)) {
                        _ActivitySignInBinding.passwordLayout.setError("Password is required");
                        return;
                    } else if (email.isEmpty() && password.isEmpty()) {
                        Toast.makeText(SignInActivity.this, "Enter Your Data", Toast.LENGTH_LONG).show();
                        return;
                    } else if (!(email.isEmpty() && password.isEmpty())) {
                        boolean validSignIn = mFirebaseAuthenticationManager.SignIn(email, password, SignInActivity.this);
                        if (!validSignIn)
                            Toast.makeText(SignInActivity.this, "Invalid Email or Password", Toast.LENGTH_LONG).show();
                        else {
                            if (mFirebaseAuthenticationManager.getCurrentUser().isEmailVerified()) {
                                startActivity(new Intent(getApplicationContext(), GroupListActivity.class));
                                finish();
                            } else {
                                Toast.makeText(SignInActivity.this, "Please, Verify Your Email Address", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }
        });
    }

    @Override
    public void update(Observable observable, Object o) {
        Toast.makeText(SignInActivity.this, o.toString(), Toast.LENGTH_LONG).show();
    }
}