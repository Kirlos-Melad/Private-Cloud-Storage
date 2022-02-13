package com.example.privatecloudstorage;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class FirebaseAuthenticationManager {
    private static FirebaseAuthenticationManager mFirebaseAuthenticationManager;
    private FirebaseAuth mFirebaseAuth;

    private FirebaseAuthenticationManager() {
        mFirebaseAuth = FirebaseAuth.getInstance();
    }

    public static FirebaseAuthenticationManager getInstance() {
        if (mFirebaseAuthenticationManager == null)
            mFirebaseAuthenticationManager = new FirebaseAuthenticationManager();

        return mFirebaseAuthenticationManager;
    }
}
