package com.example.privatecloudstorage;
import android.net.Uri;
import android.os.Build;
import android.os.FileObserver;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;

public class FirebaseStorageManager extends FileObserver {
    private static FirebaseStorageManager mFirebaseStorageManager;
    private FirebaseStorage mStorage;
    private FirebaseDatabaseManager mFirebaseDatabaseManager;
    private File mObserver;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private FirebaseStorageManager(File observed, int events){
        super(observed, events);

        mStorage = FirebaseStorage.getInstance();
        mFirebaseDatabaseManager = FirebaseDatabaseManager.getInstance();
        mObserver=observed;

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static FirebaseStorageManager getInstance(File observed) {
        return getInstance(observed, ALL_EVENTS);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static FirebaseStorageManager getInstance(File observed, int events){
        if(mFirebaseStorageManager == null)
            mFirebaseStorageManager  = new FirebaseStorageManager(observed, events);

        return mFirebaseStorageManager;
    }


    /**
     * Is Called on any passed event in a new thread
     * Must call startWatching ONCE to start listening to events
     *
     * @param event
     * @param path
     */
    @Override
    public void onEvent(int event, @Nullable String path) {
        //FileObserver Monitors files to fire events if file accessed or changed by any procces
        mFirebaseDatabaseManager.GetUserGroups().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isGroup = false;
                String group = null;

                for(DataSnapshot child : snapshot.getChildren()){
                    group = child.getValue().toString();
                    isGroup = path.endsWith(group);

                    if(path.contains(group)){
                        group = child.getKey();
                        break;
                    }
                }

                if(!isGroup) UploadFile(group, new File(mObserver.getPath()+ File.separator + path));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String getFileExtension(Uri uri) {
        /*ContentResolver contentResolverR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(contentResolverR.getType(uri));*/

        return null;
    }

    /**
     * Upload new file to cloud
     * @param groupId Group ID
     * @param file File path
     */
    private void UploadFile(String groupId, File file) {
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("*/*")
                .build();

        Uri fileUri = Uri.fromFile(file);
        StorageReference fileReference = mStorage.getReference().child(groupId ).child(fileUri.getLastPathSegment());
        fileReference.putFile(fileUri, metadata)
                .addOnSuccessListener(taskSnapshot -> {
                  //  mFirebaseDatabaseManager.AddFile(groupId, taskSnapshot.getMetadata());
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });
    }
    public void print(){
        Log.d("show:  ", mFirebaseStorageManager.mObserver.toString());
    }
}
