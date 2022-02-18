package com.example.privatecloudstorage;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.net.Uri;
import android.os.Build;
import android.os.FileObserver;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class FirebaseStorageManager {
    private static FirebaseStorageManager mFirebaseStorageManager;
    private FirebaseStorage mStorage;
    private FirebaseDatabaseManager mFirebaseDatabaseManager;
    private RecursiveFileObserver mRecursiveFileObserver;
    private ExecutorService mExecutorService;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private FirebaseStorageManager(File groupsFolder, int events){
        mStorage = FirebaseStorage.getInstance();
        mFirebaseDatabaseManager = FirebaseDatabaseManager.getInstance();
        mExecutorService = newSingleThreadExecutor();

        // Create Recursive Directory Listener
        mRecursiveFileObserver = new RecursiveFileObserver(groupsFolder.getAbsolutePath(), events,
                (event, file) -> {
            if(!file.isFile())
                return;
            //FileObserver Monitors files to fire events if file accessed or changed by any process
            mFirebaseDatabaseManager.GetUserGroups().addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean isGroup = false;
                    String group = null;
                    String path = file.getAbsolutePath();

                    for(DataSnapshot child : snapshot.getChildren()){
                        group = child.getValue().toString();
                        isGroup = path.endsWith(group);

                        if(path.contains(group)){
                            group = child.getKey();
                            break;
                        }
                    }

                    if(!isGroup) UploadFile(group, new File(path));
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        });

        // Start Monitoring the Directory
        mRecursiveFileObserver.startWatching();
    }


    public static FirebaseStorageManager getInstance(){
        return mFirebaseStorageManager;
    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static FirebaseStorageManager CreateInstance(File observed, int events){
        if(mFirebaseStorageManager == null)
            mFirebaseStorageManager  = new FirebaseStorageManager(observed, events);

        return mFirebaseStorageManager;
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
       /* fileReference.putFile(fileUri, metadata)
                .addOnSuccessListener(taskSnapshot -> {
                    // Wait for task to complete
                    mExecutorService.execute(() -> {
                        while(!(taskSnapshot.getTask().isComplete()&&taskSnapshot.getTask().isSuccessful()));
                        Log.d("output", Boolean.valueOf(taskSnapshot.getTask().isComplete() && taskSnapshot.getTask().isSuccessful()).toString());

                        mFirebaseDatabaseManager.AddFile(groupId, taskSnapshot.getMetadata());
                    });
                    mExecutorService.shutdown();
                    try {
                        mExecutorService.awaitTermination(1000, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                })
                .addOnFailureListener(e -> e.printStackTrace());*/
    }

    /**
     * Download new file from storage cloud
     *
     * @param groupId Group ID
     *  @param fileSnapshot
     */
    public void Download(Uri url, String groupPath){
        StorageReference storageReference = mStorage.getReference().child(url.toString());
        storageReference.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                storageReference.getFile(new File(groupPath, storageMetadata.getName()))
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                e.printStackTrace();
                            }
                        });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });



    }
}
