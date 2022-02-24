package com.example.privatecloudstorage.controller;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.privatecloudstorage.model.FirebaseAuthenticationManager;
import com.example.privatecloudstorage.R;
import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * allow users to sign up to make new firebase accounts
 */
public class SignUpActivity extends AppCompatActivity {
    private static final String TAG = "SignUpActivity";
    private TextInputLayout _passwordHelperTextLayout;
    private TextInputLayout _RepasswordHelperTextLayout;


    EditText _UserName, _Email, _Pass1, _Pass2;
    Button _SignUpBtn;
    TextView _SignIn;

    @Override
    public void onPanelClosed(int featureId, @NonNull Menu menu) {
        super.onPanelClosed(featureId, menu);
    }

    /**
     * handle sign up activity
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        _UserName = findViewById(R.id.UserName);
        _Email = findViewById(R.id.Email_SignUp);
        _Pass1 = findViewById(R.id.Password_Signup);
        _Pass2 = findViewById(R.id.RePassword_SignUp);
        _SignUpBtn = findViewById(R.id.SignUp_Button);
        _SignIn=findViewById(R.id.SignIn);

        _passwordHelperTextLayout=findViewById(R.id.passwordHelperTextLayout);
        _RepasswordHelperTextLayout=findViewById(R.id.RepasswordHelperTextLayout);

        /**
         * handle Sign in Text press
         */
        _SignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(SignUpActivity.this,SignInActivity.class);
                startActivity(intent);
            }
        });


        /**
         * handle Sign Up button press
         */
        _SignUpBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick (View v){
                final String userName = _UserName.getText().toString().trim();
                final String email = _Email.getText().toString();
                final String pass1 =_passwordHelperTextLayout.getEditText().getText().toString().trim();
                String pass2 = _Pass2.getText().toString();

                if(TextUtils.isEmpty(userName)){
                    _UserName.setError("Name is required");
                    return;
                }
                if(TextUtils.isEmpty(email) || !email.contains("@")){
                    _Email.setError("Email is required");
                    return;
                }
                if(TextUtils.isEmpty(pass1)){
                    _passwordHelperTextLayout.setError("Password is required");
                    return;
                }
                if(TextUtils.isEmpty(pass2)){
                    _RepasswordHelperTextLayout.setError("Re-password is required");
                    return;
                }
                if(!isValidPassword(pass1)){
                    Toast.makeText(SignUpActivity.this, "Password is not Strong Enough", Toast.LENGTH_LONG).show();
                    return;
                }
                if(!pass1.equals(pass2))
                {
                    Toast.makeText(SignUpActivity.this, "Invalid Password", Toast.LENGTH_LONG).show();
                    return;
                }

                //make object from FirebaseAuthenticationManager
                FirebaseAuthenticationManager mFirebaseAuthenticationManager = FirebaseAuthenticationManager.getInstance();
                //call ValidateSignUp to validate sign up
                boolean signedUp = mFirebaseAuthenticationManager.SignUp(email, pass1, userName, false, SignUpActivity.this);
                if (signedUp == false)
                    Toast.makeText(SignUpActivity.this, "Invalid Sign Up", Toast.LENGTH_LONG).show();
                else{
                    if(mFirebaseAuthenticationManager.getCurrentUser().isEmailVerified()){
                        startActivity(new Intent(getApplicationContext(),HomePageActivity.class));
                        finish();
                    }else{
                        Toast.makeText(SignUpActivity.this, "Please, Verify Your Email Address", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    /**
     * check password strength
     * @param password the sign up password
     * @return true if the sign up password is strong
     */
//    private boolean isValidPassword(final String password){
//        boolean isValid = true;
//        if(password.length()<6) {
//            _passwordHelperTextLayout.setError("Minimum 6 characters..");
//            isValid=false;
//        }
//        else if(!password.matches("^(?=.*[a-z])$")){
//            _passwordHelperTextLayout.setError("Must Contain 1 Lower-Case Character");
//            isValid=false;
//
//        }
//        else if(!password.matches("^(?=.*[A-Z])$")){
//            _passwordHelperTextLayout.setError("Must Contain 1 Upper-Case Character");
//            isValid=false;
//
//        }
//        else if(!password.matches("^(?=.*[@#$%^&+=])$")){
//            _passwordHelperTextLayout.setError("Must Contain 1 Special Character");
//            isValid=false;
//
//        }
//        else if(!password.matches("^(?=\\\\S+$)")){
//            _passwordHelperTextLayout.setError("Must not Contain Whitespaces");
//            isValid=false;
//        }
//        else{
//         _passwordHelperTextLayout.setError(null);}
//        return isValid;
//    }

    /**
     * check password strength
     * @param password the sign up password
     * @return true if the sign up password is strong
     */
    public boolean isValidPassword(final String password) {

        Pattern pattern;
        Matcher matcher;
//      ^                 # start-of-string
//      (?=.*[0-9])       # a digit must occur at least once
//      (?=.*[a-z])       # a lower case letter must occur at least once
//      (?=.*[A-Z])       # an upper case letter must occur at least once
//      (?=.*[@#$%^&+=])  # a special character must occur at least once you can replace with your special characters
//      (?=\\S+$)         # no whitespace allowed in the entire string
//      .{4,}             # anything, at least six places though
//      $                 # end-of-string
        final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{4,}$";

        pattern = Pattern.compile(PASSWORD_PATTERN);
        matcher = pattern.matcher(password);

        return matcher.matches();
    }
}