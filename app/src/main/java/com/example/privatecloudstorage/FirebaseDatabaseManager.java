package com.example.privatecloudstorage;

import com.google.firebase.database.FirebaseDatabase;

public class FirebaseDatabaseManager {
    private static FirebaseDatabaseManager mFirebaseDatabaseManager;
    private FirebaseDatabase mDataBase;

    private FirebaseDatabaseManager(){
        mDataBase = FirebaseDatabase.getInstance();
    }

    public static FirebaseDatabaseManager getInstance(){
        if(mFirebaseDatabaseManager == null)
            mFirebaseDatabaseManager  = new FirebaseDatabaseManager();

        return mFirebaseDatabaseManager;
    }
}
