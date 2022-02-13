package com.example.privatecloudstorage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.service.autofill.UserData;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {
    EditText mUserName, mEmail, mPass1, mPass2;
    Button mSignUpBtn;

    FirebaseAuth mFireBaseAuth;
    FirebaseDatabase mFireBaseDatabase;

    @Override
    public void onPanelClosed(int featureId, @NonNull Menu menu) {
        super.onPanelClosed(featureId, menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mUserName = findViewById(R.id.userName);
        mEmail = findViewById(R.id.email);
        mPass1 = findViewById(R.id.pass1);
        mPass2 = findViewById(R.id.pass2);
        mSignUpBtn = findViewById(R.id.signUp);

        mFireBaseAuth = FirebaseAuth.getInstance();
        mFireBaseDatabase = FirebaseDatabase.getInstance();

        mSignUpBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick (View v){
                final String userName = mUserName.getText().toString().trim();
                final String email = mEmail.getText().toString();
                final String pass1 = mPass1.getText().toString();
                String pass2 = mPass2.getText().toString();

                if(TextUtils.isEmpty(userName)){
                    mUserName.setError("Name is required");
                    return;
                }
                if(TextUtils.isEmpty(email) || !email.contains("@")){
                    mEmail.setError("Email is required");
                    return;
                }
                if(TextUtils.isEmpty(pass1)){
                    mPass1.setError("Password is required");
                    return;
                }
                if(TextUtils.isEmpty(pass2)){
                    mPass2.setError("Re-password is required");
                    return;
                }
                if(!pass1.equals(pass2))
                {
                    Toast.makeText(SignUpActivity.this, "Invalid Password", Toast.LENGTH_LONG).show();
                    return;
                }

                //TODO: Move it to FirebaseAuthenticationManager
                mFireBaseAuth.createUserWithEmailAndPassword(email, pass1).addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()) {
                            Log.w("message", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(SignUpActivity.this, "Invalid Sign Up", Toast.LENGTH_LONG).show();
                        }
                        else{
                            UserDataActivity userData = new UserDataActivity(email, pass1, userName);
                            String uid = task.getResult().getUser().getUid();
                            mFireBaseDatabase.getReference("Users").push().setValue(userData).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    startActivity(new Intent(getApplicationContext(),HomePageActivity.class));
                                }
                            });
                        }
                    }
                });

                //FirebaseAuthenticationManager authMangaer = new FirebaseAuthenticationManager();
                //authMangaer.SaveUserData(userName, email, pass1);

            }
        });
    }
}