package com.example.privatecloudstorage;

//android libraries
import android.app.Activity;
import android.util.Log;
import androidx.annotation.NonNull;

//3rd party libraries
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * manage firebase authentication
 */
public class FirebaseAuthenticationManager {
    private static FirebaseAuthenticationManager mFirebaseAuthenticationManager;
    private FirebaseAuth mFirebaseAuth;

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
     * @param email the user email
     * @param pass1 the user password
     * @param userName the user name
     * @param isOnline the user statuse
     *
     */
    public boolean SignUp(final String email, String pass1, final String userName, final boolean isOnline, Activity activity){
        mFirebaseAuth.createUserWithEmailAndPassword(email, pass1).addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(!task.isSuccessful()) {
                    Log.w("message", "createUserWithEmail:failure", task.getException());
                }
                else{
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(userName).build();

                    user.updateProfile(profileUpdates)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.d("TAG", "User profile updated.");
                                    }
                                }
                            });
                }
            }
        });
        return (mFirebaseAuth.getCurrentUser() != null);
    }

    /**
     * validate user sign in
     * @param email user email
     * @param pass user password
     * @param activity Sign in activity
     * @return true in case of successful sign in, else false
     */
    public boolean SignIn(String email, String pass, Activity activity) {
        mFirebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!task.isSuccessful())
                    Log.w("message", "SignIn:failure", task.getException());
            }
        });
        return (mFirebaseAuth.getCurrentUser() != null);
    }

    public FirebaseUser getCurrentUser(){
        return mFirebaseAuth.getCurrentUser();
    }
}
