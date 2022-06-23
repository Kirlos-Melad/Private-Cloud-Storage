package com.example.privatecloudstorage.model;
// Android Libraries
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

// 3rd Party Libraries
import com.example.privatecloudstorage.interfaces.IAction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

// Java Libraries
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;


public class FirebaseStorageManager {
    // Used for debugging
    public static final String TAG = "FirebaseStorageManager";

    private FirebaseStorage mStorage;
    private static FirebaseStorageManager mFirebaseStorageManager;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private FirebaseStorageManager(){
        mStorage = FirebaseStorage.getInstance();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static FirebaseStorageManager getInstance(){
        if(mFirebaseStorageManager == null)
            mFirebaseStorageManager  = new FirebaseStorageManager();

        return mFirebaseStorageManager;
    }

    /**
     * Upload new file to cloud
     *
     * @param file File path
     * @param action action to be executed on success
     * @param executorService thread to run on
     */
    public void UploadGroupFile(String groupId, Uri file, int versionNumber, IAction action, ExecutorService executorService) {
        executorService.execute(() -> {
            String fileName = file.getLastPathSegment();
            StorageReference fileReference = mStorage.getReference().child(groupId).child(fileName).child(String.valueOf(versionNumber)).child(fileName);

            //Start uploading file
            fileReference.putFile(file)
                    .addOnSuccessListener(uploadTask -> {
                        executorService.execute(() -> {
                            uploadTask.getTask().addOnSuccessListener(finishedUploadTask -> {
                                action.onSuccess(finishedUploadTask.getMetadata());
                            });
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.d(TAG, "Upload: line 188");
                        e.printStackTrace();
                    });
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void UploadUserFile(Uri file, IAction action, ExecutorService executorService){
        executorService.execute(() -> {
            String fileName = file.getLastPathSegment();
            StorageReference fileReference = mStorage.getReference().child(ManagersMediator.getInstance().GetCurrentUser().getUid()).child(fileName);

            //Start uploading file
            fileReference.putFile(file)
                    .addOnSuccessListener(uploadTask -> {
                        executorService.execute(() -> {
                            uploadTask.getTask().addOnSuccessListener(finishedUploadTask -> {
                                // send url
                                action.onSuccess(finishedUploadTask.getMetadata().getPath());
                            });
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.d(TAG, "Upload: failed");
                        e.printStackTrace();
                    });
        });
    }

    /**
     * Download new file from storage cloud
     *
     * @param url Group ID
     * @param downloadFile File to be downloaded
     * @param action action to be executed on success
     * @param executorService thread to run on
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void DownloadGroupFile(Uri url, File downloadFile, IAction action, ExecutorService executorService) {
        executorService.execute(() -> {
            StorageReference storageReference = mStorage.getReference().child(url.toString());
            storageReference.getFile(downloadFile)
                    .addOnSuccessListener(taskSnapshot -> {
                        executorService.execute(() -> {
                            action.onSuccess(null);
                        });
                    })
                    .addOnFailureListener(e -> {

                        e.printStackTrace();
                    });
        });
    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void DownloadUserFile(File downloadFile, IAction action, ExecutorService executorService) {
        executorService.execute(() -> {
            StorageReference storageReference = mStorage.getReference().child(ManagersMediator.getInstance().GetCurrentUser().getUid())
                    .child(downloadFile.getName());
            storageReference.getFile(downloadFile)
                    .addOnSuccessListener(taskSnapshot -> {
                        executorService.execute(() -> {
                            action.onSuccess(null);
                        });
                    })
                    .addOnFailureListener(e -> {

                        e.printStackTrace();
                    });
        });
    }

    /**
     * Delete file from storage cloud
     *
     * @param groupId group id
     * @param fileName file name
     * @param action action to be executed on success
     * @param executorService thread to run on
     */
    public void DeleteGroupFile(Uri url, IAction action, ExecutorService executorService){
        executorService.execute(() -> {

            StorageReference fileReference = mStorage.getReference().child(url.toString());
            fileReference.delete().addOnSuccessListener(unused -> {
                executorService.execute(() -> {
                    action.onSuccess(null);
                });
            }).addOnFailureListener(e -> {

                e.printStackTrace();
            });
        });
    }
}
