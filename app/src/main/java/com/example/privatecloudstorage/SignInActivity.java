package com.example.privatecloudstorage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.service.autofill.UserData;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SignInActivity extends AppCompatActivity {
    EditText mEmail, mPass;
    Button mSignInBtn;

    FirebaseAuth mFirebaseAuth;
    FirebaseDatabase mFirebaseDatabase;
    private  FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mEmail = findViewById(R.id.email);
        mPass = findViewById(R.id.pass);
        mSignInBtn = findViewById(R.id.signIn);

        //TODO: Use The class
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();

        //TODO: Move it to FirebaseAuthenticationManager
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if(firebaseUser != null) {
                    MoveToHome(firebaseUser);
                }
                else{
                    Toast.makeText(SignInActivity.this, "Enter Your Data", Toast.LENGTH_LONG).show();
                }
            }
        };
        mSignInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEmail.getText().toString().trim();
                String pass = mPass.getText().toString().trim();

                if(TextUtils.isEmpty(email)){
                    mEmail.setError("Email is required");
                    return;
                }
                else if(TextUtils.isEmpty(pass)) {
                    mPass.setError("Password is required");
                    return;
                }
                else if(email.isEmpty() && pass.isEmpty()) {
                    Toast.makeText(SignInActivity.this, "Enter Your Data", Toast.LENGTH_LONG).show();
                    return;
                }
                else if (!(email.isEmpty() && pass.isEmpty()))
                {
                    //TODO: Move it to FirebaseAuthenticationManager
                    mFirebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(SignInActivity.this,
                            new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if(!task.isSuccessful()){
                                        Toast.makeText(SignInActivity.this, "Invalid Email or password", Toast.LENGTH_LONG).show();
                                    }else{
                                        MoveToHome(task.getResult().getUser());
                                    }
                                }
                            });
                }
            }
        });
    }

    private void MoveToHome(FirebaseUser firebaseUser)
    {
        mFirebaseDatabase.getReference().child("Users").child(firebaseUser.getUid()).addValueEventListener((new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserDataActivity userDataActivity = snapshot.getValue(UserDataActivity.class);
                Intent intent = new Intent(getApplicationContext(), HomePageActivity.class);
                startActivity(intent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        }));
    }
}