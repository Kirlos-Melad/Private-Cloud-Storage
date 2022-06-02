package com.example.privatecloudstorage.controller;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.example.privatecloudstorage.databinding.ActivitySignUpBinding;
import com.example.privatecloudstorage.interfaces.IAction;
import com.example.privatecloudstorage.model.FirebaseAuthenticationManager;
import com.example.privatecloudstorage.model.ManagersMediator;

import java.util.Observable;
import java.util.Observer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * allow users to sign up to make new firebase accounts
 */
@RequiresApi(api = Build.VERSION_CODES.Q)
public class SignUpActivity extends AppCompatActivity implements Observer {
    private static final String TAG = "SignUpActivity";
    private @NonNull ActivitySignUpBinding _ActivitySignUpBinding;
    private final ManagersMediator MANAGERS_MEDIATOR = ManagersMediator.getInstance();
    private FirebaseAuthenticationManager mFirebaseAuthenticationManager;

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
        _ActivitySignUpBinding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(_ActivitySignUpBinding.getRoot());

        mFirebaseAuthenticationManager=FirebaseAuthenticationManager.getInstance();
        mFirebaseAuthenticationManager.addObserver(this);

        /**
         * handle Sign in Text press
         */
        _ActivitySignUpBinding.SignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(SignUpActivity.this,SignInActivity.class);
                startActivity(intent);
            }
        });


        /**
         * handle Sign Up button press
         */
        _ActivitySignUpBinding.SignUpButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick (View v){
                String userName= _ActivitySignUpBinding.UserName.getText().toString().trim();
                String email= _ActivitySignUpBinding.EmailSignUp.getText().toString().trim();
                String password1=_ActivitySignUpBinding.PasswordSignup.getText().toString().trim();
                String password2=_ActivitySignUpBinding.RePasswordSignUp.getText().toString().trim();

                if(TextUtils.isEmpty(userName)){
                    _ActivitySignUpBinding.userNameLayout.setError("Name is required");
                    return;
                }
                if(TextUtils.isEmpty(email) || !email.contains("@")){
                    _ActivitySignUpBinding.userNameLayout.setError("Email is required");
                    return;
                }
                if(TextUtils.isEmpty(password1)){
                    _ActivitySignUpBinding.passwordHelperTextLayout.setError("Password is required");
                    return;
                }
                if(TextUtils.isEmpty(password2)){
                    _ActivitySignUpBinding.RepasswordHelperTextLayout.setError("Re-password is required");
                    return;
                }
                if(!isValidPassword(password1)){
                    Toast.makeText(SignUpActivity.this, "Password is not Strong Enough", Toast.LENGTH_LONG).show();
                    return;
                }
                if(!password1.equals(password2))
                {
                    Toast.makeText(SignUpActivity.this, "Invalid Password", Toast.LENGTH_LONG).show();
                    return;
                }

                //make object from FirebaseAuthenticationManager
                FirebaseAuthenticationManager mFirebaseAuthenticationManager = FirebaseAuthenticationManager.getInstance();

                MANAGERS_MEDIATOR.SignUp(userName, email, password1, object -> {
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                });
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
//            _ActivitySignUpBinding.passwordHelperTextLayout.setError("Minimum 6 characters..");
//            isValid=false;
//        }
//        else if(!Pattern.matches("(?=.*[a-z])",password)){
//            _ActivitySignUpBinding.passwordHelperTextLayout.setError("Must Contain 1 Lower-Case Character");
//            isValid=false;
//
//        }
//        else if(!Pattern.matches("[A-Z]+",password)){
//            _ActivitySignUpBinding.passwordHelperTextLayout.setError("Must Contain 1 Upper-Case Character");
//            isValid=false;
//
//        }
//        else if(!password.matches("[0-9]+")){
//            _ActivitySignUpBinding.passwordHelperTextLayout.setError("Must Contain 1 Number");
//            isValid=false;

//        }
//        else if(!password.matches("[@#$%^&+=]+")){
//            _ActivitySignUpBinding.passwordHelperTextLayout.setError("Must Contain 1 Special Character");
//            isValid=false;

//        }
//        else if(!password.matches("(?=\\\\S+$)")){
//            _ActivitySignUpBinding.passwordHelperTextLayout.setError("Must not Contain Whitespaces");
//            isValid=false;
//        }
//        else{
//            _ActivitySignUpBinding.passwordHelperTextLayout.setError(null);}
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

    @Override
    public void update(Observable observable, Object o) {
        Toast.makeText(SignUpActivity.this, o.toString(), Toast.LENGTH_LONG).show();
    }
}