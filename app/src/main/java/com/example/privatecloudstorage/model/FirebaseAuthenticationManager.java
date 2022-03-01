package com.example.privatecloudstorage.model;
//android libraries
import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.util.Observable;

import androidx.annotation.NonNull;
//3rd party libraries
import com.example.privatecloudstorage.controller.SignUpActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
/**
 * manage firebase authentication
 */
public class FirebaseAuthenticationManager extends Observable {
    private static FirebaseAuthenticationManager mFirebaseAuthenticationManager;
    private FirebaseAuth mFirebaseAuth;
    private static final String TAG = "FirebaseAuthenticationManager";
    String message="";

    private FirebaseAuthenticationManager() {
        mFirebaseAuth = FirebaseAuth.getInstance();
    }

    /**
     * get FirebaseAuth instance
     * @return new mFirebaseAuthenticationManager if not already initiated
     */
    public static FirebaseAuthenticationManager getInstance() {
        if (mFirebaseAuthenticationManager == null)
            mFirebaseAuthenticationManager = new FirebaseAuthenticationManager();

        return mFirebaseAuthenticationManager;
    }

    /**
     * validte Sign Up
     * @param email    the user email
     * @param pass1    the user password
     * @param userName the user name
     * @param isOnline the user statuse
     */
    public boolean SignUp(final String email, String pass1, final String userName, final boolean isOnline, final Activity activity) {
        mFirebaseAuth.createUserWithEmailAndPassword(email, pass1).addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
            @SuppressLint("LongLogTag")
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "createUserWithEmail:failure", task.getException());
                } else {
                    mFirebaseAuth.getCurrentUser().sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful())
                            {
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(userName).build();

                                user.updateProfile(profileUpdates)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @SuppressLint("LongLogTag")
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Log.d(TAG, "User profile updated.");
                                                }
                                            }
                                        });
                                //Log.d("++++++++++++++++++++++++++++++++++++++", String.valueOf(user.isEmailVerified()));
                                 message = "Please, verify your email ";
                            }else
                                message = "Error in sending email verification email";
                            setChanged();
                            notifyObservers(message);
                        }
                    });
                }
            }
        });
        //mFirebaseAuthenticationManager.SignIn(email,pass1, activity);
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //mFirebaseAuth.getCurrentUser() != null
        return (mFirebaseAuth.getCurrentUser().isEmailVerified());
    }

    /**
     * validate user sign in
     * @param email    user email
     * @param pass     user password
     * @param activity Sign in activity
     * @return true in case of successful sign in, else false
     */
    public boolean SignIn(String email, String pass, Activity activity) {
        mFirebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
            @SuppressLint("LongLogTag")
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!task.isSuccessful())
                    Log.w(TAG, "SignIn:failure", task.getException());
            }
        });
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return (mFirebaseAuth.getCurrentUser() != null);
    }
    /**
     * gets the current user
     * @return the current user
     */
    public FirebaseUser getCurrentUser() {
        return mFirebaseAuth.getCurrentUser();
    }
    /**
     * allow user to reset his password when been forgetten
     * @param email the user email
     * @param _ProgressBar the progress bar
     * @param activity the sign in activity
     */
    public void ForgetPassword(String email, final ProgressBar _ProgressBar, final Activity activity) {
        mFirebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @SuppressLint("LongLogTag")
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                _ProgressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    message = "Check your email to change your passwored";
                } else {
                    Log.d(TAG, task.getException().getMessage());
                    message = "Error in sending password reset email";
                }
                setChanged();
                notifyObservers(message);
            }
        });
    }
}