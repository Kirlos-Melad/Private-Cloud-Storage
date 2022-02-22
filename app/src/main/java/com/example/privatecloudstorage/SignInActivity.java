package com.example.privatecloudstorage;

//android libraries
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
// 3rd party libraries


/**
 * signs users in in case if them email and password are valid
 * , if not it tell them that there's invalid name or password
 */
public class SignInActivity extends AppCompatActivity {
    EditText _Email, _Pass;
    Button _SignInBtn;
    TextView _ForgetPasword;
    ProgressBar _ProgressBar;

    FirebaseAuthenticationManager mFirebaseAuthenticationManager;
    /**
     * handle sign up activity
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        _Email = findViewById(R.id.Email);
        _Pass = findViewById(R.id.SignInPassword);
        _SignInBtn = findViewById(R.id.SignIn_Button);
        _ForgetPasword=findViewById(R.id.forgetPassword);
        _ProgressBar = findViewById(R.id.ProgressBar_ForgetPass);
        mFirebaseAuthenticationManager=FirebaseAuthenticationManager.getInstance();

        _ForgetPasword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _ProgressBar.setVisibility(View.VISIBLE);
                mFirebaseAuthenticationManager.ForgetPassword(_Email.getText().toString().trim(), _ProgressBar, SignInActivity.this);
            }
        });

        /**
         * handle Sign Up button press
         */
        _SignInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = _Email.getText().toString().trim();
                String pass = _Pass.getText().toString().trim();

                if(TextUtils.isEmpty(email)){
                    _Email.setError("Email is required");
                    return;
                }
                else if(TextUtils.isEmpty(pass)) {
                    _Pass.setError("Password is required");
                    return;
                }
                else if(email.isEmpty() && pass.isEmpty()) {
                    Toast.makeText(SignInActivity.this, "Enter Your Data", Toast.LENGTH_LONG).show();
                    return;
                }
                else if (!(email.isEmpty() && pass.isEmpty()))
                {
                    boolean validSignIn = mFirebaseAuthenticationManager.SignIn(email, pass, SignInActivity.this);
                    if(!validSignIn)
                        Toast.makeText(SignInActivity.this, "Invalid Email or Password", Toast.LENGTH_LONG).show();
                    else{
                        if(mFirebaseAuthenticationManager.getCurrentUser().isEmailVerified()){
                            startActivity(new Intent(getApplicationContext(),HomePageActivity.class));
                            finish();
                        }else{
                            Toast.makeText(SignInActivity.this, "Please, Verify Your Email Address", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        });
    }
}