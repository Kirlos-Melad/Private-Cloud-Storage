package com.example.privatecloudstorage;
import com.google.firebase.storage.FirebaseStorage;

public class FirebaseStorageManager {
    private static FirebaseStorageManager mFirebaseStorageManager;
    private FirebaseStorage mStorageManager;

    private FirebaseStorageManager(){
        mStorageManager = FirebaseStorage.getInstance();
    }

    public static FirebaseStorageManager getInstance(){
        if(mFirebaseStorageManager == null)
            mFirebaseStorageManager  = new FirebaseStorageManager();

        return mFirebaseStorageManager;
    }


}
